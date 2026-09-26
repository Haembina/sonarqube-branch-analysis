/*
 * Copyright (C) 2020-2023 Michael Clarke
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
package com.haembina.branchanalysis.server.pullrequest.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.ws.WsAction;

/**
 * A web service action about one project, named by its mandatory {@code project} parameter.
 *
 * <p>{@link #handle(Request, Response)} opens a database session, resolves the project key, lets
 * {@link #checkProjectAccess(ProjectDto)} refuse the caller, and hands the project to
 * {@link #handleProjectRequest(ProjectDto, Request, Response, DbSession)}. An unknown key fails with the
 * {@code NotFoundException} {@link ComponentFinder} throws, before either hook runs.
 */
public abstract class ProjectWsActionSupport implements WsAction {

    private static final String PROJECT_PARAMETER = "project";

    private final String actionName;
    private final DbClient dbClient;
    private final ComponentFinder componentFinder;

    /**
     * @param actionName the action's name under its controller, as the web service path ends
     * @param dbClient the client each request opens its session from
     * @param componentFinder resolves the {@code project} parameter to a project
     */
    protected ProjectWsActionSupport(String actionName, DbClient dbClient, ComponentFinder componentFinder) {
        super();
        this.actionName = actionName;
        this.dbClient = dbClient;
        this.componentFinder = componentFinder;
    }

    @Override
    public void define(WebService.NewController context) {
        WebService.NewAction action = context.createAction(actionName).setHandler(this);
        action.createParam(PROJECT_PARAMETER).setRequired(true);

        configureAction(action);
    }

    /**
     * Adds the action's own parameters and description; {@code project} is already defined.
     *
     * @param action the action being defined
     */
    protected abstract void configureAction(WebService.NewAction action);

    @Override
    public void handle(Request request, Response response) {
        String projectKey = request.mandatoryParam(PROJECT_PARAMETER);

        try (DbSession dbSession = dbClient.openSession(false)) {
            ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
            checkProjectAccess(project);
            handleProjectRequest(project, request, response, dbSession);
        }
    }

    /**
     * Refuses the caller by throwing before the request is handled. Allows everyone unless overridden.
     *
     * @param project the project the request names
     */
    protected void checkProjectAccess(ProjectDto project) {
        // Every caller may reach the project unless a subclass says otherwise
    }

    /**
     * Handles the request for a project that exists and the caller may reach.
     *
     * @param project the project the request names
     * @param request the request, for the action's own parameters
     * @param response the response to write
     * @param dbSession the open session, closed once this returns
     */
    protected abstract void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession);

    /**
     * Returns the client the action's sessions come from.
     *
     * @return the client passed to the constructor
     */
    protected DbClient getDbClient() {
        return dbClient;
    }
}
