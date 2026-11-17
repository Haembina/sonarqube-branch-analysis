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
package com.haembina.branchanalysis.housekeeping;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.setting.ProjectConfigurationLoader;

import com.haembina.branchanalysis.almclient.ChangeRequestState;
import com.haembina.branchanalysis.housekeeping.forge.ForgeReader;
import com.haembina.branchanalysis.housekeeping.forge.ForgeReaderFactory;

@ServerSide
public class AnalysisCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisCleaner.class);

    private final DbClient dbClient;
    private final ProjectConfigurationLoader projectConfigurationLoader;
    private final ForgeReaderFactory forgeReaderFactory;
    private final ComponentCleanerService componentCleanerService;
    private final System2 system2;

    public AnalysisCleaner(DbClient dbClient, ProjectConfigurationLoader projectConfigurationLoader, ForgeReaderFactory forgeReaderFactory,
                           ComponentCleanerService componentCleanerService, System2 system2) {
        this.dbClient = dbClient;
        this.projectConfigurationLoader = projectConfigurationLoader;
        this.forgeReaderFactory = forgeReaderFactory;
        this.componentCleanerService = componentCleanerService;
        this.system2 = system2;
    }

    public void cleanAll() {
        try (DbSession dbSession = dbClient.openSession(false)) {
            for (ProjectDto project : dbClient.projectDao().selectProjects(dbSession)) {
                cleanQuietly(dbSession, project);
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not clean branch and pull request analyses: {}", ex.getMessage());
        }
    }

    private void cleanQuietly(DbSession dbSession, ProjectDto project) {
        try {
            clean(dbSession, project);
        } catch (IOException | RuntimeException ex) {
            LOGGER.warn("Could not clean the analyses of project {}: {}", project.getKey(), ex.getMessage());
        }
    }

    void clean(DbSession dbSession, ProjectDto project) throws IOException {
        Configuration configuration = projectConfigurationLoader.loadProjectConfiguration(dbSession, project.getUuid());
        Optional<Integer> pullRequestDays = configuration.getInt(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE);
        Optional<Integer> branchDays = configuration.getInt(HousekeepingSettings.DELETED_BRANCH_DAYS);
        if (pullRequestDays.isEmpty() && branchDays.isEmpty()) {
            return;
        }
        Optional<ForgeReader> forge = forgeReaderFactory.forProject(dbSession, project);
        if (forge.isEmpty()) {
            return;
        }
        Instant now = Instant.ofEpochMilli(system2.now());
        List<BranchDto> branches = dbClient.branchDao().selectByProject(dbSession, project).stream()
            .filter(branch -> !branch.isMain())
            .toList();
        for (BranchDto branch : branches) {
            if (isDue(dbSession, branch, forge.get(), now, pullRequestDays, branchDays)) {
                LOGGER.info("Deleting the analysis of {} {} in project {}", branch.getBranchType(), branch.getKey(), project.getKey());
                componentCleanerService.deleteBranch(dbSession, branch);
            }
        }
    }

    private boolean isDue(DbSession dbSession, BranchDto branch, ForgeReader forge, Instant now,
                          Optional<Integer> pullRequestDays, Optional<Integer> branchDays) throws IOException {
        if (branch.getBranchType() == BranchType.PULL_REQUEST) {
            return pullRequestDays.isPresent() && isClosedFor(forge.pullRequest(branch.getKey()), now, pullRequestDays.get());
        }
        return branchDays.isPresent() && !branch.isExcludeFromPurge()
            && lastAnalysis(dbSession, branch).plus(Duration.ofDays(branchDays.get())).compareTo(now) <= 0
            && forge.branchHead(branch.getKey()).isEmpty();
    }

    private static boolean isClosedFor(ChangeRequestState state, Instant now, int days) {
        return state.closedTime().map(closed -> closed.plus(Duration.ofDays(days)).compareTo(now) <= 0).orElse(false);
    }

    private Instant lastAnalysis(DbSession dbSession, BranchDto branch) {
        return dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, branch.getUuid())
            .map(SnapshotDto::getCreatedAt)
            .map(Instant::ofEpochMilli)
            .orElse(Instant.EPOCH);
    }
}
