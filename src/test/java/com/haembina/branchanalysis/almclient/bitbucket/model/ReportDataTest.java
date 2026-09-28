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

import org.junit.jupiter.api.Test;

class ReportDataTest {

    @Test
    void shouldTypeServerAndCloudLinksAsLink() {
        DataValue.Link serverLink = new DataValue.Link("Go to SonarQube", "https://sonar/dashboard");
        DataValue.CloudLink cloudLink = new DataValue.CloudLink("Go to SonarQube", "https://sonar/dashboard");

        assertThat(new ReportData("Analysis details", serverLink).getType()).isEqualTo("LINK");
        assertThat(new ReportData("Analysis details", cloudLink).getType()).isEqualTo("LINK");
        assertThat(serverLink.getLinktext()).isEqualTo("Go to SonarQube");
        assertThat(cloudLink.getText()).isEqualTo("Go to SonarQube");
    }

    @Test
    void shouldTypePercentageAndText() {
        assertThat(new ReportData("Coverage", new DataValue.Percentage(BigDecimal.ONE)).getType()).isEqualTo("PERCENTAGE");
        assertThat(new ReportData("New Issues", new DataValue.Text("1 Issue")).getType()).isEqualTo("TEXT");
    }
}
