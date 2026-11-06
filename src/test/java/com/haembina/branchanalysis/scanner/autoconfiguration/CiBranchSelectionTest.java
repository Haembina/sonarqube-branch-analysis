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
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CiBranchSelectionTest {

    private final BranchConfigurationFactory branchConfigurationFactory = mock();
    private final ProjectBranches projectBranches = mock();

    @Test
    void shouldSelectThePullRequestWhenAKeyIsGiven() {
        BranchConfiguration pullRequest = mock();
        when(branchConfigurationFactory.createPullRequestConfiguration("7", "feature", "main", projectBranches)).thenReturn(pullRequest);

        assertThat(CiBranchSelection.select(branchConfigurationFactory, projectBranches, "7", "feature", " main ", "main"))
                .contains(pullRequest);
    }

    @Test
    void shouldPassNoBaseWhenTheBaseIsBlank() {
        BranchConfiguration pullRequest = mock();
        when(branchConfigurationFactory.createPullRequestConfiguration("7", "feature", null, projectBranches)).thenReturn(pullRequest);

        assertThat(CiBranchSelection.select(branchConfigurationFactory, projectBranches, "7", "feature", " ", null))
                .contains(pullRequest);
    }

    @Test
    void shouldSelectTheBranchWhenNoPullRequestKeyIsGiven() {
        BranchConfiguration branch = mock();
        when(branchConfigurationFactory.createBranchConfiguration("develop", projectBranches)).thenReturn(branch);

        assertThat(CiBranchSelection.select(branchConfigurationFactory, projectBranches, "", null, null, "develop"))
                .contains(branch);
    }

    @Test
    void shouldSelectNothingWithNeitherAPullRequestNorABranch() {
        assertThat(CiBranchSelection.select(branchConfigurationFactory, projectBranches, null, null, null, null)).isEmpty();
        verifyNoInteractions(branchConfigurationFactory);
    }
}
