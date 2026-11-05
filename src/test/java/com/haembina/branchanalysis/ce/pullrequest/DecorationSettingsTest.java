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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

class DecorationSettingsTest {

    private final Configuration configuration = mock();
    private final ConfigurationRepository configurationRepository = mock();
    private final DecorationSettings decorationSettings = new DecorationSettings(configurationRepository);

    @BeforeEach
    void setUp() {
        when(configurationRepository.getConfiguration()).thenReturn(configuration);
        when(configuration.getBoolean(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void shouldKeepTodaysDecorationWhenNothingIsSet() {
        assertThat(decorationSettings.isSummaryCommentSkippedWhenClean()).isFalse();
        assertThat(decorationSettings.isGithubAnnotationsEnabled()).isTrue();
        assertThat(decorationSettings.isBranchDecorationEnabled()).isFalse();
    }

    @Test
    void shouldReadEachSettingFromTheProject() {
        when(configuration.getBoolean(DecorationSettings.SUMMARY_COMMENT_SKIPPED_WHEN_CLEAN)).thenReturn(Optional.of(true));
        when(configuration.getBoolean(DecorationSettings.GITHUB_ANNOTATIONS_ENABLED)).thenReturn(Optional.of(false));
        when(configuration.getBoolean(DecorationSettings.BRANCH_DECORATION_ENABLED)).thenReturn(Optional.of(true));

        assertThat(decorationSettings.isSummaryCommentSkippedWhenClean()).isTrue();
        assertThat(decorationSettings.isGithubAnnotationsEnabled()).isFalse();
        assertThat(decorationSettings.isBranchDecorationEnabled()).isTrue();
    }

    @Test
    void shouldDefineEachSettingWithTodaysBehaviorAsItsDefault() {
        assertThat(DecorationSettings.definitions())
            .extracting(PropertyDefinition::key, PropertyDefinition::defaultValue)
            .containsExactly(
                tuple(DecorationSettings.SUMMARY_COMMENT_SKIPPED_WHEN_CLEAN, "false"),
                tuple(DecorationSettings.GITHUB_ANNOTATIONS_ENABLED, "true"),
                tuple(DecorationSettings.BRANCH_DECORATION_ENABLED, "false"));
    }
}
