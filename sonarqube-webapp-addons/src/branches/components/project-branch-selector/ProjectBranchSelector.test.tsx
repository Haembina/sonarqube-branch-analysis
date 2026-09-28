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
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import {
  mockBranch,
  mockMainBranch,
  mockPullRequest,
} from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import { Feature } from '~sq-server-commons/types/features';
import ProjectBranchSelector from './ProjectBranchSelector';

/** Where the bound platform lives; the selector never reads it. */
const ALM_URL = 'https://alm.example.com';

jest.mock('~sq-server-commons/api/branches');

const branchesHandler = new BranchesServiceMock();

const main = mockMainBranch({ name: 'main', status: { qualityGateStatus: 'OK' } });

beforeEach(() => {
  branchesHandler.emptyBranchesAndPullRequest();
  branchesHandler.addBranch(main);
  branchesHandler.addBranch(
    mockBranch({
      analysisDate: '2026-03-01',
      name: 'develop',
      status: { qualityGateStatus: 'ERROR' },
    }),
  );
  branchesHandler.addBranch(mockBranch({ name: 'never-analyzed', analysisDate: undefined }));
  branchesHandler.addPullRequest(
    mockPullRequest({
      analysisDate: '2026-04-01',
      branch: 'feature/speed',
      key: '12',
      title: 'Faster scans',
    }),
  );
});

function renderSelector(
  props: Partial<{
    almKey: string;
    current: BranchLike;
    linkToAll: string;
    linkToBranches: string;
    linkToPRs: string;
    overridePath: Parameters<typeof ProjectBranchSelector>[0]['overridePath'];
  }> = {},
  path = '/dashboard?id=my-project&branch=develop',
) {
  const { almKey, current, ...rest } = props;
  return renderComponent(
    <ProjectBranchSelector
      component={mockComponent(almKey ? { alm: { key: almKey, url: ALM_URL } } : {})}
      currentBranchLike={current ?? main}
      {...rest}
    />,
    path,
    { featureList: [Feature.BranchSupport] },
  );
}

async function open() {
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: /main/ }));
  return {
    user,
    popover: await screen.findByRole('dialog', { name: 'project_branch_selector.title' }),
  };
}

describe('capability switch-branch: open another branch or pull request of a project', () => {
  it('lists the main branch first, then the most recently analyzed, never the unanalyzed', async () => {
    renderSelector();
    const { popover } = await open();

    const links = await within(popover).findAllByRole('link');
    expect(links.map((link) => link.textContent)).toEqual([
      expect.stringContaining('main'),
      expect.stringContaining('12 – Faster scans'),
      expect.stringContaining('develop'),
    ]);
    expect(within(popover).queryByText('never-analyzed')).not.toBeInTheDocument();
    const [mainLink, , developLink] = links;
    expect(mainLink).toHaveTextContent('metric.level.OK');
    // Only the current entry, main, shows its check mark.
    expect(mainLink.querySelector('.sw-invisible')).toBeNull();
    expect(developLink.querySelector('.sw-invisible')).not.toBeNull();
  });

  it('links each entry to the same page on that branch or pull request', async () => {
    renderSelector();
    const { popover } = await open();

    expect(await within(popover).findByRole('link', { name: /develop/ })).toHaveAttribute(
      'href',
      '/dashboard?id=my-project&branch=develop',
    );
    expect(within(popover).getByRole('link', { name: /Faster scans/ })).toHaveAttribute(
      'href',
      '/dashboard?id=my-project&pullRequest=12',
    );
    expect(within(popover).getByRole('link', { name: /main/ })).toHaveAttribute(
      'href',
      '/dashboard?id=my-project',
    );
  });

  it('follows an override path, given as a path or as a function of the entry', async () => {
    renderSelector({
      overridePath: (branchLike) => ({
        pathname: '/project/issues',
        search: `?id=my-project&from=${branchLike.analysisDate ?? 'none'}`,
      }),
    });
    const { popover } = await open();

    expect(await within(popover).findByRole('link', { name: /develop/ })).toHaveAttribute(
      'href',
      '/project/issues?id=my-project&from=2026-03-01&branch=develop',
    );
  });

  it('filters by what the user types, matching a pull request by its branch too', async () => {
    renderSelector();
    const { user, popover } = await open();

    await user.type(within(popover).getByRole('searchbox'), 'speed');
    expect(within(popover).getAllByRole('link')).toHaveLength(1);
    expect(within(popover).getByRole('link', { name: /Faster scans/ })).toBeInTheDocument();

    await user.clear(within(popover).getByRole('searchbox'));
    await user.type(within(popover).getByRole('searchbox'), 'no such thing');
    expect(within(popover).queryByRole('link')).not.toBeInTheDocument();
    expect(within(popover).getByText('project_branch_selector.none')).toBeInTheDocument();
  });

  it('narrows to branches or to pull requests, with the "see all" link of each', async () => {
    renderSelector({
      almKey: 'github',
      linkToAll: '/all',
      linkToBranches: '/branches',
      linkToPRs: '/pull-requests',
    });
    const { user, popover } = await open();

    expect(
      within(popover).getByRole('link', { name: 'project_branch_selector.link.all' }),
    ).toHaveAttribute('href', '/all');

    await user.click(within(popover).getByRole('radio', { name: 'branches' }));
    expect(within(popover).queryByRole('link', { name: /Faster scans/ })).not.toBeInTheDocument();
    expect(within(popover).getByRole('link', { name: /develop/ })).toBeInTheDocument();
    expect(
      within(popover).getByRole('link', { name: 'project_branch_selector.link.branches' }),
    ).toHaveAttribute('href', '/branches');

    await user.click(within(popover).getByRole('radio', { name: 'pull_requests.github' }));
    expect(within(popover).queryByRole('link', { name: /develop/ })).not.toBeInTheDocument();
    expect(within(popover).queryByRole('link', { name: /^main/ })).not.toBeInTheDocument();
    expect(within(popover).getByRole('link', { name: /Faster scans/ })).toBeInTheDocument();
    expect(
      within(popover).getByRole('link', { name: /project_branch_selector\.link\.pull_requests/ }),
    ).toHaveAttribute('href', '/pull-requests');
  });

  it('is reached, opened and closed by keyboard alone', async () => {
    renderSelector({ current: mockBranch({ name: 'develop', analysisDate: '2026-03-01' }) });
    const user = userEvent.setup();

    await user.tab();
    expect(screen.getByRole('button', { name: /develop/ })).toHaveFocus();
    await user.keyboard('{Enter}');
    const popover = await screen.findByRole('dialog');

    expect(within(popover).getByRole('searchbox')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
