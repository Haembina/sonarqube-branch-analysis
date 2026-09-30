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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haembina.branchanalysis.almclient.azuredevops.model.CommentThread;
import com.haembina.branchanalysis.almclient.azuredevops.model.Commit;
import com.haembina.branchanalysis.almclient.azuredevops.model.enums.CommentThreadStatus;
import com.haembina.branchanalysis.almclient.azuredevops.model.enums.CommentType;

/** What each call sends to Azure DevOps and what it reads back from the JSON the service answers with. */
class AzureDevopsRestClientResponseTest {

    private final CloseableHttpClient httpClient = mock();
    private final AzureDevopsRestClient underTest = new AzureDevopsRestClient("http://test.url", "token", new ObjectMapper(), () -> httpClient);

    private void respond(int status, HttpEntity entity) throws IOException {
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(status);
        CloseableHttpResponse response = mock();
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);
        when(httpClient.execute(any())).thenReturn(response);
    }

    private void respondWithJson(String json) throws IOException {
        respond(200, new StringEntity(json, StandardCharsets.UTF_8));
    }

    private RequestBuilder sentRequest() throws IOException {
        ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.captor();
        verify(httpClient).execute(request.capture());
        return RequestBuilder.copy(request.getValue());
    }

    @Test
    void shouldReadRepositoryWithItsProject() throws IOException {
        respondWithJson("{\"remoteUrl\":\"https://dev.azure.com/org/prj/_git/repo\",\"name\":\"repo\",\"project\":{\"name\":\"prj\"}}");

        assertThat(underTest.getRepository("prj", "repo"))
                .extracting("remoteUrl", "name", "project.name")
                .containsExactly("https://dev.azure.com/org/prj/_git/repo", "repo", "prj");
        assertThat(sentRequest().getUri()).isEqualTo(URI.create("http://test.url/prj/_apis/git/repositories/repo?api-version=4.1"));
    }

    @Test
    void shouldReadThreadsWithTheirContextAndComments() throws IOException {
        respondWithJson("{\"value\":[{\"id\":7,\"status\":\"active\",\"isDeleted\":false,"
                + "\"threadContext\":{\"filePath\":\"/src/A.java\",\"rightFileStart\":{\"line\":3,\"offset\":1},\"rightFileEnd\":{\"line\":4,\"offset\":9}},"
                + "\"comments\":[{\"id\":1,\"content\":\"note\",\"author\":{\"id\":\"user-1\"},\"commentType\":\"text\"}]}]}");

        List<CommentThread> threads = underTest.retrieveThreads("prj", "repo", 5);

        assertThat(threads).singleElement().satisfies(thread -> {
            assertThat(thread.getId()).isEqualTo(7);
            assertThat(thread.getStatus()).isEqualTo(CommentThreadStatus.ACTIVE);
            assertThat(thread.isDeleted()).isFalse();
            assertThat(thread.getThreadContext().getFilePath()).isEqualTo("/src/A.java");
            assertThat(thread.getThreadContext().getRightFileStart().getLine()).isEqualTo(3);
            assertThat(thread.getThreadContext().getRightFileStart().getOffset()).isEqualTo(1);
            assertThat(thread.getThreadContext().getRightFileEnd().getLine()).isEqualTo(4);
            assertThat(thread.getComments()).singleElement().satisfies(comment -> {
                assertThat(comment.getId()).isEqualTo(1);
                assertThat(comment.getContent()).isEqualTo("note");
                assertThat(comment.getAuthor().getId()).isEqualTo("user-1");
                assertThat(comment.getCommentType()).isEqualTo(CommentType.TEXT);
            });
        });
        assertThat(sentRequest().getUri()).isEqualTo(URI.create("http://test.url/prj/_apis/git/repositories/repo/pullRequests/5/threads?api-version=4.1"));
    }

    @Test
    void shouldReadPullRequestCommits() throws IOException {
        respondWithJson("{\"value\":[{\"commitId\":\"sha-1\"},{\"commitId\":\"sha-2\"}]}");

        assertThat(underTest.getPullRequestCommits("prj", "repo", 5)).extracting(Commit::getCommitId).containsExactly("sha-1", "sha-2");
        assertThat(sentRequest().getUri()).isEqualTo(URI.create("http://test.url/prj/_apis/git/repositories/repo/pullRequests/5/commits?api-version=4.1"));
    }

    @Test
    void shouldReadAuthenticatedUserFromConnectionData() throws IOException {
        respondWithJson("{\"authenticatedUser\":{\"id\":\"sonarqube\"}}");

        assertThat(underTest.getConnectionData().getAuthenticatedUser().getId()).isEqualTo("sonarqube");
        assertThat(sentRequest().getUri()).isEqualTo(URI.create("http://test.url/_apis/ConnectionData?api-version=4.1-preview"));
    }

    @Test
    void shouldDeleteThreadCommentWithoutReadingBody() throws IOException {
        respond(200, null);

        underTest.deletePullRequestThreadComment("prj", "repo", 5, 7, 9);

        RequestBuilder request = sentRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getUri()).isEqualTo(URI.create("http://test.url/prj/_apis/git/repositories/repo/pullRequests/5/threads/7/comments/9?api-version=4.1"));
    }

    @Test
    void shouldRefuseErrorResponseCarryingBody() throws IOException {
        respond(404, new StringEntity("{\"message\":\"not found\"}", StandardCharsets.UTF_8));

        assertThatThrownBy(() -> underTest.getRepository("prj", "repo"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Azure Devops API - Expected: 200, Got: 404");
    }

    @Test
    void shouldRefuseErrorResponseWhoseBodyCannotBeRead() throws IOException {
        HttpEntity unreadable = mock();
        InputStream failing = mock();
        when(failing.read(any(), anyInt(), anyInt())).thenThrow(new IOException("reset"));
        when(unreadable.getContent()).thenReturn(failing);
        when(unreadable.getContentLength()).thenReturn(-1L);
        respond(502, unreadable);

        assertThatThrownBy(() -> underTest.getRepository("prj", "repo"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Azure Devops API - Expected: 200, Got: 502");
    }
}
