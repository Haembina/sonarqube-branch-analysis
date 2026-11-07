/*
 * Copyright (C) 2020-2025 Michael Clarke
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
package com.haembina.branchanalysis.ce.pullrequest.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.almclient.github.GithubClientFactory;
import com.haembina.branchanalysis.almclient.github.GithubConnection;
import com.haembina.branchanalysis.almclient.github.GithubGraphqlClient;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.DecorationResult;
import com.haembina.branchanalysis.ce.pullrequest.DecorationSettings;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;
import com.haembina.branchanalysis.ce.pullrequest.markup.Document;
import com.haembina.branchanalysis.ce.pullrequest.markup.Formatter;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

class GithubPullRequestDecoratorTest {

    private static final List<SoftwareQuality> QUALITIES = List.of(SoftwareQuality.MAINTAINABILITY, SoftwareQuality.RELIABILITY, SoftwareQuality.SECURITY);
    private static final List<Severity> SEVERITIES = List.of(Severity.INFO, Severity.LOW, Severity.MEDIUM, Severity.HIGH, Severity.BLOCKER);
    private static final String APP_SLUG = "sonarqube-app";
    private static final String APP_LOGIN = APP_SLUG + "[bot]";
    private static final String MARKED_SUMMARY = "old summary. **Project ID:** project-key\n";

    private final AnalysisDetails analysisDetails = mock();
    private final GithubClientFactory githubClientFactory = mock();
    private final ReportGenerator reportGenerator = mock();
    private final MarkdownFormatterFactory markdownFormatterFactory = mock();
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(102030405), ZoneId.of("UTC"));
    private final DecorationSettings decorationSettings = mock();
    private final GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(mock(ScmInfoRepository.class), githubClientFactory, reportGenerator, markdownFormatterFactory, clock, decorationSettings);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AlmSettingDto almSettingDto = mock();
    private final AnalysisSummary analysisSummary = mock();
    private final GitHub gitHub = mock();
    private final GHCheckRunBuilder stubbedCheckRunBuilder = mock(InvocationOnMock::getMock);

    @BeforeEach
    void setUp() throws IOException {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("alm-repo");
        when(projectAlmSettingDto.getMonorepo()).thenReturn(false);
        when(analysisDetails.getPullRequestId()).thenReturn("123");
        when(analysisDetails.getAnalysisDate()).thenReturn(clock.instant());
        when(analysisDetails.getAnalysisId()).thenReturn("analysis-id");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("project-key");
        when(analysisDetails.getAnalysisProjectName()).thenReturn("Project Name");
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getCommitSha()).thenReturn("commit-sha");
        List<PostAnalysisIssueVisitor.ComponentIssue> reportableIssues = IntStream.range(0, 20).mapToObj(i -> {
            PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
            Component component = mock();
            when(componentIssue.getScmPath()).thenReturn(Optional.of("path" + i));
            when(componentIssue.getComponent()).thenReturn(component);
            PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
            when(lightIssue.getMessage()).thenReturn("issue message " + i);
            when(lightIssue.getLine()).thenReturn(i);
            when(lightIssue.impacts()).thenReturn(Map.of(QUALITIES.get(i % QUALITIES.size()), severityOf(i)));
            when(componentIssue.getIssue()).thenReturn(lightIssue);
            return componentIssue;
        }).toList();
        when(analysisDetails.getScmReportableIssues()).thenReturn(reportableIssues);

        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        when(analysisSummary.getDashboardUrl()).thenReturn("dashboard-url");
        when(analysisSummary.format(any())).thenReturn("report summary");
        when(githubClientFactory.connect(any(), any())).thenReturn(new GithubConnection(gitHub, mock(GithubGraphqlClient.class), APP_SLUG));
        when(decorationSettings.isGithubAnnotationsEnabled()).thenReturn(true);
    }

    @Test
    void shouldReturnCorrectAlms() {
        assertThat(testCase.alm()).isEqualTo(List.of(ALM.GITHUB));
    }

    @Test
    void shouldThrowExceptionIfClientCreationFails() throws IOException {
        Exception dummyException = new IOException("Dummy Exception");
        when(githubClientFactory.connect(any(), any())).thenThrow(dummyException);

        assertThatThrownBy(() -> testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    void shouldDecoratePullRequestWithCorrectAnalysisAndSummaryCommentWhenEnabled() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        GHRepository repository = mock();
        when(gitHub.getRepository(any())).thenReturn(repository);
        GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
        doReturn(null).when(checkRunBuilder).create();
        when(repository.createCheckRun(any(), any())).thenReturn(checkRunBuilder);
        GHIssueComment comment1 = createComment("summary comment from current bot user, no project ID", APP_LOGIN);
        GHIssueComment comment2 = createComment("summary comment from non bot user with no project ID", "someone");
        GHIssueComment comment3 = createComment(MARKED_SUMMARY, APP_LOGIN);
        GHIssueComment comment4 = createComment(MARKED_SUMMARY, "other-app[bot]");
        GHIssueComment comment5 = createComment(MARKED_SUMMARY, "someone");
        GHPullRequest pullRequest = stubPullRequest(repository, List.of(comment1, comment2, comment3, comment4, comment5));
        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitHub).getRepository("alm-repo");
        verify(repository).createCheckRun("SonarQube Code Analysis", "commit-sha");

        ArgumentCaptor<GHCheckRunBuilder.Output> outputCaptor = ArgumentCaptor.captor();

        verify(checkRunBuilder).add(outputCaptor.capture());
        verify(checkRunBuilder).withExternalID("analysis-id");
        verify(checkRunBuilder).withStartedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withCompletedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withDetailsURL("dashboard-url");
        verify(checkRunBuilder).withStatus(GHCheckRun.Status.COMPLETED);
        verify(checkRunBuilder).withConclusion(GHCheckRun.Conclusion.SUCCESS);
        verify(checkRunBuilder).create();

        verifyNoMoreInteractions(checkRunBuilder);

        GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output("Quality Gate success", "report summary");
        for (int i = 0; i < 20; i++) {
            output.add(new GHCheckRunBuilder.Annotation(
                "path" + i,
                i,
                expectedLevel(severityOf(i)),
                "issue message " + i));
        }

        assertThat(outputCaptor.getValue()).usingRecursiveComparison().isEqualTo(output);

        DecorationResult expectedResult = DecorationResult.builder().withPullRequestUrl("http://url.of/pull/request").build();
        assertThat(decorationResult).usingRecursiveComparison().isEqualTo(expectedResult);

        verify(comment3).update("report summary");
        verify(comment3, never()).delete();
        for (GHIssueComment otherComment : List.of(comment1, comment2, comment4, comment5)) {
            verify(otherComment, never()).update(any());
            verify(otherComment, never()).delete();
        }

        verify(pullRequest, never()).comment(any(String.class));
        verify(pullRequest).getHtmlUrl();
        verify(pullRequest).getComments();
        verifyNoMoreInteractions(pullRequest, gitHub);
    }

    @Test
    void shouldPostSummaryCommentWhenNoneExists() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        GHRepository repository = stubRepository();
        GHIssueComment userComment = createComment(MARKED_SUMMARY, "someone");
        GHPullRequest pullRequest = stubPullRequest(repository, List.of(userComment));

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(pullRequest).comment("report summary");
        verify(userComment, never()).update(any());
    }

    @Test
    void shouldLeaveAnUnchangedSummaryAloneAndDeleteDuplicates() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        when(analysisSummary.format(any())).thenReturn(MARKED_SUMMARY);
        GHRepository repository = stubRepository();
        GHIssueComment summaryComment = createComment(MARKED_SUMMARY, APP_SLUG);
        GHIssueComment duplicate = createComment("older summary. **Project ID:** project-key\r", APP_LOGIN);
        GHPullRequest pullRequest = stubPullRequest(repository, List.of(summaryComment, duplicate));

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(summaryComment, never()).update(any());
        verify(summaryComment, never()).delete();
        verify(duplicate).delete();
        verify(pullRequest, never()).comment(any(String.class));
    }

    @Test
    void shouldLeaveAnnotationsOffTheCheckRunWhenDisabled() throws IOException {
        when(decorationSettings.isGithubAnnotationsEnabled()).thenReturn(false);
        GHRepository repository = stubRepository();
        stubPullRequest(repository, List.of());

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(repository).createCheckRun("SonarQube Code Analysis", "commit-sha");
        ArgumentCaptor<GHCheckRunBuilder.Output> outputCaptor = ArgumentCaptor.captor();
        verify(stubbedCheckRunBuilder).add(outputCaptor.capture());
        assertThat(outputCaptor.getValue()).usingRecursiveComparison()
            .isEqualTo(new GHCheckRunBuilder.Output("Quality Gate success", "report summary"));
    }

    @Test
    void shouldPostNoSummaryCommentOnACleanPassWhenSkipped() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        when(decorationSettings.isSummaryCommentSkippedWhenClean()).thenReturn(true);
        PostAnalysisIssueVisitor.ComponentIssue acceptedIssue = issueWithStatus(IssueStatus.ACCEPTED);
        when(analysisDetails.getIssues()).thenReturn(List.of(acceptedIssue));
        GHPullRequest pullRequest = stubPullRequest(stubRepository(), List.of());

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(pullRequest, never()).comment(any(String.class));
    }

    @Test
    void shouldStillUpdateAnExistingSummaryOnACleanPassWhenSkipped() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        when(decorationSettings.isSummaryCommentSkippedWhenClean()).thenReturn(true);
        GHIssueComment summaryComment = createComment(MARKED_SUMMARY, APP_LOGIN);
        GHPullRequest pullRequest = stubPullRequest(stubRepository(), List.of(summaryComment));

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(summaryComment).update("report summary");
        verify(pullRequest, never()).comment(any(String.class));
    }

    @Test
    void shouldPostTheSummaryWhenSkippedButTheGateFails() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        when(decorationSettings.isSummaryCommentSkippedWhenClean()).thenReturn(true);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        GHPullRequest pullRequest = stubPullRequest(stubRepository(), List.of());

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(pullRequest).comment("report summary");
    }

    @Test
    void shouldPostTheSummaryWhenSkippedButANewIssueIsOpen() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        when(decorationSettings.isSummaryCommentSkippedWhenClean()).thenReturn(true);
        PostAnalysisIssueVisitor.ComponentIssue openIssue = issueWithStatus(IssueStatus.OPEN);
        when(analysisDetails.getIssues()).thenReturn(List.of(openIssue));
        GHPullRequest pullRequest = stubPullRequest(stubRepository(), List.of());

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(pullRequest).comment("report summary");
    }

    @Test
    void shouldCreateACompletedCheckRunForAPassingBranch() throws IOException {
        when(reportGenerator.createBranchDashboardUrl(analysisDetails)).thenReturn("branch-dashboard-url");
        GHRepository repository = stubRepository();

        assertThat(testCase.decorateBranchQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto)).isTrue();

        verify(repository).createCheckRun("SonarQube Code Analysis", "commit-sha");
        ArgumentCaptor<GHCheckRunBuilder.Output> outputCaptor = ArgumentCaptor.captor();
        verify(stubbedCheckRunBuilder).add(outputCaptor.capture());
        verify(stubbedCheckRunBuilder).withStatus(GHCheckRun.Status.COMPLETED);
        verify(stubbedCheckRunBuilder).withConclusion(GHCheckRun.Conclusion.SUCCESS);
        verify(stubbedCheckRunBuilder).withDetailsURL("branch-dashboard-url");
        verify(stubbedCheckRunBuilder).create();
        assertThat(outputCaptor.getValue()).usingRecursiveComparison()
            .isEqualTo(new GHCheckRunBuilder.Output("Quality Gate success", "[View in SonarQube](branch-dashboard-url)"));
        verify(repository, never()).getPullRequest(anyInt());
    }

    @Test
    void shouldCreateAFailedCheckRunForAFailingBranch() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(projectAlmSettingDto.getMonorepo()).thenReturn(true);
        GHRepository repository = stubRepository();

        testCase.decorateBranchQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(repository).createCheckRun("[Project Name] SonarQube Code Analysis", "commit-sha");
        verify(stubbedCheckRunBuilder).withConclusion(GHCheckRun.Conclusion.FAILURE);
    }

    @Test
    void shouldReportABranchCheckRunGithubRefused() throws IOException {
        IOException refusal = new IOException("refused");
        when(githubClientFactory.connect(any(), any())).thenThrow(refusal);

        assertThatThrownBy(() -> testCase.decorateBranchQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Could not decorate branch on Github")
            .hasCause(refusal);
    }

    private static PostAnalysisIssueVisitor.ComponentIssue issueWithStatus(IssueStatus status) {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.issueStatus()).thenReturn(status);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        return componentIssue;
    }

    private GHRepository stubRepository() throws IOException {
        GHRepository repository = mock();
        when(gitHub.getRepository(any())).thenReturn(repository);
        doReturn(null).when(stubbedCheckRunBuilder).create();
        when(repository.createCheckRun(any(), any())).thenReturn(stubbedCheckRunBuilder);
        return repository;
    }

    private GHPullRequest stubPullRequest(GHRepository repository, List<GHIssueComment> comments) throws IOException {
        GHPullRequest pullRequest = mock();
        when(pullRequest.getComments()).thenReturn(comments);
        when(repository.getPullRequest(anyInt())).thenReturn(pullRequest);
        when(pullRequest.getHtmlUrl()).thenReturn(URI.create("http://url.of/pull/request").toURL());
        Formatter<Document> documentFormatter = mock();
        when(documentFormatter.format(any())).thenReturn("**Project ID:** project-key");
        when(markdownFormatterFactory.documentFormatter()).thenReturn(documentFormatter);
        return pullRequest;
    }


    @Test
    void shouldDecoratePullRequestWithCorrectAnalysisAndNoSummaryCommentWhenDisabled() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(false);
        GHRepository repository = mock();
        when(gitHub.getRepository(any())).thenReturn(repository);
        GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
        doReturn(null).when(checkRunBuilder).create();
        when(repository.createCheckRun(any(), any())).thenReturn(checkRunBuilder);
        GHPullRequest pullRequest = mock();
        when(repository.getPullRequest(anyInt())).thenReturn(pullRequest);
        when(pullRequest.getHtmlUrl()).thenReturn(URI.create("http://url.of/pull/request").toURL());
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);

        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitHub).getRepository("alm-repo");
        verify(repository).createCheckRun("SonarQube Code Analysis", "commit-sha");

        ArgumentCaptor<GHCheckRunBuilder.Output> outputCaptor = ArgumentCaptor.captor();

        verify(checkRunBuilder).add(outputCaptor.capture());
        verify(checkRunBuilder).withExternalID("analysis-id");
        verify(checkRunBuilder).withStartedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withCompletedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withDetailsURL("dashboard-url");
        verify(checkRunBuilder).withStatus(GHCheckRun.Status.COMPLETED);
        verify(checkRunBuilder).withConclusion(GHCheckRun.Conclusion.FAILURE);
        verify(checkRunBuilder).create();

        verifyNoMoreInteractions(checkRunBuilder);

        GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output("Quality Gate failed", "report summary");
        for (int i = 0; i < 20; i++) {
            output.add(new GHCheckRunBuilder.Annotation(
                "path" + i,
                i,
                expectedLevel(severityOf(i)),
                "issue message " + i));
        }

        assertThat(outputCaptor.getValue()).usingRecursiveComparison().isEqualTo(output);

        DecorationResult expectedResult = DecorationResult.builder().withPullRequestUrl("http://url.of/pull/request").build();
        assertThat(decorationResult).usingRecursiveComparison().isEqualTo(expectedResult);

        verifyNoMoreInteractions(gitHub);

        verify(pullRequest).getHtmlUrl();
        verifyNoMoreInteractions(pullRequest);
    }

    @Test
    void shouldPrefixCheckRunWithProjectNameInMonorepo() throws IOException {
        when(projectAlmSettingDto.getMonorepo()).thenReturn(true);
        GHRepository repository = mock();
        when(gitHub.getRepository(any())).thenReturn(repository);
        GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
        doReturn(null).when(checkRunBuilder).create();
        when(repository.createCheckRun(any(), any())).thenReturn(checkRunBuilder);
        GHPullRequest pullRequest = mock();
        when(repository.getPullRequest(anyInt())).thenReturn(pullRequest);
        when(pullRequest.getHtmlUrl()).thenReturn(URI.create("http://url.of/pull/request").toURL());

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(repository).createCheckRun("[Project Name] SonarQube Code Analysis", "commit-sha");
    }

    private static GHIssueComment createComment(String body, String login) throws IOException {
        GHIssueComment comment = mock();
        when(comment.getBody()).thenReturn(body);
        GHUser user = mock();
        when(user.getLogin()).thenReturn(login);
        when(comment.getUser()).thenReturn(user);
        return comment;
    }

    private static Severity severityOf(int issueIndex) {
        return SEVERITIES.get(issueIndex % SEVERITIES.size());
    }

    private static GHCheckRun.AnnotationLevel expectedLevel(Severity severity) {
        return switch (severity) {
            case INFO, LOW -> GHCheckRun.AnnotationLevel.NOTICE;
            case MEDIUM -> GHCheckRun.AnnotationLevel.WARNING;
            case HIGH, BLOCKER -> GHCheckRun.AnnotationLevel.FAILURE;
        };
    }
}
