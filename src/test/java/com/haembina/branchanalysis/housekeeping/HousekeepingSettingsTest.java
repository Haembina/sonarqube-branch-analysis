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
package com.haembina.branchanalysis.housekeeping;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.core.config.PurgeConstants;

import static org.assertj.core.api.Assertions.assertThat;

class HousekeepingSettingsTest {

    @Test
    void shouldGiveTheComputeEngineThirtyDaysBeforePurgingWhenNothingIsSet() {
        assertThat(definition(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_BRANCHES_AND_PRS).defaultValue()).isEqualTo("30");
    }

    @Test
    void shouldKeepTheUsualMainBranchNamesWhenNothingIsSet() {
        assertThat(definition(PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE).defaultValue()).isEqualTo("main,master,develop,trunk");
    }

    @Test
    void shouldKeepClosedPullRequestsAndDeletedBranchesUntilSomeoneSetsADelay() {
        assertThat(definition(HousekeepingSettings.PULL_REQUEST_DAYS_AFTER_CLOSE).defaultValue()).isEmpty();
        assertThat(definition(HousekeepingSettings.DELETED_BRANCH_DAYS).defaultValue()).isEmpty();
    }

    private static PropertyDefinition definition(String key) {
        return HousekeepingSettings.definitions().stream()
            .filter(definition -> definition.key().equals(key))
            .findFirst()
            .orElseThrow();
    }
}
