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
import { BRANCH, openBranchesPage, PROJECT, rowOf } from './sonarqube.helper';
import { expectAnnounced } from './wcag.helper';

/** The error SonarQube shows under a number of days outside its 1 to 90 range. */
const DAYS_ERROR = 'Please provide a whole number between 1 and 90.';

/** A number of days the setting refuses, below its minimum of 1. */
const TOO_FEW_DAYS = '0';

/** A number of days the setting accepts. */
const VALID_DAYS = '15';

/** The toast SonarQube shows once a branch's new code definition is saved. */
const SAVED = 'New code definition has been updated';

const branchRow = (page: Page) => page.getByRole('row').filter({ hasText: BRANCH });

/** Opens the new code dialog for `BRANCH` and picks "Number of days" in it. */
async function openDaysSetting(page: Page) {
  await page.goto(`/project/baseline?id=${PROJECT}`);
  await branchRow(page).getByRole('button', { name: `Edit ${BRANCH}` }).click();
  const dialog = page.getByRole('dialog', { name: `New Code for ${BRANCH}` });
  await dialog.getByRole('radio', { name: /Number of days/ }).check();
  return dialog;
}

test.describe('capability set-branch-new-code: give a branch its own new code definition', () => {
  test('3.3.1, 3.3.3: marks days out of range, says the range, and refuses to save', async ({
    page,
  }) => {
    const dialog = await openDaysSetting(page);
    const days = dialog.getByRole('spinbutton');
    await days.fill(TOO_FEW_DAYS);

    await expect(dialog.getByText(DAYS_ERROR)).toBeVisible();
    await expect(days).toHaveAttribute('aria-invalid', 'true');
    await expect(dialog.getByRole('button', { name: 'Save' })).toBeDisabled();

    await days.fill(VALID_DAYS);
    await expect(dialog.getByText(DAYS_ERROR)).toBeHidden();
    await page.keyboard.press('Escape');
    await expect(branchRow(page)).toContainText('Project setting');
  });

  test('4.1.3: announces an out-of-range number of days without moving focus', async ({
    page,
  }) => {
    const dialog = await openDaysSetting(page);
    const days = dialog.getByRole('spinbutton');
    await days.fill(TOO_FEW_DAYS);

    await expectAnnounced(dialog.getByText(DAYS_ERROR));
    await expect(days).toBeFocused();
    await page.keyboard.press('Escape');
  });

  test('4.1.3: announces a saved new code definition, then resets it', async ({ page }) => {
    const dialog = await openDaysSetting(page);
    await dialog.getByRole('spinbutton').fill(VALID_DAYS);
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expectAnnounced(page.getByText(SAVED));
    await expect(branchRow(page)).toContainText(`Number of days: ${VALID_DAYS}`);

    await branchRow(page)
      .getByRole('button', { name: `Show actions for branch ${BRANCH}` })
      .click();
    await page.getByRole('menuitem', { name: 'Reset To Default' }).click();
    await expect(branchRow(page)).toContainText('Project setting');
  });
});

test.describe('capability rename-main-branch: rename the main branch of a project', () => {
  test('3.3.7: opens the rename dialog holding the name the branch already has', async ({
    page,
  }) => {
    await openBranchesPage(page);
    await rowOf(page, 'main').getByRole('button', { name: 'Update main' }).click();
    await page.getByRole('menuitem', { name: 'Rename branch' }).click();
    const dialog = page.getByRole('dialog', { name: 'Rename branch' });

    await expect(dialog.getByRole('textbox', { name: 'New name' })).toHaveValue('main');
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
  });
});

test.describe('capability list-branches: see every branch and pull request of a project', () => {
  test('1.4.13: keeps a tooltip open under the pointer, and closes it on Escape', async ({
    page,
  }) => {
    await openBranchesPage(page);
    await rowOf(page, 'main').getByLabel('Help').hover();
    const tooltip = page.getByRole('tooltip');
    await expect(tooltip).toContainText('The main branch is always excluded from automatic deletion.');

    await tooltip.hover();
    await expect(tooltip).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(tooltip).toBeHidden();
  });
});

test.describe('signed out', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('WCAG 1.3.5: names the purpose of the sign-in fields for autofill', async ({ page }) => {
    await page.goto('/sessions/new');
    await expect(page.locator('#login-input')).toHaveAttribute('autocomplete', 'username');
    await expect(page.locator('#password-input')).toHaveAttribute(
      'autocomplete',
      'current-password',
    );
  });

  test('WCAG 3.3.8: signs in with a pasted or filled password and no puzzle', async ({
    page,
  }) => {
    await page.goto('/sessions/new');
    const password = page.locator('#password-input');
    await expect(password).toBeVisible();

    const blocked = await password.evaluate((input) => {
      const paste = new ClipboardEvent('paste', {
        bubbles: true,
        cancelable: true,
        clipboardData: new DataTransfer(),
      });
      input.dispatchEvent(paste);
      return paste.defaultPrevented;
    });
    expect(blocked).toBe(false);
    await expect(page.locator('iframe, canvas')).toHaveCount(0);
  });
});
