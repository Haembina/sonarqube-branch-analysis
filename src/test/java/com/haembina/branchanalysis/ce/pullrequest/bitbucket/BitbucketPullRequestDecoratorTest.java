/*
 * Copyright (C) 2020-2025 Mathias Åhsberg, Michael Clarke
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
package com.haembina.branchanalysis.ce.pullrequest.bitbucket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.almclient.bitbucket.BitbucketClient;
import com.haembina.branchanalysis.almclient.bitbucket.BitbucketClientFactory;
import com.haembina.branchanalysis.almclient.bitbucket.BitbucketException;
import com.haembina.branchanalysis.almclient.bitbucket.model.AnnotationUploadLimit;
import com.haembina.branchanalysis.almclient.bitbucket.model.BuildStatus;
import com.haembina.branchanalysis.almclient.bitbucket.model.CodeInsightsAnnotation;
import com.haembina.branchanalysis.almclient.bitbucket.model.DataValue;
import com.haembina.branchanalysis.almclient.bitbucket.model.ReportData;
import com.haembina.branchanalysis.almclient.bitbucket.model.ReportStatus;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisIssueSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

class BitbucketPullRequestDecoratorTest {

    private static final String COMMIT = "commit";
    private static final String REPORT_KEY = "com.sonarsource.sonarqube";

    private static final String ISSUE_KEY = "issue-key";
    private static final int ISSUE_LINE = 1;
    private static final String ISSUE_LINK = "https://issue-link";
    private static final String ISSUE_MESSAGE = "issue message";
    private static final String ISSUE_PATH = "/issue/path";
    private static final String DASHBOARD_URL = "https://dashboard-url";
    private static final String IMAGE_URL = "https://image-url";

    private final AnalysisDetails analysisDetails = mock();
    private final ReportGenerator reportGenerator = mock();
    private final BitbucketClient client = mock();
    private final BitbucketClientFactory bitbucketClientFactory = mock();
    private final BitbucketPullRequestDecorator underTest = new BitbucketPullRequestDecorator(bitbucketClientFactory, reportGenerator);

    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AnalysisSummary analysisSummary = mock();

    @BeforeEach
    void setUp() {
        when(bitbucketClientFactory.createClient(any(), any())).thenReturn(client);
    }

    @Test
    void testValidAnalysis() throws IOException {
        when(client.supportsCodeInsights()).thenReturn(true);
        AnnotationUploadLimit uploadLimit = new AnnotationUploadLimit(1000, 1000);
        when(client.getAnnotationUploadLimit()).thenReturn(uploadLimit);

        mockValidAnalysis();
        when(analysisSummary.getNewDuplications()).thenReturn(BigDecimal.TEN);
        when(analysisSummary.getNewCoverage()).thenReturn(BigDecimal.ONE);
        when(analysisSummary.getAcceptedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0));
        when(analysisSummary.getFixedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 12));
        when(analysisSummary.getNewIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L));
        when(analysisSummary.getSecurityHotspots()).thenReturn(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69));
        when(analysisSummary.getSummaryImageUrl()).thenReturn(IMAGE_URL);
        when(analysisSummary.getDashboardUrl()).thenReturn(DASHBOARD_URL);
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        when(client.normaliseReportKey(any())).thenReturn("reportKey");
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client).normaliseReportKey(REPORT_KEY);
        ArgumentCaptor<List<ReportData>> reportDataArgumentCaptor = ArgumentCaptor.captor();
        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, "HIGH", "BUG");
        verify(client).createLinkDataValue(DASHBOARD_URL);
        verify(client).createCodeInsightsReport(reportDataArgumentCaptor.capture(), eq("Quality Gate passed" + System.lineSeparator()), any(), eq(DASHBOARD_URL), eq(IMAGE_URL), eq(ReportStatus.PASSED));
        when(analysisSummary.getAcceptedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0));
        when(analysisSummary.getFixedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 12));
        when(analysisSummary.getNewIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L));
        when(analysisSummary.getSecurityHotspots()).thenReturn(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69));
        verify(client).deleteAnnotations(COMMIT, "reportKey");

        assertThat(reportDataArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new ReportData("New Issues", new DataValue.Text("666 Issues")),
                        new ReportData("Accepted Issues", new DataValue.Text("0 Issues")),
                        new ReportData("Fixed Issues", new DataValue.Text("12 Issues")),
                        new ReportData("Code coverage", new DataValue.Percentage(BigDecimal.ONE)),
                        new ReportData("Duplication", new DataValue.Percentage(BigDecimal.TEN)),
                        new ReportData("Analysis details", null)));
    }

    @Test
    void testNullPercentagesReplacedWithZeroValues() throws IOException {
        when(client.supportsCodeInsights()).thenReturn(true);
        when(almSettingDto.getAlm()).thenReturn(ALM.BITBUCKET_CLOUD);
        AnnotationUploadLimit uploadLimit = new AnnotationUploadLimit(1000, 1000);
        when(client.getAnnotationUploadLimit()).thenReturn(uploadLimit);

        mockValidAnalysis();
        when(analysisSummary.getNewCoverage()).thenReturn(null);
        when(analysisSummary.getNewDuplications()).thenReturn(null);
        when(analysisSummary.getAcceptedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0));
        when(analysisSummary.getFixedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 1));
        when(analysisSummary.getNewIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L));
        when(analysisSummary.getSecurityHotspots()).thenReturn(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69));
        when(client.normaliseReportKey(REPORT_KEY)).thenReturn("reportKey");
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client).normaliseReportKey(REPORT_KEY);
        ArgumentCaptor<List<ReportData>> reportDataArgumentCaptor = ArgumentCaptor.captor();
        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, "HIGH", "BUG");
        verify(client).createLinkDataValue(DASHBOARD_URL);
        verify(client).createCodeInsightsReport(reportDataArgumentCaptor.capture(), eq("Quality Gate passed" + System.lineSeparator()), any(), eq(DASHBOARD_URL), eq(String.format("%s/common/icon.png", IMAGE_URL)), eq(ReportStatus.PASSED));
        verify(client).deleteAnnotations(COMMIT, "reportKey");

        ArgumentCaptor<BuildStatus> buildStatusArgumentCaptor = ArgumentCaptor.captor();
        verify(client).submitBuildStatus(eq(COMMIT), buildStatusArgumentCaptor.capture());
        assertThat(buildStatusArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(new BuildStatus(BuildStatus.State.SUCCESSFUL, "reportKey", "SonarQube", DASHBOARD_URL));

        assertThat(reportDataArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new ReportData("New Issues", new DataValue.Text("666 Issues")),
                        new ReportData("Accepted Issues", new DataValue.Text("0 Issues")),
                        new ReportData("Fixed Issues", new DataValue.Text("1 Issue")),
                        new ReportData("Code coverage", new DataValue.Percentage(BigDecimal.ZERO)),
                        new ReportData("Duplication", new DataValue.Percentage(BigDecimal.ZERO)),
                        new ReportData("Analysis details", null)));
    }

    @ParameterizedTest(name = "{arguments}")
    @CsvSource({"100, 1000, 2",
            "1000, 1000, 1",
            "100, 1000, 10"})
    void testExceedsMaximumNumberOfAnnotations(int annotationBatchSize, int totalAllowedAnnotations, int counter) {
        // given
        AnnotationUploadLimit uploadLimit = new AnnotationUploadLimit(annotationBatchSize, totalAllowedAnnotations);

        // when
        boolean result = BitbucketPullRequestDecorator.exceedsMaximumNumberOfAnnotations(counter, uploadLimit);

        // then
        assertFalse(result);
    }

    @Test
    void shouldSkipDecorationWhenCodeInsightsAreUnsupported() throws IOException {
        when(client.supportsCodeInsights()).thenReturn(false);

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client, never()).uploadReport(any(), any(), any());
        verify(reportGenerator, never()).createAnalysisSummary(any());
    }

    @Test
    void shouldReportFailedGateWithConditionsAndMonorepoKeyAndFailedBuildStatus() throws IOException {
        mockDecoratableAnalysis(List.of(componentIssue(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH))));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("monorepo-project");
        when(projectAlmSettingDto.getMonorepo()).thenReturn(true);
        when(almSettingDto.getAlm()).thenReturn(ALM.BITBUCKET_CLOUD);
        when(analysisSummary.getFailedQualityGateConditions()).thenReturn(List.of("Coverage below 80%", "2 new bugs"));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client).normaliseReportKey("monorepo-project");
        verify(client).createCodeInsightsReport(any(),
                eq("Quality Gate failed" + System.lineSeparator() + "- Coverage below 80%" + System.lineSeparator() + "- 2 new bugs"),
                any(), eq(DASHBOARD_URL), eq(IMAGE_URL), eq(ReportStatus.FAILED));
        ArgumentCaptor<BuildStatus> buildStatus = ArgumentCaptor.captor();
        verify(client).submitBuildStatus(eq(COMMIT), buildStatus.capture());
        assertThat(buildStatus.getValue()).usingRecursiveComparison().isEqualTo(new BuildStatus(BuildStatus.State.FAILED, "reportKey", "SonarQube", DASHBOARD_URL));
    }

    @Test
    void shouldLogAndReturnWhenReportUploadFails() throws IOException {
        mockDecoratableAnalysis(List.of());
        doThrow(new IOException("down")).when(client).uploadReport(any(), any(), any());

        assertThat(underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto)).isNotNull();

        verify(client, never()).deleteAnnotations(any(), any());
    }

    @Test
    void shouldServeBothBitbucketAlms() {
        assertThat(underTest.alm()).containsExactly(ALM.BITBUCKET, ALM.BITBUCKET_CLOUD);
    }

    @ParameterizedTest(name = "{0} {1} -> {2} {3}")
    @CsvSource({"SECURITY, BLOCKER, HIGH, VULNERABILITY",
            "MAINTAINABILITY, MEDIUM, MEDIUM, CODE_SMELL",
            "RELIABILITY, LOW, LOW, BUG",
            "RELIABILITY, INFO, LOW, BUG"})
    void shouldMapHighestImpactToBitbucketSeverityAndType(SoftwareQuality quality, Severity severity, String bitbucketSeverity, String bitbucketType) throws IOException {
        mockDecoratableAnalysis(List.of(componentIssue(Map.of(quality, severity))));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, bitbucketSeverity, bitbucketType);
    }

    @Test
    void shouldRefuseIssueWithoutImpacts() throws IOException {
        mockDecoratableAnalysis(List.of(componentIssue(Map.of())));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("No severity found in impacts");
    }

    @Test
    void shouldStopUploadingOnceTotalAnnotationLimitIsReached() throws IOException {
        mockDecoratableAnalysis(List.of(componentIssue(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH)),
                componentIssue(Map.of(SoftwareQuality.RELIABILITY, Severity.LOW))));
        when(client.getAnnotationUploadLimit()).thenReturn(new AnnotationUploadLimit(1, 1));
        CodeInsightsAnnotation high = mock();
        CodeInsightsAnnotation low = mock();
        when(client.createCodeInsightsAnnotation(any(), anyInt(), any(), any(), any(), eq("HIGH"), any())).thenReturn(high);
        when(client.createCodeInsightsAnnotation(any(), anyInt(), any(), any(), any(), eq("LOW"), any())).thenReturn(low);

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client, times(1)).uploadAnnotations(eq(COMMIT), any(), eq("reportKey"));
    }

    @Test
    void shouldTruncateAnnotationsWhenBitbucketRejectsPayloadAsTooLarge() throws IOException {
        mockDecoratableAnalysis(List.of(componentIssue(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH))));
        BitbucketException tooLarge = mock();
        when(tooLarge.isError(BitbucketException.PAYLOAD_TOO_LARGE)).thenReturn(true);
        doThrow(tooLarge).when(client).uploadAnnotations(any(), any(), any());

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client).uploadAnnotations(eq(COMMIT), any(), eq("reportKey"));
    }

    @Test
    void shouldRethrowAnyOtherBitbucketRejection() throws IOException {
        mockDecoratableAnalysis(List.of(componentIssue(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH))));
        BitbucketException forbidden = mock();
        doThrow(forbidden).when(client).uploadAnnotations(any(), any(), any());

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isSameAs(forbidden);
    }

    private void mockDecoratableAnalysis(List<PostAnalysisIssueVisitor.ComponentIssue> issues) throws IOException {
        when(client.supportsCodeInsights()).thenReturn(true);
        when(client.getAnnotationUploadLimit()).thenReturn(new AnnotationUploadLimit(1000, 1000));
        when(client.normaliseReportKey(any())).thenReturn("reportKey");
        when(analysisDetails.getCommitSha()).thenReturn(COMMIT);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getScmReportableIssues()).thenReturn(issues);
        when(analysisSummary.getNewIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 1L));
        when(analysisSummary.getAcceptedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0));
        when(analysisSummary.getFixedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 0));
        when(analysisSummary.getDashboardUrl()).thenReturn(DASHBOARD_URL);
        when(analysisSummary.getSummaryImageUrl()).thenReturn(IMAGE_URL);
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        AnalysisIssueSummary analysisIssueSummary = mock();
        when(analysisIssueSummary.getIssueUrl()).thenReturn(ISSUE_LINK);
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(analysisIssueSummary);
    }

    private static PostAnalysisIssueVisitor.ComponentIssue componentIssue(Map<SoftwareQuality, Severity> impacts) {
        ReportAttributes reportAttributes = mock();
        when(reportAttributes.getScmPath()).thenReturn(Optional.of(ISSUE_PATH));
        Component component = mock();
        when(component.getReportAttributes()).thenReturn(reportAttributes);
        PostAnalysisIssueVisitor.LightIssue issue = mock();
        when(issue.impacts()).thenReturn(impacts);
        when(issue.getLine()).thenReturn(ISSUE_LINE);
        when(issue.key()).thenReturn(ISSUE_KEY);
        when(issue.getMessage()).thenReturn(ISSUE_MESSAGE);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(issue);
        when(componentIssue.getComponent()).thenReturn(component);
        return componentIssue;
    }

    private void mockValidAnalysis() {
        when(analysisDetails.getCommitSha()).thenReturn(COMMIT);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(REPORT_KEY);

        when(analysisDetails.getAnalysisDate()).thenReturn(Instant.now());

        ReportAttributes reportAttributes = mock();
        when(reportAttributes.getScmPath()).thenReturn(Optional.of(ISSUE_PATH));

        Component component = mock();
        when(component.getType()).thenReturn(Component.Type.FILE);
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue defaultIssue = mock();
        when(defaultIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(defaultIssue.impacts()).thenReturn(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH));
        when(defaultIssue.getLine()).thenReturn(ISSUE_LINE);
        when(defaultIssue.key()).thenReturn(ISSUE_KEY);
        when(defaultIssue.getMessage()).thenReturn(ISSUE_MESSAGE);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        AnalysisIssueSummary analysisIssueSummary = mock();
        when(analysisIssueSummary.getIssueUrl()).thenReturn("https://issue-link");
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(analysisIssueSummary);

        when(analysisSummary.getDashboardUrl()).thenReturn("https://dashboard-url");
        when(analysisSummary.getSummaryImageUrl()).thenReturn("https://image-url/common/icon.png");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getScmReportableIssues()).thenReturn(List.of(componentIssue));
    }

}
