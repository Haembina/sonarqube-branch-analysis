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

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import BranchLikeToolbar from './BranchLikeToolbar';

function renderToolbar(selectedCount: number) {
  const onDeleteSelected = jest.fn();
  const onFilterChange = jest.fn();
  renderComponent(
    <BranchLikeToolbar
      filter=""
      onDeleteSelected={onDeleteSelected}
      onFilterChange={onFilterChange}
      selectedCount={selectedCount}
    />,
  );
  return { onDeleteSelected, onFilterChange };
}

it('hands each keystroke of the name filter to its owner', async () => {
  const user = userEvent.setup();
  const { onFilterChange } = renderToolbar(0);

  await user.type(screen.getByRole('searchbox', { name: 'search.search_by_name' }), 'f');

  expect(onFilterChange).toHaveBeenCalledWith('f');
});

it('offers to delete only once something is selected', async () => {
  const user = userEvent.setup();
  const { onDeleteSelected } = renderToolbar(2);

  await user.click(screen.getByRole('button', { name: 'branch_analysis.delete_selected.2' }));

  expect(onDeleteSelected).toHaveBeenCalled();
});

it('keeps the delete button disabled while nothing is selected', () => {
  renderToolbar(0);

  expect(screen.getByRole('button', { name: 'branch_analysis.delete_selected.0' })).toBeDisabled();
});
