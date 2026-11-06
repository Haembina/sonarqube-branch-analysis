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
package com.haembina.branchanalysis.almclient.gitea;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;

public final class GiteaSettings {

    public static final String URL = "com.haembina.branchanalysis.gitea.url";
    public static final String TOKEN = "com.haembina.branchanalysis.gitea.token.secured";
    public static final String REPOSITORY = "com.haembina.branchanalysis.gitea.repository";

    private static final String CATEGORY = "Gitea";

    private GiteaSettings() {
    }

    public static List<PropertyDefinition> definitions() {
        return List.of(
                PropertyDefinition.builder(URL)
                        .category(CATEGORY)
                        .name("Gitea server URL")
                        .description("The Gitea or Forgejo server that pull requests of projects with a Gitea repository are decorated on, such as https://gitea.example.com.")
                        .type(PropertyType.STRING)
                        .index(1)
                        .build(),
                PropertyDefinition.builder(TOKEN)
                        .category(CATEGORY)
                        .name("Gitea access token")
                        .description("A token of the user that posts the commit status and the summary comment, with write access to repositories and issues.")
                        .type(PropertyType.PASSWORD)
                        .index(2)
                        .build(),
                PropertyDefinition.builder(REPOSITORY)
                        .category(CATEGORY)
                        .name("Gitea repository")
                        .description("The repository as owner/name. Pull requests of a project with no DevOps platform binding are decorated on it.")
                        .type(PropertyType.STRING)
                        .onlyOnConfigScopes(ConfigScope.PROJECT)
                        .index(3)
                        .build());
    }
}
