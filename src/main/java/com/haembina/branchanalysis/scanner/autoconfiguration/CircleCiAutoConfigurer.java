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

import com.haembina.branchanalysis.scanner.BranchAutoConfigurer;
import com.haembina.branchanalysis.scanner.BranchConfigurationFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import java.util.Optional;

public class CircleCiAutoConfigurer implements BranchAutoConfigurer {

    private final BranchConfigurationFactory branchConfigurationFactory;

    public CircleCiAutoConfigurer(BranchConfigurationFactory branchConfigurationFactory) {
        this.branchConfigurationFactory = branchConfigurationFactory;
    }

    @Override
    public Optional<BranchConfiguration> detectConfiguration(System2 system2, ProjectBranches projectBranches) {
        if (!Boolean.parseBoolean(system2.envVariable("CIRCLECI"))) {
            return Optional.empty();
        }
        String pullRequestKey = StringUtils.trimToNull(
                StringUtils.substringAfterLast(Strings.CS.removeEnd(system2.envVariable("CIRCLE_PULL_REQUEST"), "/"), "/"));
        String branch = system2.envVariable("CIRCLE_BRANCH");
        return CiBranchSelection.select(branchConfigurationFactory, projectBranches, pullRequestKey, branch, null, branch);
    }
}
