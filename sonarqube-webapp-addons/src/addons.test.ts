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
 */

import { addons } from './index';
import { measureUrl } from './project-freshness/measure-url.helper';
import ProjectAnalyses from './project-freshness/ProjectAnalyses';
import ReleaseMismatchAlert from './release-check/ReleaseMismatchAlert';

it('hands the webapp the release check, drawn as a global banner and on System Info', () => {
  expect(addons.monitoringAlerts?.MonitoringAlerts).toBe(ReleaseMismatchAlert);
});

it('hands the webapp every branch page it reads from the registry', () => {
  expect(Object.keys(addons.branches).sort()).toEqual([
    'BranchListSection',
    'Menu',
    'PRLink',
    'ProjectBranchSelector',
    'PullRequestOverview',
    'routes',
  ]);
  expect(typeof addons.branches.routes).toBe('function');
});

it('hands the projects page the analysis rows and measure links each card draws', () => {
  expect(addons.projectCard.ProjectAnalyses).toBe(ProjectAnalyses);
  expect(addons.projectCard.measureUrl).toBe(measureUrl);
});

it('hands the webapp the entitlement hooks', () => {
  expect(typeof addons.entitlements.useEntitlementCheckQuery).toBe('function');
  expect(typeof addons.entitlements.useEntitlementChecksQuery).toBe('function');
  expect(typeof addons.entitlements.usePurchasableFeature).toBe('function');
});
