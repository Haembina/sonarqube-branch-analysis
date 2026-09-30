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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.almclient.gitlab.GitlabClient;
import com.haembina.branchanalysis.almclient.gitlab.GitlabClientFactory;
import com.haembina.branchanalysis.almclient.gitlab.model.Commit;
import com.haembina.branchanalysis.almclient.gitlab.model.DiffRefs;
import com.haembina.branchanalysis.almclient.gitlab.model.MergeRequest;
import com.haembina.branchanalysis.almclient.gitlab.model.User;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisIssueSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

/**
 * The mocks, constants and stubbing every GitlabMergeRequestDecorator suite starts from: a merge request
 * with four commits, a SonarQube user, and an analysis with no reportable issues.
 */
abstract class GitlabMergeRequestDecoratorFixture {

    protected static final long MERGE_REQUEST_IID = 123;
    protected static final long PROJECT_ID = 101;
    protected static final String PROJECT_PATH = "dummy/repo";
    protected static final String PROJECT_KEY = "projectKey";
    protected static final String ANALYSIS_UUID = "analysis-uuid";
    protected static final String SONARQUBE_USERNAME = "sonarqube@gitlab.dummy";
    protected static final String BASE_SHA = "baseSha";
    protected static final String HEAD_SHA = "headSha";
    protected static final String START_SHA = "startSha";
    protected static final String MERGE_REQUEST_WEB_URL = "https://gitlab.dummy/path/to/mr";
    protected static final String OLD_SONARQUBE_ISSUE_COMMENT = "This issue no longer exists in SonarQube, " +
            "but due to other comments being present in this discussion, " +
            "the discussion is not being closed automatically. " +
            "Please manually resolve this discussion once the other comments have been reviewed.";

    protected final GitlabClient gitlabClient = mock();
    protected final GitlabClientFactory gitlabClientFactory = mock();
    protected final ScmInfoRepository scmInfoRepository = mock();
    protected final AnalysisDetails analysisDetails = mock();
    protected final AlmSettingDto almSettingDto = mock();
    protected final ProjectAlmSettingDto projectAlmSettingDto = mock();
    protected final MergeRequest mergeRequest = mock();
    protected final User sonarqubeUser = mock();
    protected final DiffRefs diffRefs = mock();
    protected final ReportGenerator reportGenerator = mock();
    protected final MarkdownFormatterFactory markdownFormatterFactory = mock();
    protected final AnalysisSummary analysisSummary = mock();

    protected final GitlabMergeRequestDecorator underTest = new GitlabMergeRequestDecorator(scmInfoRepository, gitlabClientFactory, reportGenerator, markdownFormatterFactory);

    @BeforeEach
    void setUp() throws IOException {
        when(analysisSummary.format(any())).thenReturn("Summary Comment");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        AnalysisIssueSummary analysisIssueSummary = mock();
        when(analysisIssueSummary.format(any())).thenReturn("Issue Summary");
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(analysisIssueSummary);
        when(gitlabClientFactory.createClient(any(), any())).thenReturn(gitlabClient);
        when(almSettingDto.getUrl()).thenReturn("http://gitlab.dummy");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(PROJECT_PATH);
        when(analysisDetails.getPullRequestId()).thenReturn(Long.toString(MERGE_REQUEST_IID));
        when(mergeRequest.getIid()).thenReturn(MERGE_REQUEST_IID);
        when(mergeRequest.getSourceProjectId()).thenReturn(PROJECT_ID);
        when(mergeRequest.getTargetProjectId()).thenReturn(PROJECT_ID);
        when(mergeRequest.getDiffRefs()).thenReturn(diffRefs);
        when(mergeRequest.getWebUrl()).thenReturn(MERGE_REQUEST_WEB_URL);
        when(diffRefs.getBaseSha()).thenReturn(BASE_SHA);
        when(diffRefs.getHeadSha()).thenReturn(HEAD_SHA);
        when(diffRefs.getStartSha()).thenReturn(START_SHA);
        when(gitlabClient.getMergeRequest(PROJECT_PATH, MERGE_REQUEST_IID)).thenReturn(mergeRequest);
        when(gitlabClient.getMergeRequestCommits(PROJECT_ID, MERGE_REQUEST_IID)).thenReturn(Arrays.stream(new String[]{"ABC", "DEF", "GHI", "JKL"})
                .map(Commit::new)
                .toList());
        when(sonarqubeUser.getUsername()).thenReturn(SONARQUBE_USERNAME);
        when(gitlabClient.getCurrentUser()).thenReturn(sonarqubeUser);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(PROJECT_KEY);
        when(analysisDetails.getAnalysisId()).thenReturn(ANALYSIS_UUID);
        when(analysisDetails.getScmReportableIssues()).thenReturn(new ArrayList<>());
    }
}
