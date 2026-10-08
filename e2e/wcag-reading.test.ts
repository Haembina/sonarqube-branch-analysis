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
import { openBranchesPage, rowOf, tabTo } from './sonarqube.helper';

/*
 * Reading and focus order the shared audit does not walk: where focus lands once a dialog
 * closes. Reading order, Tab order, sensory words, label in name, languages of parts and
 * single-key shortcuts are `@haembina/accessibility`'s probes, run by every `audit` call.
 */
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
