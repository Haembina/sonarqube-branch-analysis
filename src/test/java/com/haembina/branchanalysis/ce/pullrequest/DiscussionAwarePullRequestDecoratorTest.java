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
package com.haembina.branchanalysis.ce.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisIssueSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

class DiscussionAwarePullRequestDecoratorTest {

    private static final String SONARQUBE = "sonarqube";
    private static final String REVIEWER = "reviewer";
    private static final String COMMIT = "c1";
    private static final String CLOSED_ISSUE_MESSAGE = "This issue no longer exists in SonarQube, but due to other comments being present in this discussion, "
            + "the discussion is not being closed automatically. Please manually resolve this discussion once the other comments have been reviewed.";
    private static final String OUTDATED_SUMMARY_MESSAGE = "This summary note is outdated, but due to other comments being present in this discussion, "
            + "the discussion is not being removed. Please manually resolve this discussion once the other comments have been reviewed.";

    private final ScmInfoRepository scmInfoRepository = mock();
    private final AnalysisDetails analysis = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final List<FakeDiscussion> discussions = new ArrayList<>();
    private final List<PostAnalysisIssueVisitor.ComponentIssue> openIssues = new ArrayList<>();
    private final FakeDecorator underTest;

    DiscussionAwarePullRequestDecoratorTest() {
        ReportGenerator reportGenerator = mock();
        AnalysisSummary analysisSummary = mock();
        when(reportGenerator.createAnalysisSummary(analysis)).thenReturn(analysisSummary);
        when(analysis.getScmReportableIssues()).thenReturn(openIssues);
        when(analysis.getAnalysisProjectKey()).thenReturn("project");
        underTest = new FakeDecorator(scmInfoRepository, reportGenerator, discussions);
    }

    @Test
    void shouldCommentOnlyOnIssuesWithALinePathAndChangesetFromThisPullRequest() {
        openIssues.add(issue("new", 3, "src/New.java", true, true, COMMIT));
        openIssues.add(issue("older-commit", 4, "src/Old.java", true, true, "c0"));
        openIssues.add(issue("no-line", null, "src/NoLine.java", true, true, COMMIT));
        openIssues.add(issue("no-path", 5, null, true, true, COMMIT));
        openIssues.add(issue("no-scm", 6, "src/NoScm.java", false, true, COMMIT));
        openIssues.add(issue("no-changeset", 7, "src/NoChangeset.java", true, false, COMMIT));

        assertThat(decorate()).containsExactly("comment new src/New.java", "summary", "pipeline");
    }

    @Test
    void shouldLeaveDiscussionsStartedBySomeoneElseAlone() {
        discussions.add(new FakeDiscussion("d1", false, List.of(new FakeNote(REVIEWER, issueComment("project", "gone"), true))));

        assertThat(decorate()).containsExactly("summary", "pipeline");
    }

    @Test
    void shouldLeaveResolvedIssueDiscussionsAlone() {
        discussions.add(new FakeDiscussion("d1", true, List.of(new FakeNote(SONARQUBE, issueComment("project", "gone"), true))));

        assertThat(decorate()).containsExactly("summary", "pipeline");
    }

    @Test
    void shouldAskForManualResolutionEvenWhenAnotherUserPostedTheClosingMessage() {
        discussions.add(new FakeDiscussion("d1", false, List.of(
                new FakeNote(SONARQUBE, issueComment("project", "gone"), true),
                new FakeNote(REVIEWER, CLOSED_ISSUE_MESSAGE, true))));

        assertThat(decorate()).containsExactly("note d1 " + CLOSED_ISSUE_MESSAGE, "summary", "pipeline");
    }

    @Test
    void shouldNotRepeatTheClosingMessageItAlreadyPosted() {
        discussions.add(new FakeDiscussion("d1", false, List.of(
                new FakeNote(SONARQUBE, issueComment("project", "gone"), true),
                new FakeNote(REVIEWER, "Why?", true),
                new FakeNote(SONARQUBE, CLOSED_ISSUE_MESSAGE, true))));

        assertThat(decorate()).containsExactly("summary", "pipeline");
    }

    @Test
    void shouldAskForManualRemovalOfAnOutdatedSummaryEvenWhenAnotherUserPostedTheMessage() {
        discussions.add(new FakeDiscussion("d1", false, List.of(
                new FakeNote(SONARQUBE, "Summary\n[View in SonarQube](http://sonar/dashboard?id=project&pullRequest=1)", true),
                new FakeNote(REVIEWER, OUTDATED_SUMMARY_MESSAGE, true))));

        assertThat(decorate()).containsExactly("note d1 " + OUTDATED_SUMMARY_MESSAGE, "summary", "pipeline");
    }

    @Test
    void shouldReadTheProjectFromTheIdParameterInAMonorepo() {
        when(projectAlmSettingDto.getMonorepo()).thenReturn(true);
        discussions.add(new FakeDiscussion("d1", false, List.of(
                new FakeNote(SONARQUBE, "Issue\n[View in SonarQube](http://sonar/project/issues?issues=gone&id=project)", true))));
        discussions.add(new FakeDiscussion("d2", false, List.of(
                new FakeNote(SONARQUBE, issueComment("other-project", "gone"), true))));

        assertThat(decorate()).containsExactly("resolve d1", "summary", "pipeline");
    }

