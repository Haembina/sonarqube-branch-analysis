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
package com.haembina.branchanalysis.almclient.bitbucket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haembina.branchanalysis.almclient.bitbucket.model.BitbucketConfiguration;
import com.haembina.branchanalysis.almclient.bitbucket.model.cloud.CloudAnnotation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** The token exchange, the repository lookup and the debug record of each annotation upload. */
class BitbucketCloudClientRequestTest {

    private final OkHttpClient httpClient = mock();
    private final Call call = mock();
    private final Response response = mock();
    private final ResponseBody body = mock();
    private final Logger clientLogger = (Logger) LoggerFactory.getLogger(BitbucketCloudClient.class);
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() throws IOException {
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.body()).thenReturn(body);
        previousLevel = clientLogger.getLevel();
        logged.start();
        clientLogger.addAppender(logged);
    }

    @AfterEach
    void tearDown() {
        clientLogger.detachAppender(logged);
        clientLogger.setLevel(previousLevel);
    }

    private Request sentRequest() {
        ArgumentCaptor<Request> request = ArgumentCaptor.captor();
        verify(httpClient).newCall(request.capture());
        return request.getValue();
    }

    @Test
    void shouldExchangeClientCredentialsForBearerToken() throws IOException {
        when(body.string()).thenReturn("{\"access_token\":\"bearer\"}");

        assertThat(BitbucketCloudClient.negotiateBearerToken("id", "secret", new ObjectMapper(), httpClient)).isEqualTo("bearer");

        Request request = sentRequest();
        assertThat(request.url()).hasToString("https://bitbucket.org/site/oauth2/access_token");
        assertThat(request.header("Authorization")).isEqualTo("Basic aWQ6c2VjcmV0");
    }

    @Test
    void shouldReportFailedTokenExchange() throws IOException {
        when(call.execute()).thenThrow(new IOException("unreachable"));
        ObjectMapper objectMapper = new ObjectMapper();

        assertThatThrownBy(() -> BitbucketCloudClient.negotiateBearerToken("id", "secret", objectMapper, httpClient))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve bearer token")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void shouldReadRepositorySlug() throws IOException {
        when(response.isSuccessful()).thenReturn(true);
        when(body.string()).thenReturn("{\"slug\":\"repository\"}");
        BitbucketCloudClient underTest = new BitbucketCloudClient(new ObjectMapper(), httpClient, new BitbucketConfiguration("project", "repository"));

        assertThat(underTest.retrieveRepository().getSlug()).isEqualTo("repository");

        Request request = sentRequest();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.url()).hasToString("https://api.bitbucket.org/2.0/repositories/project/repository");
    }

    @Test
    void shouldLeaveAnnotationsInPlaceBecauseReportUploadReplacesThem() {
        BitbucketCloudClient underTest = new BitbucketCloudClient(new ObjectMapper(), httpClient, new BitbucketConfiguration("project", "repository"));

        underTest.deleteAnnotations("commit", "reportKey");

        verifyNoInteractions(httpClient);
    }

    @Test
    void shouldLogAnnotationPayloadAtDebug() throws IOException {
        clientLogger.setLevel(Level.DEBUG);
        when(response.isSuccessful()).thenReturn(true);
        ObjectMapper objectMapper = mock();
        when(objectMapper.writeValueAsString(any())).thenReturn("[annotations]");
        BitbucketCloudClient underTest = new BitbucketCloudClient(objectMapper, httpClient, new BitbucketConfiguration("project", "repository"));

        underTest.uploadAnnotations("commit", Set.of(mock(CloudAnnotation.class)), "reportKey");

        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage).contains("Create annotations: [annotations]");
    }

    @Test
    void shouldLogWhyAnnotationPayloadCouldNotBeWrittenAtDebug() throws IOException {
        clientLogger.setLevel(Level.DEBUG);
        when(response.isSuccessful()).thenReturn(true);
        ObjectMapper objectMapper = mock();
        when(objectMapper.writeValueAsString(any())).thenReturn("[annotations]").thenThrow(new JsonProcessingException("cyclic") { });
        BitbucketCloudClient underTest = new BitbucketCloudClient(objectMapper, httpClient, new BitbucketConfiguration("project", "repository"));

        underTest.uploadAnnotations("commit", Set.of(mock(CloudAnnotation.class)), "reportKey");

        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message).startsWith("Create annotations: An error occurred whilst converting annotations to JSON: ").endsWith(": cyclic"));
    }
}
