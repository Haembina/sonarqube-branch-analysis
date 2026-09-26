/*
 * Copyright (C) 2021-2026 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;

import com.haembina.branchanalysis.almclient.gitlab.model.CommitNote;
import com.haembina.branchanalysis.almclient.gitlab.model.Discussion;
import com.haembina.branchanalysis.almclient.gitlab.model.MergeRequestNote;
import com.haembina.branchanalysis.almclient.gitlab.model.Note;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;

/**
 * How the decorator starts a discussion for each new issue on a commit in the merge request.
 */
class GitlabMergeRequestDecoratorIssueCommentTest extends GitlabMergeRequestDecoratorFixture {

    @Test
    void shouldThrowErrorIfSubmittingNewIssueToGitlabFails() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(lightIssue.getLine()).thenReturn(999);

        Component component = mock();

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);
        when(componentIssue.getScmPath()).thenReturn(Optional.of("path-to-file"));

        when(analysisDetails.getScmReportableIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(new ArrayList<>());

        Changeset changeset = mock();
        when(changeset.getRevision()).thenReturn("DEF");

        ScmInfo scmInfo = mock();
        when(scmInfo.hasChangesetForLine(999)).thenReturn(true);
        when(scmInfo.getChangesetForLine(999)).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));

        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit commit comment to Gitlab");

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new CommitNote("Issue Summary", BASE_SHA, START_SHA, HEAD_SHA, "path-to-file", "path-to-file", 999));
    }

    @Test
    void shouldStartNewDiscussionForNewIssueFromCommitInMergeRequest() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(lightIssue.getLine()).thenReturn(999);

        Component component = mock();

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);
        when(componentIssue.getScmPath()).thenReturn(Optional.of("path-to-file"));

        when(analysisDetails.getScmReportableIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(new ArrayList<>());

        Changeset changeset = mock();
        when(changeset.getRevision()).thenReturn("DEF");

        ScmInfo scmInfo = mock();
        when(scmInfo.hasChangesetForLine(999)).thenReturn(true);
        when(scmInfo.getChangesetForLine(999)).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient, times(2)).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getAllValues().get(0))
                .usingRecursiveComparison()
                .isEqualTo(new CommitNote("Issue Summary", BASE_SHA, START_SHA, HEAD_SHA, "path-to-file", "path-to-file", 999));
        assertThat(mergeRequestNoteArgumentCaptor.getAllValues().get(1)).isNotInstanceOf(CommitNote.class);
    }

    @Test
    void shouldNotStartNewDiscussionForIssueWithExistingCommentFromCommitInMergeRequest() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(lightIssue.getLine()).thenReturn(999);

        Component component = mock();

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);
        when(componentIssue.getScmPath()).thenReturn(Optional.of("path-to-file"));

        Note note = mock();
        when(note.getBody()).thenReturn("Reported issue\n[View in SonarQube](http://domain.url/sonar/issue?issues=issueKey1&id=" + PROJECT_KEY + ")");
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.isResolvable()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussion-id");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));
        when(analysisDetails.getScmReportableIssues()).thenReturn(Collections.singletonList(componentIssue));

        Changeset changeset = mock();
        when(changeset.getRevision()).thenReturn("DEF");

        ScmInfo scmInfo = mock();
        when(scmInfo.hasChangesetForLine(999)).thenReturn(true);
        when(scmInfo.getChangesetForLine(999)).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }

    @Test
    void shouldNotCreateCommentsForIssuesWithNoLineNumbers() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(lightIssue.getLine()).thenReturn(null);

        Component component = mock();

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        when(analysisDetails.getScmReportableIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(new ArrayList<>());

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
        verify(scmInfoRepository, never()).getScmInfo(any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }
}
