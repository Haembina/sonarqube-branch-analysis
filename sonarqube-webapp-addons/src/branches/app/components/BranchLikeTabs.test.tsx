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

import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ComponentProps } from 'react';
import { deleteBranch } from '~sq-server-commons/api/branches';
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import { mockBranch, mockPullRequest } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import { Feature } from '~sq-server-commons/types/features';
import BranchLikeTabs from './BranchLikeTabs';

jest.mock('~sq-server-commons/api/branches');

jest.mock('./BranchLikeTable', () => {
  const { BranchLikeTable } = jest.requireActual('./BranchLikeTable');
  return {
    __esModule: true,
    default: (props: ComponentProps<typeof BranchLikeTable>) => (
      <>
        <button
          onClick={() => props.onSetAsMain(mockBranch({ name: 'develop' }) as BranchLike)}
          type="button"
        >
          set branch
        </button>
        <button onClick={() => props.onSetAsMain(mockPullRequest())} type="button">
          set pull request
        </button>
        <BranchLikeTable {...props} />
      </>
    ),
  };
});

const branchesHandler = new BranchesServiceMock();

beforeEach(() => {
  branchesHandler.reset();
});

function renderTabs() {
  renderComponent(
    <BranchLikeTabs component={mockComponent()} fetchComponent={jest.fn()} />,
    '/',
    { featureList: [Feature.BranchSupport] },
  );
}

const selectBox = (name: string) =>
  screen.getByRole('checkbox', { name: `branch_analysis.select.${name}` });

const deleteSelected = (count: number) =>
  screen.getByRole('button', { name: `branch_analysis.delete_selected.${count}` });

const filterField = () => screen.getByRole('searchbox', { name: 'search.search_by_name' });

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

describe('capability filter-branches: find a branch or pull request by name', () => {
  it('narrows the table by name as the user types, whatever the case', async () => {
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');

    await user.type(filterField(), 'NORMAL');

    expect(screen.getByText('normal-branch')).toBeInTheDocument();
    expect(screen.queryByText('delete-branch')).not.toBeInTheDocument();
    expect(screen.queryByText('main')).not.toBeInTheDocument();
  });

  it('says when no name matches', async () => {
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');

    await user.type(filterField(), 'nothing-like-it');

    expect(screen.getByText('no_results_for_x.nothing-like-it')).toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: 'check_all' })).not.toBeInTheDocument();
  });
});

describe('capability bulk-delete-branches: delete several branches or pull requests at once', () => {
  it('deletes the selected branches after one confirmation, and never offers the main branch', async () => {
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');

    expect(screen.queryByRole('checkbox', { name: 'branch_analysis.select.main' })).not.toBeInTheDocument();
    expect(deleteSelected(0)).toBeDisabled();

    await user.click(selectBox('delete-branch'));
    await user.click(selectBox('normal-branch'));
    await user.click(deleteSelected(2));

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('branch_analysis.delete_selected.title.2')).toBeInTheDocument();
    expect(within(dialog).getByText('delete-branch')).toBeInTheDocument();
    expect(within(dialog).getByText('normal-branch')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'delete' }));

    expect(await screen.findByText('main')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByText('delete-branch')).not.toBeInTheDocument();
    expect(screen.queryByText('normal-branch')).not.toBeInTheDocument();
    expect(filterField()).toHaveFocus();
  });

  it('names the ones it could not delete and keeps them selected', async () => {
    jest.mocked(deleteBranch).mockRejectedValueOnce(new Error('refused'));
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');

    await user.click(screen.getByRole('checkbox', { name: 'check_all' }));
    await user.click(deleteSelected(2));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'delete' }));

    const dialog = screen.getByRole('dialog');
    expect(
      await within(dialog).findByText('branch_analysis.delete_selected.failed.delete-branch'),
    ).toBeInTheDocument();
    expect(within(dialog).getByText('branch_analysis.delete_selected.title.1')).toBeInTheDocument();
    expect(deleteSelected(1)).toBeEnabled();
  });

  it('selects every shown branch at once, and clears them again', async () => {
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');

    await user.click(selectBox('normal-branch'));
    expect(screen.getByRole('checkbox', { name: 'check_all' })).toHaveAttribute(
      'aria-checked',
      'mixed',
    );

    await user.click(screen.getByRole('checkbox', { name: 'check_all' }));
    expect(selectBox('delete-branch')).toBeChecked();
    expect(deleteSelected(2)).toBeEnabled();

    await user.click(screen.getByRole('checkbox', { name: 'uncheck_all' }));
    expect(selectBox('delete-branch')).not.toBeChecked();
    expect(selectBox('normal-branch')).not.toBeChecked();
  });

  it('deletes only the selected rows the filter still shows', async () => {
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');

    await user.click(screen.getByRole('checkbox', { name: 'check_all' }));
    await user.type(filterField(), 'normal');

    expect(deleteSelected(1)).toBeEnabled();
  });

  it('puts focus back on its button when the confirmation is cancelled, deleting nothing', async () => {
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');

    await user.click(selectBox('delete-branch'));
    await user.click(deleteSelected(1));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'cancel' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByText('delete-branch')).toBeInTheDocument();
    expect(deleteSelected(1)).toHaveFocus();
  });

  it('selects pull requests by keyboard and clears the selection on a tab change', async () => {
    const user = userEvent.setup();
    renderTabs();
    await screen.findByText('delete-branch');
    await user.click(selectBox('delete-branch'));

    await user.click(
      screen.getByRole('tab', { name: 'project_branch_pull_request.tabs.pull_requests' }),
    );
    expect(deleteSelected(0)).toBeDisabled();

    (
      await screen.findByRole('checkbox', {
        name: 'branch_analysis.select.01 – TEST-191 update master',
      })
    ).focus();
    await user.keyboard(' ');
    await user.click(deleteSelected(1));

    expect(
      within(screen.getByRole('dialog')).getByText(
        'branch_analysis.delete_selected.title.pull_requests.1',
      ),
    ).toBeInTheDocument();
  });
});
