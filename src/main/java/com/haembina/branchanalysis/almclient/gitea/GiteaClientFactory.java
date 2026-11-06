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
package com.haembina.branchanalysis.almclient.gitea;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haembina.branchanalysis.InvalidConfigurationException;

@ComputeEngineSide
public class GiteaClientFactory {

    private static final int TIMEOUT_MILLISECONDS = 30_000;
    private static final String API_PATH = "/api/v1";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public GiteaClient createClient(Configuration configuration) {
        String url = setting(configuration, GiteaSettings.URL)
                .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "Gitea server URL must be specified"));
        String token = setting(configuration, GiteaSettings.TOKEN)
                .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "Gitea access token must be specified"));

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(TIMEOUT_MILLISECONDS)
                .setConnectTimeout(TIMEOUT_MILLISECONDS)
                .setSocketTimeout(TIMEOUT_MILLISECONDS)
                .build();
        HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties().setDefaultRequestConfig(requestConfig);
        return new GiteaRestClient(url.strip().replaceAll("/+$", "") + API_PATH, token, objectMapper, builder::build);
    }

    private static Optional<String> setting(Configuration configuration, String key) {
        return configuration.get(key).filter(StringUtils::isNotBlank);
    }
}
