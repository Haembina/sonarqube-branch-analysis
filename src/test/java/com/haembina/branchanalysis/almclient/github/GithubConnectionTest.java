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
package com.haembina.branchanalysis.almclient.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

class GithubConnectionTest {

    private final GithubConnection connection = new GithubConnection(mock(GitHub.class), mock(GithubGraphqlClient.class), "sonarqube-app");

    @Test
    void shouldRecognizeTheAppByItsRestAndGraphqlLogins() {
        assertThat(connection.isAppLogin("sonarqube-app[bot]")).isTrue();
        assertThat(connection.isAppLogin("sonarqube-app")).isTrue();
    }

    @Test
    void shouldNotRecognizeAnotherLogin() {
        assertThat(connection.isAppLogin("other-app[bot]")).isFalse();
        assertThat(connection.isAppLogin("sonarqube-app-two")).isFalse();
    }
}
