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

// The end-to-end suite over the branch pages of a running SonarQube. It needs a server whose
// project already holds scripts/integration-test.sh's analyses, named by SONARQUBE_URL and
// SONARQUBE_PROJECT, which is how that script runs it:
//   bash scripts/integration-test.sh <jar> <webapp dir> npm run test:e2e

import { defineConfig, devices } from '@playwright/test';

/** How long one case may take, in milliseconds: a first page load compiles SonarQube's bundles. */
const CASE_TIMEOUT = 60_000;

export default defineConfig({
  testDir: 'e2e',
  testMatch: '*.test.ts',
  globalSetup: './e2e/global-setup.ts',
  timeout: CASE_TIMEOUT,
  forbidOnly: Boolean(process.env.CI),
  retries: 0,
  workers: 1, // One SonarQube and one project, whose branches the dialog specs rename and delete.
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'build/reports/e2e' }]],
  outputDir: 'build/e2e-results',
  use: {
    baseURL: process.env.SONARQUBE_URL ?? 'http://localhost:9000',
    // Outside outputDir, so an uploaded failure artifact carries no session.
    storageState: 'build/e2e-auth/storage-state.json',
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
  projects: [
    { name: 'pages', testIgnore: 'branch-dialogs.test.ts' },
    // The dialogs change the project the other specs read, so they run last.
    { name: 'dialogs', testMatch: 'branch-dialogs.test.ts', dependencies: ['pages'] },
  ],
});
