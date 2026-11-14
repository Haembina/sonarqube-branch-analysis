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
package com.haembina.branchanalysis.almclient.azuredevops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

class DefaultAzureDevopsClientFactoryTest {

    private static final int TIMEOUT_MILLISECONDS = 30_000;

    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final DefaultAzureDevopsClientFactory underTest = new DefaultAzureDevopsClientFactory(mock(Settings.class));

    @Test
    void shouldCreateRestClientFromUrlAndToken() {
        when(almSettingDto.getUrl()).thenReturn("https://dev.azure.com/org");
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn("token");

        assertThat(underTest.createClient(projectAlmSettingDto, almSettingDto)).isInstanceOf(AzureDevopsRestClient.class);
    }

    @Test
    void shouldBoundEveryHttpTimeoutAtThirtySeconds() {
        when(almSettingDto.getUrl()).thenReturn("https://dev.azure.com/org");
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn("token");
        HttpClientBuilder builder = mock();
        when(builder.useSystemProperties()).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any())).thenReturn(builder);

        try (MockedStatic<HttpClientBuilder> mockedBuilder = mockStatic(HttpClientBuilder.class)) {
            mockedBuilder.when(HttpClientBuilder::create).thenReturn(builder);

            underTest.createClient(projectAlmSettingDto, almSettingDto);
        }

        verify(builder).useSystemProperties();
        ArgumentCaptor<RequestConfig> requestConfig = ArgumentCaptor.captor();
        verify(builder).setDefaultRequestConfig(requestConfig.capture());
        assertThat(requestConfig.getValue().getConnectionRequestTimeout()).isEqualTo(TIMEOUT_MILLISECONDS);
        assertThat(requestConfig.getValue().getConnectTimeout()).isEqualTo(TIMEOUT_MILLISECONDS);
        assertThat(requestConfig.getValue().getSocketTimeout()).isEqualTo(TIMEOUT_MILLISECONDS);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void shouldRefuseMissingUrl(String url) {
        when(almSettingDto.getUrl()).thenReturn(url);

        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("ALM URL must be provided");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void shouldRefuseMissingToken(String token) {
        when(almSettingDto.getUrl()).thenReturn("https://dev.azure.com/org");
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn(token);

        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Personal Access Token must be provided");
    }
}
