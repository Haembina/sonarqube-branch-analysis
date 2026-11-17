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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

import com.haembina.branchanalysis.housekeeping.forge.ForgeReader;
import com.haembina.branchanalysis.housekeeping.forge.ForgeReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MainHeadReaderTest {

    private static final long START = 1_000_000L;

    private final ForgeReaderFactory forgeReaderFactory = mock();
    private final ForgeReader forge = mock();
    private final System2 system2 = mock();
    private final DbSession dbSession = mock();
    private final ProjectDto project = new ProjectDto().setUuid("project-uuid").setKey("project");
    private final MainHeadReader underTest = new MainHeadReader(forgeReaderFactory, system2);

    @Test
    void asksTheForgeOnceInTenMinutes() throws IOException {
        when(forgeReaderFactory.forProject(dbSession, project)).thenReturn(Optional.of(forge));
        when(forge.branchHead("main")).thenReturn(Optional.of("abc123")).thenReturn(Optional.of("def456"));
        when(system2.now()).thenReturn(START, START + MainHeadReader.CACHE_LIFETIME.toMillis() - 1, START + MainHeadReader.CACHE_LIFETIME.toMillis());

        assertThat(underTest.read(dbSession, project, "main")).contains("abc123");
        assertThat(underTest.read(dbSession, project, "main")).contains("abc123");
        assertThat(underTest.read(dbSession, project, "main")).contains("def456");
        verify(forge, times(2)).branchHead("main");
    }

    @Test
    void readsNoHeadForAProjectBoundToNoForge() throws IOException {
        when(forgeReaderFactory.forProject(dbSession, project)).thenReturn(Optional.empty());
        when(system2.now()).thenReturn(START);

        assertThat(underTest.read(dbSession, project, "main")).isEmpty();
    }

    @Test
    void readsNoHeadWhenTheForgeFails() throws IOException {
        when(forgeReaderFactory.forProject(dbSession, project)).thenReturn(Optional.of(forge));
        when(forge.branchHead("main")).thenThrow(new IOException("forge down"));
        when(system2.now()).thenReturn(START);

        assertThat(underTest.read(dbSession, project, "main")).isEmpty();
    }
}
