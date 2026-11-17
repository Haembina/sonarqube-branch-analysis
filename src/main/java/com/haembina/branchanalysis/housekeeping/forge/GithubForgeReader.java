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
import java.util.Optional;

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import com.haembina.branchanalysis.almclient.ChangeRequestState;

final class GithubForgeReader implements ForgeReader {

    private final GHRepository repository;

    GithubForgeReader(GHRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChangeRequestState pullRequest(String key) throws IOException {
        GHPullRequest pullRequest = repository.getPullRequest(Integer.parseInt(key));
        if (pullRequest.getState() == GHIssueState.OPEN) {
            return ChangeRequestState.OPEN;
        }
        Instant closed = Optional.ofNullable(pullRequest.getMergedAt())
                .or(() -> Optional.ofNullable(pullRequest.getClosedAt()))
                .map(Date::toInstant)
                .orElse(Instant.EPOCH);
        return new ChangeRequestState(closed);
    }

    @Override
    public Optional<String> branchHead(String branch) throws IOException {
        try {
            return Optional.of(repository.getBranch(branch).getSHA1());
        } catch (GHFileNotFoundException ex) {
            return Optional.empty();
        }
    }
}
