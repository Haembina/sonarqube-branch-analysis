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
import { deleteBranch, deletePullRequest } from '~sq-server-commons/api/branches';
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import { mockBranch, mockPullRequest } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import DeleteBranchModal from './DeleteBranchModal';

jest.mock('~sq-server-commons/api/branches');

const branchesHandler = new BranchesServiceMock();

beforeEach(() => {
  branchesHandler.reset();
});

function renderModal(branchLike: BranchLike, onClose = jest.fn()) {
  renderComponent(
    <DeleteBranchModal branchLike={branchLike} component={mockComponent()} onClose={onClose} />,
  );
  return onClose;
}

it('deletes a branch through the branch API and closes', async () => {
  const user = userEvent.setup();
  const onClose = renderModal(mockBranch({ name: 'feature/old' }));

  expect(
    screen.getByText('project_branch_pull_request.branch.delete.are_you_sure.feature/old'),
  ).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'delete' }));

  expect(deleteBranch).toHaveBeenCalledWith({ branch: 'feature/old', project: 'my-project' });
  expect(onClose).toHaveBeenCalled();
});

it('deletes a pull request through the pull request API', async () => {
  const user = userEvent.setup();
  const onClose = renderModal(mockPullRequest({ key: '42', title: 'Tidy up' }));

  expect(
    screen.getByRole('heading', { name: 'project_branch_pull_request.pull_request.delete' }),
  ).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'delete' }));

  expect(deletePullRequest).toHaveBeenCalledWith({ project: 'my-project', pullRequest: '42' });
  expect(onClose).toHaveBeenCalled();
});

it('stays open with the button usable again when the server refuses', async () => {
  const user = userEvent.setup();
  jest.mocked(deleteBranch).mockRejectedValueOnce(new Error('refused'));
  const onClose = renderModal(mockBranch({ name: 'feature/old' }));

  await user.click(screen.getByRole('button', { name: 'delete' }));

  expect(await screen.findByRole('button', { name: 'delete' })).toBeEnabled();
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(onClose).not.toHaveBeenCalled();
});

it('closes on cancel', async () => {
  const user = userEvent.setup();
  const onClose = renderModal(mockBranch());

  await user.click(screen.getByRole('button', { name: 'cancel' }));

  expect(onClose).toHaveBeenCalled();
});
