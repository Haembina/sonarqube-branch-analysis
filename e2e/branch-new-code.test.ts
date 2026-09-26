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

import { expect, test, type Page } from '@playwright/test';
import {
  arrowTo,
  BRANCH,
  expectNoAxeViolations,
  expectVisibleFocus,
  OPEN_DIALOG,
  PROJECT,
  tabTo,
} from './sonarqube.helper';

/** The fixture branch's row: its name sits beside an icon, not in an element of its own. */
const branchRow = (page: Page) => page.getByRole('row').filter({ hasText: BRANCH });

test.describe('capability set-branch-new-code: give a branch its own new code definition', () => {
  test('sets a number of days for a branch, then resets it, by keyboard alone', async ({
    page,
  }) => {
    await page.goto(`/project/baseline?id=${PROJECT}`);
    await expect(
      page.getByRole('heading', { name: 'Set a specific setting for a branch' }),
    ).toBeVisible();
    await expect(branchRow(page)).toContainText('Project setting');
    await expectNoAxeViolations(page);

    await tabTo(page, branchRow(page).getByRole('button', { name: `Edit ${BRANCH}` }));
    await expectVisibleFocus(page);
    await page.keyboard.press('Enter');
    const dialog = page.getByRole('dialog', { name: `New Code for ${BRANCH}` });
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('button', { name: 'Save' })).toBeDisabled();
    await expectNoAxeViolations(page, OPEN_DIALOG);

    // Tab enters the group of choices at its first; the arrows move between them.
    const days = dialog.getByRole('radio', { name: /Number of days/ });
    await tabTo(page, dialog.getByRole('radio').first());
    await arrowTo(page, days);
    await page.keyboard.press('Space');
    await expect(days).toBeChecked();
    await tabTo(page, dialog.getByRole('spinbutton'));
    await page.keyboard.press('ControlOrMeta+A');
    await page.keyboard.type('15');
    await tabTo(page, dialog.getByRole('button', { name: 'Save' }));
    await page.keyboard.press('Enter');

    await expect(dialog).toBeHidden();
    await expect(branchRow(page)).toContainText('Number of days: 15');

    await tabTo(
      page,
      branchRow(page).getByRole('button', { name: `Show actions for branch ${BRANCH}` }),
    );
    await page.keyboard.press('Enter');
    await expect(page.getByRole('menuitem', { name: 'Reset To Default' })).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(branchRow(page)).toContainText('Project setting');
  });

  test('closes the dialog with Escape and keeps the setting', async ({ page }) => {
    await page.goto(`/project/baseline?id=${PROJECT}`);
    await branchRow(page).getByRole('button', { name: `Edit ${BRANCH}` }).click();
    const dialog = page.getByRole('dialog', { name: `New Code for ${BRANCH}` });
    await expect(dialog).toBeVisible();

    await page.keyboard.press('Escape');

    await expect(dialog).toBeHidden();
    await expect(branchRow(page)).toContainText('Project setting');
  });
});
