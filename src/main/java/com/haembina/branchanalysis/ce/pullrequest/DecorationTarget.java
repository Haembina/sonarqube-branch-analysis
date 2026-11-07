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
package com.haembina.branchanalysis.ce.pullrequest;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;

record DecorationTarget(String pullRequestId, boolean branch) {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecorationTarget.class);

    static Optional<DecorationTarget> find(PostProjectAnalysisTask.ProjectAnalysis projectAnalysis, DecorationSettings decorationSettings) {
        Optional<Branch> optionalBranch = projectAnalysis.getBranch();
        if (optionalBranch.isEmpty()) {
            LOGGER.trace("Current analysis has no branch. Task being skipped");
            return Optional.empty();
        }

        Branch analysedBranch = optionalBranch.get();
        if (Branch.Type.PULL_REQUEST == analysedBranch.getType()) {
            Optional<String> optionalPullRequestId = analysedBranch.getName();
            if (optionalPullRequestId.isEmpty()) {
                LOGGER.warn("No pull request ID has been submitted with the Pull Request. Analysis will be skipped");
            }
            return optionalPullRequestId.map(pullRequestId -> new DecorationTarget(pullRequestId, false));
        }

        if (decorationSettings.isBranchDecorationEnabled()) {
            return Optional.of(new DecorationTarget(null, true));
        }
        LOGGER.trace("Current analysis is not for a Pull Request and branch decoration is off. Task being skipped");
        return Optional.empty();
    }
}
