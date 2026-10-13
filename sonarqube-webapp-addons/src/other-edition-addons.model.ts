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
import type { HttpHandler } from 'msw';
import type { ComponentType, ReactNode } from 'react';

type EditionComponent = ComponentType<Record<string, unknown>>;

type EditionRoutes = () => ReactNode;

type BadgeExpiration = string | number | Date;

export interface OtherEditionAddons {
  aica?: {
    AICA_SETTINGS_PATH: string;
    ProfileAICodeSuggestionBanner: EditionComponent;
    ProfileRecommendedForAiIcon: EditionComponent;
    aicaSettingsRoutes: EditionRoutes;
    getAICodeSettingsUrl: (componentKey: string, qualifier: string) => string;
    isQualityProfileRecommendedForAI?: (profile: unknown) => boolean;
  };
  applicationReport?: { componentRoutes: EditionRoutes };
  applications?: { componentRoutes: EditionRoutes };
  architecture?: {
    ArchitectureAddonProvider: EditionComponent;
    ArchitectureNavMenu: EditionComponent;
    OpenIntendedArchitecture: EditionComponent;
    externalInterfacesRoutes?: EditionRoutes;
    getArchitectureDevMockHandlers?: () => HttpHandler[];
    orgArchitectureRoutes?: EditionRoutes;
    patternsRoutes?: EditionRoutes;
    policiesRoutes?: EditionRoutes;
    routes: EditionRoutes;
  };
  issueSandbox?: {
    SandboxIssuesIntro?: EditionComponent;
    SandboxSettingContainer: EditionComponent;
  };
  jira?: {
    InstanceJiraBinding: EditionComponent;
    IssueJiraWorkItem: EditionComponent;
    JiraProjectBinding: EditionComponent;
    JiraTicketStatusFacet: EditionComponent;
  };
  license?: {
    StartupLicenseCheckBanner: EditionComponent;
    getLicenseDevMockHandlers?: () => HttpHandler[];
    routes: EditionRoutes;
  };
  monitoringAlerts?: { MonitoringAlerts: EditionComponent };
  portfolios?: {
    PortfolioDashboardsListRoute?: string;
    PortfolioHealthDashboardDefaultKey?: string;
    componentAdminRoutes: EditionRoutes;
    componentRoutes: EditionRoutes;
    getPortfolioHealthDashboardRoute?: (dashboardKey: string, componentKey: string) => string;
    globalAdminRoutes: EditionRoutes;
    globalRoutes: EditionRoutes;
  };
  remediationAgent?: {
    AI_CAPABILITIES_NEW_BADGE_EXPIRATION_DATE: BadgeExpiration;
    AI_CAPABILITIES_SETTINGS_PATH: string;
    AgentUnsupportedIssuesMessage: EditionComponent;
    BacklogJobAssignButton: EditionComponent;
    PROJECT_AGENT_ACTIVITY_NEW_BADGE_EXPIRATION_DATE: BadgeExpiration;
    RemediationAgentAddOnBanner: EditionComponent;
    globalAdminRoutes: EditionRoutes;
    projectRoutes: EditionRoutes;
    projectSettingsRoutes: EditionRoutes;
  };
  sca?: {
    LICENSE_ROUTE_NAME: string;
    PROJECT_LICENSE_ROUTE_NAME: string;
    ScaReportOverviewOptions: EditionComponent;
    getReleasesUrl: (options: { newParams: Record<string, string> }) => string;
    licenseRoutes: ReactNode;
    projectRoutes: ReactNode;
  };
  securityReports?: { componentRoutes: EditionRoutes };
  slack?: { SlackIntegrationConfiguration: EditionComponent };
}
