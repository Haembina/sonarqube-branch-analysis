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

export interface AddonSurface {
  name: string;
  open: (page: Page) => Promise<Locator>;
}

export const SWITCHER = 'Branches and pull requests';

export const BRANCH_LIST: AddonSurface = {
  name: 'branch list',
  open: async (page) => {
    await openBranchesPage(page);
    return page.locator('#project-branch-like');
  },
};

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

export const PULL_REQUEST_OVERVIEW: AddonSurface = {
  name: 'pull request overview',
  open: async (page) => {
    await page.goto(`/dashboard?id=${PROJECT}&pullRequest=1`);
    await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible();
    await expect(page.getByTestId('overview__measures-new_violations')).toBeVisible();
    return page.getByRole('main');
  },
};

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

export async function expectAnnounced(text: Locator): Promise<void> {
  await expect(text).toBeVisible();
  const live = await text.evaluate((element) => {
    const region = element.closest('[aria-live], [role="status"], [role="alert"], [role="log"]');
    return region !== null && region.getAttribute('aria-live') !== 'off';
  });
  expect(live).toBe(true);
}

export async function namesAround(anchor: Locator, container: string): Promise<string[]> {
  return anchor.evaluate((element, selector) => {
    const holder = element.closest(selector) ?? document.body;
    return Array.from(holder.querySelectorAll('a, button')).map(
      (control) => control.getAttribute('aria-label') ?? control.textContent?.trim() ?? '',
    );
  }, container);
}

export function common(names: string[], lists: string[][]): string[] {
  return names.filter((name) => lists.every((list) => list.includes(name)));
}
