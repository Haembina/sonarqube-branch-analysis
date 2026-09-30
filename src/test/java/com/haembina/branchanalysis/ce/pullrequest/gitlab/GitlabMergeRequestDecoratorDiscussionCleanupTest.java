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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.haembina.branchanalysis.almclient.gitlab.model.CommitNote;
import com.haembina.branchanalysis.almclient.gitlab.model.Discussion;
import com.haembina.branchanalysis.almclient.gitlab.model.MergeRequestNote;
import com.haembina.branchanalysis.almclient.gitlab.model.Note;
import com.haembina.branchanalysis.almclient.gitlab.model.User;

/**
 * How the decorator closes, keeps or comments on discussions for issues that no longer exist.
 */
class GitlabMergeRequestDecoratorDiscussionCleanupTest extends GitlabMergeRequestDecoratorFixture {

    @Test
    void shouldCloseDiscussionWithSingleResolvableNoteFromSonarqubeUserButNoIssueIdInBody() throws IOException {
        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Post with no issue ID");
        when(note.isResolvable()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);    }

    @Test
    void shouldNotCloseDiscussionWithSingleNonResolvableNoteFromSonarqubeUserButNoIssueIdInBody() throws IOException {
        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Post with no issue ID");
        when(note.isResolvable()).thenReturn(false);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
    }

    @Test
    void shouldNotCloseDiscussionWithMultipleResolvableNotesFromSonarqubeUserButNoId() throws IOException {
        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Another post with no issue ID\nbut containing a new line");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock();
        when(note2.getAuthor()).thenReturn(sonarqubeUser);
        when(note2.getBody()).thenReturn("Additional post from user");
        when(note2.isResolvable()).thenReturn(true);


        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId2");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }

    @Test
    void shouldCloseDiscussionWithResolvableNoteFromSonarqubeUserAndOnlySystemNoteFromOtherUser() throws IOException {
        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("[View in SonarQube](http://host.domain/issue?issues=issueId&id=" + PROJECT_KEY + ")");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock();
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("System post on behalf of user");
        when(note2.isSystem()).thenReturn(true);


        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId2");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<String> discussionIdArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).resolveMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), discussionIdArgumentCaptor.capture());

        assertThat(discussionIdArgumentCaptor.getValue()).isEqualTo("discussionId2");
    }

    @Test
    void shouldNotAttemptCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndAnotherUserWithNoId() throws IOException {
        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Yet another post with no issue ID");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock();
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Post from another user");
        when(note2.isResolvable()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId3");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }

    @Test
    void shouldNotCommentOrAttemptCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndACloseMessageWithNoId() throws IOException {
        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("And another post with no issue ID\nNo View in SonarQube link");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock();
        when(note2.getAuthor()).thenReturn(sonarqubeUser);
        when(note2.getBody()).thenReturn("dummy");
        when(note2.isResolvable()).thenReturn(true);

        Note note3 = mock();
        when(note3.getAuthor()).thenReturn(sonarqubeUser);
        when(note3.getBody()).thenReturn("other comment");
        when(note3.isResolvable()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId4");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2, note3));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
    }

    @Test
    void shouldCommentAboutCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndAnotherUserWithIssuedId() throws IOException {
        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Sonarqube reported issue\n[View in SonarQube](https://dummy.url.with.subdomain/path/to/sonarqube?paramters=many&values=complex%20and+encoded&issues=new-issue&id=" + PROJECT_KEY + ")");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock();
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Message from another user");
        when(note2.isResolvable()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId5");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());

        ArgumentCaptor<String> discussionIdArgumentCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<String> noteContentArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussionNote(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), discussionIdArgumentCaptor.capture(), noteContentArgumentCaptor.capture());

        assertThat(discussionIdArgumentCaptor.getValue()).isEqualTo("discussionId5");
        assertThat(noteContentArgumentCaptor.getValue()).isEqualTo(OLD_SONARQUBE_ISSUE_COMMENT);
    }

    @Test
    void shouldThrowErrorIfUnableToCleanUpDiscussionOnGitlab() throws IOException {
        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Sonarqube reported issue\n[View in SonarQube](https://dummy.url.with.subdomain/path/to/sonarqube?paramters=many&values=complex%20and+encoded&issues=issuedId&id=" + PROJECT_KEY + ")");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock();
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Message from another user");
        when(note2.isResolvable()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId5");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));
        doThrow(new IOException("dummy")).when(gitlabClient).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not add note to Merge Request discussion");
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());

        ArgumentCaptor<String> discussionIdArgumentCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<String> noteContentArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussionNote(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), discussionIdArgumentCaptor.capture(), noteContentArgumentCaptor.capture());

        assertThat(discussionIdArgumentCaptor.getValue()).isEqualTo("discussionId5");
        assertThat(noteContentArgumentCaptor.getValue()).isEqualTo(OLD_SONARQUBE_ISSUE_COMMENT);
    }

    @Test
    void shouldNotCommentOrAttemptCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndACloseMessageWithIssueId() throws IOException {
        Note note = mock();
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("And another post with an issue ID\n[View in SonarQube](url)");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock();
        when(note2.getAuthor()).thenReturn(sonarqubeUser);
        when(note2.getBody()).thenReturn(OLD_SONARQUBE_ISSUE_COMMENT);
        when(note2.isResolvable()).thenReturn(true);

        Note note3 = mock();
        when(note3.getAuthor()).thenReturn(sonarqubeUser);
        when(note3.getBody()).thenReturn("Some additional comment");
        when(note3.isResolvable()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId6");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2, note3));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
    }
}
