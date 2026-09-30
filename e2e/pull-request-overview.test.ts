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

import { expect, test } from '@playwright/test';
import { expectNoAxeViolations, expectVisibleFocus, PROJECT, tabTo } from './sonarqube.helper';

test.describe('capability pull-request-overview: read the quality gate and new-code measures of a pull request', () => {
  test('shows the gate, the branches it merges and the new-code measures', async ({ page }) => {
    await page.goto(`/dashboard?id=${PROJECT}&pullRequest=1`);

    await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible();
    await expect(page).toHaveTitle(/Overview/);
    // The gate's state is a word, not only a colour.
    await expect(page.getByText('Passed', { exact: true }).first()).toBeVisible();
    await expect(
      page.getByText('for merge into main from feature/pull-request').first(),
    ).toBeVisible();

    const newIssues = page.getByTestId('overview__measures-new_violations');
    await expect(newIssues).toContainText('New issues');
    await expect(page.getByTestId('overview__measures-new_accepted_issues')).toBeVisible();
    await expect(page.getByTestId('overview__measures-pull_request_fixed_issues')).toBeVisible();
    const box = await newIssues.boundingBox();
    expect(box?.width).toBeGreaterThan(0);
    expect(box?.height).toBeGreaterThan(0);
    await expectNoAxeViolations(page);
  });

  test('reaches the new issues of the pull request by keyboard alone', async ({ page }) => {
    await page.goto(`/dashboard?id=${PROJECT}&pullRequest=1`);
    const issues = page.getByTestId('overview__measures-new_violations').getByRole('link');

    await tabTo(page, issues);
    await expectVisibleFocus(page);
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(/\/project\/issues\?.*pullRequest=1/);
  });
});
