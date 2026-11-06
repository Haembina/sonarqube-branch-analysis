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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.internal.MapSettings;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.haembina.branchanalysis.InvalidConfigurationException;
import com.haembina.branchanalysis.almclient.gitea.model.User;

class GiteaClientFactoryTest {

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.extensionOptions()
            .failOnUnmatchedRequests(true)
            .build();

    private final GiteaClientFactory underTest = new GiteaClientFactory();

    @Test
    void createsAClientOnTheServerApi() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/user"))
                .withHeader("Authorization", equalTo("token secret"))
                .willReturn(okJson("{\"id\": 7, \"login\": \"sonarqube\"}")));
        MapSettings settings = new MapSettings()
                .setProperty(GiteaSettings.URL, wireMockExtension.baseUrl() + "/")
                .setProperty(GiteaSettings.TOKEN, "secret");

        GiteaClient client = underTest.createClient(settings.asConfig());

        assertThat(client.getCurrentUser()).isEqualTo(new User(7, "sonarqube"));
    }

    @Test
    void refusesAMissingServerUrl() {
        MapSettings settings = new MapSettings().setProperty(GiteaSettings.TOKEN, "secret");

        assertThatThrownBy(() -> underTest.createClient(settings.asConfig()))
                .isInstanceOfSatisfying(InvalidConfigurationException.class,
                        ex -> assertThat(ex.getScope()).isEqualTo(InvalidConfigurationException.Scope.GLOBAL))
                .hasMessage("Gitea server URL must be specified");
    }

    @Test
    void refusesABlankToken() {
        MapSettings settings = new MapSettings()
                .setProperty(GiteaSettings.URL, "https://gitea.example.com")
                .setProperty(GiteaSettings.TOKEN, " ");

        assertThatThrownBy(() -> underTest.createClient(settings.asConfig()))
                .isInstanceOfSatisfying(InvalidConfigurationException.class,
                        ex -> assertThat(ex.getScope()).isEqualTo(InvalidConfigurationException.Scope.GLOBAL))
                .hasMessage("Gitea access token must be specified");
    }
}
