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

import { expect, type Locator, type Page } from '@playwright/test';
import { BRANCH, openBranchesPage, PROJECT } from './sonarqube.helper';

/** A surface the addons draw, and the node that holds it once the page has loaded. */
export interface AddonSurface {
  name: string;
  open: (page: Page) => Promise<Locator>;
}

/** The label of the branch switcher's popover, on every project page that draws it. */
export const SWITCHER = 'Branches and pull requests';

/*
 * The four surfaces the addons draw, one per capability the specs title them under. Each
 * `open` navigates and returns the node the addons' markup sits in, so a measurement never
 * reaches SonarSource's own chrome.
 */

/** The branch list, where the list-branches capability is used. */
export const BRANCH_LIST: AddonSurface = {
  name: 'branch list',
  open: async (page) => {
    await openBranchesPage(page);
    return page.locator('#project-branch-like');
  },
};

/** The branch new code table, where the set-branch-new-code capability is used. */
export const BRANCH_NEW_CODE: AddonSurface = {
  name: 'branch new code',
  open: async (page) => {
    await page.goto(`/project/baseline?id=${PROJECT}`);
    const table = page
      .getByRole('table')
      .filter({ has: page.getByRole('row').filter({ hasText: BRANCH }) });
    await expect(table).toBeVisible();
    return table;
  },
};

/** The pull request overview, where the pull-request-overview capability is used. */
export const PULL_REQUEST_OVERVIEW: AddonSurface = {
  name: 'pull request overview',
  open: async (page) => {
    await page.goto(`/dashboard?id=${PROJECT}&pullRequest=1`);
    await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible();
    // The heading draws before the measures load; a read in between sees only "Loading".
    await expect(page.getByTestId('overview__measures-new_violations')).toBeVisible();
    return page.getByRole('main');
  },
};

/** The branch switcher's popover, where the switch-branch capability is used. */
export const BRANCH_SWITCHER: AddonSurface = {
  name: 'branch switcher',
  open: async (page) => {
    await page.goto(`/dashboard?id=${PROJECT}`);
    await page.getByRole('button', { name: 'main', exact: true }).click();
    const popover = page.getByRole('dialog', { name: SWITCHER });
    await expect(popover).toBeVisible();
    return popover;
  },
};

/**
 * Asserts `text` sits inside a live region, so a screen reader announces it without moving
 * focus to it (WCAG 4.1.3): an ancestor carrying `aria-live` other than `off`, or the role
 * `status`, `alert` or `log`.
 */
export async function expectAnnounced(text: Locator): Promise<void> {
  await expect(text).toBeVisible();
  const live = await text.evaluate((element) => {
    const region = element.closest('[aria-live], [role="status"], [role="alert"], [role="log"]');
    return region !== null && region.getAttribute('aria-live') !== 'off';
  });
  expect(live).toBe(true);
}

/**
 * The accessible names of the controls in the navigation holding `anchor`, in document order:
 * each link's and button's `aria-label`, or its text.
 */
export async function namesAround(anchor: Locator, container: string): Promise<string[]> {
  return anchor.evaluate((element, selector) => {
    const holder = element.closest(selector) ?? document.body;
    return Array.from(holder.querySelectorAll('a, button')).map(
      (control) => control.getAttribute('aria-label') ?? control.textContent?.trim() ?? '',
    );
  }, container);
}

/** Keeps from `names` only the entries every one of `lists` holds, in their original order. */
export function common(names: string[], lists: string[][]): string[] {
  return names.filter((name) => lists.every((list) => list.includes(name)));
}
