/*
 * Copyright (C) 2020-2026 Michael Clarke and the contributors to mc1arke/sonarqube-community-branch-plugin
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
package com.haembina.branchanalysis.almclient.azuredevops.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The status of a comment thread
 * Enum names match those here: https://docs.microsoft.com/en-us/rest/api/azure/devops/git/pull-request-threads/get?#commentthreadstatus
 */
public enum CommentThreadStatus {
    /**
     * The thread status is unknown.
     */
    @JsonProperty("unknown")
    UNKNOWN,
    /**
     * The thread status is active.
     */
    @JsonProperty("active")
    ACTIVE,
    /**
     * The thread status is resolved as fixed.
     */
    @JsonProperty("fixed")
    FIXED,
    /**
     * The thread status is resolved as won't fix.
     */
    @JsonProperty("wontFix")
    WONTFIX,
    /**
     * The thread status is closed.
     */
    @JsonProperty("closed")
    CLOSED,
    /**
     * The thread status is resolved as by design.
     */
    @JsonProperty("byDesign")
    BYDESIGN,
    /**
     * The thread status is pending.
     */
    @JsonProperty("pending")
    PENDING
}

