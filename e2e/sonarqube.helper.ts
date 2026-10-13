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

import { test as base, type Scope, type ShortcutSetting } from '@haembina/accessibility/playwright';
import { expect, type Locator, type Page } from '@playwright/test';
import { addonLanguageTables } from './language-tables.helper';

const SHORTCUTS_PREFERENCE = 'sonarqube.preferences.keyboard_shortcuts_enabled';

export const SONARQUBE_SHORTCUTS: ShortcutSetting = {
  off: async (page) => {
    await page.evaluate((key) => localStorage.setItem(key, 'false'), SHORTCUTS_PREFERENCE);
  },
  on: async (page) => {
    await page.evaluate((key) => localStorage.removeItem(key), SHORTCUTS_PREFERENCE);
  },
};

export const test = base.extend({
  accessibilityLanguageTables: async ({ request }, use) => {
    await use(await addonLanguageTables(request));
  },
  accessibilityShortcuts: async ({}, use) => {
    await use(SONARQUBE_SHORTCUTS);
  },
});

export { expect };

const SONARSOURCE_NODES = [
  '[data-testid="sidebar-navigation-wrapper"]',
  '#graph-type',
  'a[href^="/tutorials?"]',
  'a[href*="category=pull_request_decoration_binding"]',
  'a[href*="sonarsource.com/products/sonarqube/why-upgrade"]',
];

export const OPEN_DIALOG: Scope = { include: ['[role="dialog"]'], exclude: SONARSOURCE_NODES };

export const BRANCH_LIST: Scope = { include: ['#project-branch-like'], exclude: SONARSOURCE_NODES };

export const NEW_CODE_SETTINGS: Scope = { include: ['#branch-new-code-settings'], exclude: SONARSOURCE_NODES };

export const BRANCH_SWITCHER: Scope = { include: ['#project-branch-selector'], exclude: SONARSOURCE_NODES };

export const PULL_REQUEST_PAGE: Scope = { include: ['main'], exclude: SONARSOURCE_NODES };

export function reopening(page: Page, open: () => Promise<void>): { reload: () => Promise<void> } {
  return {
    reload: async () => {
      await page.reload();
      await open();
    },
  };
}

export const PROJECT = process.env.SONARQUBE_PROJECT ?? 'branch-analysis-it';

export const BRANCH = 'feature/integration';

export const PULL_REQUEST = '1 – feature/pull-request';

export const CI_PULL_REQUEST = '7 – feature/ci';

export async function openBranchesPage(page: Page): Promise<void> {
  await page.goto(`/project/branches?id=${PROJECT}`);
  await expect(page.getByRole('heading', { name: 'Branches & Pull Requests' })).toBeVisible();
  await expect(page.getByRole('row').nth(1)).toBeVisible();
}

export function rowOf(page: Page, name: string): Locator {
  return page.getByRole('row').filter({ has: page.getByText(name, { exact: true }) });
}

const TAB_LIMIT = 80;

export async function tabTo(page: Page, target: Locator): Promise<void> {
  for (let presses = 0; presses < TAB_LIMIT; presses++) {
    if (await target.evaluate((element) => element === document.activeElement)) {
      return;
    }
    await page.keyboard.press('Tab');
  }
  await expect(target).toBeFocused();
}

export async function arrowTo(page: Page, target: Locator): Promise<void> {
  for (let presses = 0; presses < TAB_LIMIT; presses++) {
    if (await target.evaluate((element) => element === document.activeElement)) {
      return;
    }
    await page.keyboard.press('ArrowDown');
  }
  await expect(target).toBeFocused();
}

export async function expectVisibleFocus(page: Page): Promise<void> {
  const focused = page.locator(':focus');
  await expect(focused).toBeInViewport();
  const indicator = await focused.evaluate((element) => {
    const style = getComputedStyle(element);
    const box = element.getBoundingClientRect();
    const top = document.elementFromPoint(box.left + box.width / 2, box.top + box.height / 2);
    return {
      outline: style.outlineStyle,
      shadow: style.boxShadow,
      uncovered: top !== null && (element === top || element.contains(top)),
    };
  });
  expect(indicator.outline !== 'none' || indicator.shadow !== 'none').toBe(true);
  expect(indicator.uncovered).toBe(true);
}
