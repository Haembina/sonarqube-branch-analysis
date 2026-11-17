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
package com.haembina.branchanalysis.server.freshness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisFreshnessWsTest {

    private static final long MAIN_ANALYZED = 1_760_000_000_000L;
    private static final long BRANCH_ANALYZED = MAIN_ANALYZED + 1_000L;
    private static final long NEWER_BRANCH_ANALYZED = MAIN_ANALYZED + 2_000L;
    private static final long PULL_REQUEST_ANALYZED = MAIN_ANALYZED + 3_000L;

    private final DbClient dbClient = mock();
    private final DbSession dbSession = mock();
    private final ProjectDao projectDao = mock();
    private final BranchDao branchDao = mock();
    private final SnapshotDao snapshotDao = mock();
    private final UserSession userSession = mock();
    private final MainHeadReader mainHeadReader = mock();
    private final Request request = mock();
    private final Response response = mock();
    private final Response.Stream stream = mock();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ProjectDto project = new ProjectDto().setUuid("project-uuid").setKey("project");
    private final List<BranchDto> branches = new ArrayList<>();
    private final List<SnapshotDto> snapshots = new ArrayList<>();
    private final AnalysisFreshnessWs underTest = new AnalysisFreshnessWs(dbClient, userSession, mainHeadReader);

    @BeforeEach
    void setUp() {
        when(dbClient.openSession(false)).thenReturn(dbSession);
        when(dbClient.projectDao()).thenReturn(projectDao);
        when(dbClient.branchDao()).thenReturn(branchDao);
        when(dbClient.snapshotDao()).thenReturn(snapshotDao);
        when(request.mandatoryParamAsStrings(AnalysisFreshnessWs.PROJECTS_PARAMETER)).thenReturn(List.of("project"));
        when(projectDao.selectProjectsByKeys(dbSession, List.of("project"))).thenReturn(List.of(project));
        when(userSession.keepAuthorizedEntities(ProjectPermission.USER, List.of(project))).thenReturn(List.of(project));
        when(branchDao.selectByProject(dbSession, project)).thenReturn(branches);
        when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(), anyCollection())).thenReturn(snapshots);
        when(response.stream()).thenReturn(stream);
        when(stream.output()).thenReturn(output);
    }

    private void analyzed(BranchType type, String name, boolean main, Long analysisDate, String revision) {
        BranchDto branch = new BranchDto().setUuid(name + "-uuid").setKey(name).setBranchType(type).setIsMain(main);
        branches.add(branch);
        if (analysisDate != null) {
            snapshots.add(new SnapshotDto().setRootComponentUuid(branch.getUuid()).setAnalysisDate(analysisDate).setRevision(revision));
        }
    }

    private String answer() throws IOException {
        underTest.handle(request, response);
        verify(stream).setMediaType("application/json");
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void definesTheListActionForUpToAHundredProjects() {
        WebService.Context context = new WebService.Context();

        underTest.define(context);

        WebService.Action action = context.controller("api/analysis_freshness").action("list");
        assertThat(action.handler()).isSameAs(underTest);
        assertThat(action.param(AnalysisFreshnessWs.PROJECTS_PARAMETER).isRequired()).isTrue();
        assertThat(action.param(AnalysisFreshnessWs.PROJECTS_PARAMETER).maxValuesAllowed()).isEqualTo(AnalysisFreshnessWs.MAX_PROJECTS);
    }

    @Test
    void namesTheNewestBranchAndPullRequestBesideMain() throws IOException {
        analyzed(BranchType.BRANCH, "main", true, MAIN_ANALYZED, "abc123");
        analyzed(BranchType.BRANCH, "feature-old", false, BRANCH_ANALYZED, "b1");
        analyzed(BranchType.BRANCH, "feature-new", false, NEWER_BRANCH_ANALYZED, "b2");
        analyzed(BranchType.BRANCH, "feature-never", false, null, null);
        analyzed(BranchType.PULL_REQUEST, "12", false, PULL_REQUEST_ANALYZED, "p1");
        when(mainHeadReader.read(dbSession, project, "main")).thenReturn(Optional.of("abc123"));

        assertThat(answer()).isEqualTo("{\"projects\":[{\"key\":\"project\","
            + "\"main\":{\"name\":\"main\",\"analysisDate\":\"" + DateUtils.formatDateTime(MAIN_ANALYZED) + "\",\"revision\":\"abc123\"},"
            + "\"newestBranch\":{\"name\":\"feature-new\",\"analysisDate\":\"" + DateUtils.formatDateTime(NEWER_BRANCH_ANALYZED) + "\",\"revision\":\"b2\"},"
            + "\"newestPullRequest\":{\"name\":\"12\",\"analysisDate\":\"" + DateUtils.formatDateTime(PULL_REQUEST_ANALYZED) + "\",\"revision\":\"p1\"},"
            + "\"mainState\":\"CURRENT\"}]}");
    }

    @Test
    void saysMainMovedWhenTheForgeHeadIsNotTheAnalyzedCommit() throws IOException {
        analyzed(BranchType.BRANCH, "main", true, MAIN_ANALYZED, "abc123");
        when(mainHeadReader.read(dbSession, project, "main")).thenReturn(Optional.of("def456"));

        assertThat(answer()).contains("\"mainState\":\"MOVED\"").doesNotContain("newestBranch").doesNotContain("newestPullRequest");
    }

    @Test
    void cannotTellWhenTheForgeGivesNoHead() throws IOException {
        analyzed(BranchType.BRANCH, "main", true, MAIN_ANALYZED, "abc123");
        when(mainHeadReader.read(dbSession, project, "main")).thenReturn(Optional.empty());

        assertThat(answer()).contains("\"mainState\":\"UNKNOWN\"");
    }

    @Test
    void cannotTellWhenTheAnalysisRecordedNoRevision() throws IOException {
        analyzed(BranchType.BRANCH, "main", true, MAIN_ANALYZED, null);

        assertThat(answer()).contains("\"main\":{\"name\":\"main\",\"analysisDate\"").contains("\"mainState\":\"UNKNOWN\"");
    }

    @Test
    void cannotTellWhenMainWasNeverAnalyzed() throws IOException {
        analyzed(BranchType.BRANCH, "main", true, null, null);

        assertThat(answer()).isEqualTo("{\"projects\":[{\"key\":\"project\",\"main\":{\"name\":\"main\"},\"mainState\":\"UNKNOWN\"}]}");
    }

    @Test
    void cannotTellForAProjectWithNoMainBranch() throws IOException {
        assertThat(answer()).isEqualTo("{\"projects\":[{\"key\":\"project\",\"mainState\":\"UNKNOWN\"}]}");
    }

    @Test
    void leavesOutProjectsTheUserCannotBrowse() throws IOException {
        when(userSession.keepAuthorizedEntities(ProjectPermission.USER, List.of(project))).thenReturn(List.of());

        assertThat(answer()).isEqualTo("{\"projects\":[]}");
    }
}
