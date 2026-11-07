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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;

import com.haembina.branchanalysis.almclient.gitlab.model.PipelineStatus;
import com.haembina.branchanalysis.almclient.gitlab.model.Project;

class GitlabMergeRequestDecoratorBranchTest extends GitlabMergeRequestDecoratorFixture {

    private static final String BRANCH_DASHBOARD_URL = "https://sonarqube.dummy/dashboard?id=" + PROJECT_KEY + "&branch=develop";

    @BeforeEach
    void setUpBranch() throws IOException {
        Project project = mock();
        when(project.getId()).thenReturn(PROJECT_ID);
        when(gitlabClient.getProject(PROJECT_PATH)).thenReturn(project);
        when(reportGenerator.createBranchDashboardUrl(analysisDetails)).thenReturn(BRANCH_DASHBOARD_URL);
        when(analysisDetails.getCommitSha()).thenReturn("branch sha");
    }

    @Test
    void shouldSetASuccessfulCommitStatusForAPassingBranch() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);

        assertThat(underTest.decorateBranchQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto)).isTrue();

        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("branch sha"), pipelineStatusArgumentCaptor.capture());
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.SUCCESS, BRANCH_DASHBOARD_URL, null, null));
        verify(gitlabClient, never()).getMergeRequest(any(), anyLong());
    }

    @Test
    void shouldSetAFailedCommitStatusForAFailingBranchInItsPipeline() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.getScannerProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PIPELINE_ID)).thenReturn(Optional.of("12"));

        underTest.decorateBranchQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.captor();
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("branch sha"), pipelineStatusArgumentCaptor.capture());
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.FAILED, BRANCH_DASHBOARD_URL, null, 12L));
    }

    @Test
    void shouldReportACommitStatusGitlabRefused() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        IOException refusal = new IOException("refused");
        doThrow(refusal).when(gitlabClient).setMergeRequestPipelineStatus(anyLong(), any(), any());

        assertThatThrownBy(() -> underTest.decorateBranchQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not set the branch commit status in Gitlab")
                .hasCause(refusal);
    }
}
