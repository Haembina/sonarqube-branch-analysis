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
package com.haembina.branchanalysis.almclient.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.haembina.branchanalysis.almclient.github.model.ReviewThread;
import com.haembina.branchanalysis.almclient.github.model.ReviewThreadComment;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GithubGraphqlClient {

    private static final MediaType APPLICATION_JSON = MediaType.get("application/json");
    private static final String ENTERPRISE_REST_PATH = "/api/v3";
    private static final String REVIEW_THREADS_QUERY = """
        query($owner: String!, $name: String!, $number: Int!, $cursor: String) {
          repository(owner: $owner, name: $name) {
            pullRequest(number: $number) {
              reviewThreads(first: 100, after: $cursor) {
                pageInfo { hasNextPage endCursor }
                nodes { id isResolved comments(first: 100) { nodes { id body author { login } } } }
              }
            }
          }
        }""";
    private static final String RESOLVE_THREAD_MUTATION =
        "mutation($threadId: ID!) { resolveReviewThread(input: {threadId: $threadId}) { thread { id } } }";
    private static final String REPLY_TO_THREAD_MUTATION = """
        mutation($threadId: ID!, $body: String!) {
          addPullRequestReviewThreadReply(input: {pullRequestReviewThreadId: $threadId, body: $body}) { comment { id } }
        }""";
    private static final String DELETE_COMMENT_MUTATION =
        "mutation($commentId: ID!) { deletePullRequestReviewComment(input: {id: $commentId}) { pullRequestReview { id } } }";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String token;

    public GithubGraphqlClient(OkHttpClient httpClient, ObjectMapper objectMapper, String apiUrl, String token) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.endpoint = graphqlEndpoint(apiUrl);
        this.token = token;
    }

    static String graphqlEndpoint(String apiUrl) {
        String trimmed = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        if (trimmed.endsWith(ENTERPRISE_REST_PATH)) {
            return trimmed.substring(0, trimmed.length() - ENTERPRISE_REST_PATH.length()) + "/api/graphql";
        }
        return trimmed + "/graphql";
    }

    public List<ReviewThread> getReviewThreads(String repository, int pullRequestNumber) throws IOException {
        String[] ownerAndName = repository.split("/", 2);
        List<ReviewThread> threads = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage;
        do {
            ObjectNode variables = objectMapper.createObjectNode()
                .put("owner", ownerAndName[0])
                .put("name", ownerAndName[1])
                .put("number", pullRequestNumber)
                .put("cursor", cursor);
            JsonNode reviewThreads = execute(REVIEW_THREADS_QUERY, variables)
                .path("repository").path("pullRequest").path("reviewThreads");
            for (JsonNode thread : reviewThreads.path("nodes")) {
                threads.add(toReviewThread(thread));
            }
            JsonNode pageInfo = reviewThreads.path("pageInfo");
            hasNextPage = pageInfo.path("hasNextPage").asBoolean();
            cursor = pageInfo.path("endCursor").asText(null);
        } while (hasNextPage);
        return threads;
    }

    public void resolveReviewThread(String threadId) throws IOException {
        execute(RESOLVE_THREAD_MUTATION, objectMapper.createObjectNode().put("threadId", threadId));
    }

    public void replyToReviewThread(String threadId, String body) throws IOException {
        execute(REPLY_TO_THREAD_MUTATION, objectMapper.createObjectNode().put("threadId", threadId).put("body", body));
    }

    public void deleteReviewComment(String commentId) throws IOException {
        execute(DELETE_COMMENT_MUTATION, objectMapper.createObjectNode().put("commentId", commentId));
    }

    private static ReviewThread toReviewThread(JsonNode thread) {
        List<ReviewThreadComment> comments = StreamSupport.stream(thread.path("comments").path("nodes").spliterator(), false)
            .map(comment -> new ReviewThreadComment(comment.path("id").asText(), comment.path("body").asText(),
                comment.path("author").path("login").asText(null)))
            .toList();
        return new ReviewThread(thread.path("id").asText(), thread.path("isResolved").asBoolean(), comments);
    }

    private JsonNode execute(String query, ObjectNode variables) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode().put("query", query);
        payload.set("variables", variables);
        Request request = new Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer " + token)
            .post(RequestBody.create(objectMapper.writeValueAsString(payload), APPLICATION_JSON))
            .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("GitHub GraphQL request failed with HTTP " + response.code() + ": " + body);
            }
            JsonNode result = objectMapper.readTree(body);
            JsonNode errors = result.path("errors");
            if (!errors.isEmpty()) {
                throw new IOException("GitHub GraphQL request failed: " + errors);
            }
            return result.path("data");
        }
    }
}
