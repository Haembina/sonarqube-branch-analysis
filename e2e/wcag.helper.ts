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

/** The ratio WCAG 1.4.11 asks of a control's boundary or state against what is beside it. */
export const NON_TEXT_CONTRAST = 3;

/** An sRGB color as its red, green and blue channels, each 0 to 255. */
type Rgb = [number, number, number];

/** White, the page SonarQube draws when no ancestor paints a background of its own. */
const WHITE: Rgb = [255, 255, 255];

/**
 * Parses a computed `rgb()` or `rgba()` color, blending a translucent one over `behind`.
 * Returns undefined for `none`, `transparent` and a fully transparent color; throws on any
 * other notation.
 */
function parseColor(color: string, behind: Rgb): Rgb | undefined {
  if (color === 'none' || color === 'transparent') {
    return undefined;
  }
  const match = /^rgba?\(([^)]+)\)$/.exec(color);
  if (!match) {
    throw new Error(`Unparsed color ${color}`);
  }
  const [r, g, b, alpha = 1] = match[1].split(/[\s,/]+/).map(Number);
  if (alpha === 0) {
    return undefined;
  }
  const blend = (channel: number, under: number) => alpha * channel + (1 - alpha) * under;
  return [blend(r, behind[0]), blend(g, behind[1]), blend(b, behind[2])];
}

/** WCAG's relative luminance of an sRGB color, from 0 for black to 1 for white. */
function luminance([r, g, b]: Rgb): number {
  const linear = (channel: number) => {
    const c = channel / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
}

/** WCAG's contrast ratio between two colors, from 1 for equal colors to 21 for black on white. */
function contrastRatio(a: Rgb, b: Rgb): number {
  const [light, dark] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (light + 0.05) / (dark + 0.05);
}

/**
 * The computed colors `control` paints, and the first opaque background among its ancestors:
 * each border wider than zero, each box shadow, each background, each SVG fill and each glyph
 * set as text, of the control and of every element inside it.
 */
async function paintedColors(control: Locator): Promise<{ behind?: string; drawn: string[] }> {
  return control.evaluate((element) => {
    const opaque = /^rgb\(|^rgba\([^)]*[\s,/]1\)$/;
    let behind: string | undefined;
    for (let node = element.parentElement; node && !behind; node = node.parentElement) {
      const background = getComputedStyle(node).backgroundColor;
      behind = opaque.test(background) ? background : undefined;
    }

    const drawn: string[] = [];
    for (const part of [element, ...Array.from(element.querySelectorAll('*'))]) {
      const style = getComputedStyle(part);
      const bordered = parseFloat(style.borderTopWidth) > 0 && style.borderTopStyle !== 'none';
      const glyph = Array.from(part.childNodes).some(
        (node) => node.nodeType === Node.TEXT_NODE && node.textContent?.trim(),
      );
      drawn.push(style.backgroundColor);
      if (bordered) {
        drawn.push(style.borderTopColor);
      }
      drawn.push(...(style.boxShadow.match(/rgba?\([^)]+\)/g) ?? []));
      if (part instanceof SVGElement) {
        drawn.push(style.fill === 'none' ? style.stroke : style.fill);
      }
      if (glyph) {
        drawn.push(style.color);
      }
    }
    return { behind, drawn };
  });
}

/**
 * Resolves once every finite animation and transition on `control` and inside it has
 * finished. A switch read mid-transition paints a blend of its two states: the checked
 * branch switch measured 2.9:1 there and passes at rest.
 */
async function settled(control: Locator): Promise<void> {
  await control.evaluate((element) =>
    Promise.all(
      element
        .getAnimations({ subtree: true })
        .filter((animation) => animation.effect?.getComputedTiming().endTime !== Infinity)
        .map((animation) => animation.finished),
    ),
  );
}

/**
 * The best contrast ratio any part of `control` draws against the color behind it, as
 * `paintedColors` reads them once `control` has settled; a translucent color is blended
 * over that background first. Throws on a computed color that is not `rgb()` or `rgba()`.
 */
export async function nonTextContrast(control: Locator): Promise<number> {
  await settled(control);
  const { behind, drawn } = await paintedColors(control);
  const ground = behind === undefined ? WHITE : (parseColor(behind, WHITE) ?? WHITE);
  return Math.max(
    ...drawn.map((color) => contrastRatio(parseColor(color, ground) ?? ground, ground)),
  );
}

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
