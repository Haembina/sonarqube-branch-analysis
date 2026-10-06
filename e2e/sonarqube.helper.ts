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

/** The project scripts/integration-test.sh analyses: main, a branch and two pull requests. */
export const PROJECT = process.env.SONARQUBE_PROJECT ?? 'branch-analysis-it';

/** The fixture's non-main branch, analyzed with `sonar.branch.name`. */
export const BRANCH = 'feature/integration';

/** The fixture's pull request analyzed with `sonar.pullrequest.*`, as the pages name it. */
export const PULL_REQUEST = '1 – feature/pull-request';

/** The fixture's pull request detected from GitHub Actions variables, as the pages name it. */
export const CI_PULL_REQUEST = '7 – feature/ci';

/** Opens the project's "Branches & Pull Requests" page and waits for its table. */
export async function openBranchesPage(page: Page): Promise<void> {
  await page.goto(`/project/branches?id=${PROJECT}`);
  await expect(page.getByRole('heading', { name: 'Branches & Pull Requests' })).toBeVisible();
  await expect(page.getByRole('row').nth(1)).toBeVisible();
}

/** The table row naming a branch or pull request. */
export function rowOf(page: Page, name: string): Locator {
  return page.getByRole('row').filter({ has: page.getByText(name, { exact: true }) });
}

/** How many Tab presses a walk may take before the spec gives up on reaching its target. */
const TAB_LIMIT = 80;

/**
 * Presses Tab until the target holds focus, as a keyboard user walks a page, so the browser
 * treats the focus as keyboard focus. Fails when the target is not reached.
 */
export async function tabTo(page: Page, target: Locator): Promise<void> {
  for (let presses = 0; presses < TAB_LIMIT; presses++) {
    if (await target.evaluate((element) => element === document.activeElement)) {
      return;
    }
    await page.keyboard.press('Tab');
  }
  await expect(target).toBeFocused();
}

/**
 * Presses ArrowDown until the target holds focus, as a keyboard user moves through a menu or a
 * group of choices. Fails when the target is not reached.
 */
export async function arrowTo(page: Page, target: Locator): Promise<void> {
  for (let presses = 0; presses < TAB_LIMIT; presses++) {
    if (await target.evaluate((element) => element === document.activeElement)) {
      return;
    }
    await page.keyboard.press('ArrowDown');
  }
  await expect(target).toBeFocused();
}

/**
 * Asserts the focused element draws a visible focus indicator: an outline or a box shadow in
 * its computed style (WCAG 2.4.7), and that it lies inside the viewport with nothing drawn
 * over its center (2.4.11).
 */
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
