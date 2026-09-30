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

public class CommentPosition {
    
    private final int line;
    private final int offset;

    @JsonCreator
    public CommentPosition(@JsonProperty("line") int line, @JsonProperty("offset") int offset){
        this.line = line;
        this.offset = offset;
    }
    
    /**
     *The line number of a thread's position. Starts at 1. ///
     */
    public int getLine() {
        return this.line;
    }

    /**
     *The character offset of a thread's position inside of a line. Starts at 0.
     */
    public int getOffset() {
        return this.offset;
    }
}
