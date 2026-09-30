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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ServerPropertiesTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"5, false",
            "5.14.9, false",
            "5.15, true",
            "5.15.0, true",
            "5.16, true",
            "6, true"})
    void shouldOfferCodeInsightsFromVersion515WhateverNumberOfParts(String version, boolean hasCodeInsights) {
        assertThat(new ServerProperties(version).hasCodeInsightsApi()).isEqualTo(hasCodeInsights);
    }
}
