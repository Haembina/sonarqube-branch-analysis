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
import { BRANCH, openBranchesPage, rowOf, tabTo } from './sonarqube.helper';
import {
  type AddonSurface,
  BRANCH_LIST,
  BRANCH_NEW_CODE,
  BRANCH_SWITCHER,
  clippedText,
  NON_TEXT_CONTRAST,
  nonTextContrast,
  overflowPast,
  PULL_REQUEST_OVERVIEW,
  TEXT_SPACING_CSS,
} from './wcag.helper';

/** A phone held upright, then turned, in CSS pixels. */
const PORTRAIT = { width: 800, height: 1280 };
const LANDSCAPE = { width: 1280, height: 800 };

/** A 1280 by 800 window at 200% zoom, which is how a browser resizes text (WCAG 1.4.4). */
const ZOOMED_200 = { width: 640, height: 400 };

/** The width WCAG 1.4.10 asks content to reflow into: 1280 CSS pixels at 400% zoom. */
const REFLOW_WIDTH = 320;
const REFLOW = { width: REFLOW_WIDTH, height: 256 };

/** The largest box an icon bitmap is drawn in; a bitmap any larger could be carrying words. */
const ICON_PX = 32;

/** The shortest cycle an animation may repeat on: three flashes a second at most (2.3.1). */
const FLASH_CYCLE_MS = 1000 / 3;

/** Declares the layout cases for one surface, under the capability describe that calls it. */
function layoutCases(surface: AddonSurface) {
  test.describe(`WCAG layout of the ${surface.name}`, () => {
    test('1.3.4: shows the same content upright and turned, and no style locks either', async ({
      page,
    }) => {
      // Digits are dropped so an age such as "2 minutes ago" ticking over between reads is no
      // difference in content.
      const content = async () => (await region.innerText()).trim().replace(/\d+/g, '#');
      await page.setViewportSize(PORTRAIT);
      const region = await surface.open(page);
      const upright = await content();
      await page.setViewportSize(LANDSCAPE);
      await expect(region).toBeVisible();
      expect(await content()).toBe(upright);

      const orientationRules = await page.evaluate(() =>
        Array.from(document.styleSheets).flatMap((sheet) => {
          try {
            return Array.from(sheet.cssRules)
              .filter((rule) => rule instanceof CSSMediaRule)
              .map((rule) => (rule as CSSMediaRule).conditionText)
              .filter((condition) => condition.includes('orientation'));
          } catch {
            return []; // A cross-origin sheet hides its rules; SonarQube serves none.
          }
        }),
      );
      expect(orientationRules).toEqual([]);
    });

    test('1.4.4: cuts off no text at 200% zoom', async ({ page }) => {
      await page.setViewportSize(ZOOMED_200);
      const region = await surface.open(page);
      expect(await clippedText(region)).toEqual([]);
    });

    test('1.4.10: reflows into 320 CSS pixels, tables aside', async ({ page }) => {
      await page.setViewportSize(REFLOW);
      const region = await surface.open(page);
      expect(await overflowPast(region, REFLOW_WIDTH)).toEqual([]);
      expect(await clippedText(region)).toEqual([]);
    });

    test('1.4.12: cuts off no text with WCAG text spacing forced on', async ({ page }) => {
      const region = await surface.open(page);
      await page.addStyleTag({ content: TEXT_SPACING_CSS });
      expect(await clippedText(region)).toEqual([]);
    });

    test('1.4.5: draws its words as text: no bitmap larger than an icon, no SVG text', async ({
      page,
    }) => {
      const region = await surface.open(page);
      const images = await region.evaluate((root, icon) => {
        const bitmaps = Array.from(root.querySelectorAll('img, canvas, picture')).filter(
          (image) => {
            const box = image.getBoundingClientRect();
            return box.width > icon || box.height > icon;
          },
        );
        const lettered = Array.from(root.querySelectorAll('svg')).filter((svg) =>
          svg.querySelector('text'),
        );
        return [...bitmaps, ...lettered].map((image) => {
          const box = image.getBoundingClientRect();
          return `${image.tagName} ${Math.round(box.width)}x${Math.round(box.height)}`;
        });
      }, ICON_PX);
      expect(images).toEqual([]);
    });

    test('2.3.1: runs no animation that repeats faster than three times a second', async ({
      page,
    }) => {
      await surface.open(page);
      const fast = await page.evaluate(
        (shortest) =>
          document
            .getAnimations()
            .map((animation) => animation.effect?.getComputedTiming())
            .filter(
              (timing) =>
                timing !== undefined &&
                (timing.iterations ?? 1) > 1 &&
                Number(timing.duration) < shortest,
            )
            .map((timing) => `${String(timing?.duration)}ms x ${String(timing?.iterations)}`),
        FLASH_CYCLE_MS,
      );
      expect(fast).toEqual([]);
    });
  });
}

