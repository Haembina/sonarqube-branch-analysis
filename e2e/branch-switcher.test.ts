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

import { expect, test, type Page } from '@playwright/test';
import {
  BRANCH,
  expectNoAxeViolations,
  expectVisibleFocus,
  OPEN_DIALOG,
  PROJECT,
  PULL_REQUEST,
  tabTo,
} from './sonarqube.helper';

/** The switcher's popover: a dialog named, like its heading, for what it lists. */
function switcherOf(page: Page) {
  return page.getByRole('dialog', { name: 'Branches and pull requests' });
}

async function openSwitcher(page: Page) {
  await page.goto(`/dashboard?id=${PROJECT}`);
  const button = page.getByRole('button', { name: 'main', exact: true });
  await button.click();
  const popover = switcherOf(page);
  await expect(popover).toBeVisible();
  return { button, popover };
}

test.describe('capability switch-branch: open another branch or pull request of a project', () => {
  test('lists main first and opens the branch a user picks', async ({ page }) => {
    const { popover } = await openSwitcher(page);
    await expectNoAxeViolations(page, OPEN_DIALOG);

    await expect(popover.getByRole('listitem').first()).toContainText('main');
    // A second way to the branches page, beside the project's own navigation.
    await expect(popover.getByRole('link', { name: 'View all' })).toHaveAttribute(
      'href',
      `/project/branches?id=${PROJECT}`,
    );
    await expect(popover.getByRole('link', { name: new RegExp(BRANCH) })).toBeVisible();
    await expect(popover.getByRole('link', { name: new RegExp(PULL_REQUEST) })).toBeVisible();

    await popover.getByRole('link', { name: new RegExp(BRANCH) }).click();
    await expect(page).toHaveURL(new RegExp(`branch=${encodeURIComponent(BRANCH)}`));
    await expect(page.getByRole('button', { name: BRANCH, exact: true })).toBeVisible();
  });

  test('filters on what the user types, and narrows to pull requests', async ({ page }) => {
    const { popover } = await openSwitcher(page);
    const url = page.url();

    await popover.getByRole('searchbox', { name: 'Search branches or pull requests' }).fill('ci');
    // Typing filters in place; it navigates nowhere.
    await expect(page).toHaveURL(url);
    await expect(popover.getByRole('link', { name: /7 – feature\/ci/ })).toBeVisible();
    await expect(popover.getByRole('link', { name: new RegExp(BRANCH) })).toHaveCount(0);

    await popover.getByRole('searchbox').fill('nothing matches this');
    await expect(popover.getByText('No results')).toBeVisible();

    await popover.getByRole('searchbox').fill('');
    await popover.getByRole('radio', { name: 'Pull Requests' }).click();
    await expect(popover.getByRole('link', { name: new RegExp(PULL_REQUEST) })).toBeVisible();
    await expectNoAxeViolations(page, OPEN_DIALOG);
    await expect(popover.getByRole('link', { name: new RegExp(BRANCH) })).toHaveCount(0);
  });

  test('opens, walks and leaves by keyboard alone', async ({ page }) => {
    await page.goto(`/dashboard?id=${PROJECT}`);
    const button = page.getByRole('button', { name: 'main', exact: true });
    await expect(button).toBeVisible();
    await expectNoAxeViolations(page);

    await tabTo(page, button);
    await expectVisibleFocus(page);
    await page.keyboard.press('Enter');
    const popover = switcherOf(page);
    await expect(popover).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(popover).toBeHidden();
    await expect(button).toBeFocused();

    await page.keyboard.press('Enter');
    await expect(popover).toBeVisible();
    // Tab walks the search box, the filters and "View all" before the entries.
    await tabTo(page, popover.getByRole('link', { name: new RegExp(BRANCH) }));
    await expectVisibleFocus(page);
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(new RegExp(`branch=${encodeURIComponent(BRANCH)}`));
  });
});
