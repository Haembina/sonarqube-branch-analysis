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
import { loadProjectFreshness, ProjectFreshness } from './project-freshness.adapter';
import ProjectAnalyses from './ProjectAnalyses';

jest.mock('./project-freshness.adapter');

const MAIN_DATE = '2026-11-17T09:00:00+0100';
const BRANCH_DATE = '2026-11-17T10:00:00+0100';
const PULL_REQUEST_DATE = '2026-11-17T11:00:00+0100';

function answer(freshness: ProjectFreshness | null) {
  jest.mocked(loadProjectFreshness).mockResolvedValue(freshness);
}

function renderAnalyses(analysisDate?: string) {
  return renderComponent(
    <div data-testid="card">
      <ProjectAnalyses analysisDate={analysisDate} projectKey="project" />
    </div>,
  );
}

beforeEach(() => {
  jest.mocked(loadProjectFreshness).mockReset();
});

describe("capability project-freshness: see when each project's branches and pull requests were last analyzed", () => {
  it('draws a row per analysis, each linking to its overview with its name and date in a tooltip', async () => {
    answer({
      key: 'project',
      main: { analysisDate: MAIN_DATE, name: 'main', revision: 'abc123' },
      mainState: 'CURRENT',
      newestBranch: { analysisDate: BRANCH_DATE, name: 'feature/one' },
      newestPullRequest: { analysisDate: PULL_REQUEST_DATE, name: '12' },
    });
    renderAnalyses(MAIN_DATE);

    const branch = await screen.findByRole('link', { name: /feature\/one/ });
    expect(branch).toHaveAttribute('href', '/summary/new_code?branch=feature%2Fone&id=project');
    expect(branch).toHaveAttribute('title', expect.stringMatching(/^branch_analysis\.freshness\.title\..*\.feature\/one$/));
    expect(screen.getByRole('link', { name: /main$/ })).toHaveAttribute('href', '/project/overview?id=project');
    expect(screen.getByRole('link', { name: /branch_analysis\.freshness\.pull_request_name\.12/ })).toHaveAttribute(
      'href',
      '/summary/new_code?id=project&pullRequest=12',
    );
    expect(screen.queryByText('branch_analysis.freshness.main_moved')).not.toBeInTheDocument();
    expect(loadProjectFreshness).toHaveBeenCalledWith('project');
  });

  it('says main has new commits when the forge moved past its analysis', async () => {
    answer({ key: 'project', main: { analysisDate: MAIN_DATE, name: 'main' }, mainState: 'MOVED' });
    renderAnalyses(MAIN_DATE);

    expect(await screen.findByText('branch_analysis.freshness.main_moved')).toBeInTheDocument();
    expect(screen.getAllByRole('link')).toHaveLength(1);
  });

  it('draws main from the card alone until the server answers, and when it answers nothing', async () => {
    answer(null);
    renderAnalyses(MAIN_DATE);

    await waitFor(() => expect(loadProjectFreshness).toHaveBeenCalled());
    expect(screen.getByRole('link', { name: /branch_analysis\.freshness\.main/ })).toHaveAttribute(
      'href',
      '/project/overview?id=project',
    );
  });

  it('skips an analysis with no date', async () => {
    answer({ key: 'project', mainState: 'UNKNOWN', newestBranch: { name: 'feature/never' } });
    renderAnalyses();

    await waitFor(() => expect(loadProjectFreshness).toHaveBeenCalled());
    expect(screen.queryAllByRole('link')).toHaveLength(0);
  });
});