test.describe('capability set-branch-new-code: give a branch its own new code definition', () => {
  layoutCases(BRANCH_NEW_CODE);
});

test.describe('capability pull-request-overview: read the quality gate and new-code measures of a pull request', () => {
  layoutCases(PULL_REQUEST_OVERVIEW);
});

test.describe('capability switch-branch: open another branch or pull request of a project', () => {
  layoutCases(BRANCH_SWITCHER);
});

test.describe('capability list-branches: see every branch and pull request of a project', () => {
  layoutCases(BRANCH_LIST);
});

// WCAG 1.4.11 reads the branch list's controls and the rename dialog opened from it.
test.describe('capability list-branches: see every branch and pull request of a project', () => {
  test('1.4.11: draws each control and its states at 3:1 against the page', async ({ page }) => {
    await openBranchesPage(page);
    const row = rowOf(page, BRANCH);
    const keep = row.getByRole('switch');
    const kept = await keep.isChecked();

    expect(await nonTextContrast(keep)).toBeGreaterThanOrEqual(NON_TEXT_CONTRAST);
    await keep.click();
    await expect(keep).toBeChecked({ checked: !kept });
    expect(await nonTextContrast(keep)).toBeGreaterThanOrEqual(NON_TEXT_CONTRAST);
    await keep.click();
    await expect(keep).toBeChecked({ checked: kept });

    const actions = row.getByRole('button', { name: `Update ${BRANCH}` });
    expect(await nonTextContrast(actions)).toBeGreaterThanOrEqual(NON_TEXT_CONTRAST);
  });

  test('1.4.11: draws the focus indicator at 3:1 against the page', async ({ page }) => {
    await openBranchesPage(page);
    const actions = rowOf(page, BRANCH).getByRole('button', { name: `Update ${BRANCH}` });
    await tabTo(page, actions);
    const indicator = await actions.evaluate((element) => {
      const style = getComputedStyle(element);
      return style.outlineStyle === 'none'
        ? (/rgba?\([^)]+\)/.exec(style.boxShadow)?.[0] ?? 'none')
        : style.outlineColor;
    });
    const probe = await page.evaluate((color) => {
      const swatch = document.createElement('span');
      swatch.style.border = `2px solid ${color}`;
      swatch.id = 'focus-indicator-swatch';
      document.getElementById('project-branch-like')?.append(swatch);
      return swatch.id;
    }, indicator);
    expect(await nonTextContrast(page.locator(`#${probe}`))).toBeGreaterThanOrEqual(
      NON_TEXT_CONTRAST,
    );
  });

  test('1.4.11: draws the rename field boundary at 3:1 against the dialog', async ({ page }) => {
    await openBranchesPage(page);
    await rowOf(page, 'main').getByRole('button', { name: 'Update main' }).click();
    await page.getByRole('menuitem', { name: 'Rename branch' }).click();
    const name = page.getByRole('dialog', { name: 'Rename branch' }).getByRole('textbox');
    expect(await nonTextContrast(name)).toBeGreaterThanOrEqual(NON_TEXT_CONTRAST);
    await page.keyboard.press('Escape');
  });
});
