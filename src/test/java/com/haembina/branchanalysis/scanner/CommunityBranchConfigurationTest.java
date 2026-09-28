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
package com.haembina.branchanalysis.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.sonar.scanner.scan.branch.BranchType;

class CommunityBranchConfigurationTest {

    @Test
    void shouldExposePullRequestItWasResolvedTo() {
        CommunityBranchConfiguration underTest = new CommunityBranchConfiguration("feature", BranchType.PULL_REQUEST, "main", "main", "42");

        assertThat(underTest.branchType()).isEqualTo(BranchType.PULL_REQUEST);
        assertThat(underTest.branchName()).isEqualTo("feature");
        assertThat(underTest.referenceBranchName()).isEqualTo("main");
        assertThat(underTest.targetBranchName()).isEqualTo("main");
        assertThat(underTest.pullRequestKey()).isEqualTo("42");
    }

    @Test
    void shouldRefusePullRequestKeyForBranch() {
        CommunityBranchConfiguration underTest = new CommunityBranchConfiguration("release", BranchType.BRANCH, "main", null, null);

        assertThatThrownBy(underTest::pullRequestKey)
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Only a branch of type PULL_REQUEST can have a Pull Request key");
    }
}
