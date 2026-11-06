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
package com.haembina.branchanalysis.ce.pullrequest.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestReviewCommentBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.mockito.invocation.InvocationOnMock;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.almclient.github.GithubClientFactory;
import com.haembina.branchanalysis.almclient.github.GithubConnection;
import com.haembina.branchanalysis.almclient.github.GithubGraphqlClient;
import com.haembina.branchanalysis.almclient.github.model.ReviewThread;
import com.haembina.branchanalysis.almclient.github.model.ReviewThreadComment;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.DecorationSettings;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;
import com.haembina.branchanalysis.ce.pullrequest.markup.Document;
import com.haembina.branchanalysis.ce.pullrequest.markup.Formatter;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisIssueSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

class GithubPullRequestDecoratorReviewCommentTest {

    private static final String APP_LOGIN = "sonarqube-app";
    private static final String HEAD_SHA = "head-sha";
    private static final String FILE_PATH = "src/File.java";

    private final ScmInfoRepository scmInfoRepository = mock();
    private final GithubClientFactory githubClientFactory = mock();
    private final ReportGenerator reportGenerator = mock();
    private final MarkdownFormatterFactory markdownFormatterFactory = mock();
    private final DecorationSettings decorationSettings = mock();
    private final GithubPullRequestDecorator underTest = new GithubPullRequestDecorator(scmInfoRepository, githubClientFactory,
        reportGenerator, markdownFormatterFactory, Clock.fixed(Instant.ofEpochSecond(1000), ZoneId.of("UTC")), decorationSettings);

    private final GithubGraphqlClient graphql = mock();
    private final GHRepository repository = mock();
    private final GHPullRequest pullRequest = mock();
    private final GHPullRequestReviewCommentBuilder reviewCommentBuilder = mock(InvocationOnMock::getMock);
    private final GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
    private final AnalysisDetails analysisDetails = mock();
    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();

