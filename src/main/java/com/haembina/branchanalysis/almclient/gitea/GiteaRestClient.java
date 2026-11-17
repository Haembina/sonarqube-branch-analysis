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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haembina.branchanalysis.almclient.ChangeRequestState;
import com.haembina.branchanalysis.almclient.gitea.model.Comment;
import com.haembina.branchanalysis.almclient.gitea.model.CommitStatus;
import com.haembina.branchanalysis.almclient.gitea.model.PullRequest;
import com.haembina.branchanalysis.almclient.gitea.model.User;

class GiteaRestClient implements GiteaClient {

    private final String apiUrl;
    private final String token;
    private final ObjectMapper objectMapper;
    private final Supplier<CloseableHttpClient> httpClientFactory;

    GiteaRestClient(String apiUrl, String token, ObjectMapper objectMapper, Supplier<CloseableHttpClient> httpClientFactory) {
        this.apiUrl = apiUrl;
        this.token = token;
        this.objectMapper = objectMapper;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public User getCurrentUser() throws IOException {
        return read(new HttpGet(apiUrl + "/user"), new TypeReference<>() {});
    }

    @Override
    public PullRequest getPullRequest(String repository, long index) throws IOException {
        return read(new HttpGet(repositoryUrl(repository) + "/pulls/" + index), new TypeReference<>() {});
    }

    @Override
    public List<Comment> getComments(String repository, long index) throws IOException {
        return read(new HttpGet(repositoryUrl(repository) + "/issues/" + index + "/comments"), new TypeReference<>() {});
    }

    @Override
    public void createComment(String repository, long index, String body) throws IOException {
        send(new HttpPost(repositoryUrl(repository) + "/issues/" + index + "/comments"), Map.of("body", body), HttpStatus.SC_CREATED);
    }

    @Override
    public void editComment(String repository, long commentId, String body) throws IOException {
        send(new HttpPatch(repositoryUrl(repository) + "/issues/comments/" + commentId), Map.of("body", body), HttpStatus.SC_OK);
    }

    @Override
    public void setCommitStatus(String repository, String sha, CommitStatus status) throws IOException {
        send(new HttpPost(repositoryUrl(repository) + "/statuses/" + sha), status, HttpStatus.SC_CREATED);
    }

    @Override
    public ChangeRequestState getPullRequestState(String repository, long index) throws IOException {
        JsonNode pullRequest = read(new HttpGet(repositoryUrl(repository) + "/pulls/" + index), new TypeReference<>() {});
        if ("open".equals(pullRequest.path("state").asText())) {
            return ChangeRequestState.OPEN;
        }
        return ChangeRequestState.closedAt(pullRequest.path("merged_at").asText(null), pullRequest.path("closed_at").asText(null));
    }

    @Override
    public Optional<String> getBranchHead(String repository, String branch) throws IOException {
        HttpGet request = new HttpGet(repositoryUrl(repository) + "/branches/" + URLEncoder.encode(branch, StandardCharsets.UTF_8));
        return execute(request, Set.of(HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND), response -> {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return Optional.empty();
            }
            JsonNode body = objectMapper.readTree(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            return Optional.of(body.path("commit").path("id").asText());
        });
    }

    private String repositoryUrl(String repository) {
        return apiUrl + "/repos/" + repository;
    }

    private <T> T read(HttpRequestBase request, TypeReference<T> type) throws IOException {
        return execute(request, HttpStatus.SC_OK,
                entity -> objectMapper.readValue(EntityUtils.toString(entity, StandardCharsets.UTF_8), type));
    }

    private void send(HttpEntityEnclosingRequestBase request, Object payload, int expectedStatus) throws IOException {
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));
        execute(request, expectedStatus, entity -> null);
    }

    private <T> T execute(HttpRequestBase request, int expectedStatus, ResponseReader<T> reader) throws IOException {
        return execute(request, Set.of(expectedStatus), response -> reader.read(response.getEntity()));
    }

    private <T> T execute(HttpRequestBase request, Set<Integer> expectedStatuses, ResponseHandler<T> handler) throws IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "token " + token);
        request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

        try (CloseableHttpClient httpClient = httpClientFactory.get();
             CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (!expectedStatuses.contains(status)) {
                throw new IllegalStateException(String.format("Gitea answered %s %s with %d, expected %s",
                        request.getMethod(), request.getURI(), status, expectedStatuses));
            }
            return handler.handle(response);
        }
    }

    @FunctionalInterface
    private interface ResponseReader<T> {
        T read(HttpEntity entity) throws IOException;
    }

    @FunctionalInterface
    private interface ResponseHandler<T> {
        T handle(CloseableHttpResponse response) throws IOException;
    }
}
