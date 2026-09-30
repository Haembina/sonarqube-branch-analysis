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
import { BRANCH, openBranchesPage, rowOf, tabTo } from './sonarqube.helper';
import {
  type AddonSurface,
  BRANCH_LIST,
  BRANCH_NEW_CODE,
  BRANCH_SWITCHER,
  PULL_REQUEST_OVERVIEW,
} from './wcag.helper';

/** How far one box may sit above the one before it and still read as the same line, in px. */
const LINE_SLACK_PX = 2;

/**
 * Single keys SonarSource's webapp binds outside the addons, which are SonarSource's to answer
 * for, each with what it does: `s` focuses the global search in the top bar.
 */
const SONARSOURCE_KEYS = new Set(['s']);

/** How many Tab presses a walk through the branch list takes at most. */
const WALK_LIMIT = 40;

/**
 * Words that point at a control by where it sits, how it looks or its color, which a user who
 * cannot see the layout cannot follow (WCAG 1.3.3).
 */
const SENSORY =
  /\b(on the (left|right)|to the (left|right)|(above|below)\b|(red|green|round|square) (button|icon|dot))/i;

/**
 * The top and left edges of `boxes`, in document order, whose order a reader of the screen
 * would not follow: a box that starts above the line of the one before it.
 */
async function outOfOrder(boxes: Locator): Promise<string[]> {
  const edges = await boxes.evaluateAll((elements) =>
    elements.map((element) => {
      const box = element.getBoundingClientRect();
      return { top: box.top, left: box.left, text: element.textContent?.trim() ?? '' };
    }),
  );
  return edges.flatMap((edge, index) => {
    const before = edges[index - 1];
    const aboveLine = before !== undefined && edge.top < before.top - LINE_SLACK_PX;
    const backOnLine =
      before !== undefined &&
      Math.abs(edge.top - before.top) <= LINE_SLACK_PX &&
      edge.left < before.left;
    return aboveLine || backOnLine ? [`${edge.text} after ${before.text}`] : [];
  });
}

/** Tabs from `start` until focus leaves `region`, returning the box of every stop inside it. */
async function focusStops(page: Page, region: Locator, start: Locator) {
  await tabTo(page, start);
  const stops: { top: number; bottom: number; name: string }[] = [];
  for (let presses = 0; presses < WALK_LIMIT; presses++) {
    const stop = await region.evaluate((root) => {
      const focused = document.activeElement;
      if (!focused || !root.contains(focused)) {
        return undefined;
      }
      const box = focused.getBoundingClientRect();
      const name = focused.getAttribute('aria-label') ?? focused.textContent?.trim() ?? '';
      return { top: box.top, bottom: box.bottom, name };
    });
    if (!stop) {
      break;
    }
    stops.push(stop);
    await page.keyboard.press('Tab');
  }
  return stops;
}

test.describe('capability list-branches: see every branch and pull request of a project', () => {
  test('1.3.2: reads the branch rows and their cells in the order they are drawn', async ({
    page,
  }) => {
    await openBranchesPage(page);
    const table = page.locator('#project-branch-like').getByRole('row');
    expect(await outOfOrder(table)).toEqual([]);
    expect(await outOfOrder(rowOf(page, BRANCH).getByRole('cell'))).toEqual([]);
  });

  test('2.4.3: tabs through the branch list in the order it is drawn', async ({ page }) => {
    await openBranchesPage(page);
    const region = page.locator('#project-branch-like');
    const stops = await focusStops(page, region, region.getByRole('tab').first());

    expect(stops.length).toBeGreaterThan(1);
    // A stop drawn wholly above the one before it goes back up the page; controls of one row
    // differ in height and so in top edge, and still read as that row.
    const backwards = stops.flatMap((stop, index) => {
      const before = stops[index - 1];
      const above = before !== undefined && stop.bottom <= before.top;
      return above ? [`${stop.name} after ${before.name}`] : [];
    });
    expect(backwards).toEqual([]);
  });
});

test.describe('capability switch-branch: open another branch or pull request of a project', () => {
  test('1.3.2: reads the branch switcher entries in the order they are drawn', async ({
    page,
  }) => {
    const popover = await BRANCH_SWITCHER.open(page);
    expect(await outOfOrder(popover.getByRole('listitem'))).toEqual([]);
  });
});

