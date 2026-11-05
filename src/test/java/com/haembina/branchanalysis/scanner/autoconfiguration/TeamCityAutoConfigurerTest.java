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
package com.haembina.branchanalysis.scanner.autoconfiguration;

import com.haembina.branchanalysis.scanner.BranchConfigurationFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamCityAutoConfigurerTest {

    private final System2 system2 = mock();
    private final BranchConfigurationFactory branchConfigurationFactory = mock();
    private final ProjectBranches projectBranches = mock();
    private final TeamCityAutoConfigurer underTest = new TeamCityAutoConfigurer(branchConfigurationFactory);

    @TempDir
    private Path directory;

    @Test
    void shouldDetectNothingOutsideTeamCity() {
        assertThat(underTest.detectConfiguration(system2, projectBranches)).isEmpty();
    }

    @Test
    void shouldDetectNothingWithoutABuildPropertiesFile() {
        when(system2.envVariable("TEAMCITY_VERSION")).thenReturn("2026.1");

        assertThat(underTest.detectConfiguration(system2, projectBranches)).isEmpty();
    }

    @Test
    void shouldDetectNothingWhenTheBuildPropertiesFileCannotBeRead() {
        runInTeamCity(directory.resolve("missing.properties"));

        assertThat(underTest.detectConfiguration(system2, projectBranches)).isEmpty();
        verifyNoInteractions(branchConfigurationFactory);
    }

    @Test
    void shouldDetectTheBranchFromTheBuildPropertiesAlone() throws IOException {
        runInTeamCity(write("build.properties", "teamcity.build.branch=develop"));
        BranchConfiguration branch = mock();
        when(branchConfigurationFactory.createBranchConfiguration("develop", projectBranches)).thenReturn(branch);

        assertThat(underTest.detectConfiguration(system2, projectBranches)).contains(branch);
    }

    @Test
    void shouldDetectThePullRequestFromTheConfigurationParameters() throws IOException {
        Path configuration = write("configuration.properties", """
                teamcity.build.branch=pull/42
                teamcity.pullRequest.number=42
                teamcity.pullRequest.source.branch=feature
                teamcity.pullRequest.target.branch=main
                """);
        runInTeamCity(write("build.properties",
                "teamcity.configuration.properties.file=" + configuration.toString().replace("\\", "\\\\")));
        BranchConfiguration pullRequest = mock();
        when(branchConfigurationFactory.createPullRequestConfiguration("42", "feature", "main", projectBranches)).thenReturn(pullRequest);

        assertThat(underTest.detectConfiguration(system2, projectBranches)).contains(pullRequest);
    }

    private void runInTeamCity(Path buildProperties) {
        when(system2.envVariable("TEAMCITY_VERSION")).thenReturn("2026.1");
        when(system2.envVariable("TEAMCITY_BUILD_PROPERTIES_FILE")).thenReturn(buildProperties.toString());
    }

    private Path write(String name, String content) throws IOException {
        return Files.writeString(directory.resolve(name), content, StandardCharsets.UTF_8);
    }
}
