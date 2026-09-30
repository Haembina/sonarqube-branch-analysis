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
package com.haembina.branchanalysis.server.pullrequest.ws.binding.action;

import com.haembina.branchanalysis.server.pullrequest.ws.ProjectWsActionSupport;
import org.sonar.db.DbClient;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almsettings.ws.AlmSettingsWsAction;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

/**
 * An ALM binding web service action about one project, refused to a caller without its permission on the project.
 */
public abstract class ProjectWsAction extends ProjectWsActionSupport implements AlmSettingsWsAction {

    private final UserSession userSession;
    private final ProjectPermission permission;

    /**
     * An action that needs {@link ProjectPermission#ADMIN} on the project.
     */
    protected ProjectWsAction(String actionName, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        this(actionName, dbClient, componentFinder, userSession, ProjectPermission.ADMIN);
    }

    /**
     * An action that needs {@code permission} on the project.
     */
    protected ProjectWsAction(String actionName, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ProjectPermission permission) {
        super(actionName, dbClient, componentFinder);
        this.userSession = userSession;
        this.permission = permission;
    }

    /**
     * Throws {@code ForbiddenException} when the caller lacks the action's permission on the project.
     */
    @Override
    protected void checkProjectAccess(ProjectDto project) {
        userSession.checkEntityPermission(permission, project);
    }
}
