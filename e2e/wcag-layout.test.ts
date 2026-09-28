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
  ADDON_SURFACES,
  clippedText,
  NON_TEXT_CONTRAST,
  nonTextContrast,
  overflowPast,
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

/** The largest box an icon is drawn in; an image any larger could be carrying words. */
const ICON_PX = 32;

/** The shortest cycle an animation may repeat on: three flashes a second at most (2.3.1). */
const FLASH_CYCLE_MS = 1000 / 3;

for (const surface of ADDON_SURFACES) {
  test.describe(`WCAG layout of the ${surface.name}`, () => {
    test('1.3.4: shows the same content upright and turned, and no style locks either', async ({
      page,
    }) => {
      await page.setViewportSize(PORTRAIT);
      const region = await surface.open(page);
      const upright = (await region.innerText()).trim();
      await page.setViewportSize(LANDSCAPE);
      await expect(region).toBeVisible();
      expect((await region.innerText()).trim()).toBe(upright);

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

    test('1.4.5: draws its words as text, with no image larger than an icon', async ({
      page,
    }) => {
      const region = await surface.open(page);
      const images = await region.evaluate((root, icon) => {
        const found: string[] = [];
        for (const image of Array.from(root.querySelectorAll('img, canvas, svg, picture'))) {
          const box = image.getBoundingClientRect();
          if (box.width > icon || box.height > icon || image.querySelector('text')) {
            found.push(`${image.tagName} ${Math.round(box.width)}x${Math.round(box.height)}`);
          }
        }
        return found;
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

test.describe('WCAG 1.4.11 non-text contrast of the branch list', () => {
  test('draws each control and its states at 3:1 against the page', async ({ page }) => {
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

  test('draws the focus indicator at 3:1 against the page', async ({ page }) => {
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

  test('draws the rename field boundary at 3:1 against the dialog', async ({ page }) => {
    await openBranchesPage(page);
    await rowOf(page, 'main').getByRole('button', { name: 'Update main' }).click();
    await page.getByRole('menuitem', { name: 'Rename branch' }).click();
    const name = page.getByRole('dialog', { name: 'Rename branch' }).getByRole('textbox');
    expect(await nonTextContrast(name)).toBeGreaterThanOrEqual(NON_TEXT_CONTRAST);
    await page.keyboard.press('Escape');
  });
});
