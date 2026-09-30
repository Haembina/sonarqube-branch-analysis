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
package com.haembina.branchanalysis.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Status context that uniquely identifies the status.
 */
public class GitStatusContext {
    
    private final String name;
    private final String genre;

    @JsonCreator
    public GitStatusContext(@JsonProperty("genre") String genre, @JsonProperty("name") String name){
        this.genre = genre;
        this.name = name;
    }

    /**
     *  Genre of the status. Typically name of the service/tool generating the status, can be empty.
     */
    public String getGenre() {
        return this.genre;
    }

    /**
     * Name identifier of the status, cannot be null or empty.
     */
    public String getName() {
        return this.name;
    }
}
