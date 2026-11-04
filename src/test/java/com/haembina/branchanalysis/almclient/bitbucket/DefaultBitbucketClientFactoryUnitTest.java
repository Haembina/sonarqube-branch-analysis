/*
 * Copyright (C) 2020-2024 Marvin Wichmann, Michael Clarke
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
package com.haembina.branchanalysis.almclient.bitbucket;

import com.haembina.branchanalysis.InvalidConfigurationException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultBitbucketClientFactoryUnitTest {

    @Test
    void testCreateClientIsCloudIfCloudConfig() throws IOException {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD)
                .setClientId("clientId")
                .setAppId("appId")
                .setClientSecret("clientSecret");
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
                .setAlmRepo("almRepo");
        OkHttpClient.Builder builder = mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS);
        when(builder.addInterceptor(any())).thenReturn(builder);

        ResponseBody responseBody = mock();
        when(responseBody.string()).thenReturn("{\"access_token\": \"dummy\"}");
        when(builder.build().newCall(any()).execute().body()).thenReturn(responseBody);

        Settings settings = mock();
        Encryption encryption = mock();

        // when
        when(settings.getEncryption()).thenReturn(encryption);
        HttpClientBuilderFactory httpClientBuilderFactory = mock();
        when(httpClientBuilderFactory.createClientBuilder()).then(i -> builder);
        BitbucketClient client = new DefaultBitbucketClientFactory(settings, httpClientBuilderFactory).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertThat(client).isInstanceOf(BitbucketCloudClient.class);

        ArgumentCaptor<Interceptor> interceptorArgumentCaptor = ArgumentCaptor.captor();
        verify(builder, times(2)).addInterceptor(interceptorArgumentCaptor.capture());

        Interceptor.Chain chain = mock();
        Request request = mock();
        when(chain.request()).thenReturn(request);
        Request.Builder requestBuilder = mock();
        when(requestBuilder.addHeader(any(), any())).thenReturn(requestBuilder);
        when(request.newBuilder()).thenReturn(requestBuilder);

        Request request2 = mock();
        when(requestBuilder.build()).thenReturn(request2);

        interceptorArgumentCaptor.getValue().intercept(chain);

        verify(requestBuilder).addHeader("Authorization", "Bearer dummy");
        verify(requestBuilder).addHeader("Accept", "application/json");
        verify(chain).proceed(request2);
    }

    @Test
    void testCreateClientIfNotCloudConfig() {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET)
                .setUrl("url")
                .setPersonalAccessToken("personalAccessToken");
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
                .setAlmRepo("almRepo")
                .setAlmSlug("almSlug");

        Settings settings = mock();
        Encryption encryption = mock();

        // when
        when(settings.getEncryption()).thenReturn(encryption);
        HttpClientBuilderFactory httpClientBuilderFactory = mock();
        when(httpClientBuilderFactory.createClientBuilder()).then(i -> mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS));
        BitbucketClient client = new DefaultBitbucketClientFactory(settings, httpClientBuilderFactory).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertThat(client).isInstanceOf(BitbucketServerClient.class);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "ALM Repo must be set in configuration|PROJECT|BITBUCKET|| slug|url|token|app|client|secret",
            "App ID must be set in configuration|GLOBAL|BITBUCKET_CLOUD|repo|slug|url|token| |client|secret",
            "Client ID must be set in configuration|GLOBAL|BITBUCKET_CLOUD|repo|slug|url|token|app||secret",
            "Client Secret must be set in configuration|GLOBAL|BITBUCKET_CLOUD|repo|slug|url|token|app|client|",
            "ALM slug must be set in configuration|PROJECT|BITBUCKET|repo||url|token|app|client|secret",
            "URL must be set in configuration|GLOBAL|BITBUCKET|repo|slug| |token|app|client|secret",
            "Personal access token must be set in configuration|PROJECT|BITBUCKET|repo|slug|url||app|client|secret"})
    void shouldRefuseIncompleteConfiguration(String message, InvalidConfigurationException.Scope scope, ALM alm, String repo, String slug,
                                             String url, String token, String appId, String clientId, String clientSecret) {
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getAlm()).thenReturn(alm);
        when(almSettingDto.getUrl()).thenReturn(url);
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn(token);
        when(almSettingDto.getAppId()).thenReturn(appId);
        when(almSettingDto.getClientId()).thenReturn(clientId);
        when(almSettingDto.getDecryptedClientSecret(any())).thenReturn(clientSecret);
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto().setAlmRepo(repo).setAlmSlug(slug);
        HttpClientBuilderFactory httpClientBuilderFactory = mock();
        when(httpClientBuilderFactory.createClientBuilder()).then(i -> mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS));
        DefaultBitbucketClientFactory underTest = new DefaultBitbucketClientFactory(mock(Settings.class), httpClientBuilderFactory);

        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isExactlyInstanceOf(InvalidConfigurationException.class)
                .hasMessage(message)
                .extracting("scope").isEqualTo(scope);
    }

}
