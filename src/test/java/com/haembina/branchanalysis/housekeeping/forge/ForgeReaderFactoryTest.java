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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.setting.ProjectConfigurationLoader;

import com.haembina.branchanalysis.almclient.ChangeRequestState;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForgeReaderFactoryTest {

    private static final ChangeRequestState CLOSED = ChangeRequestState.closedAt("2026-10-01T10:00:00Z");

    private final DbClient dbClient = mock();
    private final DbSession dbSession = mock();
    private final ProjectAlmSettingDao projectAlmSettingDao = mock();
    private final AlmSettingDao almSettingDao = mock();
    private final ProjectConfigurationLoader configurationLoader = mock();
    private final GithubClientFactory githubClientFactory = mock();
    private final GitlabClientFactory gitlabClientFactory = mock();
    private final AzureDevopsClientFactory azureDevopsClientFactory = mock();
    private final BitbucketClientFactory bitbucketClientFactory = mock();
    private final GiteaClientFactory giteaClientFactory = mock();
    private final ProjectDto project = new ProjectDto().setUuid("project-uuid").setKey("project");
    private final ProjectAlmSettingDto binding = new ProjectAlmSettingDto().setAlmSettingUuid("alm-uuid").setAlmRepo("owner/repo").setAlmSlug("azure-project");
    private final MapSettings settings = new MapSettings();
    private final ForgeReaderFactory underTest = new ForgeReaderFactory(dbClient, configurationLoader, githubClientFactory, gitlabClientFactory,
        azureDevopsClientFactory, bitbucketClientFactory, giteaClientFactory);

    @BeforeEach
    void setUp() {
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        when(configurationLoader.loadProjectConfiguration(dbSession, "project-uuid")).thenReturn(settings.asConfig());
    }

    private AlmSettingDto boundTo(ALM alm) {
        AlmSettingDto almSetting = new AlmSettingDto().setAlm(alm);
        when(projectAlmSettingDao.selectByProject(dbSession, project)).thenReturn(Optional.of(binding));
        when(almSettingDao.selectByUuid(dbSession, "alm-uuid")).thenReturn(Optional.of(almSetting));
        return almSetting;
    }

    @Test
    void readsGithubThroughTheBoundRepository() throws IOException {
        AlmSettingDto almSetting = boundTo(ALM.GITHUB);
        GitHub github = mock();
        GHRepository repository = mock();
        when(githubClientFactory.createClient(almSetting, binding)).thenReturn(github);
        when(github.getRepository("owner/repo")).thenReturn(repository);

        assertThat(underTest.forProject(dbSession, project)).get().isInstanceOf(GithubForgeReader.class);
    }

    @Test
    void readsGitlabThroughTheBoundProject() throws IOException {
        AlmSettingDto almSetting = boundTo(ALM.GITLAB);
        GitlabClient client = mock();
        when(gitlabClientFactory.createClient(binding, almSetting)).thenReturn(client);
        when(client.getMergeRequestState("owner/repo", 7)).thenReturn(CLOSED);
        when(client.getBranchHead("owner/repo", "main")).thenReturn(Optional.of("abc123"));

        ForgeReader reader = underTest.forProject(dbSession, project).orElseThrow();

        assertThat(reader.pullRequest("7")).isEqualTo(CLOSED);
        assertThat(reader.branchHead("main")).contains("abc123");
    }

    @Test
    void readsAzureDevopsThroughTheBoundProjectAndRepository() throws IOException {
        AlmSettingDto almSetting = boundTo(ALM.AZURE_DEVOPS);
        AzureDevopsClient client = mock();
        when(azureDevopsClientFactory.createClient(binding, almSetting)).thenReturn(client);
        when(client.retrievePullRequestState("azure-project", "owner/repo", 7)).thenReturn(CLOSED);
        when(client.retrieveBranchHead("azure-project", "owner/repo", "main")).thenReturn(Optional.of("abc123"));

        ForgeReader reader = underTest.forProject(dbSession, project).orElseThrow();

        assertThat(reader.pullRequest("7")).isEqualTo(CLOSED);
        assertThat(reader.branchHead("main")).contains("abc123");
    }

    @Test
    void readsBitbucketServerAndCloudThroughTheBoundRepository() throws IOException {
        for (ALM alm : new ALM[] {ALM.BITBUCKET, ALM.BITBUCKET_CLOUD}) {
            AlmSettingDto almSetting = boundTo(alm);
            BitbucketClient client = mock();
            when(bitbucketClientFactory.createClient(binding, almSetting)).thenReturn(client);
            when(client.retrievePullRequestState("7")).thenReturn(CLOSED);
            when(client.retrieveBranchHead("main")).thenReturn(Optional.empty());

            ForgeReader reader = underTest.forProject(dbSession, project).orElseThrow();

            assertThat(reader.pullRequest("7")).isEqualTo(CLOSED);
            assertThat(reader.branchHead("main")).isEmpty();
        }
    }

    @Test
    void readsNothingWhenTheBindingNamesAMissingConnection() throws IOException {
        when(projectAlmSettingDao.selectByProject(dbSession, project)).thenReturn(Optional.of(binding));
        when(almSettingDao.selectByUuid(dbSession, "alm-uuid")).thenReturn(Optional.empty());

        assertThat(underTest.forProject(dbSession, project)).isEmpty();
    }

    @Test
    void readsGiteaThroughTheProjectsGiteaSettings() throws IOException {
        when(projectAlmSettingDao.selectByProject(dbSession, project)).thenReturn(Optional.empty());
        settings.setProperty(GiteaSettings.REPOSITORY, " owner/repo ");
        GiteaClient client = mock();
        when(giteaClientFactory.createClient(any())).thenReturn(client);
        when(client.getPullRequestState("owner/repo", 7)).thenReturn(CLOSED);
        when(client.getBranchHead("owner/repo", "main")).thenReturn(Optional.of("abc123"));

        ForgeReader reader = underTest.forProject(dbSession, project).orElseThrow();

        assertThat(reader.pullRequest("7")).isEqualTo(CLOSED);
        assertThat(reader.branchHead("main")).contains("abc123");
    }

    @Test
    void readsNothingForAProjectBoundToNoForge() throws IOException {
        when(projectAlmSettingDao.selectByProject(dbSession, project)).thenReturn(Optional.empty());
        settings.setProperty(GiteaSettings.REPOSITORY, " ");

        assertThat(underTest.forProject(dbSession, project)).isEmpty();
    }
}
