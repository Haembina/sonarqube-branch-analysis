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
package com.haembina.branchanalysis.almclient.gitlab.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class MergeRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void shouldReadSourceAndTargetProjectsOfAForkedMergeRequest() throws JsonProcessingException {
        MergeRequest mergeRequest = objectMapper.readValue("{\"iid\":7,\"source_project_id\":11,\"target_project_id\":22,"
                + "\"web_url\":\"https://gitlab/mr/7\",\"diff_refs\":{\"base_sha\":\"b\",\"start_sha\":\"s\",\"head_sha\":\"h\"}}", MergeRequest.class);

        assertThat(mergeRequest.getSourceProjectId()).isEqualTo(11);
        assertThat(mergeRequest.getTargetProjectId()).isEqualTo(22);
        assertThat(mergeRequest.getIid()).isEqualTo(7);
    }
}
