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

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.haembina.branchanalysis.almclient.github.model.ReviewThread;
import com.haembina.branchanalysis.almclient.github.model.ReviewThreadComment;

import okhttp3.OkHttpClient;

class GithubGraphqlClientTest {

    private static final String PAGES = "review thread pages";

    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension.newInstance()
        .failOnUnmatchedRequests(true)
        .build();

    private GithubGraphqlClient client() {
        return new GithubGraphqlClient(new OkHttpClient(), new ObjectMapper(), wireMock.baseUrl() + "/", "installation-token");
    }

    @Test
    void shouldAddressGithubComAndEnterpriseGraphqlEndpoints() {
        assertThat(GithubGraphqlClient.graphqlEndpoint("https://api.github.com")).isEqualTo("https://api.github.com/graphql");
        assertThat(GithubGraphqlClient.graphqlEndpoint("https://ghe.example/api/v3/")).isEqualTo("https://ghe.example/api/graphql");
    }

    @Test
    void shouldReadEveryPageOfReviewThreads() throws IOException {
        wireMock.stubFor(post(urlEqualTo("/graphql")).inScenario(PAGES).whenScenarioStateIs(STARTED)
            .withHeader("Authorization", equalTo("Bearer installation-token"))
            .withRequestBody(matchingJsonPath("$.variables.owner", equalTo("owner")))
            .withRequestBody(matchingJsonPath("$.variables.name", equalTo("repo")))
            .withRequestBody(matchingJsonPath("$.variables.number", equalTo("12")))
            .willReturn(okJson("""
                {"data": {"repository": {"pullRequest": {"reviewThreads": {
                  "pageInfo": {"hasNextPage": true, "endCursor": "cursor-1"},
                  "nodes": [{"id": "thread-1", "isResolved": false, "comments": {"nodes": [
                    {"id": "comment-1", "body": "first", "author": {"login": "sonarqube-app"}},
                    {"id": "comment-2", "body": "reply", "author": null}
                  ]}}]
                }}}}}"""))
            .willSetStateTo("second page"));
        wireMock.stubFor(post(urlEqualTo("/graphql")).inScenario(PAGES).whenScenarioStateIs("second page")
            .withRequestBody(matchingJsonPath("$.variables.cursor", equalTo("cursor-1")))
            .willReturn(okJson("""
                {"data": {"repository": {"pullRequest": {"reviewThreads": {
                  "pageInfo": {"hasNextPage": false, "endCursor": null},
                  "nodes": [{"id": "thread-2", "isResolved": true, "comments": {"nodes": []}}]
                }}}}}""")));

        List<ReviewThread> threads = client().getReviewThreads("owner/repo", 12);

        assertThat(threads).containsExactly(
            new ReviewThread("thread-1", false, List.of(
                new ReviewThreadComment("comment-1", "first", "sonarqube-app"),
                new ReviewThreadComment("comment-2", "reply", null))),
            new ReviewThread("thread-2", true, List.of()));
    }

    @Test
    void shouldResolveReplyToAndDeleteThroughMutations() throws IOException {
        wireMock.stubFor(post(urlEqualTo("/graphql")).willReturn(okJson("{\"data\": {}}")));
        GithubGraphqlClient client = client();

        client.resolveReviewThread("thread-1");
        client.replyToReviewThread("thread-2", "no longer exists");
        client.deleteReviewComment("comment-3");

        wireMock.verify(postRequestedFor(urlEqualTo("/graphql"))
            .withRequestBody(matchingJsonPath("$.query", containing("resolveReviewThread")))
            .withRequestBody(matchingJsonPath("$.variables.threadId", equalTo("thread-1"))));
        wireMock.verify(postRequestedFor(urlEqualTo("/graphql"))
            .withRequestBody(matchingJsonPath("$.query", containing("addPullRequestReviewThreadReply")))
            .withRequestBody(matchingJsonPath("$.variables.threadId", equalTo("thread-2")))
            .withRequestBody(matchingJsonPath("$.variables.body", equalTo("no longer exists"))));
        wireMock.verify(postRequestedFor(urlEqualTo("/graphql"))
            .withRequestBody(matchingJsonPath("$.query", containing("deletePullRequestReviewComment")))
            .withRequestBody(matchingJsonPath("$.variables.commentId", equalTo("comment-3"))));
    }

    @Test
    void shouldFailOnAnHttpError() {
        wireMock.stubFor(post(urlEqualTo("/graphql")).willReturn(serverError().withBody("down")));

        assertThatThrownBy(() -> client().resolveReviewThread("thread-1"))
            .isInstanceOf(IOException.class)
            .hasMessage("GitHub GraphQL request failed with HTTP 500: down");
    }

    @Test
    void shouldFailOnGraphqlErrors() {
        wireMock.stubFor(post(urlEqualTo("/graphql"))
            .willReturn(okJson("{\"errors\": [{\"message\": \"Resource not accessible by integration\"}]}")));

        assertThatThrownBy(() -> client().resolveReviewThread("thread-1"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Resource not accessible by integration");
    }
}
