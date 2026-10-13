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
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import { mockBranch, mockPullRequest } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import BranchLikeTabs from './BranchLikeTabs';

jest.mock('./BranchLikeTable', () => ({
  __esModule: true,
  default: ({ onSetAsMain }: { onSetAsMain: (branchLike: BranchLike) => void }) => (
    <>
      <button onClick={() => onSetAsMain(mockBranch({ name: 'develop' }))} type="button">
        set branch
      </button>
      <button onClick={() => onSetAsMain(mockPullRequest())} type="button">
        set pull request
      </button>
    </>
  ),
}));

const branchesHandler = new BranchesServiceMock();

beforeEach(() => {
  branchesHandler.reset();
});

function renderTabs() {
  renderComponent(<BranchLikeTabs component={mockComponent()} fetchComponent={jest.fn()} />);
}

it('opens the set-as-main dialog for a branch', async () => {
  const user = userEvent.setup();
  renderTabs();

  await user.click(screen.getByRole('button', { name: 'set branch' }));

  expect(screen.getByRole('dialog')).toBeInTheDocument();
});

it('opens no dialog when a pull request is offered as the main branch', async () => {
  const user = userEvent.setup();
  renderTabs();

  await user.click(screen.getByRole('button', { name: 'set pull request' }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'set pull request' })).toBeEnabled();
});
