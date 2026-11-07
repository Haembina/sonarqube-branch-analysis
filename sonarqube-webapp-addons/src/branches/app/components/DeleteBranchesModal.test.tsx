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
import { deletePullRequest } from '~sq-server-commons/api/branches';
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import { mockBranch, mockPullRequest } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import DeleteBranchesModal from './DeleteBranchesModal';

jest.mock('~sq-server-commons/api/branches');

const branchesHandler = new BranchesServiceMock();

beforeEach(() => {
  branchesHandler.reset();
});

function renderModal(branchLikes: BranchLike[]) {
  const onClose = jest.fn();
  const onDone = jest.fn();
  renderComponent(
    <DeleteBranchesModal
      branchLikes={branchLikes}
      component={mockComponent()}
      onClose={onClose}
      onDone={onDone}
    />,
  );
  return { onClose, onDone };
}

it('deletes every branch it lists and reports none left over', async () => {
  const user = userEvent.setup();
  const branches = [mockBranch({ name: 'delete-branch' }), mockBranch({ name: 'normal-branch' })];
  const { onDone } = renderModal(branches);

  expect(screen.getByText('branch_analysis.delete_selected.title.2')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'delete' }));

  expect(onDone).toHaveBeenCalledWith([]);
  expect(branchesHandler.branches.map(({ name }) => name)).toEqual(['main']);
});

it('reports the pull requests it could not delete', async () => {
  jest.mocked(deletePullRequest).mockRejectedValueOnce(new Error('refused'));
  const user = userEvent.setup();
  const refused = mockPullRequest({ key: '01', title: 'refused' });
  const { onDone } = renderModal([refused, mockPullRequest({ key: '02', title: 'kept' })]);

  expect(screen.getByText('branch_analysis.delete_selected.title.pull_requests.2')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'delete' }));

  expect(onDone).toHaveBeenCalledWith([refused]);
  expect(
    await screen.findByText('branch_analysis.delete_selected.failed.01 – refused'),
  ).toBeInTheDocument();
});

it('closes on cancel and deletes nothing', async () => {
  const user = userEvent.setup();
  const { onClose, onDone } = renderModal([mockBranch({ name: 'delete-branch' })]);

  await user.click(screen.getByRole('button', { name: 'cancel' }));

  expect(onClose).toHaveBeenCalled();
  expect(onDone).not.toHaveBeenCalled();
  expect(branchesHandler.branches).toHaveLength(3);
});
