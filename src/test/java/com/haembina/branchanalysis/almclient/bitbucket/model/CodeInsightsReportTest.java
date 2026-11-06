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
package com.haembina.branchanalysis.almclient.bitbucket.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class CodeInsightsReportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSendEveryFieldOfTheReportAndItsData() throws JsonProcessingException {
        CodeInsightsReport report = new CodeInsightsReport(
                List.of(new ReportData("Coverage", new Percentage(new BigDecimal("12.5"))), new ReportData("New Issues", new Text("3 Issues"))),
                "details", "title", "reporter", "https://sonar/dashboard", "PASS");

        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(report))).isEqualTo(objectMapper.readTree("""
                {"data": [{"title": "Coverage", "value": 12.5, "type": "PERCENTAGE"},
                          {"title": "New Issues", "value": "3 Issues", "type": "TEXT"}],
                 "details": "details", "title": "title", "reporter": "reporter",
                 "link": "https://sonar/dashboard", "result": "PASS"}
                """));
    }
}
