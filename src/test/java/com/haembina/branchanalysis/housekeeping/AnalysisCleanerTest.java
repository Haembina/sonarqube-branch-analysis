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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.setting.ProjectConfigurationLoader;

import com.haembina.branchanalysis.almclient.ChangeRequestState;
import com.haembina.branchanalysis.housekeeping.forge.ForgeReader;
import com.haembina.branchanalysis.housekeeping.forge.ForgeReaderFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalysisCleanerTest {

    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");

    private final DbClient dbClient = mock();
    private final DbSession dbSession = mock();
    private final ProjectDao projectDao = mock();
    private final BranchDao branchDao = mock();
    private final SnapshotDao snapshotDao = mock();
    private final ProjectConfigurationLoader configurationLoader = mock();
    private final ForgeReaderFactory forgeReaderFactory = mock();
    private final ForgeReader forge = mock();
    private final ComponentCleanerService cleanerService = mock();
    private final System2 system2 = mock();
    private final ProjectDto project = new ProjectDto().setUuid("project-uuid").setKey("project");
    private final MapSettings settings = new MapSettings();
    private final AnalysisCleaner underTest = new AnalysisCleaner(dbClient, configurationLoader, forgeReaderFactory, cleanerService, system2);

    @BeforeEach
    void setUp() throws IOException {
        when(dbClient.openSession(false)).thenReturn(dbSession);
        when(dbClient.projectDao()).thenReturn(projectDao);
        when(dbClient.branchDao()).thenReturn(branchDao);
        when(dbClient.snapshotDao()).thenReturn(snapshotDao);
        when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project));
        when(configurationLoader.loadProjectConfiguration(dbSession, "project-uuid")).thenReturn(settings.asConfig());
        when(forgeReaderFactory.forProject(dbSession, project)).thenReturn(Optional.of(forge));
        when(system2.now()).thenReturn(NOW.toEpochMilli());
    }

    private static BranchDto pullRequest(String key) {
        return new BranchDto().setUuid("pr-" + key).setKey(key).setBranchType(BranchType.PULL_REQUEST).setIsMain(false);
    }

    private static BranchDto branch(String name) {
        return new BranchDto().setUuid("branch-" + name).setKey(name).setBranchType(BranchType.BRANCH).setIsMain(false);
    }

    private void analyzedDaysAgo(BranchDto branch, int days) {
        SnapshotDto snapshot = new SnapshotDto().setCreatedAt(NOW.minus(Duration.ofDays(days)).toEpochMilli());
        when(snapshotDao.selectLastAnalysisByRootComponentUuid(dbSession, branch.getUuid())).thenReturn(Optional.of(snapshot));
    }

    private void branches(BranchDto... branches) {
        when(branchDao.selectByProject(dbSession, project)).thenReturn(List.of(branches));
    }

    @Test
    void asksNoForgeWhenNeitherDelayIsSet() {
        underTest.cleanAll();

        verifyNoInteractions(forgeReaderFactory, cleanerService);
    }

    @Test
    void deletesNothingForAProjectBoundToNoForge() throws IOException {
        settings.setProperty(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE, 1);
        when(forgeReaderFactory.forProject(dbSession, project)).thenReturn(Optional.empty());

        underTest.cleanAll();

        verifyNoInteractions(cleanerService, branchDao);
    }

    @Test
    void deletesAPullRequestClosedLongerThanTheDelay() throws IOException {
        settings.setProperty(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE, 1);
        BranchDto merged = pullRequest("12");
        BranchDto open = pullRequest("13");
        BranchDto justClosed = pullRequest("14");
        branches(merged, open, justClosed);
        when(forge.pullRequest("12")).thenReturn(new ChangeRequestState(NOW.minus(Duration.ofDays(3))));
        when(forge.pullRequest("13")).thenReturn(ChangeRequestState.OPEN);
        when(forge.pullRequest("14")).thenReturn(new ChangeRequestState(NOW.minus(Duration.ofHours(12))));

        underTest.cleanAll();

        verify(cleanerService).deleteBranch(dbSession, merged);
        verify(cleanerService, never()).deleteBranch(dbSession, open);
        verify(cleanerService, never()).deleteBranch(dbSession, justClosed);
    }

    @Test
    void deletesAPullRequestTheMomentItClosesWithADelayOfZero() throws IOException {
        settings.setProperty(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE, 0);
        BranchDto closed = pullRequest("12");
        branches(closed);
        when(forge.pullRequest("12")).thenReturn(new ChangeRequestState(NOW));

        underTest.cleanAll();

        verify(cleanerService).deleteBranch(dbSession, closed);
    }

    @Test
    void keepsPullRequestsWhenOnlyTheBranchDelayIsSet() throws IOException {
        settings.setProperty(HousekeepingSettings.DELETED_BRANCH_DAYS, 1);
        branches(pullRequest("12"));

        underTest.cleanAll();

        verify(forge, never()).pullRequest(any());
        verifyNoInteractions(cleanerService);
    }

    @Test
    void deletesABranchGoneFromTheForgeAndIdleLongerThanTheDelay() throws IOException {
        settings.setProperty(HousekeepingSettings.DELETED_BRANCH_DAYS, 7);
        BranchDto gone = branch("gone");
        BranchDto alive = branch("alive");
        BranchDto recent = branch("recent");
        BranchDto neverAnalyzed = branch("never-analyzed");
        branches(gone, alive, recent, neverAnalyzed);
        analyzedDaysAgo(gone, 10);
        analyzedDaysAgo(alive, 10);
        analyzedDaysAgo(recent, 2);
        when(snapshotDao.selectLastAnalysisByRootComponentUuid(dbSession, neverAnalyzed.getUuid())).thenReturn(Optional.empty());
        when(forge.branchHead("gone")).thenReturn(Optional.empty());
        when(forge.branchHead("alive")).thenReturn(Optional.of("abc123"));
        when(forge.branchHead("never-analyzed")).thenReturn(Optional.empty());

        underTest.cleanAll();

        verify(cleanerService).deleteBranch(dbSession, gone);
        verify(cleanerService).deleteBranch(dbSession, neverAnalyzed);
        verify(cleanerService, never()).deleteBranch(dbSession, alive);
        verify(cleanerService, never()).deleteBranch(dbSession, recent);
        verify(forge, never()).branchHead("recent");
    }

    @Test
    void deletesAGoneBranchOnTheDayItsDelayRunsOut() throws IOException {
        settings.setProperty(HousekeepingSettings.DELETED_BRANCH_DAYS, 7);
        BranchDto due = branch("due");
        branches(due);
        analyzedDaysAgo(due, 7);
        when(forge.branchHead("due")).thenReturn(Optional.empty());

        underTest.cleanAll();

        verify(cleanerService).deleteBranch(dbSession, due);
    }

    @Test
    void keepsBranchesKeptWhenInactiveAndTheMainBranch() throws IOException {
        settings.setProperty(HousekeepingSettings.DELETED_BRANCH_DAYS, 0);
        settings.setProperty(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE, 0);
        BranchDto kept = branch("release-1").setExcludeFromPurge(true);
        BranchDto main = branch("main").setIsMain(true);
        branches(kept, main);

        underTest.cleanAll();

        verify(forge, never()).branchHead(any());
        verifyNoInteractions(cleanerService);
    }

    @Test
    void keepsBranchesWhenOnlyThePullRequestDelayIsSet() throws IOException {
        settings.setProperty(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE, 1);
        branches(branch("gone"));

        underTest.cleanAll();

        verify(forge, never()).branchHead(any());
        verifyNoInteractions(cleanerService);
    }

    @Test
    void goesOnToTheNextProjectWhenAForgeFails() throws IOException {
        ProjectDto other = new ProjectDto().setUuid("other-uuid").setKey("other");
        when(projectDao.selectProjects(dbSession)).thenReturn(List.of(project, other));
        settings.setProperty(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE, 0);
        when(configurationLoader.loadProjectConfiguration(dbSession, "other-uuid")).thenReturn(settings.asConfig());
        when(forgeReaderFactory.forProject(dbSession, project)).thenThrow(new IOException("forge down"));
        ForgeReader otherForge = mock();
        when(forgeReaderFactory.forProject(dbSession, other)).thenReturn(Optional.of(otherForge));
        BranchDto closed = pullRequest("3");
        when(branchDao.selectByProject(dbSession, other)).thenReturn(List.of(closed));
        when(otherForge.pullRequest("3")).thenReturn(new ChangeRequestState(NOW));

        underTest.cleanAll();

        verify(cleanerService).deleteBranch(dbSession, closed);
    }

    @Test
    void survivesADatabaseThatCannotListTheProjects() {
        when(projectDao.selectProjects(dbSession)).thenThrow(new IllegalStateException("database down"));

        underTest.cleanAll();

        verifyNoInteractions(cleanerService);
        verify(dbSession).close();
    }
}
