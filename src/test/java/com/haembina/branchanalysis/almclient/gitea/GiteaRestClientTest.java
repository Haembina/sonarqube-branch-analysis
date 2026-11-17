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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Instant;

import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.haembina.branchanalysis.almclient.ChangeRequestState;
import com.haembina.branchanalysis.almclient.gitea.model.Comment;
import com.haembina.branchanalysis.almclient.gitea.model.CommitStatus;
import com.haembina.branchanalysis.almclient.gitea.model.PullRequest;
import com.haembina.branchanalysis.almclient.gitea.model.User;

class GiteaRestClientTest {

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.extensionOptions()
            .failOnUnmatchedRequests(true)
            .build();

    private final GiteaRestClient underTest = new GiteaRestClient(wireMockExtension.baseUrl() + "/api/v1", "secret",
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false), HttpClients::createSystem);

    @Test
    void readsTheUserTheTokenBelongsTo() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/user"))
                .withHeader("Authorization", equalTo("token secret"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(okJson("{\"id\": 7, \"login\": \"sonarqube\", \"email\": \"bot@example.com\"}")));

        assertThat(underTest.getCurrentUser()).isEqualTo(new User(7, "sonarqube"));
    }

    @Test
    void readsThePullRequest() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/repos/owner/repo/pulls/12"))
                .withHeader("Authorization", equalTo("token secret"))
                .willReturn(okJson("{\"number\": 12, \"html_url\": \"https://gitea.example.com/owner/repo/pulls/12\", \"state\": \"open\"}")));

        assertThat(underTest.getPullRequest("owner/repo", 12))
                .isEqualTo(new PullRequest(12, "https://gitea.example.com/owner/repo/pulls/12"));
    }

    @Test
    void readsThePullRequestComments() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/repos/owner/repo/issues/12/comments"))
                .withHeader("Authorization", equalTo("token secret"))
                .willReturn(okJson("[{\"id\": 3, \"body\": \"first\", \"user\": {\"id\": 7, \"login\": \"sonarqube\"}},"
                        + "{\"id\": 4, \"body\": \"second\", \"user\": {\"id\": 8, \"login\": \"someone\"}}]")));

        assertThat(underTest.getComments("owner/repo", 12)).containsExactly(
                new Comment(3, "first", new User(7, "sonarqube")),
                new Comment(4, "second", new User(8, "someone")));
    }

    @Test
    void postsAComment() throws IOException {
        wireMockExtension.stubFor(post(urlEqualTo("/api/v1/repos/owner/repo/issues/12/comments"))
                .withHeader("Authorization", equalTo("token secret"))
                .withRequestBody(equalToJson("{\"body\": \"summary\"}"))
                .willReturn(aResponse().withStatus(201).withBody("{\"id\": 5}")));

        underTest.createComment("owner/repo", 12, "summary");

        wireMockExtension.verify(postRequestedFor(urlEqualTo("/api/v1/repos/owner/repo/issues/12/comments")));
    }

    @Test
    void editsAComment() throws IOException {
        wireMockExtension.stubFor(patch(urlEqualTo("/api/v1/repos/owner/repo/issues/comments/5"))
                .withHeader("Authorization", equalTo("token secret"))
                .withRequestBody(equalToJson("{\"body\": \"new summary\"}"))
                .willReturn(okJson("{\"id\": 5}")));

        underTest.editComment("owner/repo", 5, "new summary");

        wireMockExtension.verify(patchRequestedFor(urlEqualTo("/api/v1/repos/owner/repo/issues/comments/5")));
    }

    @Test
    void setsACommitStatus() throws IOException {
        wireMockExtension.stubFor(post(urlEqualTo("/api/v1/repos/owner/repo/statuses/abc123"))
                .withHeader("Authorization", equalTo("token secret"))
                .withRequestBody(equalToJson("{\"state\": \"failure\", \"target_url\": \"https://sonar/dashboard\","
                        + " \"description\": \"Quality Gate failed\", \"context\": \"SonarQube\"}"))
                .willReturn(aResponse().withStatus(201)));

        underTest.setCommitStatus("owner/repo", "abc123",
                new CommitStatus(CommitStatus.State.FAILURE, "https://sonar/dashboard", "Quality Gate failed", "SonarQube"));

        wireMockExtension.verify(postRequestedFor(urlEqualTo("/api/v1/repos/owner/repo/statuses/abc123")));
    }

    @Test
    void throwsWhenGiteaAnswersAnotherStatus() {
        wireMockExtension.stubFor(post(urlEqualTo("/api/v1/repos/owner/repo/statuses/abc123"))
                .willReturn(aResponse().withStatus(403)));

        CommitStatus status = new CommitStatus(CommitStatus.State.SUCCESS, "https://sonar/dashboard", "Quality Gate passed", "SonarQube");
        assertThatThrownBy(() -> underTest.setCommitStatus("owner/repo", "abc123", status))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Gitea answered POST " + wireMockExtension.baseUrl() + "/api/v1/repos/owner/repo/statuses/abc123 with 403, expected [201]");
    }

    @Test
    void readsAnOpenPullRequestAsOpen() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/repos/owner/repo/pulls/12"))
                .withHeader("Authorization", equalTo("token secret"))
                .willReturn(okJson("{\"number\": 12, \"state\": \"open\", \"merged_at\": null, \"closed_at\": null}")));

        assertThat(underTest.getPullRequestState("owner/repo", 12)).isEqualTo(ChangeRequestState.OPEN);
    }

    @Test
    void readsAMergedPullRequestAsClosedWhenItMerged() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/repos/owner/repo/pulls/12"))
                .willReturn(okJson("{\"number\": 12, \"state\": \"closed\", \"merged_at\": \"2026-10-01T10:00:00Z\","
                        + " \"closed_at\": \"2026-10-02T10:00:00Z\"}")));

        assertThat(underTest.getPullRequestState("owner/repo", 12).closedTime()).contains(Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    void readsAClosedPullRequestAsClosedWhenItClosed() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/repos/owner/repo/pulls/12"))
                .willReturn(okJson("{\"number\": 12, \"state\": \"closed\", \"merged_at\": null, \"closed_at\": \"2026-10-02T10:00:00Z\"}")));

        assertThat(underTest.getPullRequestState("owner/repo", 12).closedTime()).contains(Instant.parse("2026-10-02T10:00:00Z"));
    }

    @Test
    void readsTheCommitABranchPointsAt() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/repos/owner/repo/branches/feature%2Fone"))
                .withHeader("Authorization", equalTo("token secret"))
                .willReturn(okJson("{\"name\": \"feature/one\", \"commit\": {\"id\": \"abc123\"}}")));

        assertThat(underTest.getBranchHead("owner/repo", "feature/one")).contains("abc123");
    }

    @Test
    void readsNoCommitForADeletedBranch() throws IOException {
        wireMockExtension.stubFor(get(urlEqualTo("/api/v1/repos/owner/repo/branches/gone"))
                .willReturn(aResponse().withStatus(404)));

        assertThat(underTest.getBranchHead("owner/repo", "gone")).isEmpty();
    }
}
