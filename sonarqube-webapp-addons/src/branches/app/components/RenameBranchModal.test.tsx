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
import { renameBranch } from '~sq-server-commons/api/branches';
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import { mockMainBranch } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import RenameBranchModal from './RenameBranchModal';

jest.mock('~sq-server-commons/api/branches');

const branchesHandler = new BranchesServiceMock();

beforeEach(() => {
  branchesHandler.reset();
});

function renderModal(onClose = jest.fn()) {
  renderComponent(
    <RenameBranchModal
      branch={mockMainBranch({ name: 'master' })}
      component={mockComponent()}
      onClose={onClose}
    />,
  );
  return onClose;
}

it('renames the main branch through the API and closes', async () => {
  const user = userEvent.setup();
  const onClose = renderModal();

  const input = screen.getByRole('textbox');
  expect(input).toHaveValue('master');
  await user.clear(input);
  await user.type(input, 'main');
  await user.click(screen.getByRole('button', { name: 'rename' }));

  expect(renameBranch).toHaveBeenCalledWith('my-project', 'main');
  expect(onClose).toHaveBeenCalled();
});

it('offers no rename while the name is unchanged or empty', async () => {
  const user = userEvent.setup();
  renderModal();

  const submit = screen.getByRole('button', { name: 'rename' });
  expect(submit).toBeDisabled();

  await user.clear(screen.getByRole('textbox'));
  expect(submit).toBeDisabled();
  await user.keyboard('{Enter}');

  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(screen.getByRole('textbox')).toHaveValue('');
});

it('stays open when the server refuses the name', async () => {
  const user = userEvent.setup();
  jest.mocked(renameBranch).mockRejectedValueOnce(new Error('refused'));
  const onClose = renderModal();

  await user.clear(screen.getByRole('textbox'));
  await user.type(screen.getByRole('textbox'), 'taken');
  await user.click(screen.getByRole('button', { name: 'rename' }));

  expect(await screen.findByRole('button', { name: 'rename' })).toBeEnabled();
  expect(screen.getByRole('textbox')).toHaveValue('taken');
  expect(onClose).not.toHaveBeenCalled();
});
