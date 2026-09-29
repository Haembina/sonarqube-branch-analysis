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
package com.haembina.branchanalysis.ce.pullrequest.gitlab;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.haembina.branchanalysis.almclient.gitlab.GitlabClient;
import com.haembina.branchanalysis.almclient.gitlab.model.Discussion;
import com.haembina.branchanalysis.almclient.gitlab.model.MergeRequest;
import com.haembina.branchanalysis.almclient.gitlab.model.Note;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisIssueSummary;

/** An issue the decorator cannot place on a line, and the discussion calls GitLab fails. */
class GitlabMergeRequestDecoratorFailureTest {

    private static final long PROJECT_ID = 101;
    private static final long MERGE_REQUEST_IID = 123;

    private final GitlabClient client = mock();
    private final GitlabMergeRequestDecorator underTest = new GitlabMergeRequestDecorator(mock(), mock(), mock(), mock());
    private final MergeRequest mergeRequest = mock();
    private final Discussion discussion = mock();

    GitlabMergeRequestDecoratorFailureTest() {
        when(mergeRequest.getTargetProjectId()).thenReturn(PROJECT_ID);
        when(mergeRequest.getIid()).thenReturn(MERGE_REQUEST_IID);
        when(discussion.getId()).thenReturn("discussion-id");
    }

    @Test
    void shouldRefuseInlineNoteForIssueWithoutLine() {
        PostAnalysisIssueVisitor.LightIssue issue = mock();
        when(issue.getLine()).thenReturn(null);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(issue);
        AnalysisDetails analysis = mock();
        AnalysisIssueSummary summary = mock();

        assertThatThrownBy(() -> underTest.submitCommitNoteForIssue(client, mergeRequest, componentIssue, "src/A.java", analysis, summary))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("No line is associated with this issue");
        verifyNoInteractions(client);
    }

    @Test
    void shouldResolveDiscussion() throws IOException {
        underTest.resolveDiscussion(client, discussion, mergeRequest);

        verify(client).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, "discussion-id");
    }

    @Test
    void shouldReportFailedDiscussionResolution() throws IOException {
        doThrow(new IOException("down")).when(client).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, "discussion-id");

        assertThatThrownBy(() -> underTest.resolveDiscussion(client, discussion, mergeRequest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not resolve Merge Request discussion")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void shouldReportFailedDiscussionDeletion() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(9L);
        doThrow(new IOException("down")).when(client).deleteMergeRequestDiscussionNote(PROJECT_ID, MERGE_REQUEST_IID, "discussion-id", 9L);
        List<Note> notes = List.of(note);

        assertThatThrownBy(() -> underTest.deleteDiscussion(client, discussion, mergeRequest, notes))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not delete Merge Request discussion");
    }
}