test.describe('capability rename-main-branch: rename the main branch of a project', () => {
  test('2.4.3: puts focus back on the control that opened the dialog when it closes', async ({
    page,
  }) => {
    await openBranchesPage(page);
    const actions = rowOf(page, 'main').getByRole('button', { name: 'Update main' });
    await tabTo(page, actions);
    await page.keyboard.press('Enter');
    await page.getByRole('menuitem', { name: 'Rename branch' }).press('Enter');
    const dialog = page.getByRole('dialog', { name: 'Rename branch' });
    await expect(dialog.getByRole('textbox', { name: 'New name' })).toBeFocused();

    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(actions).toBeFocused();
  });
});

/** Declares the reading cases for one surface, under the capability describe that calls it. */
function readingCases(surface: AddonSurface) {
  test.describe(`WCAG reading of the ${surface.name}`, () => {
    test('1.3.3: names no control by its place, shape or color', async ({ page }) => {
      const region = await surface.open(page);
      expect((await region.innerText()).match(SENSORY)).toBeNull();
    });

    test('2.5.3: says each control’s visible words in its accessible name', async ({ page }) => {
      const region = await surface.open(page);
      const mismatched = await region.evaluate((root) => {
        const selector =
          'a, button, [role="tab"], [role="switch"], [role="menuitem"], [role="radio"], [role="link"]';
        return Array.from(root.querySelectorAll(selector)).flatMap((control) => {
          // Icon fonts draw glyphs from the private use area, which are pictures rather than
          // words; a label of symbols alone, such as the "-" of a missing measure, is no text.
          const visible = (control as HTMLElement).innerText
            .replace(/[\uE000-\uF8FF]/g, '')
            .trim()
            .replace(/\s+/g, ' ');
          if (!/[\p{L}\p{N}]/u.test(visible)) {
            return [];
          }
          const labelledBy = control
            .getAttribute('aria-labelledby')
            ?.split(' ')
            .map((id) => document.getElementById(id)?.textContent ?? '')
            .join(' ');
          const name = (labelledBy ?? control.getAttribute('aria-label') ?? visible)
            .trim()
            .replace(/\s+/g, ' ');
          return !name.toLowerCase().includes(visible.toLowerCase())
            ? [`"${visible}" named "${name}"`]
            : [];
        });
      });
      expect(mismatched).toEqual([]);
    });

    test('3.1.2: declares no language but the page’s English', async ({ page }) => {
      const region = await surface.open(page);
      await expect(page.locator('html')).toHaveAttribute('lang', 'en');
      const other = await region.evaluate((root) =>
        Array.from(root.querySelectorAll('[lang]'))
          .map((element) => element.getAttribute('lang') ?? '')
          .filter((lang) => !lang.startsWith('en')),
      );
      expect(other).toEqual([]);
    });
  });
}

test.describe('capability set-branch-new-code: give a branch its own new code definition', () => {
  readingCases(BRANCH_NEW_CODE);
});

test.describe('capability pull-request-overview: read the quality gate and new-code measures of a pull request', () => {
  readingCases(PULL_REQUEST_OVERVIEW);
});

test.describe('capability switch-branch: open another branch or pull request of a project', () => {
  readingCases(BRANCH_SWITCHER);
});

test.describe('capability list-branches: see every branch and pull request of a project', () => {
  readingCases(BRANCH_LIST);

  test('2.1.4: does nothing on a single letter or digit pressed on the branch list', async ({
    page,
  }) => {
    await openBranchesPage(page);
    const url = page.url();
    const tab = page.getByRole('tab', { name: 'Branches' });
    await tab.focus();

    const keys = [...'abcdefghijklmnopqrstuvwxyz0123456789'].filter(
      (key) => !SONARSOURCE_KEYS.has(key),
    );
    for (const key of keys) {
      await page.keyboard.press(key);
      await expect(page).toHaveURL(url);
      await expect(page.getByRole('dialog')).toHaveCount(0);
      await expect(page.getByRole('menu')).toHaveCount(0);
      await expect(tab).toBeFocused();
    }
  });
});
