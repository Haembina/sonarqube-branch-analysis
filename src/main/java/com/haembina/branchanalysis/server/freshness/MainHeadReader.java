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
package com.haembina.branchanalysis.server.freshness;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

import com.haembina.branchanalysis.housekeeping.forge.ForgeReader;
import com.haembina.branchanalysis.housekeeping.forge.ForgeReaderFactory;

@ServerSide
public class MainHeadReader {

    static final Duration CACHE_LIFETIME = Duration.ofMinutes(10);

    private static final Logger LOGGER = LoggerFactory.getLogger(MainHeadReader.class);

    private final ForgeReaderFactory forgeReaderFactory;
    private final System2 system2;
    private final Map<String, CachedHead> cache = new ConcurrentHashMap<>();

    public MainHeadReader(ForgeReaderFactory forgeReaderFactory, System2 system2) {
        this.forgeReaderFactory = forgeReaderFactory;
        this.system2 = system2;
    }

    public Optional<String> read(DbSession dbSession, ProjectDto project, String mainBranch) {
        long now = system2.now();
        String key = project.getUuid() + "\n" + mainBranch;
        CachedHead cached = cache.get(key);
        if (cached != null && now - cached.readAt() < CACHE_LIFETIME.toMillis()) {
            return cached.head();
        }
        Optional<String> head = fetch(dbSession, project, mainBranch);
        cache.put(key, new CachedHead(head, now));
        return head;
    }

    private Optional<String> fetch(DbSession dbSession, ProjectDto project, String mainBranch) {
        try {
            Optional<ForgeReader> forge = forgeReaderFactory.forProject(dbSession, project);
            return forge.isPresent() ? forge.get().branchHead(mainBranch) : Optional.empty();
        } catch (IOException | RuntimeException ex) {
            LOGGER.debug("Could not read the head of {} in project {}: {}", mainBranch, project.getKey(), ex.getMessage());
            return Optional.empty();
        }
    }

    private record CachedHead(Optional<String> head, long readAt) {
    }
}