    @BeforeEach
    void setUp() throws IOException {
        GitHub gitHub = mock();
        when(gitHub.getRepository("owner/repo")).thenReturn(repository);
        when(githubClientFactory.connect(any(), any())).thenReturn(new GithubConnection(gitHub, graphql, APP_LOGIN));
        when(decorationSettings.isGithubInlineCommentsEnabled()).thenReturn(true);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("owner/repo");

        when(analysisDetails.getPullRequestId()).thenReturn("12");
        when(analysisDetails.getCommitSha()).thenReturn(HEAD_SHA);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("project-key");
        when(analysisDetails.getAnalysisDate()).thenReturn(Instant.ofEpochSecond(500));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);

        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.format(any())).thenReturn("summary");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenAnswer(invocation -> {
            PostAnalysisIssueVisitor.ComponentIssue issue = invocation.getArgument(0);
            String body = issueBody(issue.getIssue().key());
            AnalysisIssueSummary issueSummary = mock();
            when(issueSummary.format(any())).thenReturn(body);
            return issueSummary;
        });

        when(repository.getPullRequest(12)).thenReturn(pullRequest);
        when(repository.createCheckRun(any(), any())).thenReturn(checkRunBuilder);
        doReturn(null).when(checkRunBuilder).create();
        when(pullRequest.getNumber()).thenReturn(12);
        when(pullRequest.getHtmlUrl()).thenReturn(URI.create("https://github.dummy/owner/repo/pull/12").toURL());
        when(pullRequest.createReviewComment()).thenReturn(reviewCommentBuilder);
        doReturn(null).when(reviewCommentBuilder).create();
        GHPullRequestCommitDetail commit = mock();
        when(commit.getSha()).thenReturn(HEAD_SHA);
        PagedIterable<GHPullRequestCommitDetail> commits = mock();
        when(commits.toList()).thenReturn(List.of(commit));
        when(pullRequest.listCommits()).thenReturn(commits);
        when(graphql.getReviewThreads("owner/repo", 12)).thenReturn(List.of());
    }

    @Test
    @DisplayName("capability github-inline-comments: see each new issue as a review comment on its line, resolved once fixed")
    void shouldPostAReviewCommentOnTheLineOfEachNewIssue() throws IOException {
        PostAnalysisIssueVisitor.ComponentIssue first = issue("issue-a", 3);
        PostAnalysisIssueVisitor.ComponentIssue second = issue("issue-b", 7);
        when(analysisDetails.getScmReportableIssues()).thenReturn(List.of(first, second));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(reviewCommentBuilder, times(2)).commitId(HEAD_SHA);
        verify(reviewCommentBuilder, times(2)).path(FILE_PATH);
        verify(reviewCommentBuilder).line(3);
        verify(reviewCommentBuilder).line(7);
        verify(reviewCommentBuilder).body(issueBody("issue-a"));
        verify(reviewCommentBuilder).body(issueBody("issue-b"));
        verify(reviewCommentBuilder, times(2)).create();
    }

    @Test
    void shouldNotRepeatACommentAndShouldResolveTheThreadOfAFixedIssue() throws IOException {
        PostAnalysisIssueVisitor.ComponentIssue stillOpen = issue("issue-a", 3);
        when(analysisDetails.getScmReportableIssues()).thenReturn(List.of(stillOpen));
        when(graphql.getReviewThreads("owner/repo", 12)).thenReturn(List.of(
            new ReviewThread("thread-a", false, List.of(comment("issue-a", APP_LOGIN + "[bot]"))),
            new ReviewThread("thread-b", false, List.of(comment("issue-b", APP_LOGIN))),
            new ReviewThread("thread-c", false, List.of(comment("issue-c", APP_LOGIN), new ReviewThreadComment("reply", "Done?", "reviewer"))),
            new ReviewThread("thread-d", false, List.of(comment("issue-d", "reviewer"))),
            new ReviewThread("thread-e", true, List.of(comment("issue-e", APP_LOGIN)))));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(pullRequest, never()).createReviewComment();
        verify(graphql).resolveReviewThread("thread-b");
        verify(graphql).replyToReviewThread(eq("thread-c"), anyString());
        verify(graphql, never()).resolveReviewThread("thread-a");
        verify(graphql, never()).resolveReviewThread("thread-c");
        verify(graphql, never()).resolveReviewThread("thread-d");
        verify(graphql, never()).resolveReviewThread("thread-e");
    }

    @Test
    void shouldLeaveDecorationUnchangedWhenInlineCommentsAreOff() throws IOException {
        when(decorationSettings.isGithubInlineCommentsEnabled()).thenReturn(false);
        PostAnalysisIssueVisitor.ComponentIssue newIssue = issue("issue-a", 3);
        when(analysisDetails.getScmReportableIssues()).thenReturn(List.of(newIssue));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verifyNoInteractions(graphql, scmInfoRepository);
        verify(pullRequest, never()).createReviewComment();
        verify(checkRunBuilder).create();
    }

    @Test
    void shouldDeleteAThreadHoldingAnOutdatedSummary() throws IOException {
        when(graphql.getReviewThreads("owner/repo", 12)).thenReturn(List.of(new ReviewThread("thread-s", false, List.of(
            new ReviewThreadComment("summary-1", "[View in SonarQube](https://sonar.dummy/dashboard?id=project-key&pullRequest=12)", APP_LOGIN),
            new ReviewThreadComment("summary-2", "outdated", APP_LOGIN)))));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(graphql).deleteReviewComment("summary-1");
        verify(graphql).deleteReviewComment("summary-2");
    }

    @Test
    void shouldCarryOnWhenGithubRefusesAReviewComment() throws IOException {
        PostAnalysisIssueVisitor.ComponentIssue outsideDiff = issue("issue-a", 3);
        when(analysisDetails.getScmReportableIssues()).thenReturn(List.of(outsideDiff));
        doThrow(new IOException("line is not part of the diff")).when(reviewCommentBuilder).create();

        assertThat(underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto).getPullRequestUrl())
            .contains("https://github.dummy/owner/repo/pull/12");
        verify(checkRunBuilder).create();
    }

    @Test
    void shouldReportReviewThreadsGithubWouldNotList() throws IOException {
        when(graphql.getReviewThreads(any(), anyInt())).thenThrow(new IOException("dummy"));

        assertDecorationFails("Could not retrieve review threads from Github");
    }

    @Test
    void shouldReportCommitsGithubWouldNotList() throws IOException {
        PagedIterable<GHPullRequestCommitDetail> commits = mock();
        when(commits.toList()).thenThrow(new IOException("dummy"));
        when(pullRequest.listCommits()).thenReturn(commits);

        assertDecorationFails("Could not retrieve commits for Pull Request from Github");
    }

    @Test
    void shouldReportAThreadGithubWouldNotResolve() throws IOException {
        when(graphql.getReviewThreads("owner/repo", 12)).thenReturn(List.of(new ReviewThread("thread-b", false, List.of(comment("issue-b", APP_LOGIN)))));
        doThrow(new IOException("dummy")).when(graphql).resolveReviewThread(any());

        assertDecorationFails("Could not resolve review thread on Github");
    }

    @Test
    void shouldReportAThreadGithubWouldNotTakeAReplyOn() throws IOException {
        when(graphql.getReviewThreads("owner/repo", 12)).thenReturn(List.of(new ReviewThread("thread-c", false, List.of(
            comment("issue-c", APP_LOGIN), new ReviewThreadComment("reply", "Done?", "reviewer")))));
        doThrow(new IOException("dummy")).when(graphql).replyToReviewThread(any(), any());

        assertDecorationFails("Could not reply to review thread on Github");
    }

    @Test
    void shouldReportAThreadGithubWouldNotDelete() throws IOException {
        when(graphql.getReviewThreads("owner/repo", 12)).thenReturn(List.of(new ReviewThread("thread-s", false, List.of(
            new ReviewThreadComment("summary-1", "[View in SonarQube](https://sonar.dummy/dashboard?id=project-key&pullRequest=12)", APP_LOGIN)))));
        doThrow(new IOException("dummy")).when(graphql).deleteReviewComment(any());

        assertDecorationFails("Could not delete review thread on Github");
    }

    @Test
    void shouldReportAPullRequestGithubWouldNotReturn() throws IOException {
        when(repository.getPullRequest(12)).thenThrow(new IOException("dummy"));

        assertDecorationFails("Could not retrieve Pull Request details from Github");
    }

    @Test
    void shouldReportACheckRunGithubRefused() throws IOException {
        when(decorationSettings.isGithubInlineCommentsEnabled()).thenReturn(false);
        doThrow(new IOException("dummy")).when(checkRunBuilder).create();

        assertDecorationFails("Could not create check run on Github");
    }

    @Test
    void shouldReportASummaryCommentGithubRefused() throws IOException {
        when(decorationSettings.isGithubInlineCommentsEnabled()).thenReturn(false);
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        Formatter<Document> documentFormatter = mock();
        when(documentFormatter.format(any())).thenReturn("**Project ID:** project-key");
        when(markdownFormatterFactory.documentFormatter()).thenReturn(documentFormatter);
        when(pullRequest.getComments()).thenThrow(new IOException("dummy"));

        assertDecorationFails("Could not submit summary comment to Github");
    }

    private void assertDecorationFails(String message) {
        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage(message)
            .hasCauseInstanceOf(IOException.class);
    }

    private PostAnalysisIssueVisitor.ComponentIssue issue(String key, int line) {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.key()).thenReturn(key);
        when(lightIssue.getLine()).thenReturn(line);
        when(lightIssue.getMessage()).thenReturn("message for " + key);
        when(lightIssue.impacts()).thenReturn(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM));
        Component component = mock();
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);
        when(componentIssue.getScmPath()).thenReturn(Optional.of(FILE_PATH));

        Changeset changeset = mock();
        when(changeset.getRevision()).thenReturn(HEAD_SHA);
        ScmInfo scmInfo = mock();
        when(scmInfo.hasChangesetForLine(line)).thenReturn(true);
        when(scmInfo.getChangesetForLine(line)).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));
        return componentIssue;
    }

    private static ReviewThreadComment comment(String issueKey, String login) {
        return new ReviewThreadComment("comment-" + issueKey, issueBody(issueKey), login);
    }

    private static String issueBody(String issueKey) {
        return "message for " + issueKey + "\n\n[View in SonarQube](https://sonar.dummy/project/issues?id=project-key&pullRequest=12&issues="
            + issueKey + "&open=" + issueKey + ")";
    }
}