    @Test
    void shouldMatchHotspotCommentsByTheirHotspotKey() {
        openIssues.add(issue("HS1", 3, "src/Hotspot.java", true, true, COMMIT));
        discussions.add(new FakeDiscussion("d1", false, List.of(
                new FakeNote(SONARQUBE, "[View in SonarQube](http://sonar/security_hotspots?id=project&hotspots=HS1)", true))));
        discussions.add(new FakeDiscussion("d2", false, List.of(
                new FakeNote(SONARQUBE, "[View in SonarQube](http://sonar/security_hotspots?id=project&hotspots=HS-gone)", true))));

        assertThat(decorate()).containsExactly("resolve d2", "summary", "pipeline");
    }

    private List<String> decorate() {
        underTest.decorateQualityGateStatus(analysis, mock(AlmSettingDto.class), projectAlmSettingDto);
        return underTest.actions;
    }

    private static String issueComment(String project, String issueKey) {
        return "Issue\n[View in SonarQube](http://sonar/project/issues?id=" + project + "&issues=" + issueKey + ")";
    }

    private PostAnalysisIssueVisitor.ComponentIssue issue(String key, Integer line, String scmPath, boolean hasScmInfo,
                                                          boolean hasChangeset, String revision) {
        Component component = mock();
        when(component.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes = mock();
        when(reportAttributes.getScmPath()).thenReturn(Optional.ofNullable(scmPath));
        when(component.getReportAttributes()).thenReturn(reportAttributes);
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.key()).thenReturn(key);
        when(lightIssue.getLine()).thenReturn(line);
        ScmInfo scmInfo = mock();
        when(scmInfo.hasChangesetForLine(anyInt())).thenReturn(hasChangeset);
        Changeset changeset = Changeset.newChangesetBuilder().setRevision(revision).setDate(1L).build();
        when(scmInfo.getChangesetForLine(anyInt())).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(hasScmInfo ? Optional.of(scmInfo) : Optional.empty());
        return new PostAnalysisIssueVisitor.ComponentIssue(component, lightIssue);
    }

    private record FakeNote(String author, String content, boolean userNote) {
    }

    private record FakeDiscussion(String id, boolean closed, List<FakeNote> notes) {
    }

    private static final class FakeDecorator extends DiscussionAwarePullRequestDecorator<String, String, String, FakeDiscussion, FakeNote> {

        private final List<FakeDiscussion> discussions;
        private final List<String> actions = new ArrayList<>();

        FakeDecorator(ScmInfoRepository scmInfoRepository, ReportGenerator reportGenerator, List<FakeDiscussion> discussions) {
            super(scmInfoRepository, reportGenerator);
            this.discussions = discussions;
        }

        @Override
        public List<ALM> alm() {
            return List.of();
        }

        @Override
        protected boolean isInlineCommentsEnabled(ProjectAlmSettingDto projectAlmSettingDto) {
            return true;
        }

        @Override
        protected String createClient(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
            return "client";
        }

        @Override
        protected Optional<String> createFrontEndUrl(String pullRequest, AnalysisDetails analysisDetails) {
            return Optional.empty();
        }

        @Override
        protected String getPullRequest(String client, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto, AnalysisDetails analysis) {
            return "pull-request";
        }

        @Override
        protected String getCurrentUser(String client) {
            return SONARQUBE;
        }

        @Override
        protected List<String> getCommitIdsForPullRequest(String client, String pullRequest) {
            return List.of(COMMIT);
        }

        @Override
        protected void submitPipelineStatus(String client, String pullRequest, AnalysisDetails analysis, AnalysisSummary analysisSummary,
                                            ProjectAlmSettingDto projectAlmSettingDto) {
            actions.add("pipeline");
        }

        @Override
        protected void submitCommitNoteForIssue(String client, String pullRequest, PostAnalysisIssueVisitor.ComponentIssue issue, String filePath,
                                                AnalysisDetails analysis, AnalysisIssueSummary analysisIssueSummary) {
            actions.add("comment " + issue.getIssue().key() + " " + filePath);
        }

        @Override
        protected String getNoteContent(String client, FakeNote note) {
            return note.content();
        }

        @Override
        protected List<FakeNote> getNotesForDiscussion(String client, FakeDiscussion discussion) {
            return discussion.notes();
        }

        @Override
        protected boolean isClosed(FakeDiscussion discussion, List<FakeNote> notesInDiscussion) {
            return discussion.closed();
        }

        @Override
        protected boolean isUserNote(FakeNote note) {
            return note.userNote();
        }

        @Override
        protected void addNoteToDiscussion(String client, FakeDiscussion discussion, String pullRequest, String note) {
            actions.add("note " + discussion.id() + " " + note);
        }

        @Override
        protected void resolveDiscussion(String client, FakeDiscussion discussion, String pullRequest) {
            actions.add("resolve " + discussion.id());
        }

        @Override
        protected void deleteDiscussion(String client, FakeDiscussion discussion, String pullRequest, List<FakeNote> notesForDiscussion) {
            actions.add("delete " + discussion.id());
        }

        @Override
        protected void submitSummaryNote(String client, String pullRequest, AnalysisDetails analysis, AnalysisSummary analysisSummary) {
            actions.add("summary");
        }

        @Override
        protected List<FakeDiscussion> getDiscussions(String client, String pullRequest) {
            return discussions;
        }

        @Override
        protected boolean isNoteFromCurrentUser(FakeNote note, String user) {
            return user.equals(note.author());
        }
    }
}
