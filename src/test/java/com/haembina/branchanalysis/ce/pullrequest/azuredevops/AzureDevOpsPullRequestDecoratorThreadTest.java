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
package com.haembina.branchanalysis.ce.pullrequest.azuredevops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

import com.haembina.branchanalysis.almclient.azuredevops.AzureDevopsClient;
import com.haembina.branchanalysis.almclient.azuredevops.model.Comment;
import com.haembina.branchanalysis.almclient.azuredevops.model.CommentThread;
import com.haembina.branchanalysis.almclient.azuredevops.model.CreateCommentThreadRequest;
import com.haembina.branchanalysis.almclient.azuredevops.model.Project;
import com.haembina.branchanalysis.almclient.azuredevops.model.PullRequest;
import com.haembina.branchanalysis.almclient.azuredevops.model.Repository;
import com.haembina.branchanalysis.almclient.azuredevops.model.enums.CommentThreadStatus;
import com.haembina.branchanalysis.almclient.azuredevops.model.enums.CommentType;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisIssueSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;

/** The thread and comment calls the decorator makes on a pull request, and how each one reports a failed call. */
class AzureDevOpsPullRequestDecoratorThreadTest {

    private static final String PROJECT = "azure-project";
    private static final String REPOSITORY = "azure-repo";
    private static final int PULL_REQUEST_ID = 321;
    private static final int THREAD_ID = 12;

    private final AzureDevopsClient client = mock();
    private final MarkdownFormatterFactory markdownFormatterFactory = mock();
    private final AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(mock(), mock(), mock(), markdownFormatterFactory);
    private final PullRequest pullRequest = pullRequest();
    private final CommentThread thread = thread();

    private static PullRequest pullRequest() {
        Project project = mock();
        when(project.getName()).thenReturn(PROJECT);
        Repository repository = mock();
        when(repository.getProject()).thenReturn(project);
        when(repository.getName()).thenReturn(REPOSITORY);
        PullRequest pullRequest = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(pullRequest.getId()).thenReturn(PULL_REQUEST_ID);
        return pullRequest;
    }

    private static CommentThread thread() {
        CommentThread thread = mock();
        when(thread.getId()).thenReturn(THREAD_ID);
        return thread;
    }

