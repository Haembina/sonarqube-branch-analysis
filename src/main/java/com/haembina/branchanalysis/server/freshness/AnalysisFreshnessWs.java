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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.user.UserSession;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class AnalysisFreshnessWs implements WebService, RequestHandler {

    static final String PROJECTS_PARAMETER = "projects";
    static final int MAX_PROJECTS = 100;

    private final DbClient dbClient;
    private final UserSession userSession;
    private final MainHeadReader mainHeadReader;
    private final ObjectMapper objectMapper = JsonMapper.builder()
        .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS))
        .build();

    public AnalysisFreshnessWs(DbClient dbClient, UserSession userSession, MainHeadReader mainHeadReader) {
        this.dbClient = dbClient;
        this.userSession = userSession;
        this.mainHeadReader = mainHeadReader;
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController("api/analysis_freshness")
            .setDescription("When each project's main branch, branches and pull requests were last analyzed, and whether main has moved since.");
        controller.createAction("list")
            .setDescription("Lists the newest analysis of main, of any branch and of any pull request for each project, "
                + "and whether the forge's main has commits its last analysis did not see.")
            .setHandler(this)
            .createParam(PROJECTS_PARAMETER)
            .setDescription("Comma-separated project keys")
            .setRequired(true)
            .setMaxValuesAllowed(MAX_PROJECTS);
        controller.done();
    }

    @Override
    public void handle(Request request, Response response) throws IOException {
        List<String> keys = request.mandatoryParamAsStrings(PROJECTS_PARAMETER);
        List<ProjectFreshness> projects;
        try (DbSession dbSession = dbClient.openSession(false)) {
            List<ProjectDto> visible = userSession.keepAuthorizedEntities(ProjectPermission.USER, dbClient.projectDao().selectProjectsByKeys(dbSession, keys));
            projects = visible.stream().map(project -> freshness(dbSession, project)).toList();
        }
        Response.Stream stream = response.stream();
        stream.setMediaType("application/json");
        stream.setStatus(200);
        try (OutputStream output = stream.output()) {
            objectMapper.writeValue(output, new FreshnessResponse(projects));
        }
    }

    private ProjectFreshness freshness(DbSession dbSession, ProjectDto project) {
        Collection<BranchDto> branches = dbClient.branchDao().selectByProject(dbSession, project);
        Map<String, SnapshotDto> analyses = dbClient.snapshotDao()
            .selectLastAnalysesByRootComponentUuids(dbSession, branches.stream().map(BranchDto::getUuid).toList()).stream()
            .collect(Collectors.toMap(SnapshotDto::getRootComponentUuid, Function.identity()));
        Optional<BranchDto> main = branches.stream().filter(BranchDto::isMain).findFirst();
        Analysis mainAnalysis = main.map(branch -> analysis(branch, analyses)).orElse(null);
        return new ProjectFreshness(project.getKey(), mainAnalysis,
            newest(branches, analyses, BranchType.BRANCH), newest(branches, analyses, BranchType.PULL_REQUEST),
            main.flatMap(branch -> mainState(dbSession, project, branch, analyses.get(branch.getUuid()))).orElse(MainState.UNKNOWN));
    }

    private Optional<MainState> mainState(DbSession dbSession, ProjectDto project, BranchDto main, SnapshotDto analysis) {
        return Optional.ofNullable(analysis).map(SnapshotDto::getRevision)
            .flatMap(revision -> mainHeadReader.read(dbSession, project, main.getKey())
                .map(head -> head.equals(revision) ? MainState.CURRENT : MainState.MOVED));
    }

    private static Analysis newest(Collection<BranchDto> branches, Map<String, SnapshotDto> analyses, BranchType type) {
        return branches.stream()
            .filter(branch -> !branch.isMain() && branch.getBranchType() == type && analyses.containsKey(branch.getUuid()))
            .max(Comparator.comparing(branch -> analyses.get(branch.getUuid()).getAnalysisDate()))
            .map(branch -> analysis(branch, analyses))
            .orElse(null);
    }

    private static Analysis analysis(BranchDto branch, Map<String, SnapshotDto> analyses) {
        SnapshotDto snapshot = analyses.get(branch.getUuid());
        return snapshot == null ? new Analysis(branch.getKey(), null, null)
            : new Analysis(branch.getKey(), DateUtils.formatDateTime(snapshot.getAnalysisDate()), snapshot.getRevision());
    }

    record FreshnessResponse(List<ProjectFreshness> projects) {
    }

    record ProjectFreshness(String key, Analysis main, Analysis newestBranch, Analysis newestPullRequest, MainState mainState) {
    }

    enum MainState {
        CURRENT, MOVED, UNKNOWN
    }

    record Analysis(String name, String analysisDate, String revision) {
    }
}
