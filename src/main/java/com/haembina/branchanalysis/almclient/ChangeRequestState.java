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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record ChangeRequestState(Instant closedInstant) {

    public static final ChangeRequestState OPEN = new ChangeRequestState(null);

    public static ChangeRequestState closedAt(String... timestamps) {
        return new ChangeRequestState(Arrays.stream(timestamps)
            .filter(Objects::nonNull)
            .filter(timestamp -> !timestamp.isBlank())
            .findFirst()
            .map(timestamp -> OffsetDateTime.parse(timestamp).toInstant())
            .orElse(Instant.EPOCH));
    }

    public static ChangeRequestState closedAtMillis(long epochMillis) {
        return new ChangeRequestState(Instant.ofEpochMilli(epochMillis));
    }

    public boolean isOpen() {
        return closedInstant == null;
    }

    public Optional<Instant> closedTime() {
        return Optional.ofNullable(closedInstant);
    }
}
