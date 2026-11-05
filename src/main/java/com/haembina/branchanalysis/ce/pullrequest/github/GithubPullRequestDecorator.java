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
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.almclient.github.GithubClientFactory;
import com.haembina.branchanalysis.almclient.github.GithubConnection;
import com.haembina.branchanalysis.almclient.github.model.ReviewThread;
import com.haembina.branchanalysis.almclient.github.model.ReviewThreadComment;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.DecorationSettings;
import com.haembina.branchanalysis.ce.pullrequest.DiscussionAwarePullRequestDecorator;
import com.haembina.branchanalysis.ce.pullrequest.PostAnalysisIssueVisitor;
import com.haembina.branchanalysis.ce.pullrequest.markup.Bold;
import com.haembina.branchanalysis.ce.pullrequest.markup.Document;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.markup.Text;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisIssueSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

public class GithubPullRequestDecorator extends DiscussionAwarePullRequestDecorator<GithubDecorationClient, GHPullRequest, GithubConnection, ReviewThread, ReviewThreadComment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubPullRequestDecorator.class);
    private static final String DEFAULT_CHECK_RUN_NAME = "SonarQube Code Analysis";
    private final GithubClientFactory githubClientFactory;
    private final MarkdownFormatterFactory markdownFormatterFactory;
    private final Clock clock;
    private final DecorationSettings decorationSettings;

    public GithubPullRequestDecorator(ScmInfoRepository scmInfoRepository, GithubClientFactory githubClientFactory,
                                      ReportGenerator reportGenerator, MarkdownFormatterFactory markdownFormatterFactory,
                                      Clock clock, DecorationSettings decorationSettings) {
        super(scmInfoRepository, reportGenerator);
        this.githubClientFactory = githubClientFactory;
        this.markdownFormatterFactory = markdownFormatterFactory;
        this.clock = clock;
        this.decorationSettings = decorationSettings;
    }

    @Override
    public boolean decorateBranchQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto,
                                                   ProjectAlmSettingDto projectAlmSettingDto) {
        try {
            GithubConnection connection = githubClientFactory.connect(almSettingDto, projectAlmSettingDto);
            GHRepository repository = connection.client().getRepository(projectAlmSettingDto.getAlmRepo());
            String dashboardUrl = getReportGenerator().createBranchDashboardUrl(analysisDetails);
            GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output(checkRunTitle(analysisDetails),
                String.format("[View in SonarQube](%s)", dashboardUrl));
            submitCheckRun(repository, analysisDetails, projectAlmSettingDto.getMonorepo(), dashboardUrl, output);
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate branch on Github", ex);
        }
    }

    @Override
    public List<ALM> alm() {
        return List.of(ALM.GITHUB);
    }

    @Override
    protected boolean isInlineCommentsEnabled(ProjectAlmSettingDto projectAlmSettingDto) {
        return decorationSettings.isGithubInlineCommentsEnabled();
    }

    @Override
    protected GithubDecorationClient createClient(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        try {
            GithubConnection connection = githubClientFactory.connect(almSettingDto, projectAlmSettingDto);
            GHRepository repository = connection.client().getRepository(projectAlmSettingDto.getAlmRepo());
            return new GithubDecorationClient(connection, repository, projectAlmSettingDto);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Github", ex);
        }
    }

    @Override
    protected Optional<String> createFrontEndUrl(GHPullRequest pullRequest, AnalysisDetails analysisDetails) {
        return Optional.of(pullRequest.getHtmlUrl().toExternalForm());
    }

    @Override
    protected GHPullRequest getPullRequest(GithubDecorationClient client, AlmSettingDto almSettingDto,
                                           ProjectAlmSettingDto projectAlmSettingDto, AnalysisDetails analysis) {
        try {
            return client.repository().getPullRequest(Integer.parseInt(analysis.getPullRequestId()));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Pull Request details from Github", ex);
        }
    }

    @Override
    protected GithubConnection getCurrentUser(GithubDecorationClient client) {
        return client.connection();
    }

    @Override
    protected List<String> getCommitIdsForPullRequest(GithubDecorationClient client, GHPullRequest pullRequest) {
        try {
            return pullRequest.listCommits().toList().stream().map(GHPullRequestCommitDetail::getSha).toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve commits for Pull Request from Github", ex);
        }
    }

    @Override
    protected void submitPipelineStatus(GithubDecorationClient client, GHPullRequest pullRequest, AnalysisDetails analysis,
                                        AnalysisSummary analysisSummary, ProjectAlmSettingDto projectAlmSettingDto) {
        GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output(checkRunTitle(analysis), analysisSummary.format(markdownFormatterFactory));
        if (decorationSettings.isGithubAnnotationsEnabled()) {
            for (PostAnalysisIssueVisitor.ComponentIssue componentIssue : analysis.getScmReportableIssues()) {
                output.add(new GHCheckRunBuilder.Annotation(
                    componentIssue.getScmPath().orElseThrow(),
                    Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0),
                    mapToGithubAnnotationLevel(componentIssue.getIssue().impacts().values()),
                    Optional.ofNullable(componentIssue.getIssue().getMessage()).orElseThrow())
                );
            }
        }
        try {
            submitCheckRun(client.repository(), analysis, projectAlmSettingDto.getMonorepo(), analysisSummary.getDashboardUrl(), output);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create check run on Github", ex);
        }
    }

    @Override
    protected void submitCommitNoteForIssue(GithubDecorationClient client, GHPullRequest pullRequest,
                                            PostAnalysisIssueVisitor.ComponentIssue issue, String filePath,
                                            AnalysisDetails analysis, AnalysisIssueSummary analysisIssueSummary) {
        int line = issue.getIssue().getLine();
        try {
            pullRequest.createReviewComment()
                .commitId(analysis.getCommitSha())
                .path(filePath)
                .line(line)
                .body(analysisIssueSummary.format(markdownFormatterFactory))
                .create();
        } catch (IOException ex) {
            LOGGER.warn("Github refused a review comment on {} line {}", filePath, line, ex);
        }
    }

    @Override
    protected void submitSummaryNote(GithubDecorationClient client, GHPullRequest pullRequest, AnalysisDetails analysis,
                                     AnalysisSummary analysisSummary) {
        if (!Optional.ofNullable(client.projectAlmSettingDto().getSummaryCommentEnabled()).orElse(false)) {
            return;
        }
        boolean createIfMissing = !(decorationSettings.isSummaryCommentSkippedWhenClean() && isCleanPass(analysis));
        try {
            postSummaryComment(client.connection(), pullRequest, analysisSummary.format(markdownFormatterFactory),
                analysis.getAnalysisProjectKey(), createIfMissing);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not submit summary comment to Github", ex);
        }
    }

    @Override
    protected List<ReviewThread> getDiscussions(GithubDecorationClient client, GHPullRequest pullRequest) {
        try {
            return client.connection().graphql().getReviewThreads(client.projectAlmSettingDto().getAlmRepo(), pullRequest.getNumber());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve review threads from Github", ex);
        }
    }

    @Override
    protected boolean isNoteFromCurrentUser(ReviewThreadComment note, GithubConnection user) {
        return user.isAppLogin(note.authorLogin());
    }

    @Override
    protected String getNoteContent(GithubDecorationClient client, ReviewThreadComment note) {
        return note.body();
    }

    @Override
    protected List<ReviewThreadComment> getNotesForDiscussion(GithubDecorationClient client, ReviewThread discussion) {
        return discussion.comments();
    }

    @Override
    protected boolean isClosed(ReviewThread discussion, List<ReviewThreadComment> notesInDiscussion) {
        return discussion.resolved();
    }

    @Override
    protected boolean isUserNote(ReviewThreadComment note) {
        return true;
    }

    @Override
    protected void addNoteToDiscussion(GithubDecorationClient client, ReviewThread discussion, GHPullRequest pullRequest, String note) {
        try {
            client.connection().graphql().replyToReviewThread(discussion.id(), note);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not reply to review thread on Github", ex);
        }
    }

    @Override
    protected void resolveDiscussion(GithubDecorationClient client, ReviewThread discussion, GHPullRequest pullRequest) {
        try {
            client.connection().graphql().resolveReviewThread(discussion.id());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not resolve review thread on Github", ex);
        }
    }

    @Override
    protected void deleteDiscussion(GithubDecorationClient client, ReviewThread discussion, GHPullRequest pullRequest,
                                    List<ReviewThreadComment> notesForDiscussion) {
        try {
            for (ReviewThreadComment note : notesForDiscussion) {
                client.connection().graphql().deleteReviewComment(note.id());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete review thread on Github", ex);
        }
    }

    private static String checkRunTitle(AnalysisDetails analysisDetails) {
        return "Quality Gate " + (analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? "success" : "failed");
    }

    private void submitCheckRun(GHRepository repository, AnalysisDetails analysisDetails, boolean isMonorepo,
                                String detailsUrl, GHCheckRunBuilder.Output output) throws IOException {
        String checkRunName = isMonorepo
            ? String.format("[%s] %s", analysisDetails.getAnalysisProjectName(), DEFAULT_CHECK_RUN_NAME)
            : DEFAULT_CHECK_RUN_NAME;
        repository.createCheckRun(checkRunName, analysisDetails.getCommitSha())
            .withStartedAt(Date.from(analysisDetails.getAnalysisDate()))
            .withCompletedAt(Date.from(clock.instant()))
            .withStatus(GHCheckRun.Status.COMPLETED)
            .withConclusion(analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? GHCheckRun.Conclusion.SUCCESS : GHCheckRun.Conclusion.FAILURE)
            .withDetailsURL(detailsUrl)
            .withExternalID(analysisDetails.getAnalysisId())
            .add(output)
            .create();
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
