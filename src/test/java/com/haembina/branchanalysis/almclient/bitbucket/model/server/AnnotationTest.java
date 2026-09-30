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

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AnnotationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSendIssueMessageAsBitbucketServerMessage() throws JsonProcessingException {
        Annotation annotation = new Annotation("issue-key", 12, "https://sonar/issue", "Remove this", "src/A.java", "HIGH", "BUG");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(annotation));

        assertThat(json.get("message").asText()).isEqualTo("Remove this");
        assertThat(json.get("externalId").asText()).isEqualTo("issue-key");
        assertThat(json.get("type").asText()).isEqualTo("BUG");
    }
}
