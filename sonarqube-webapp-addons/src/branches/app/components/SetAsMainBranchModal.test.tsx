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
import { setMainBranch } from '~sq-server-commons/api/branches';
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import { mockBranch } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import SetAsMainBranchModal from './SetAsMainBranchModal';

jest.mock('~sq-server-commons/api/branches');

const branchesHandler = new BranchesServiceMock();

beforeEach(() => {
  branchesHandler.reset();
});

function renderModal() {
  const onClose = jest.fn();
  const onSetAsMain = jest.fn();
  renderComponent(
    <SetAsMainBranchModal
      branch={mockBranch({ name: 'develop' })}
      component={mockComponent()}
      onClose={onClose}
      onSetAsMain={onSetAsMain}
    />,
  );
  return { onClose, onSetAsMain };
}

it('warns about the reindex, then makes the branch main through the API', async () => {
  const user = userEvent.setup();
  const { onSetAsMain } = renderModal();

  expect(
    screen.getByText('project_branch_pull_request.branch.main_branch.requires_reindex'),
  ).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /documentation/ })).toBeInTheDocument();
  await user.click(
    screen.getByRole('button', { name: 'project_branch_pull_request.branch.set_main' }),
  );

  expect(setMainBranch).toHaveBeenCalledWith('my-project', 'develop');
  expect(onSetAsMain).toHaveBeenCalled();
});

it('stays open when the server refuses', async () => {
  const user = userEvent.setup();
  jest.mocked(setMainBranch).mockRejectedValueOnce(new Error('refused'));
  const { onClose, onSetAsMain } = renderModal();

  await user.click(
    screen.getByRole('button', { name: 'project_branch_pull_request.branch.set_main' }),
  );

  expect(
    await screen.findByRole('button', { name: 'project_branch_pull_request.branch.set_main' }),
  ).toBeEnabled();
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(onSetAsMain).not.toHaveBeenCalled();
  expect(onClose).not.toHaveBeenCalled();
});
