/*
 * Copyright (C) 2020-2025 Michael Clarke
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

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.almclient.github.GithubClientFactory;
import com.haembina.branchanalysis.almclient.github.GithubConnection;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.DecorationResult;
import com.haembina.branchanalysis.ce.pullrequest.DecorationSettings;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;
import com.haembina.branchanalysis.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.haembina.branchanalysis.ce.pullrequest.markup.Bold;
import com.haembina.branchanalysis.ce.pullrequest.markup.Document;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.markup.Text;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

public class GithubPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final String DEFAULT_CHECK_RUN_NAME = "SonarQube Code Analysis";
    private final GithubClientFactory githubClientFactory;
    private final ReportGenerator reportGenerator;
    private final MarkdownFormatterFactory markdownFormatterFactory;
    private final Clock clock;
    private final DecorationSettings decorationSettings;

    public GithubPullRequestDecorator(GithubClientFactory githubClientFactory, ReportGenerator reportGenerator,
                                      MarkdownFormatterFactory markdownFormatterFactory, Clock clock,
                                      DecorationSettings decorationSettings) {
        this.githubClientFactory = githubClientFactory;
        this.reportGenerator = reportGenerator;
        this.markdownFormatterFactory = markdownFormatterFactory;
        this.clock = clock;
        this.decorationSettings = decorationSettings;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto,
                                          ProjectAlmSettingDto projectAlmSettingDto) {
        try {
            GithubConnection connection = githubClientFactory.connect(almSettingDto, projectAlmSettingDto);
            GHRepository repository = connection.client().getRepository(projectAlmSettingDto.getAlmRepo());

            GHPullRequest pullRequest = createCheckRun(connection, repository, analysisDetails, projectAlmSettingDto.getMonorepo(),
                    Optional.ofNullable(projectAlmSettingDto.getSummaryCommentEnabled()).orElse(false));

            return DecorationResult.builder()
                    .withPullRequestUrl(pullRequest.getHtmlUrl().toExternalForm())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Github", ex);
        }

    }

    @Override
    public List<ALM> alm() {
        return List.of(ALM.GITHUB);
    }


    private GHPullRequest createCheckRun(GithubConnection connection, GHRepository repository, AnalysisDetails analysisDetails,
            boolean isMonorepo, boolean postSummaryComment) throws IOException {
        AnalysisSummary analysisSummary = reportGenerator.createAnalysisSummary(analysisDetails);
        String summary = analysisSummary.format(markdownFormatterFactory);

        GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output("Quality Gate " + (analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? "success" : "failed"), summary);
        if (decorationSettings.isGithubAnnotationsEnabled()) {
            for (PostAnalysisIssueVisitor.ComponentIssue componentIssue : analysisDetails.getScmReportableIssues()) {
                output.add(new GHCheckRunBuilder.Annotation(
                    componentIssue.getScmPath().orElseThrow(),
                    Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0),
                    mapToGithubAnnotationLevel(componentIssue.getIssue().impacts().values()),
                    Optional.ofNullable(componentIssue.getIssue().getMessage()).orElseThrow())
                );
            }
        }

        String checkRunName = isMonorepo
            ? String.format("[%s] %s", analysisDetails.getAnalysisProjectName(), DEFAULT_CHECK_RUN_NAME)
            : DEFAULT_CHECK_RUN_NAME;
        repository.createCheckRun(checkRunName, analysisDetails.getCommitSha())
            .withStartedAt(Date.from(analysisDetails.getAnalysisDate()))
            .withCompletedAt(Date.from(clock.instant()))
            .withStatus(GHCheckRun.Status.COMPLETED)
            .withConclusion(analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? GHCheckRun.Conclusion.SUCCESS : GHCheckRun.Conclusion.FAILURE)
            .withDetailsURL(analysisSummary.getDashboardUrl())
            .withExternalID(analysisDetails.getAnalysisId())
            .add(output)
            .create();

        GHPullRequest pullRequest = repository.getPullRequest(Integer.parseInt(analysisDetails.getPullRequestId()));
        if (postSummaryComment) {
            boolean createIfMissing = !(decorationSettings.isSummaryCommentSkippedWhenClean() && isCleanPass(analysisDetails));
            postSummaryComment(connection, pullRequest, summary, analysisDetails.getAnalysisProjectKey(), createIfMissing);
        }
        return pullRequest;
    }

    private static boolean isCleanPass(AnalysisDetails analysisDetails) {
        return analysisDetails.getQualityGateStatus() == QualityGate.Status.OK
            && analysisDetails.getIssues().stream().noneMatch(issue -> issue.getIssue().issueStatus() == IssueStatus.OPEN);
    }

    private void postSummaryComment(GithubConnection connection, GHPullRequest pullRequest, String summary, String projectId,
                                    boolean createIfMissing) throws IOException {
        String projectCommentMarker = markdownFormatterFactory.documentFormatter().format(new Document(new Bold(new Text("Project ID:")), new Text(" " + projectId)));

        List<GHIssueComment> summaryComments = new ArrayList<>();
        for (GHIssueComment comment : pullRequest.getComments()) {
            if (connection.isAppLogin(comment.getUser().getLogin())
                && (comment.getBody().contains(projectCommentMarker + "\n") || comment.getBody().contains(projectCommentMarker + "\r"))) {
                summaryComments.add(comment);
            }
        }

        if (summaryComments.isEmpty()) {
            if (createIfMissing) {
                pullRequest.comment(summary);
            }
            return;
        }

        GHIssueComment summaryComment = summaryComments.get(0);
        if (!summary.equals(summaryComment.getBody())) {
            summaryComment.update(summary);
        }
        for (GHIssueComment duplicate : summaryComments.subList(1, summaryComments.size())) {
            duplicate.delete();
        }
    }

    private static GHCheckRun.AnnotationLevel mapToGithubAnnotationLevel(Collection<Severity> sonarqubeSeverity) {
        Severity maxSeverity = sonarqubeSeverity.stream().max(Severity::compareTo).orElseThrow();
        return switch (maxSeverity) {
            case LOW, INFO -> GHCheckRun.AnnotationLevel.NOTICE;
            case MEDIUM -> GHCheckRun.AnnotationLevel.WARNING;
            case HIGH, BLOCKER -> GHCheckRun.AnnotationLevel.FAILURE;
        };
    }

}
