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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class ErrorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LinkedHashSet<ErrorMessage> errors;

    public ErrorResponse(@JsonProperty("errors") Set<ErrorMessage> errors) {
        this.errors = Optional.ofNullable(errors).map(LinkedHashSet::new).orElse(null);
    }

    public Set<ErrorMessage> getErrors() {
        return Optional.ofNullable(errors).map(Collections::unmodifiableSet).orElse(null);
    }

    public static class ErrorMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String message;

        public ErrorMessage(@JsonProperty("message") String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }
    }
}

