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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;

class GiteaSettingsTest {

    @Test
    void definesAGlobalServerAndTokenAndAProjectRepository() {
        List<PropertyDefinition> definitions = GiteaSettings.definitions();

        assertThat(definitions).extracting(PropertyDefinition::key, PropertyDefinition::type, PropertyDefinition::configScopes)
                .containsExactly(
                        tuple(GiteaSettings.URL, PropertyType.STRING, List.of()),
                        tuple(GiteaSettings.TOKEN, PropertyType.PASSWORD, List.of()),
                        tuple(GiteaSettings.REPOSITORY, PropertyType.STRING, List.of(ConfigScope.PROJECT)));
    }
}
