/*
 * Copyright (C) 2020 Mathias Åhsberg
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
package com.haembina.branchanalysis.almclient.bitbucket.model.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ServerProperties implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CODE_INSIGHT_VERSION = "5.15";

    private final String version;

    @JsonCreator
    public ServerProperties(@JsonProperty("version") String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public boolean hasCodeInsightsApi() {
        return isAtLeast(CODE_INSIGHT_VERSION);
    }

    private boolean isAtLeast(String minimumVersion) {
        String[] current = semver(version);
        String[] minimum = semver(minimumVersion);

        for (int i = 0; i < minimum.length; i++) {
            int thisPart = i < current.length ? Integer.parseInt(current[i]) : 0;
            int thatPart = Integer.parseInt(minimum[i]);
            if (thisPart > thatPart) {
                return true;
            }
            if (thisPart < thatPart) {
                return false;
            }
        }
        return true;
    }

    private String[] semver(String v) {
        return v.split("\\.");
    }
}