    private static PostAnalysisIssueVisitor.ComponentIssue issueSpanning(int startLine, int startOffset, int endLine, int endOffset) {
        DbIssues.Locations locations = DbIssues.Locations.newBuilder()
                .setTextRange(DbCommons.TextRange.newBuilder()
                        .setStartLine(startLine).setStartOffset(startOffset)
                        .setEndLine(endLine).setEndOffset(endOffset))
                .build();
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.getLocations()).thenReturn(locations);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        return componentIssue;
    }

    @ParameterizedTest
    @ValueSource(strings = {"src/Main.java", "/src/Main.java"})
    void shouldCreateActiveThreadOnIssueRangeWithOneBasedOffsetsAndRootedPath(String filePath) throws IOException {
        AnalysisIssueSummary summary = mock();
        when(summary.format(markdownFormatterFactory)).thenReturn("issue note");

        underTest.submitCommitNoteForIssue(client, pullRequest, issueSpanning(4, 0, 6, 9), filePath, mock(), summary);

        ArgumentCaptor<CreateCommentThreadRequest> request = ArgumentCaptor.captor();
        verify(client).createThread(eq(PROJECT), eq(REPOSITORY), eq(PULL_REQUEST_ID), request.capture());
        CreateCommentThreadRequest sent = request.getValue();
        assertThat(sent.getStatus()).isEqualTo(CommentThreadStatus.ACTIVE);
        assertThat(sent.getComments()).singleElement().extracting("content").isEqualTo("issue note");
        assertThat(sent.getThreadContext().getFilePath()).isEqualTo("/src/Main.java");
        assertThat(sent.getThreadContext().getRightFileStart()).extracting("line", "offset").containsExactly(4, 1);
        assertThat(sent.getThreadContext().getRightFileEnd()).extracting("line", "offset").containsExactly(6, 10);
    }

    @Test
    void shouldReportFailedIssueThread() throws IOException {
        AnalysisIssueSummary summary = mock();
        when(summary.format(markdownFormatterFactory)).thenReturn("issue note");
        when(client.createThread(anyString(), anyString(), anyInt(), any())).thenThrow(new IOException("down"));
        PostAnalysisIssueVisitor.ComponentIssue issue = issueSpanning(1, 0, 1, 1);
        AnalysisDetails analysis = mock();

        assertThatThrownBy(() -> underTest.submitCommitNoteForIssue(client, pullRequest, issue, "a.java", analysis, summary))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit commit comment to Azure Devops")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void shouldLeaveSummaryThreadOpenWhenQualityGateFails() throws IOException {
        AnalysisDetails analysis = mock();
        when(analysis.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        AnalysisSummary summary = mock();
        when(client.createThread(anyString(), anyString(), anyInt(), any())).thenReturn(thread);

        underTest.submitSummaryNote(client, pullRequest, analysis, summary);

        verify(client, never()).resolvePullRequestThread(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void shouldReportFailedSummaryThread() throws IOException {
        AnalysisSummary summary = mock();
        AnalysisDetails analysis = mock();
        when(client.createThread(anyString(), anyString(), anyInt(), any())).thenThrow(new IOException("down"));

        assertThatThrownBy(() -> underTest.submitSummaryNote(client, pullRequest, analysis, summary))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit summary comment to Azure Devops");
    }

    @Test
    void shouldReportFailedPullRequestRetrieval() throws IOException {
        AlmSettingDto almSetting = mock();
        ProjectAlmSettingDto projectAlmSetting = mock();
        when(projectAlmSetting.getAlmSlug()).thenReturn(PROJECT);
        when(projectAlmSetting.getAlmRepo()).thenReturn(REPOSITORY);
        AnalysisDetails analysis = mock();
        when(analysis.getPullRequestId()).thenReturn("321");
        when(client.retrievePullRequest(PROJECT, REPOSITORY, PULL_REQUEST_ID)).thenThrow(new IOException("down"));

        assertThatThrownBy(() -> underTest.getPullRequest(client, almSetting, projectAlmSetting, analysis))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve Pull Request details");
    }

    @Test
    void shouldReportFailedCommitRetrieval() throws IOException {
        when(client.getPullRequestCommits(PROJECT, REPOSITORY, PULL_REQUEST_ID)).thenThrow(new IOException("down"));

        assertThatThrownBy(() -> underTest.getCommitIdsForPullRequest(client, pullRequest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve commit details for Pull Request");
    }

    @Test
    void shouldReportFailedDiscussionRetrieval() throws IOException {
        when(client.retrieveThreads(PROJECT, REPOSITORY, PULL_REQUEST_ID)).thenThrow(new IOException("down"));

        assertThatThrownBy(() -> underTest.getDiscussions(client, pullRequest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve discussions from Azure Devops");
    }

    @Test
    void shouldReportFailedNoteOnDiscussion() throws IOException {
        doThrow(new IOException("down")).when(client).addCommentToThread(eq(PROJECT), eq(REPOSITORY), eq(PULL_REQUEST_ID), eq(THREAD_ID), any());

        assertThatThrownBy(() -> underTest.addNoteToDiscussion(client, thread, pullRequest, "note"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not add note to Pull Request comment thread on Azure Devops");
    }

    @Test
    void shouldResolveDiscussionThread() throws IOException {
        underTest.resolveDiscussion(client, thread, pullRequest);

        verify(client).resolvePullRequestThread(PROJECT, REPOSITORY, PULL_REQUEST_ID, THREAD_ID);
    }

    @Test
    void shouldReportFailedDiscussionResolution() throws IOException {
        doThrow(new IOException("down")).when(client).resolvePullRequestThread(PROJECT, REPOSITORY, PULL_REQUEST_ID, THREAD_ID);

        assertThatThrownBy(() -> underTest.resolveDiscussion(client, thread, pullRequest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not resolve Pull Request comment thread on Azure Devops");
    }

    @Test
    void shouldReportFailedDiscussionDeletion() throws IOException {
        Comment note = mock();
        when(note.getId()).thenReturn(7);
        doThrow(new IOException("down")).when(client).deletePullRequestThreadComment(PROJECT, REPOSITORY, PULL_REQUEST_ID, THREAD_ID, 7);
        List<Comment> notes = List.of(note);

        assertThatThrownBy(() -> underTest.deleteDiscussion(client, thread, pullRequest, notes))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not delete Pull Request comment thread on Azure Devops");
    }

    @Test
    void shouldTreatDeletedOrClosedThreadAsClosed() {
        CommentThread deleted = mock();
        when(deleted.isDeleted()).thenReturn(true);
        CommentThread closed = mock();
        when(closed.getStatus()).thenReturn(CommentThreadStatus.CLOSED);
        CommentThread active = mock();
        when(active.getStatus()).thenReturn(CommentThreadStatus.ACTIVE);

        assertThat(underTest.isClosed(deleted, List.of())).isTrue();
        assertThat(underTest.isClosed(closed, List.of())).isTrue();
        assertThat(underTest.isClosed(active, List.of())).isFalse();
    }

    @Test
    void shouldTreatOnlyTextCommentsAsUserNotes() {
        Comment text = mock();
        when(text.getCommentType()).thenReturn(CommentType.TEXT);
        Comment codeChange = mock();
        when(codeChange.getCommentType()).thenReturn(CommentType.CODECHANGE);

        assertThat(underTest.isUserNote(text)).isTrue();
        assertThat(underTest.isUserNote(codeChange)).isFalse();
    }

    @Test
    void shouldReadIssueFromLegacySeeInSonarQubeLink() {
        Comment note = mock();
        when(note.getContent()).thenReturn("[See in SonarQube](http://host/project/issues?id=projectKey&issues=AXyz)");

        assertThat(underTest.parseIssueDetails(client, note))
                .get().extracting("projectKey", "issueKey").containsExactly("projectKey", "AXyz");
    }

    @Test
    void shouldFindNoIssueInNoteWithoutContent() {
        Comment note = mock();

        assertThat(underTest.parseIssueDetails(client, note)).isEmpty();
    }

    @Test
    void shouldPreferViewInSonarQubeLinkOverLegacyLink() {
        Comment note = mock();
        when(note.getContent()).thenReturn("[View in SonarQube](http://host/project/issues?id=projectKey&issues=current)"
                + System.lineSeparator() + "[See in SonarQube](http://host/project/issues?id=projectKey&issues=legacy)");

        assertThat(underTest.parseIssueDetails(client, note))
                .get().extracting("issueKey").isEqualTo("current");
    }
}
