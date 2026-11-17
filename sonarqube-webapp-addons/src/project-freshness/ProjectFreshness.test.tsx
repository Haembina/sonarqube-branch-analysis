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

import { screen, waitFor } from '@testing-library/react';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { loadProjectFreshness, ProjectFreshness as Freshness } from './project-freshness.adapter';
import ProjectFreshness from './ProjectFreshness';

jest.mock('./project-freshness.adapter');

const BRANCH_DATE = '2026-11-17T10:00:00+0100';
const PULL_REQUEST_DATE = '2026-11-17T11:00:00+0100';

function answer(freshness: Freshness | null) {
  jest.mocked(loadProjectFreshness).mockResolvedValue(freshness);
}

function renderFreshness() {
  return renderComponent(
    <div data-testid="card">
      <ProjectFreshness projectKey="project" />
    </div>,
  );
}

beforeEach(() => {
  jest.mocked(loadProjectFreshness).mockReset();
});

it('names the newest branch and pull request analysis, each with its name in a tooltip', async () => {
  answer({
    key: 'project',
    main: { analysisDate: BRANCH_DATE, name: 'main', revision: 'abc123' },
    mainState: 'CURRENT',
    newestBranch: { analysisDate: BRANCH_DATE, name: 'feature/one' },
    newestPullRequest: { analysisDate: PULL_REQUEST_DATE, name: '12' },
  });
  renderFreshness();

  expect(await screen.findByText(/^branch_analysis\.freshness\.branch/)).toHaveAttribute(
    'title',
    expect.stringMatching(/^branch_analysis\.freshness\.title\..*\.feature\/one$/),
  );
  expect(screen.getByText(/^branch_analysis\.freshness\.pull_request/)).toBeInTheDocument();
  expect(screen.queryByText(/branch_analysis\.freshness\.main_moved/)).not.toBeInTheDocument();
  expect(loadProjectFreshness).toHaveBeenCalledWith('project');
});

it('says main has new commits when the forge moved past its analysis', async () => {
  answer({ key: 'project', main: { analysisDate: BRANCH_DATE, name: 'main' }, mainState: 'MOVED' });
  renderFreshness();

  expect(await screen.findByText('branch_analysis.freshness.main_moved.main')).toBeInTheDocument();
  expect(screen.queryByText(/^branch_analysis\.freshness\.branch/)).not.toBeInTheDocument();
});

it('skips a branch with no analysis date', async () => {
  answer({ key: 'project', mainState: 'UNKNOWN', newestBranch: { name: 'feature/never' } });
  renderFreshness();

  await waitFor(() => expect(loadProjectFreshness).toHaveBeenCalled());
  expect(screen.getByTestId('card')).toBeEmptyDOMElement();
});

it('draws nothing for a project the server answered nothing for', async () => {
  answer(null);
  renderFreshness();

  await waitFor(() => expect(loadProjectFreshness).toHaveBeenCalled());
  expect(screen.getByTestId('card')).toBeEmptyDOMElement();
});
