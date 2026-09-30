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
package com.haembina.branchanalysis.almclient.bitbucket.model.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

class CreateReportRequestTest {

    @Test
    void shouldSendReportCreationDate() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        CreateReportRequest report = new CreateReportRequest(List.of(), "details", "title", "reporter",
                Instant.parse("2026-09-28T10:00:00Z"), "https://sonar/dashboard", "https://sonar/logo.png", "PASS");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(report));

        assertThat(json.has("createdDate")).isTrue();
        assertThat(json.get("result").asText()).isEqualTo("PASS");
    }
}
