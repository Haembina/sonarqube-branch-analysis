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
  CI_PULL_REQUEST,
  expectNoAxeViolations,
  expectVisibleFocus,
  OPEN_DIALOG,
  openBranchesPage,
  rowOf,
  tabTo,
} from './sonarqube.helper';

// Each case leaves the project as the next one expects it, so they run in this order.
test.describe.configure({ mode: 'serial' });

/** Opens a row's actions menu with the mouse and picks one entry. */
async function pickAction(page: Page, name: string, action: string) {
  await rowOf(page, name).getByRole('button', { name: `Update ${name}` }).click();
  await page.getByRole('menuitem', { name: action }).click();
}

/** Opens a row's actions menu from the keyboard and picks one entry. */
async function pickActionByKeyboard(page: Page, name: string, action: string) {
  await tabTo(page, rowOf(page, name).getByRole('button', { name: `Update ${name}` }));
  await expectVisibleFocus(page);
  await page.keyboard.press('Enter');
  await arrowTo(page, page.getByRole('menuitem', { name: action }));
  await page.keyboard.press('Enter');
}

test.describe('capability rename-main-branch: rename the main branch of a project', () => {
  test('renames the main branch, and back again by keyboard alone', async ({ page }) => {
    await openBranchesPage(page);

    await pickAction(page, 'main', 'Rename branch');
    const dialog = page.getByRole('dialog', { name: 'Rename branch' });
    const name = dialog.getByRole('textbox', { name: 'New name' });
    await expect(name).toBeFocused();
    await expect(dialog.getByRole('button', { name: 'Rename' })).toBeDisabled();
    await expectNoAxeViolations(page, OPEN_DIALOG);
    await name.fill('trunk');
    await dialog.getByRole('button', { name: 'Rename' }).click();
    await expect(dialog).toBeHidden();
    await expect(rowOf(page, 'trunk')).toContainText('Main Branch');

    await pickActionByKeyboard(page, 'trunk', 'Rename branch');
    await expect(name).toBeFocused();
    await page.keyboard.press('ControlOrMeta+A');
    await page.keyboard.type('main');
    await page.keyboard.press('Enter');
    await expect(dialog).toBeHidden();
    await expect(rowOf(page, 'main')).toContainText('Main Branch');
  });
});

test.describe('capability set-main-branch: make another branch the main branch of a project', () => {
  test('makes a branch main, then restores main by keyboard alone', async ({ page }) => {
    await openBranchesPage(page);

    await pickAction(page, BRANCH, 'Set as main branch');
    const dialog = page.getByRole('dialog', { name: `Set "${BRANCH}" as the main branch` });
    await expect(dialog).toBeVisible();
    await expectNoAxeViolations(page, OPEN_DIALOG);
    await dialog.getByRole('button', { name: 'Set as main branch' }).click();
    await expect(dialog).toBeHidden();
    await expect(rowOf(page, BRANCH)).toContainText('Main Branch');

    await pickActionByKeyboard(page, 'main', 'Set as main branch');
    const back = page.getByRole('dialog', { name: 'Set "main" as the main branch' });
    await expect(back).toBeVisible();
    await tabTo(page, back.getByRole('button', { name: 'Set as main branch' }));
    await expectVisibleFocus(page);
    await page.keyboard.press('Enter');
    await expect(back).toBeHidden();
    await expect(rowOf(page, 'main')).toContainText('Main Branch');
  });
});

test.describe('capability delete-branch: delete a branch or pull request of a project', () => {
  test('deletes a pull request by keyboard alone', async ({ page }) => {
    await openBranchesPage(page);
    await page.getByRole('tab', { name: 'Pull Requests' }).click();

    await pickActionByKeyboard(page, CI_PULL_REQUEST, 'Delete Pull Request');
    const dialog = page.getByRole('dialog', { name: 'Delete Pull Request' });
    await expect(dialog).toBeVisible();
    await expectNoAxeViolations(page, OPEN_DIALOG);
    await tabTo(page, dialog.getByRole('button', { name: 'Delete' }));
    await page.keyboard.press('Enter');

    await expect(dialog).toBeHidden();
    await expect(rowOf(page, CI_PULL_REQUEST)).toHaveCount(0);
  });

  test('asks before deleting a branch, and keeps it on cancel', async ({ page }) => {
    await openBranchesPage(page);

    await pickAction(page, BRANCH, 'Delete branch');
    const dialog = page.getByRole('dialog', { name: 'Delete branch' });
    await expect(dialog).toContainText(BRANCH);
    await expectNoAxeViolations(page, OPEN_DIALOG);
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(rowOf(page, BRANCH)).toBeVisible();

    await pickAction(page, BRANCH, 'Delete branch');
    await dialog.getByRole('button', { name: 'Delete' }).click();
    await expect(dialog).toBeHidden();
    await expect(rowOf(page, BRANCH)).toHaveCount(0);
  });
});
