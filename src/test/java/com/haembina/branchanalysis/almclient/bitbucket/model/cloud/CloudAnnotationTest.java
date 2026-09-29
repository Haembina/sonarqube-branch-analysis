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
package com.haembina.branchanalysis.almclient.bitbucket.model.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class CloudAnnotationTest {

    @Test
    void shouldSendIssueMessageAsBitbucketCloudSummary() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        CloudAnnotation annotation = new CloudAnnotation("issue-key", 12, "https://sonar/issue", "Remove this", "src/A.java", "HIGH", "BUG");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(annotation));

        assertThat(json.get("summary").asText()).isEqualTo("Remove this");
        assertThat(json.has("message")).isFalse();
        assertThat(json.get("external_id").asText()).isEqualTo("issue-key");
        assertThat(json.get("annotation_type").asText()).isEqualTo("BUG");
        assertThat(json.get("line").asInt()).isEqualTo(12);
    }
}
