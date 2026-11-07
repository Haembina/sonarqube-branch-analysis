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

import com.haembina.branchanalysis.scanner.BranchAutoConfigurer;
import com.haembina.branchanalysis.scanner.BranchConfigurationFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class TeamCityAutoConfigurer implements BranchAutoConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamCityAutoConfigurer.class);

    private static final String CONFIGURATION_PROPERTIES_FILE = "teamcity.configuration.properties.file";

    private final BranchConfigurationFactory branchConfigurationFactory;

    public TeamCityAutoConfigurer(BranchConfigurationFactory branchConfigurationFactory) {
        this.branchConfigurationFactory = branchConfigurationFactory;
    }

    @Override
    public Optional<BranchConfiguration> detectConfiguration(System2 system2, ProjectBranches projectBranches) {
        String buildPropertiesFile = system2.envVariable("TEAMCITY_BUILD_PROPERTIES_FILE");
        if (StringUtils.isEmpty(system2.envVariable("TEAMCITY_VERSION")) || StringUtils.isEmpty(buildPropertiesFile)) {
            return Optional.empty();
        }

        Properties parameters = new Properties();
        try {
            load(parameters, buildPropertiesFile);
            String configurationPropertiesFile = parameters.getProperty(CONFIGURATION_PROPERTIES_FILE);
            if (StringUtils.isNotEmpty(configurationPropertiesFile)) {
                load(parameters, configurationPropertiesFile);
            }
        } catch (IOException ex) {
            LOGGER.atWarn().setMessage("Could not read the TeamCity build parameters from {}")
                    .addArgument(buildPropertiesFile).setCause(ex).log();
            return Optional.empty();
        }

        return CiBranchSelection.select(branchConfigurationFactory, projectBranches,
                parameters.getProperty("teamcity.pullRequest.number"),
                parameters.getProperty("teamcity.pullRequest.source.branch"),
                parameters.getProperty("teamcity.pullRequest.target.branch"),
                parameters.getProperty("teamcity.build.branch"));
    }

    private static void load(Properties parameters, String file) throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of(file), StandardCharsets.UTF_8)) {
            parameters.load(reader);
        }
    }
}
