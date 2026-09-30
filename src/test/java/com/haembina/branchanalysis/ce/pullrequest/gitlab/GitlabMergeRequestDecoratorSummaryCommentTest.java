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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.haembina.branchanalysis.almclient.gitlab.model.Discussion;
import com.haembina.branchanalysis.almclient.gitlab.model.Note;
import com.haembina.branchanalysis.almclient.gitlab.model.User;

/**
 * How the decorator deletes or annotates an outdated summary comment's discussion.
 */
class GitlabMergeRequestDecoratorSummaryCommentTest extends GitlabMergeRequestDecoratorFixture {

    @Test
    void shouldDeleteSummaryCommentIfNoOtherCommentsInDiscussion() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(101L);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(note.isSystem()).thenReturn(false);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient).deleteMergeRequestDiscussionNote(PROJECT_ID, MERGE_REQUEST_IID, "discussionId", 101);
        verify(gitlabClient).getMergeRequestDiscussions(PROJECT_ID, MERGE_REQUEST_IID);
    }

    @Test
    void shouldAddNoteToSummaryCommentThreadIfOtherCommentsInDiscussion() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(101L);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(note.isSystem()).thenReturn(false);

        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("username");
        Note note2 = mock();
        when(note2.getId()).thenReturn(102L);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Another comment");
        when(note2.isSystem()).thenReturn(false);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(List.of(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient).addMergeRequestDiscussionNote(PROJECT_ID, MERGE_REQUEST_IID, "discussionId", "This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being removed. Please manually resolve this discussion once the other comments have been reviewed.");
        verify(gitlabClient, never()).deleteMergeRequestDiscussionNote(anyLong(), anyLong(), any(), anyLong());
        verify(gitlabClient).getMergeRequestDiscussions(PROJECT_ID, MERGE_REQUEST_IID);
    }

    @Test
    void shouldNotAddNoteToSummaryCommentThreadIfOtherCommentsInDiscussionAndNoteAlreadyPresent() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(101L);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(note.isSystem()).thenReturn(false);

        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("username");
        Note note2 = mock();
        when(note2.getId()).thenReturn(102L);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Another comment");
        when(note2.isSystem()).thenReturn(false);

        Note note3 = mock();
        when(note3.getId()).thenReturn(102L);
        when(note3.getAuthor()).thenReturn(sonarqubeUser);
        when(note3.getBody()).thenReturn("This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being removed. Please manually resolve this discussion once the other comments have been reviewed.");
        when(note3.isSystem()).thenReturn(false);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(List.of(note, note2, note3));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
        verify(gitlabClient, never()).deleteMergeRequestDiscussionNote(anyLong(), anyLong(), any(), anyLong());
        verify(gitlabClient).getMergeRequestDiscussions(PROJECT_ID, MERGE_REQUEST_IID);
    }

    @Test
    void shouldDeleteResolvedSummaryCommentIfNoOtherCommentsInDiscussion() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(101L);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(note.isSystem()).thenReturn(false);
        when(note.isResolved()).thenReturn(true);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient).deleteMergeRequestDiscussionNote(PROJECT_ID, MERGE_REQUEST_IID, "discussionId", 101);
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
    }

    @Test
    void shouldAddNoteToResolvedSummaryCommentThreadIfOtherCommentsInDiscussion() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(101L);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(note.isSystem()).thenReturn(false);
        when(note.isResolved()).thenReturn(true);

        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("username");
        Note note2 = mock();
        when(note2.getId()).thenReturn(102L);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Another comment");
        when(note2.isSystem()).thenReturn(false);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(List.of(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient).addMergeRequestDiscussionNote(PROJECT_ID, MERGE_REQUEST_IID, "discussionId", "This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being removed. Please manually resolve this discussion once the other comments have been reviewed.");
        verify(gitlabClient, never()).deleteMergeRequestDiscussionNote(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    void shouldNotRepostOutdatedNoteToResolvedSummaryCommentThreadIfAlreadyPresent() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(101L);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(note.isSystem()).thenReturn(false);
        when(note.isResolved()).thenReturn(true);

        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("username");
        Note note2 = mock();
        when(note2.getId()).thenReturn(102L);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Another comment");
        when(note2.isSystem()).thenReturn(false);

        Note note3 = mock();
        when(note3.getId()).thenReturn(103L);
        when(note3.getAuthor()).thenReturn(sonarqubeUser);
        when(note3.getBody()).thenReturn("This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being removed. Please manually resolve this discussion once the other comments have been reviewed.");
        when(note3.isSystem()).thenReturn(false);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(List.of(note, note2, note3));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
        verify(gitlabClient, never()).deleteMergeRequestDiscussionNote(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    void shouldNotTryAndCleanupNonSummaryNote() throws IOException {
        Note note = mock();
        when(note.getId()).thenReturn(101L);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Not Summary comment" + System.lineSeparator() + "[Don't View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(note.isSystem()).thenReturn(false);

        User otherUser = mock();
        when(otherUser.getUsername()).thenReturn("username");
        Note note2 = mock();
        when(note2.getId()).thenReturn(102L);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Another comment");
        when(note2.isSystem()).thenReturn(false);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(List.of(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
        verify(gitlabClient, never()).deleteMergeRequestDiscussionNote(anyLong(), anyLong(), any(), anyLong());
        verify(gitlabClient).getMergeRequestDiscussions(PROJECT_ID, MERGE_REQUEST_IID);
    }
}
