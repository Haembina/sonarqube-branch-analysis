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

import { expect, test, type Locator, type Page } from '@playwright/test';
import { BRANCH, openBranchesPage, PROJECT, PULL_REQUEST, rowOf } from './sonarqube.helper';
import { common, namesAround, SWITCHER } from './wcag.helper';

/** The project pages the addons draw on or beside, each opened as a user reaches it. */
const PAGES: Record<string, (page: Page) => Promise<void>> = {
  overview: async (page) => {
    await page.goto(`/dashboard?id=${PROJECT}`);
    await expect(page.getByRole('button', { name: 'main', exact: true })).toBeVisible();
  },
  'pull request overview': async (page) => {
    await page.goto(`/dashboard?id=${PROJECT}&pullRequest=1`);
    await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible();
  },
  'branch list': openBranchesPage,
  'branch new code': async (page) => {
    await page.goto(`/project/baseline?id=${PROJECT}`);
    await expect(page.getByRole('row').filter({ hasText: BRANCH })).toBeVisible();
  },
};

/** The project sidebar, which SonarQube opens on hover or focus and draws on every page. */
const SIDEBAR = '[data-testid="sidebar-navigation-wrapper"]';

/** The global bar across the top of every page, which holds the Help menu. */
const GLOBAL_BAR = 'header, nav, [role="banner"]';

/**
 * Reads, on every page in `PAGES`, the names of the controls in the nearest `container` around
 * the node `anchor` finds there.
 */
async function namesOnEveryPage(page: Page, anchor: (page: Page) => Locator, container: string) {
  const lists: string[][] = [];
  for (const open of Object.values(PAGES)) {
    await open(page);
    lists.push(await namesAround(anchor(page).first(), container));
  }
  return lists;
}

test.describe('capability list-branches: see every branch and pull request of a project', () => {
  test('2.4.5: reaches the branch list from the project sidebar', async ({ page }) => {
    await PAGES.overview(page);
    // The sidebar is folded to no width until the header's button opens it.
    await page.getByRole('button', { name: 'Open sidebar' }).click();
    const sidebar = page.getByTestId('sidebar-navigation-wrapper');
    const link = sidebar.getByRole('link', { name: 'Branches & Pull Requests' });
    // The Project group folds its links away until it is opened, and may already be open.
    if (!(await link.isVisible())) {
      await sidebar.getByRole('button', { name: 'Project', exact: true }).click();
    }
    await link.click();
    await expect(page.getByRole('heading', { name: 'Branches & Pull Requests' })).toBeVisible();
  });

  test('2.4.5: reaches the branch list from the branch switcher', async ({ page }) => {
    await PAGES.overview(page);
    await page.getByRole('button', { name: 'main', exact: true }).click();
    await page.getByRole('dialog', { name: SWITCHER }).getByRole('link', { name: 'View all' }).click();
    await expect(page.getByRole('heading', { name: 'Branches & Pull Requests' })).toBeVisible();
  });
});

test.describe('WCAG 3.2.3 consistent navigation', () => {
  test('orders the project sidebar the same on every page', async ({ page }) => {
    const lists = await namesOnEveryPage(page, (on) => on.locator(SIDEBAR), SIDEBAR);
    const shared = lists.map((list) => common(list, lists));
    expect(shared[0].length).toBeGreaterThan(1);
    for (const list of shared) {
      expect(list).toEqual(shared[0]);
    }
  });
});

test.describe('WCAG 3.2.6 consistent help', () => {
  test('keeps the Help menu in the same place among the global controls on every page', async ({
    page,
  }) => {
    const lists = await namesOnEveryPage(
      page,
      (on) => on.getByRole('button', { name: 'Help', exact: true }),
      GLOBAL_BAR,
    );
    const shared = lists.map((list) => common(list, lists));
    expect(shared[0]).toContain('Help');
    for (const list of shared) {
      expect(list).toEqual(shared[0]);
    }
  });
});

test.describe('capability list-branches: see every branch and pull request of a project', () => {
  test('3.2.4: names each row’s actions the same way on both tabs of the branch list', async ({
    page,
  }) => {
    await openBranchesPage(page);
    await expect(rowOf(page, 'main').getByRole('button', { name: 'Update main' })).toBeVisible();
    await expect(rowOf(page, BRANCH).getByRole('button', { name: `Update ${BRANCH}` })).toBeVisible();

    await page.getByRole('tab', { name: 'Pull Requests' }).click();
    await expect(
      rowOf(page, PULL_REQUEST).getByRole('button', { name: `Update ${PULL_REQUEST}` }),
    ).toBeVisible();
  });

  test('3.2.4: words a pull request’s gate the same in the list and on its overview', async ({
    page,
  }) => {
    await openBranchesPage(page);
    await page.getByRole('tab', { name: 'Pull Requests' }).click();
    await expect(rowOf(page, PULL_REQUEST)).toContainText('Passed');

    await PAGES['pull request overview'](page);
    await expect(page.getByText('Passed', { exact: true }).first()).toBeVisible();
  });
});

test.describe('capability switch-branch: open another branch or pull request of a project', () => {
  test('3.2.4: labels the switcher the same on the branch and pull request overviews', async ({
    page,
  }) => {
    await PAGES.overview(page);
    await page.getByRole('button', { name: 'main', exact: true }).click();
    await expect(page.getByRole('dialog', { name: SWITCHER })).toBeVisible();

    await PAGES['pull request overview'](page);
    await page.getByRole('button', { name: PULL_REQUEST, exact: true }).click();
    await expect(page.getByRole('dialog', { name: SWITCHER })).toBeVisible();
  });
});
