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
package com.haembina.branchanalysis.scanner.autoconfiguration;

import com.haembina.branchanalysis.scanner.BranchConfigurationFactory;
import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildkiteAutoConfigurerTest {

    private final System2 system2 = mock();
    private final BranchConfigurationFactory branchConfigurationFactory = mock();
    private final ProjectBranches projectBranches = mock();
    private final BuildkiteAutoConfigurer underTest = new BuildkiteAutoConfigurer(branchConfigurationFactory);

    @Test
    void shouldDetectNothingOutsideBuildkite() {
        when(system2.envVariable("BUILDKITE_BRANCH")).thenReturn("main");

        assertThat(underTest.detectConfiguration(system2, projectBranches)).isEmpty();
    }

    @Test
    void shouldReadFalseAsNoPullRequest() {
        when(system2.envVariable("BUILDKITE")).thenReturn("true");
        when(system2.envVariable("BUILDKITE_BRANCH")).thenReturn("develop");
        when(system2.envVariable("BUILDKITE_PULL_REQUEST")).thenReturn("false");
        BranchConfiguration branch = mock();
        when(branchConfigurationFactory.createBranchConfiguration("develop", projectBranches)).thenReturn(branch);

        assertThat(underTest.detectConfiguration(system2, projectBranches)).contains(branch);
    }

    @Test
    void shouldDetectThePullRequestWithItsBaseBranch() {
        when(system2.envVariable("BUILDKITE")).thenReturn("true");
        when(system2.envVariable("BUILDKITE_BRANCH")).thenReturn("feature");
        when(system2.envVariable("BUILDKITE_PULL_REQUEST")).thenReturn("33");
        when(system2.envVariable("BUILDKITE_PULL_REQUEST_BASE_BRANCH")).thenReturn("main");
        BranchConfiguration pullRequest = mock();
        when(branchConfigurationFactory.createPullRequestConfiguration("33", "feature", "main", projectBranches)).thenReturn(pullRequest);

        assertThat(underTest.detectConfiguration(system2, projectBranches)).contains(pullRequest);
    }
}
