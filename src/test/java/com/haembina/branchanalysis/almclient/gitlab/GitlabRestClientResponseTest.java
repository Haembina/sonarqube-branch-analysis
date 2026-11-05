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
package com.haembina.branchanalysis.almclient.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haembina.branchanalysis.almclient.gitlab.model.Commit;
import com.haembina.branchanalysis.almclient.gitlab.model.Discussion;
import com.haembina.branchanalysis.almclient.gitlab.model.MergeRequestNote;
import com.haembina.branchanalysis.almclient.gitlab.model.Note;
import com.haembina.branchanalysis.almclient.gitlab.model.PipelineStatus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** What each call reads back from GitLab, across pages, and what it does with a status GitLab refuses. */
class GitlabRestClientResponseTest {

    private final CloseableHttpClient httpClient = mock();
    private final LinkHeaderReader linkHeaderReader = mock();
    private final GitlabRestClient underTest = new GitlabRestClient("http://gitlab.test/api/v4", "token", linkHeaderReader, new ObjectMapper(), () -> httpClient);
    private final Logger clientLogger = (Logger) LoggerFactory.getLogger(GitlabRestClient.class);
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        previousLevel = clientLogger.getLevel();
        logged.start();
        clientLogger.addAppender(logged);
    }

    @AfterEach
    void tearDown() {
        clientLogger.detachAppender(logged);
        clientLogger.setLevel(previousLevel);
    }

    private static CloseableHttpResponse response(int status, HttpEntity entity) {
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(status);
        CloseableHttpResponse response = mock();
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);
        return response;
    }

    private static CloseableHttpResponse json(String body) {
        return response(200, new StringEntity(body, StandardCharsets.UTF_8));
    }

    @Test
    void shouldReadProjectBySlug() throws IOException {
        CloseableHttpResponse response = json("{\"id\":42}");
        when(httpClient.execute(any())).thenReturn(response);

        assertThat(underTest.getProject("group/project").getId()).isEqualTo(42);

        ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.captor();
        verify(httpClient).execute(request.capture());
        assertThat(request.getValue().getURI()).hasToString("http://gitlab.test/api/v4/projects/group%2Fproject");
    }

    @Test
    void shouldFollowNextLinkAcrossPages() throws IOException {
        CloseableHttpResponse first = json("[{\"id\":\"sha-1\"}]");
        when(first.getFirstHeader("Link")).thenReturn(new BasicHeader("Link", "<next>; rel=\"next\""));
        CloseableHttpResponse second = json("[{\"id\":\"sha-2\"}]");
        when(httpClient.execute(any())).thenReturn(first, second);
        when(linkHeaderReader.findNextLink("<next>; rel=\"next\"")).thenReturn(Optional.of("http://gitlab.test/api/v4/page2"));

        List<Commit> commits = underTest.getMergeRequestCommits(1, 2);

        assertThat(commits).extracting(Commit::getId).containsExactly("sha-1", "sha-2");
    }

    @Test
    void shouldSendPipelineIdAndCoverageWithStatus() throws IOException {
        CloseableHttpResponse response = response(201, null);
        when(httpClient.execute(any())).thenReturn(response);

        underTest.setMergeRequestPipelineStatus(1, "sha", new PipelineStatus("SonarQube", "Quality Gate passed", PipelineStatus.State.SUCCESS, "http://sonar", BigDecimal.TEN, 77L));

        ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.captor();
        verify(httpClient).execute(request.capture());
        assertThat(request.getValue().getURI()).hasToString("http://gitlab.test/api/v4/projects/1/statuses/sha?state=success");
        assertThat(((HttpEntityEnclosingRequest) request.getValue()).getEntity().getContent())
                .hasContent("name=SonarQube&target_url=http%3A%2F%2Fsonar&description=Quality+Gate+passed&pipeline_id=77&coverage=10");
        assertThat(request.getValue().getFirstHeader("Content-type").getValue()).isEqualTo("application/x-www-form-urlencoded");
    }

    @Test
    void shouldReadMergeRequestDiscussionsWithTheirNotesSendingTheToken() throws IOException {
        CloseableHttpResponse response = json("""
                [{"id": "discussion", "notes": [
                    {"id": 5, "system": false, "author": {"username": "sonar"}, "body": "Resolved issue", "resolved": true, "resolvable": true},
                    {"id": 6, "system": true, "author": {"username": "sonar"}, "body": "Summary", "resolved": false, "resolvable": false}]}]
                """);
        when(httpClient.execute(any())).thenReturn(response);

        List<Discussion> discussions = underTest.getMergeRequestDiscussions(1, 2);

        assertThat(discussions).singleElement().extracting(Discussion::getNotes).asInstanceOf(InstanceOfAssertFactories.list(Note.class))
                .extracting(Note::getId, Note::getBody, Note::isResolved, Note::isResolvable)
                .containsExactly(tuple(5L, "Resolved issue", true, true), tuple(6L, "Summary", false, false));
        ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.captor();
        verify(httpClient).execute(request.capture());
        assertThat(request.getValue().getURI()).hasToString("http://gitlab.test/api/v4/projects/1/merge_requests/2/discussions");
        assertThat(request.getValue().getFirstHeader("PRIVATE-TOKEN").getValue()).isEqualTo("token");
    }

    @Test
    void shouldPostADiscussionAsAForm() throws IOException {
        CloseableHttpResponse response = response(201, new StringEntity("{\"id\": \"discussion\", \"notes\": []}", StandardCharsets.UTF_8));
        when(httpClient.execute(any())).thenReturn(response);

        assertThat(underTest.addMergeRequestDiscussion(1, 2, new MergeRequestNote("Issue")).getId()).isEqualTo("discussion");

        ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.captor();
        verify(httpClient).execute(request.capture());
        assertThat(request.getValue().getFirstHeader("Content-type").getValue()).isEqualTo("application/x-www-form-urlencoded");
        assertThat(((HttpEntityEnclosingRequest) request.getValue()).getEntity().getContent()).hasContent("body=Issue");
    }

    static Stream<Arguments> callsGitlabRefuses() {
        return Stream.of(
                Arguments.of("note", 201, (GitlabCall) client -> client.addMergeRequestDiscussionNote(1, 2, "discussion", "note")),
                Arguments.of("pipeline status", 201, (GitlabCall) client -> client.setMergeRequestPipelineStatus(1, "sha",
                        new PipelineStatus("SonarQube", "Quality Gate passed", PipelineStatus.State.SUCCESS, "http://sonar", null, null))),
                Arguments.of("commits", 200, (GitlabCall) client -> client.getMergeRequestCommits(1, 2)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("callsGitlabRefuses")
    void shouldThrowWhenGitlabRefusesTheCall(String name, int expectedStatus, GitlabCall gitlabCall) throws IOException {
        CloseableHttpResponse response = response(500, new StringEntity("[]", StandardCharsets.UTF_8));
        when(httpClient.execute(any())).thenReturn(response);

        assertThatThrownBy(() -> gitlabCall.call(underTest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Gitlab API - Expected: " + expectedStatus + ", Got: 500");
    }

    @FunctionalInterface
    interface GitlabCall {
        void call(GitlabRestClient client) throws IOException;
    }

    @Test
    void shouldAcceptStatusGitlabAlreadyHolds() throws IOException {
        CloseableHttpResponse response = response(400, null);
        when(response.toString()).thenReturn("HTTP/1.1 400 Bad Request {\"message\":\"Cannot transition status via :run from :running\"}");
        when(httpClient.execute(any())).thenReturn(response);
        PipelineStatus status = new PipelineStatus("SonarQube", "Quality Gate passed", PipelineStatus.State.SUCCESS, "http://sonar", null, null);

        underTest.setMergeRequestPipelineStatus(1, "sha", status);

        verify(httpClient).execute(any());
    }

    @Test
    void shouldLogSuccessMessageAtDebug() throws IOException {
        clientLogger.setLevel(Level.DEBUG);
        CloseableHttpResponse response = response(204, null);
        when(response.toString()).thenReturn("HTTP/1.1 204");
        when(httpClient.execute(any())).thenReturn(response);

        underTest.deleteMergeRequestDiscussionNote(1, 2, "discussion", 3);

        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("Commit discussions note deleted" + System.lineSeparator() + "HTTP/1.1 204");
    }

    @Test
    void shouldLogGitlabsExplanationOfAnErrorResponse() throws IOException {
        CloseableHttpResponse response = response(403, new StringEntity("{\"message\":\"403 Forbidden\"}", StandardCharsets.UTF_8));
        when(httpClient.execute(any())).thenReturn(response);

        assertThatThrownBy(() -> underTest.getProject("group/project"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Gitlab API - Expected: 200, Got: 403");
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message).endsWith("{\"message\":\"403 Forbidden\"}"));
    }

    @Test
    void shouldRefuseErrorResponseWhoseBodyCannotBeRead() throws IOException {
        InputStream failing = mock();
        when(failing.read(any(), anyInt(), anyInt())).thenThrow(new IOException("reset"));
        HttpEntity unreadable = mock();
        when(unreadable.getContent()).thenReturn(failing);
        when(unreadable.getContentLength()).thenReturn(-1L);
        CloseableHttpResponse response = response(502, unreadable);
        when(httpClient.execute(any())).thenReturn(response);

        assertThatThrownBy(() -> underTest.getProject("group/project"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Gitlab API - Expected: 200, Got: 502");
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage).contains("Could not decode response entity");
    }
}
