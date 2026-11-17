/*
 * Copyright (C) 2020-2026 Michael Clarke
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
package com.haembina.branchanalysis;

import com.haembina.branchanalysis.almclient.azuredevops.DefaultAzureDevopsClientFactory;
import com.haembina.branchanalysis.almclient.bitbucket.DefaultBitbucketClientFactory;
import com.haembina.branchanalysis.almclient.bitbucket.HttpClientBuilderFactory;
import com.haembina.branchanalysis.almclient.gitea.GiteaSettings;
import com.haembina.branchanalysis.almclient.github.GithubClientFactory;
import com.haembina.branchanalysis.almclient.gitlab.DefaultGitlabClientFactory;
import com.haembina.branchanalysis.almclient.gitlab.DefaultLinkHeaderReader;
import com.haembina.branchanalysis.ce.CommunityReportAnalysisComponentProvider;
import com.haembina.branchanalysis.ce.pullrequest.DecorationSettings;
import com.haembina.branchanalysis.almclient.gitea.GiteaClientFactory;
import com.haembina.branchanalysis.housekeeping.AnalysisCleaner;
import com.haembina.branchanalysis.housekeeping.AnalysisCleanerSchedule;
import com.haembina.branchanalysis.housekeeping.HousekeepingSettings;
import com.haembina.branchanalysis.housekeeping.forge.ForgeReaderFactory;
import com.haembina.branchanalysis.scanner.BranchConfigurationFactory;
import com.haembina.branchanalysis.scanner.CommunityBranchConfigurationLoader;
import com.haembina.branchanalysis.scanner.CommunityBranchParamsValidator;
import com.haembina.branchanalysis.scanner.CommunityProjectBranchesLoader;
import com.haembina.branchanalysis.scanner.ScannerPullRequestPropertySensor;
import com.haembina.branchanalysis.scanner.autoconfiguration.AzureDevopsAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.BitbucketPipelinesAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.CirrusCiAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.CodeMagicAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.GithubActionsAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.GitlabCiAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.JenkinsAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.BuildkiteAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.CircleCiAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.DroneAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.TeamCityAutoConfigurer;
import com.haembina.branchanalysis.scanner.autoconfiguration.WoodpeckerAutoConfigurer;
import com.haembina.branchanalysis.server.CommunityBranchFeatureExtension;
import com.haembina.branchanalysis.server.CommunityBranchSupportDelegate;
import com.haembina.branchanalysis.server.MonoRepoFeature;
import com.haembina.branchanalysis.server.pullrequest.validator.AzureDevopsValidator;
import com.haembina.branchanalysis.server.pullrequest.validator.BitbucketValidator;
import com.haembina.branchanalysis.server.pullrequest.validator.GithubValidator;
import com.haembina.branchanalysis.server.pullrequest.validator.GitlabValidator;
import com.haembina.branchanalysis.server.pullrequest.ws.binding.action.DeleteBindingAction;
import com.haembina.branchanalysis.server.pullrequest.ws.binding.action.SetAzureBindingAction;
import com.haembina.branchanalysis.server.pullrequest.ws.binding.action.SetBitbucketBindingAction;
import com.haembina.branchanalysis.server.pullrequest.ws.binding.action.SetBitbucketCloudBindingAction;
import com.haembina.branchanalysis.server.pullrequest.ws.binding.action.SetGithubBindingAction;
import com.haembina.branchanalysis.server.pullrequest.ws.binding.action.SetGitlabBindingAction;
import com.haembina.branchanalysis.server.pullrequest.ws.binding.action.ValidateBindingAction;
import com.haembina.branchanalysis.server.pullrequest.ws.pullrequest.PullRequestWs;
import com.haembina.branchanalysis.server.pullrequest.ws.pullrequest.action.DeleteAction;
import com.haembina.branchanalysis.server.pullrequest.ws.pullrequest.action.ListAction;
import com.haembina.branchanalysis.server.pullrequest.ws.support.SupportWs;
import com.haembina.branchanalysis.server.pullrequest.ws.support.action.InfoWsAction;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;
import org.sonar.core.extension.CoreExtension;

/**
 * Registers the plugin's settings and its web server, compute engine and scanner extensions.
 */
