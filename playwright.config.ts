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

import { playwrightGpuPreset } from '@haembina/config';
import { defineConfig, devices } from '@playwright/test';

const CASE_TIMEOUT = 60_000;

const EXPECT_TIMEOUT = 20_000;

const NEW_CODE = 'branch-new-code.test.ts';

const INPUT = 'wcag-input.test.ts';

const DIALOGS = 'branch-dialogs.test.ts';

export default defineConfig({
  testDir: 'e2e',
  testMatch: '*.test.ts',
  globalSetup: './e2e/global.setup.ts',
  timeout: CASE_TIMEOUT,
  expect: { timeout: EXPECT_TIMEOUT },
  forbidOnly: Boolean(process.env.CI),
  retries: 0,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'build/reports/e2e' }],
    [
      '@haembina/accessibility/reporter',
      { absences: ['no-media', 'no-timing', 'no-pointer-gestures', 'no-motion-actuation'] },
    ],
  ],
  outputDir: 'build/e2e-results',
  use: {
    baseURL: process.env.SONARQUBE_URL ?? 'http://localhost:9000',
    storageState: 'build/e2e-auth/storage-state.json',
    trace: 'retain-on-failure',
    bypassCSP: true,
    ...devices['Desktop Chrome'],
    ...playwrightGpuPreset(),
  },
  projects: [
    { name: 'pages', testIgnore: [NEW_CODE, INPUT, DIALOGS] },
    { name: 'new-code', testMatch: NEW_CODE, dependencies: ['pages'] },
    { name: 'input', testMatch: INPUT, dependencies: ['new-code'] },
    { name: 'dialogs', testMatch: DIALOGS, dependencies: ['input'] },
  ],
});
