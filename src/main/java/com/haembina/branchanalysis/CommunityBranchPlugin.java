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
import com.haembina.branchanalysis.almclient.github.GithubClientFactory;
import com.haembina.branchanalysis.almclient.gitlab.DefaultGitlabClientFactory;
import com.haembina.branchanalysis.almclient.gitlab.DefaultLinkHeaderReader;
import com.haembina.branchanalysis.ce.CommunityReportAnalysisComponentProvider;
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
import org.sonar.core.config.PurgeConstants;
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

                /* org.sonar.db.purge.PurgeConfiguration uses the value for the this property if it's configured, so it only
                needs to be specified here, but doesn't need any additional classes to perform the relevant purge/cleanup
                */
                                  PropertyDefinition
                                          .builder(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_BRANCHES_AND_PRS)
                                          .name("Number of days before purging inactive branches and pull requests")
                                          .description(
                                                  "Branches and pull requests are permanently deleted when there has been no analysis for the configured number of days.")
                                          .category(CoreProperties.CATEGORY_HOUSEKEEPING)
                                          .subCategory(CoreProperties.SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS).defaultValue("30")
                                          .type(PropertyType.INTEGER)
                                          .index(1)
                                          .build()
                                  ,

                                  PropertyDefinition
                                          .builder(PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE)
                                          .name("Branches to keep when inactive")
                                          .description("By default, branches and pull requests are automatically deleted when inactive. This setting allows you "
                                                + "to protect branches (but not pull requests) from this deletion. When a branch is created with a name that "
                                                + "matches any of the regular expressions on the list of values of this setting, the branch will not be deleted "
                                                + "automatically even when it becomes inactive. Example:"
                                                + "<ul><li>develop</li><li>release-.*</li></ul>")
                                          .category(CoreProperties.CATEGORY_HOUSEKEEPING)
                                          .subCategory(CoreProperties.SUBCATEGORY_BRANCHES_AND_PULL_REQUESTS)
                                          .multiValues(true)
                                          .defaultValue("main,master,develop,trunk")
                                          .onConfigScopes(ConfigScope.PROJECT)
                                          .index(2)
                                          .build()

                                 );

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
                                  JenkinsAutoConfigurer.class);
        }
    }
}
