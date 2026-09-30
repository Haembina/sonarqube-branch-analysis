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
import { ComponentQualifier } from '~shared/types/component';
import {
  mockBranch,
  mockMainBranch,
  mockPullRequest,
} from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { mockRouter } from '~sq-server-commons/helpers/testMocks';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import MenuWithRouter, { Menu } from './Menu';

const main = mockMainBranch({ name: 'main' });
const develop = mockBranch({ name: 'develop' });
const release = mockBranch({ name: 'release-1' });
const pullRequest = mockPullRequest({
  base: 'develop',
  key: '7',
  target: 'develop',
  title: 'Faster scans',
});
const orphan = mockPullRequest({ key: '9', title: 'Lost work', target: 'gone', isOrphan: true });
const branchLikes: BranchLike[] = [main, develop, release, pullRequest, orphan];

function renderMenu(
  overrides: Partial<{ branchLikes: BranchLike[]; canAdmin: boolean; current: BranchLike }> = {},
) {
  const onClose = jest.fn();
  const router = mockRouter();
  renderComponent(
    <Menu
      branchLikes={overrides.branchLikes ?? branchLikes}
      canAdminComponent={overrides.canAdmin ?? false}
      component={mockComponent({ key: 'project', qualifier: ComponentQualifier.Project })}
      currentBranchLike={overrides.current ?? develop}
      onClose={onClose}
      router={router}
    />,
  );
  return { onClose, router };
}

const item = (name: string) => screen.getByRole('menuitem', { name: new RegExp(name) });

describe('the branch and pull request menu', () => {
  it('lists branches with their pull requests nested, orphans last, and says how many', () => {
    renderMenu();

    expect(item('main')).toBeInTheDocument();
    expect(item('develop')).toBeInTheDocument();
    expect(item('7 – Faster scans')).toBeInTheDocument();
    expect(item('9 – Lost work')).toBeInTheDocument();
    expect(screen.getByText('branch_like_navigation.orphan_pull_requests')).toBeInTheDocument();
    expect(screen.getByText('results_shown_x.5')).toBeInTheDocument();
  });

  it('filters branches by name and pull requests by title or key', async () => {
    const user = userEvent.setup();
    renderMenu();

    await user.type(screen.getByRole('searchbox'), 'FASTER');
    expect(item('7 – Faster scans')).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: /develop/ })).not.toBeInTheDocument();

    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'rel');
    expect(item('release-1')).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: /main/ })).not.toBeInTheDocument();

    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'nothing-like-this');
    expect(screen.getByText('no_results_for_x.nothing-like-this')).toBeInTheDocument();
  });

  it('moves the highlight with the arrow keys and opens it with Enter', async () => {
    const user = userEvent.setup();
    const { onClose, router } = renderMenu();

    expect(screen.getByRole('searchbox')).toHaveFocus();
    expect(item('develop')).toHaveClass('branch-like-active');

    await user.keyboard('{ArrowDown}');
    expect(item('7 – Faster scans')).toHaveClass('branch-like-active');
    await user.keyboard('{ArrowUp}{ArrowUp}');
    expect(item('main')).toHaveClass('branch-like-active');
    await user.keyboard('{ArrowUp}');
    expect(item('main')).toHaveClass('branch-like-active');

    await user.keyboard('{Enter}');
    expect(onClose).toHaveBeenCalled();
    expect(router.push).toHaveBeenCalledWith(
      expect.objectContaining({ pathname: '/dashboard' }),
    );
  });

  it('opens the entry a user clicks', async () => {
    const user = userEvent.setup();
    const { router } = renderMenu();

    await user.click(item('release-1'));

    expect(router.push).toHaveBeenCalledWith(
      expect.objectContaining({ search: expect.stringContaining('branch=release-1') }),
    );
  });

  it('highlights the first entry when the current one is not listed, and nothing when empty', async () => {
    const user = userEvent.setup();
    const { router } = renderMenu({ current: mockBranch({ name: 'elsewhere' }) });

    expect(item('main')).toHaveClass('branch-like-active');

    await user.type(screen.getByRole('searchbox'), 'zzz');
    await user.keyboard('{Enter}');
    expect(router.push).not.toHaveBeenCalled();
    expect(screen.getByText('no_results_for_x.zzz')).toBeInTheDocument();
  });

  it('shows no entry and no highlight for a project without branches', () => {
    renderMenu({ branchLikes: [] });

    expect(screen.queryByRole('menuitem')).not.toBeInTheDocument();
    expect(screen.getByText('no_results_for_x')).toBeInTheDocument();
  });

  it('links an administrator to the branches page, closing the menu', async () => {
    const user = userEvent.setup();
    const { onClose } = renderMenu({ canAdmin: true });

    await user.click(screen.getByRole('link', { name: 'branch_like_navigation.manage' }));

    expect(onClose).toHaveBeenCalled();
  });

  it('is exported wrapped with the router', () => {
    renderComponent(
      <MenuWithRouter
        branchLikes={branchLikes}
        component={mockComponent()}
        currentBranchLike={main}
        onClose={jest.fn()}
      />,
    );

    expect(item('main')).toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: 'branch_like_navigation.manage' }),
    ).not.toBeInTheDocument();
  });
});