public class CommunityBranchPlugin implements Plugin, CoreExtension {

    public static final String IMAGE_URL_BASE = "com.haembina.branchanalysis.image-url-base";

    /**
     * Whether a scan with no branch or pull request parameters reads its CI environment to pick one. {@code true}
     * unless set otherwise; with {@code false} such a scan analyses the main branch.
     */
    public static final String AUTO_CONFIGURATION_ENABLED = "com.haembina.branchanalysis.auto-configuration.enabled";

    @Override
    public String getName() {
        return "Haembina Branch Analysis";
    }

    @Override
    public void load(CoreExtension.Context context) {
        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityReportAnalysisComponentProvider.class);
        } else if (SonarQubeSide.SERVER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityBranchFeatureExtension.class, CommunityBranchSupportDelegate.class,
                                  DeleteBindingAction.class,
                                  SetGithubBindingAction.class,
                                  SetAzureBindingAction.class,
                                  SetBitbucketBindingAction.class,
                                  SetBitbucketCloudBindingAction.class,
                                  SetGitlabBindingAction.class,
                    ValidateBindingAction.class,
                    DeleteAction.class,
                    ListAction.class,
                    PullRequestWs.class,

                    GithubValidator.class,
                    GithubClientFactory.class,
                    DefaultLinkHeaderReader.class,
                    HttpClientBuilderFactory.class,
                    DefaultBitbucketClientFactory.class,
                    BitbucketValidator.class,
                    GitlabValidator.class,
                    DefaultGitlabClientFactory.class,
                    DefaultAzureDevopsClientFactory.class,
                    AzureDevopsValidator.class,

                    InfoWsAction.class,
                    SupportWs.class,

                    GiteaClientFactory.class,
                    ForgeReaderFactory.class,
                    AnalysisCleaner.class,
                    AnalysisCleanerSchedule.class);

        }

        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide() ||
            SonarQubeSide.SERVER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(PropertyDefinition.builder(IMAGE_URL_BASE)
                                          .category(CoreProperties.CATEGORY_GENERAL)
                                          .subCategory(CoreProperties.SUBCATEGORY_GENERAL)
                                          .onConfigScopes(ConfigScope.APP)
                                          .name("Images base URL")
                                          .description("Base URL used to load the images for the PR comments (please use this only if images are not displayed properly).")
                                          .type(PropertyType.STRING)
                                          .build(),
                PropertyDefinition.builder(AUTO_CONFIGURATION_ENABLED)
                                          .category(CoreProperties.CATEGORY_GENERAL)
                                          .subCategory(CoreProperties.SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS)
                                          .onConfigScopes(ConfigScope.PROJECT)
                                          .name("Detect branches and pull requests from CI")
                                          .description("When a scan names no branch or pull request, read the CI environment "
                                                + "(GitHub Actions, GitLab CI, Azure Pipelines and others) to decide which one it is. "
                                                + "When disabled, such a scan analyses the main branch.")
                                          .type(PropertyType.BOOLEAN)
                                          .defaultValue("true")
                                          .build(),
                MonoRepoFeature.class);
            context.addExtensions(GiteaSettings.definitions());
            context.addExtensions(DecorationSettings.definitions());
            context.addExtensions(HousekeepingSettings.definitions());

        }
    }

    @Override
    public void define(Plugin.Context context) {
        if (SonarQubeSide.SCANNER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityProjectBranchesLoader.class,
                                  CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class,
                                  ScannerPullRequestPropertySensor.class, BranchConfigurationFactory.class,
                                  AzureDevopsAutoConfigurer.class, BitbucketPipelinesAutoConfigurer.class,
                                  CirrusCiAutoConfigurer.class, CodeMagicAutoConfigurer.class,
                                  GithubActionsAutoConfigurer.class, GitlabCiAutoConfigurer.class,
                                  JenkinsAutoConfigurer.class, WoodpeckerAutoConfigurer.class,
                                  BuildkiteAutoConfigurer.class, CircleCiAutoConfigurer.class,
                                  TeamCityAutoConfigurer.class, DroneAutoConfigurer.class);
        }
    }
}
