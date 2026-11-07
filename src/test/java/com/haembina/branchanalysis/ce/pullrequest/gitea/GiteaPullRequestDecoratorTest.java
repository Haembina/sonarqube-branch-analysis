/*
 * Copyright (C) 2026 Haembina
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.haembina.branchanalysis.ce.pullrequest.gitea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

import com.haembina.branchanalysis.InvalidConfigurationException;
import com.haembina.branchanalysis.almclient.gitea.GiteaClient;
import com.haembina.branchanalysis.almclient.gitea.GiteaClientFactory;
import com.haembina.branchanalysis.almclient.gitea.GiteaSettings;
import com.haembina.branchanalysis.almclient.gitea.model.Comment;
import com.haembina.branchanalysis.almclient.gitea.model.CommitStatus;
import com.haembina.branchanalysis.almclient.gitea.model.PullRequest;
import com.haembina.branchanalysis.almclient.gitea.model.User;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.DecorationResult;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

class GiteaPullRequestDecoratorTest {

    private static final String REPOSITORY = "owner/repo";
    private static final String DASHBOARD_URL = "https://sonar.example.com/dashboard?id=project-key&pullRequest=12";
    private static final long BOT_ID = 7;

    private final MapSettings settings = new MapSettings();
    private final ConfigurationRepository configurationRepository = mock();
    private final GiteaClientFactory giteaClientFactory = mock();
    private final GiteaClient giteaClient = mock();
    private final ReportGenerator reportGenerator = mock();
    private final MarkdownFormatterFactory markdownFormatterFactory = new MarkdownFormatterFactory();
    private final AnalysisDetails analysisDetails = mock();

    private final GiteaPullRequestDecorator underTest = new GiteaPullRequestDecorator(configurationRepository,
            giteaClientFactory, reportGenerator, markdownFormatterFactory);

    @BeforeEach
    void setUp() throws IOException {
        when(configurationRepository.getConfiguration()).thenAnswer(invocation -> settings.asConfig());
        when(giteaClientFactory.createClient(any())).thenReturn(giteaClient);
        when(analysisDetails.getPullRequestId()).thenReturn("12");
        when(analysisDetails.getCommitSha()).thenReturn("abc123");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("project-key");
        when(giteaClient.getCurrentUser()).thenReturn(new User(BOT_ID, "sonarqube"));
        when(giteaClient.getPullRequest(REPOSITORY, 12)).thenReturn(new PullRequest(12, "https://gitea.example.com/owner/repo/pulls/12"));
    }

    @Test
    void isConfiguredOnlyWhenTheProjectNamesARepository() {
        assertThat(underTest.isConfigured()).isFalse();

        settings.setProperty(GiteaSettings.REPOSITORY, " ");
        assertThat(underTest.isConfigured()).isFalse();

        settings.setProperty(GiteaSettings.REPOSITORY, REPOSITORY);
        assertThat(underTest.isConfigured()).isTrue();
    }

    @Test
    void decoratesAPassingPullRequestWithASuccessStatusAndANewSummaryComment() throws IOException {
        settings.setProperty(GiteaSettings.REPOSITORY, REPOSITORY);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        AnalysisSummary analysisSummary = summary("project-key");
        when(reportGenerator.createAnalysisSummary(analysisDetails)).thenReturn(analysisSummary);
        when(giteaClient.getComments(REPOSITORY, 12)).thenReturn(List.of(
                new Comment(1, analysisSummary.format(markdownFormatterFactory), new User(BOT_ID + 1, "someone")),
                new Comment(2, "unrelated", new User(BOT_ID, "sonarqube")),
                new Comment(3, summary("project-key-2").format(markdownFormatterFactory), new User(BOT_ID, "sonarqube"))));

        DecorationResult result = underTest.decorateQualityGateStatus(analysisDetails);

        verify(giteaClient).setCommitStatus(REPOSITORY, "abc123", new CommitStatus(CommitStatus.State.SUCCESS,
                DASHBOARD_URL, "Quality Gate passed", GiteaPullRequestDecorator.STATUS_CONTEXT));
        verify(giteaClient).createComment(REPOSITORY, 12, analysisSummary.format(markdownFormatterFactory));
        verify(giteaClient, never()).editComment(anyString(), anyLong(), anyString());
        assertThat(result.getPullRequestUrl()).contains("https://gitea.example.com/owner/repo/pulls/12");
    }

    @Test
    void decoratesAFailingPullRequestWithAFailureStatusAndEditsTheSummaryComment() throws IOException {
        settings.setProperty(GiteaSettings.REPOSITORY, REPOSITORY);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        AnalysisSummary analysisSummary = summary("project-key");
        when(reportGenerator.createAnalysisSummary(analysisDetails)).thenReturn(analysisSummary);
        String newSummary = analysisSummary.format(markdownFormatterFactory);
        when(giteaClient.getComments(REPOSITORY, 12)).thenReturn(List.of(
                new Comment(4, newSummary.replace("Coverage", "Old coverage").replace("\n", "\r\n"), new User(BOT_ID, "sonarqube"))));

        underTest.decorateQualityGateStatus(analysisDetails);

        verify(giteaClient).setCommitStatus(REPOSITORY, "abc123", new CommitStatus(CommitStatus.State.FAILURE,
                DASHBOARD_URL, "Quality Gate failed", GiteaPullRequestDecorator.STATUS_CONTEXT));
        verify(giteaClient).editComment(REPOSITORY, 4, newSummary);
        verify(giteaClient, never()).createComment(anyString(), anyLong(), anyString());
    }

    @Test
    void refusesARepositoryThatIsNotOwnerAndName() {
        settings.setProperty(GiteaSettings.REPOSITORY, "https://gitea.example.com/owner/repo");

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails))
                .isInstanceOfSatisfying(InvalidConfigurationException.class,
                        ex -> assertThat(ex.getScope()).isEqualTo(InvalidConfigurationException.Scope.PROJECT))
                .hasMessage("Gitea repository must be set as owner/name");
        verify(giteaClientFactory, never()).createClient(any());
    }

    @Test
    void reportsAFailedCallToGitea() throws IOException {
        settings.setProperty(GiteaSettings.REPOSITORY, REPOSITORY);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        AnalysisSummary analysisSummary = summary("project-key");
        when(reportGenerator.createAnalysisSummary(analysisDetails)).thenReturn(analysisSummary);
        IOException failure = new IOException("connection refused");
        when(giteaClient.getCurrentUser()).thenThrow(failure);

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not decorate Pull Request on Gitea")
                .hasCause(failure);
    }

    private static AnalysisSummary summary(String projectKey) {
        return AnalysisSummary.builder()
                .withProjectKey(projectKey)
                .withDashboardUrl(DASHBOARD_URL)
                .withStatusDescription("Failed")
                .withStatusImageUrl("https://sonar.example.com/failed.png")
                .withFailedQualityGateConditions(List.of("Coverage below 80%"))
                .build();
    }
}
