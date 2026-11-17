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
package com.haembina.branchanalysis.housekeeping.forge;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.setting.ProjectConfigurationLoader;

import com.haembina.branchanalysis.almclient.azuredevops.AzureDevopsClient;
import com.haembina.branchanalysis.almclient.azuredevops.AzureDevopsClientFactory;
import com.haembina.branchanalysis.almclient.bitbucket.BitbucketClient;
import com.haembina.branchanalysis.almclient.bitbucket.BitbucketClientFactory;
import com.haembina.branchanalysis.almclient.gitea.GiteaClient;
import com.haembina.branchanalysis.almclient.gitea.GiteaClientFactory;
import com.haembina.branchanalysis.almclient.gitea.GiteaSettings;
import com.haembina.branchanalysis.almclient.github.GithubClientFactory;
import com.haembina.branchanalysis.almclient.gitlab.GitlabClient;
import com.haembina.branchanalysis.almclient.gitlab.GitlabClientFactory;

@ServerSide
public class ForgeReaderFactory {

    private final DbClient dbClient;
    private final ProjectConfigurationLoader projectConfigurationLoader;
    private final GithubClientFactory githubClientFactory;
    private final GitlabClientFactory gitlabClientFactory;
    private final AzureDevopsClientFactory azureDevopsClientFactory;
    private final BitbucketClientFactory bitbucketClientFactory;
    private final GiteaClientFactory giteaClientFactory;

    public ForgeReaderFactory(DbClient dbClient, ProjectConfigurationLoader projectConfigurationLoader, GithubClientFactory githubClientFactory,
                              GitlabClientFactory gitlabClientFactory, AzureDevopsClientFactory azureDevopsClientFactory,
                              BitbucketClientFactory bitbucketClientFactory, GiteaClientFactory giteaClientFactory) {
        this.dbClient = dbClient;
        this.projectConfigurationLoader = projectConfigurationLoader;
        this.githubClientFactory = githubClientFactory;
        this.gitlabClientFactory = gitlabClientFactory;
        this.azureDevopsClientFactory = azureDevopsClientFactory;
        this.bitbucketClientFactory = bitbucketClientFactory;
        this.giteaClientFactory = giteaClientFactory;
    }

    public Optional<ForgeReader> forProject(DbSession dbSession, ProjectDto project) throws IOException {
        Optional<ProjectAlmSettingDto> binding = dbClient.projectAlmSettingDao().selectByProject(dbSession, project);
        if (binding.isPresent()) {
            Optional<AlmSettingDto> almSetting = dbClient.almSettingDao().selectByUuid(dbSession, binding.get().getAlmSettingUuid());
            return almSetting.isPresent() ? Optional.of(bound(almSetting.get(), binding.get())) : Optional.empty();
        }
        Configuration configuration = projectConfigurationLoader.loadProjectConfiguration(dbSession, project.getUuid());
        Optional<String> giteaRepository = configuration.get(GiteaSettings.REPOSITORY).map(String::strip).filter(StringUtils::isNotEmpty);
        if (giteaRepository.isEmpty()) {
            return Optional.empty();
        }
        GiteaClient client = giteaClientFactory.createClient(configuration);
        String repository = giteaRepository.get();
        return Optional.of(new DelegatingForgeReader(key -> client.getPullRequestState(repository, Long.parseLong(key)),
            branch -> client.getBranchHead(repository, branch)));
    }

    private ForgeReader bound(AlmSettingDto almSetting, ProjectAlmSettingDto binding) throws IOException {
        String repository = binding.getAlmRepo();
        return switch (almSetting.getAlm()) {
            case GITHUB -> new GithubForgeReader(githubClientFactory.createClient(almSetting, binding).getRepository(repository));
            case GITLAB -> {
                GitlabClient client = gitlabClientFactory.createClient(binding, almSetting);
                yield new DelegatingForgeReader(key -> client.getMergeRequestState(repository, Long.parseLong(key)),
                    branch -> client.getBranchHead(repository, branch));
            }
            case AZURE_DEVOPS -> {
                AzureDevopsClient client = azureDevopsClientFactory.createClient(binding, almSetting);
                String azureProject = binding.getAlmSlug();
                yield new DelegatingForgeReader(key -> client.retrievePullRequestState(azureProject, repository, Integer.parseInt(key)),
                    branch -> client.retrieveBranchHead(azureProject, repository, branch));
            }
            case BITBUCKET, BITBUCKET_CLOUD -> {
                BitbucketClient client = bitbucketClientFactory.createClient(binding, almSetting);
                yield new DelegatingForgeReader(client::retrievePullRequestState, client::retrieveBranchHead);
            }
        };
    }
}
