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
package com.haembina.branchanalysis.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class CommunityComponentKeyTest {

    @Test
    void shouldExposeTheKeyAndBranchNameOfABranch() {
        CommunityComponentKey underTest = new CommunityComponentKey("project", "release-1.1", null);

        assertThat(underTest.getKey()).isEqualTo("project");
        assertThat(underTest.getBranchName()).isEqualTo(Optional.of("release-1.1"));
        assertThat(underTest.getPullRequestKey()).isEmpty();
    }

    @Test
    void shouldExposeTheKeyAndPullRequestKeyOfAPullRequest() {
        CommunityComponentKey underTest = new CommunityComponentKey("project", null, "42");

        assertThat(underTest.getKey()).isEqualTo("project");
        assertThat(underTest.getBranchName()).isEmpty();
        assertThat(underTest.getPullRequestKey()).isEqualTo(Optional.of("42"));
    }

}
