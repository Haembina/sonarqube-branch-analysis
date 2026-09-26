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
import {
  BRANCH,
  CI_PULL_REQUEST,
  expectNoAxeViolations,
  expectVisibleFocus,
  openBranchesPage,
  PULL_REQUEST,
  rowOf,
  tabTo,
} from './sonarqube.helper';

test.describe('capability list-branches: see every branch and pull request of a project', () => {
  test('lists the branches, main first, then the pull requests on their tab', async ({ page }) => {
    await openBranchesPage(page);
    await expectNoAxeViolations(page);

    await expect(page).toHaveTitle(/Branches & Pull Requests/);
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    const header = page.getByRole('row').first();
    await expect(header).toContainText('Branch');
    await expect(header).toContainText('Last Analysis Date');
    await expect(header).toContainText('Keep when inactive');

    const rows = page.getByRole('row');
    await expect(rows.nth(1)).toContainText('main');
    await expect(rows.nth(1)).toContainText('Main Branch');
    await expect(rowOf(page, BRANCH)).toBeVisible();
    // The gate status is spelled out beside its colour.
    await expect(rowOf(page, BRANCH)).toContainText('Passed');

    await page.getByRole('tab', { name: 'Pull Requests' }).click();
    await expect(page.getByRole('tab', { name: 'Pull Requests' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await expect(rowOf(page, PULL_REQUEST)).toBeVisible();
    await expect(rowOf(page, CI_PULL_REQUEST)).toBeVisible();
    await expect(rowOf(page, BRANCH)).toHaveCount(0);
    await expectNoAxeViolations(page);
  });

  test('switches tabs by keyboard alone, and only when asked', async ({ page }) => {
    await openBranchesPage(page);
    const branchesTab = page.getByRole('tab', { name: 'Branches' });

    await tabTo(page, branchesTab);
    await expectVisibleFocus(page);
    await page.keyboard.press('Tab');
    await expect(page.getByRole('tab', { name: 'Pull Requests' })).toBeFocused();
    // Focus alone changes nothing on screen.
    await expect(rowOf(page, BRANCH)).toBeVisible();

    await page.keyboard.press('Enter');
    await expect(rowOf(page, PULL_REQUEST)).toBeVisible();
  });

  test('keeps a branch when it is inactive, from its switch', async ({ page }) => {
    await openBranchesPage(page);
    const keep = rowOf(page, BRANCH).getByRole('switch', {
      name: `Keep when inactive: ${BRANCH}`,
    });
    const kept = await keep.isChecked();

    await tabTo(page, keep);
    await page.keyboard.press('Space');
    await expect(keep).toBeChecked({ checked: !kept });

    await page.reload();
    await expect(rowOf(page, BRANCH).getByRole('switch')).toBeChecked({ checked: !kept });
    await expect(rowOf(page, 'main').getByRole('switch')).toBeDisabled();
  });
});
