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
package com.haembina.branchanalysis.housekeeping.forge;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import com.haembina.branchanalysis.almclient.ChangeRequestState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GithubForgeReaderTest {

    private static final Instant MERGED = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant CLOSED = Instant.parse("2026-10-02T10:00:00Z");

    private final GHRepository repository = mock();
    private final GithubForgeReader underTest = new GithubForgeReader(repository);

    private GHPullRequest pullRequest(GHIssueState state, Instant merged, Instant closed) throws IOException {
        GHPullRequest pullRequest = mock();
        when(pullRequest.getState()).thenReturn(state);
        when(pullRequest.getMergedAt()).thenReturn(merged == null ? null : Date.from(merged));
        when(pullRequest.getClosedAt()).thenReturn(closed == null ? null : Date.from(closed));
        when(repository.getPullRequest(12)).thenReturn(pullRequest);
        return pullRequest;
    }

    @Test
    void readsAnOpenPullRequestAsOpen() throws IOException {
        pullRequest(GHIssueState.OPEN, null, null);

        assertThat(underTest.pullRequest("12")).isEqualTo(ChangeRequestState.OPEN);
    }

    @Test
    void readsAMergedPullRequestAsClosedWhenItMerged() throws IOException {
        pullRequest(GHIssueState.CLOSED, MERGED, CLOSED);

        assertThat(underTest.pullRequest("12").closedTime()).contains(MERGED);
    }

    @Test
    void readsAClosedPullRequestAsClosedWhenItClosed() throws IOException {
        pullRequest(GHIssueState.CLOSED, null, CLOSED);

        assertThat(underTest.pullRequest("12").closedTime()).contains(CLOSED);
    }

    @Test
    void readsAClosedPullRequestWithNoDateAsClosedLongAgo() throws IOException {
        pullRequest(GHIssueState.CLOSED, null, null);

        assertThat(underTest.pullRequest("12").closedTime()).contains(Instant.EPOCH);
    }

    @Test
    void readsTheCommitABranchPointsAt() throws IOException {
        GHBranch branch = mock();
        when(branch.getSHA1()).thenReturn("abc123");
        when(repository.getBranch("main")).thenReturn(branch);

        assertThat(underTest.branchHead("main")).contains("abc123");
    }

    @Test
    void readsNoCommitForADeletedBranch() throws IOException {
        when(repository.getBranch("gone")).thenThrow(new GHFileNotFoundException("Branch not found"));

        assertThat(underTest.branchHead("gone")).isEmpty();
    }
}
