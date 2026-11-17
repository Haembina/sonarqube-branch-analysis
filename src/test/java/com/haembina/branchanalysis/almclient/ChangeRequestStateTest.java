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
package com.haembina.branchanalysis.almclient;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeRequestStateTest {

    @Test
    void anOpenChangeRequestHasNoCloseTime() {
        assertThat(ChangeRequestState.OPEN.isOpen()).isTrue();
        assertThat(ChangeRequestState.OPEN.closedTime()).isEmpty();
    }

    @Test
    void takesTheFirstTimestampGiven() {
        ChangeRequestState state = ChangeRequestState.closedAt(null, " ", "2026-10-01T12:00:00+02:00", "2026-10-02T00:00:00Z");

        assertThat(state.isOpen()).isFalse();
        assertThat(state.closedTime()).contains(Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    void aClosedChangeRequestWithNoTimestampClosedLongAgo() {
        assertThat(ChangeRequestState.closedAt().closedTime()).contains(Instant.EPOCH);
    }

    @Test
    void readsEpochMilliseconds() {
        assertThat(ChangeRequestState.closedAtMillis(1_000L).closedTime()).contains(Instant.ofEpochMilli(1_000L));
    }
}
