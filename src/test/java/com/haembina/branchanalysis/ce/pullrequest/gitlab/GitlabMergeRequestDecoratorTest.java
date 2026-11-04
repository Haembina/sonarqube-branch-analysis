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
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.db.alm.setting.ALM;

import com.haembina.branchanalysis.almclient.gitlab.model.Discussion;
import com.haembina.branchanalysis.almclient.gitlab.model.MergeRequestNote;
import com.haembina.branchanalysis.almclient.gitlab.model.PipelineStatus;
import com.haembina.branchanalysis.ce.pullrequest.DecorationResult;

/**
 * The decorator's type, its failures reaching GitLab, the pipeline status and summary it posts, and the
 * merge request URL it reports.
 */
class GitlabMergeRequestDecoratorTest extends GitlabMergeRequestDecoratorFixture {

    @Test
    void shouldReturnCorrectDecoratorType() {
        assertThat(underTest.alm()).containsOnly(ALM.GITLAB);
    }

    @Test
    void shouldThrowErrorWhenPullRequestKeyNotNumeric() {
        when(analysisDetails.getPullRequestId()).thenReturn("non-MR-IID");

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not parse Merge Request ID");
    }

    @Test
    void shouldThrowErrorWhenGitlabMergeRequestRetrievalFails() throws IOException {
        when(gitlabClient.getMergeRequest(any(), anyLong())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve Merge Request details");
    }

    @Test
    void shouldThrowErrorWhenGitlabUserRetrievalFails() throws IOException {
        when(gitlabClient.getCurrentUser()).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve current user details");
    }

    @Test
    void shouldThrowErrorWhenGitlabMergeRequestCommitsRetrievalFails() throws IOException {
        when(gitlabClient.getMergeRequestCommits(anyLong(), anyLong())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve commit details for Merge Request");
    }

    @Test
    void shouldThrowErrorWhenGitlabMergeRequestDiscussionRetrievalFails() throws IOException {
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve Merge Request discussions");
    }

    @Test
    void shouldSubmitSuccessfulPipelineStatusAndResolvedSummaryCommentOnSuccessAnalysis() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getCommitSha()).thenReturn("commitsha");

        when(analysisSummary.format(any())).thenReturn("Summary comment");
        when(analysisSummary.getDashboardUrl()).thenReturn("https://sonarqube.dummy/dashboard?id=projectKey&pullRequest=123");

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("dicussion id");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, "dicussion id");
        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("commitsha"), pipelineStatusArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new MergeRequestNote("Summary comment"));
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.SUCCESS, "https://sonarqube.dummy/dashboard?id=" + PROJECT_KEY + "&pullRequest=" + MERGE_REQUEST_IID, null, null));
    }

    @Test
    void shouldSubmitFailedPipelineStatusAndUnresolvedSummaryCommentOnFailedAnalysis() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.getCommitSha()).thenReturn("other sha");
        when(analysisDetails.getScannerProperty("com.haembina.branchanalysis.pullrequest.gitlab.pipelineId")).thenReturn(Optional.of("11"));

        when(analysisSummary.format(any())).thenReturn("Different Summary comment");
        when(analysisSummary.getDashboardUrl()).thenReturn("https://sonarqube2.dummy/dashboard?id=projectKey&pullRequest=123");
        when(analysisSummary.getNewCoverage()).thenReturn(BigDecimal.TEN);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("dicussion id 2");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, "dicussion id 2");
        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("other sha"), pipelineStatusArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new MergeRequestNote("Different Summary comment"));
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.FAILED, "https://sonarqube2.dummy/dashboard?id=" + PROJECT_KEY + "&pullRequest=" + MERGE_REQUEST_IID, BigDecimal.TEN, 11L));
    }

    @Test
    void shouldThrowErrorWhenSubmitPipelineStatusToGitlabFails() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.getCommitSha()).thenReturn("other sha");
        when(analysisDetails.getScannerProperty("com.haembina.branchanalysis.pullrequest.gitlab.pipelineId")).thenReturn(Optional.of("11"));

        when(analysisSummary.format(any())).thenReturn("Different Summary comment");
        when(analysisSummary.getDashboardUrl()).thenReturn("https://sonarqube2.dummy/dashboard?id=projectKey&pullRequest=123");
        when(analysisSummary.getNewCoverage()).thenReturn(BigDecimal.TEN);

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("dicussion id 2");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);
        doThrow(new IOException("dummy")).when(gitlabClient).setMergeRequestPipelineStatus(anyLong(), any(), any());

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not update pipeline status in Gitlab");

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, "dicussion id 2");
        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("other sha"), pipelineStatusArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new MergeRequestNote("Different Summary comment"));
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.FAILED, "https://sonarqube2.dummy/dashboard?id=" + PROJECT_KEY + "&pullRequest=" + MERGE_REQUEST_IID, BigDecimal.TEN, 11L));
    }

    @Test
    void shouldThrowErrorWhenSubmitAnalysisToGitlabFails() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.getCommitSha()).thenReturn("other sha");
        when(analysisDetails.getScannerProperty("com.haembina.branchanalysis.pullrequest.gitlab.pipelineId")).thenReturn(Optional.of("11"));

        when(analysisSummary.format(any())).thenReturn("Different Summary comment");

        Discussion discussion = mock();
        when(discussion.getId()).thenReturn("dicussion id 2");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);
        doThrow(new IOException("dummy")).when(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), any());

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit summary comment to Gitlab");

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, "dicussion id 2");
        verify(gitlabClient, never()).setMergeRequestPipelineStatus(anyLong(), any(), any());

        assertThat(mergeRequestNoteArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new MergeRequestNote("Different Summary comment"));
    }

    @Test
    void shouldReturnWebUrlFromMergeRequestIfScannerPropertyNotSet() {
        assertThat(underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .usingRecursiveComparison()
                .isEqualTo(DecorationResult.builder().withPullRequestUrl(MERGE_REQUEST_WEB_URL).build());
    }

    @Test
    void shouldReturnWebUrlFromScannerPropertyIfSet() {
        when(analysisDetails.getScannerProperty("sonar.pullrequest.gitlab.projectUrl")).thenReturn(Optional.of(MERGE_REQUEST_WEB_URL + "/additional"));
        assertThat(underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .usingRecursiveComparison()
                .isEqualTo(DecorationResult.builder().withPullRequestUrl(MERGE_REQUEST_WEB_URL + "/additional/merge_requests/" + MERGE_REQUEST_IID).build());
    }
}
