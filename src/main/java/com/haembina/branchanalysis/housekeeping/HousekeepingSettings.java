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

import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;
import org.sonar.core.config.PurgeConstants;

public final class HousekeepingSettings {

    private HousekeepingSettings() {
    }

    public static List<PropertyDefinition> definitions() {
        return List.of(
            PropertyDefinition.builder(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_BRANCHES_AND_PRS)
                .name("Number of days before purging inactive branches and pull requests")
                .description("Branches and pull requests are permanently deleted when there has been no analysis for the configured number of days.")
                .category(CoreProperties.CATEGORY_HOUSEKEEPING)
                .subCategory(CoreProperties.SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS)
                .defaultValue("30")
                .type(PropertyType.INTEGER)
                .index(1)
                .build(),
            PropertyDefinition.builder(PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE)
                .name("Branches to keep when inactive")
                .description("By default, branches and pull requests are automatically deleted when inactive. This setting allows you "
                    + "to protect branches (but not pull requests) from this deletion. When a branch is created with a name that "
                    + "matches any of the regular expressions on the list of values of this setting, the branch will not be deleted "
                    + "automatically even when it becomes inactive. Example:"
                    + "<ul><li>develop</li><li>release-.*</li></ul>")
                .category(CoreProperties.CATEGORY_HOUSEKEEPING)
                .subCategory(CoreProperties.SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS)
                .multiValues(true)
                .defaultValue("main,master,develop,trunk")
                .onConfigScopes(ConfigScope.PROJECT)
                .index(2)
                .build());
    }
}
