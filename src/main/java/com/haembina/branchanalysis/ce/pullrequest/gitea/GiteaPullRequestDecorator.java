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
package com.haembina.branchanalysis.ce.pullrequest.gitea;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

import com.haembina.branchanalysis.InvalidConfigurationException;
import com.haembina.branchanalysis.almclient.gitea.GiteaClient;
import com.haembina.branchanalysis.almclient.gitea.GiteaClientFactory;
import com.haembina.branchanalysis.almclient.gitea.GiteaSettings;
import com.haembina.branchanalysis.almclient.gitea.model.Comment;
import com.haembina.branchanalysis.almclient.gitea.model.CommitStatus;
import com.haembina.branchanalysis.ce.pullrequest.AnalysisDetails;
import com.haembina.branchanalysis.ce.pullrequest.DecorationResult;
import com.haembina.branchanalysis.ce.pullrequest.markup.Bold;
import com.haembina.branchanalysis.ce.pullrequest.markup.Document;
import com.haembina.branchanalysis.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.haembina.branchanalysis.ce.pullrequest.markup.Text;
import com.haembina.branchanalysis.ce.pullrequest.report.AnalysisSummary;
import com.haembina.branchanalysis.ce.pullrequest.report.ReportGenerator;

public class GiteaPullRequestDecorator {

    static final String STATUS_CONTEXT = "SonarQube Code Analysis";
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile("[\\w.-]+/[\\w.-]+");

    private final ConfigurationRepository configurationRepository;
    private final GiteaClientFactory giteaClientFactory;
    private final ReportGenerator reportGenerator;
    private final MarkdownFormatterFactory markdownFormatterFactory;

    public GiteaPullRequestDecorator(ConfigurationRepository configurationRepository, GiteaClientFactory giteaClientFactory,
                                     ReportGenerator reportGenerator, MarkdownFormatterFactory markdownFormatterFactory) {
        this.configurationRepository = configurationRepository;
        this.giteaClientFactory = giteaClientFactory;
        this.reportGenerator = reportGenerator;
        this.markdownFormatterFactory = markdownFormatterFactory;
    }

    public boolean isConfigured() {
        return repository(configurationRepository.getConfiguration()).isPresent();
    }

    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysisDetails) {
        Configuration configuration = configurationRepository.getConfiguration();
        String repository = repository(configuration)
                .filter(value -> REPOSITORY_PATTERN.matcher(value).matches())
                .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT,
                        "Gitea repository must be set as owner/name"));
        GiteaClient client = giteaClientFactory.createClient(configuration);
        AnalysisSummary analysisSummary = reportGenerator.createAnalysisSummary(analysisDetails);
        long index = Long.parseLong(analysisDetails.getPullRequestId());

        try {
            client.setCommitStatus(repository, analysisDetails.getCommitSha(), commitStatus(analysisDetails, analysisSummary));
            postSummaryComment(client, repository, index, analysisSummary.format(markdownFormatterFactory),
                    analysisDetails.getAnalysisProjectKey());
            return DecorationResult.builder()
                    .withPullRequestUrl(client.getPullRequest(repository, index).htmlUrl())
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Gitea", ex);
        }
    }

    private static Optional<String> repository(Configuration configuration) {
        return configuration.get(GiteaSettings.REPOSITORY).map(String::strip).filter(StringUtils::isNotEmpty);
    }

    private static CommitStatus commitStatus(AnalysisDetails analysisDetails, AnalysisSummary analysisSummary) {
        boolean passed = analysisDetails.getQualityGateStatus() == QualityGate.Status.OK;
        return new CommitStatus(passed ? CommitStatus.State.SUCCESS : CommitStatus.State.FAILURE,
                analysisSummary.getDashboardUrl(), "Quality Gate " + (passed ? "passed" : "failed"), STATUS_CONTEXT);
    }

    private void postSummaryComment(GiteaClient client, String repository, long index, String summary, String projectKey) throws IOException {
        String projectMarker = markdownFormatterFactory.documentFormatter()
                .format(new Document(new Bold(new Text("Project ID:")), new Text(" " + projectKey)));
        long userId = client.getCurrentUser().id();

        Optional<Comment> summaryComment = client.getComments(repository, index).stream()
                .filter(comment -> comment.user().id() == userId)
                .filter(comment -> comment.body().lines().anyMatch(projectMarker::equals))
                .findFirst();

        if (summaryComment.isPresent()) {
            client.editComment(repository, summaryComment.get().id(), summary);
        } else {
            client.createComment(repository, index, summary);
        }
    }
}
