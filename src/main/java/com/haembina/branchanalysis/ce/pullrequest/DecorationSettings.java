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
package com.haembina.branchanalysis.ce.pullrequest;

import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

public class DecorationSettings {

    public static final String SUMMARY_COMMENT_SKIPPED_WHEN_CLEAN = "com.haembina.branchanalysis.pullrequest.summary-comment.skip-when-clean";
    public static final String GITHUB_ANNOTATIONS_ENABLED = "com.haembina.branchanalysis.pullrequest.github.annotations.enabled";
    public static final String BRANCH_DECORATION_ENABLED = "com.haembina.branchanalysis.branch-decoration.enabled";

    private final ConfigurationRepository configurationRepository;

    public DecorationSettings(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public static List<PropertyDefinition> definitions() {
        return List.of(
            booleanSetting(SUMMARY_COMMENT_SKIPPED_WHEN_CLEAN, false)
                .name("Skip the summary comment on a clean pass")
                .description("Post no summary comment on a pull request whose quality gate passes with no new issues. "
                    + "A summary comment already on the pull request is still updated.")
                .build(),
            booleanSetting(GITHUB_ANNOTATIONS_ENABLED, true)
                .name("Annotate GitHub check runs")
                .description("Add each new issue as an annotation on the GitHub check run. "
                    + "When disabled, the check run carries only the quality gate and the summary.")
                .build(),
            booleanSetting(BRANCH_DECORATION_ENABLED, false)
                .name("Decorate branch analyses")
                .description("Show the quality gate of a branch analysis, such as main, on its commit: "
                    + "a check run on GitHub and a commit status on GitLab.")
                .build());
    }

    public boolean isBranchDecorationEnabled() {
        return isEnabled(BRANCH_DECORATION_ENABLED, false);
    }

    public boolean isSummaryCommentSkippedWhenClean() {
        return isEnabled(SUMMARY_COMMENT_SKIPPED_WHEN_CLEAN, false);
    }

    public boolean isGithubAnnotationsEnabled() {
        return isEnabled(GITHUB_ANNOTATIONS_ENABLED, true);
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        return configurationRepository.getConfiguration().getBoolean(key).orElse(defaultValue);
    }

    private static PropertyDefinition.Builder booleanSetting(String key, boolean defaultValue) {
        return PropertyDefinition.builder(key)
            .category(CoreProperties.CATEGORY_GENERAL)
            .subCategory(CoreProperties.SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS)
            .onConfigScopes(ConfigScope.PROJECT)
            .type(PropertyType.BOOLEAN)
            .defaultValue(Boolean.toString(defaultValue));
    }
}
