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
  type AddonSurface,
  BRANCH_LIST,
  BRANCH_NEW_CODE,
  BRANCH_SWITCHER,
  PULL_REQUEST_OVERVIEW,
} from './wcag.helper';

/*
 * What the shared audit cannot tell from a page: that no picture stands in for words (1.4.5).
 * Orientation, flashes and non-text contrast are `@haembina/accessibility`'s probes, run by
 * every `audit` call.
 */

/** The largest box an icon bitmap is drawn in; a bitmap any larger could be carrying words. */
const ICON_PX = 32;

/** Declares the layout case for one surface, under the capability describe that calls it. */
function layoutCases(surface: AddonSurface) {
  test.describe(`WCAG layout of the ${surface.name}`, () => {
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
