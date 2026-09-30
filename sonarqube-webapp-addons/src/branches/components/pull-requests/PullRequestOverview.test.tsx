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

import { screen, waitFor, within } from '@testing-library/react';
import { MetricKey } from '~shared/types/metrics';
import AlmSettingsServiceMock from '~sq-server-commons/api/mocks/AlmSettingsServiceMock';
import BranchesServiceMock from '~sq-server-commons/api/mocks/BranchesServiceMock';
import { PARENT_COMPONENT_KEY } from '~sq-server-commons/api/mocks/data/ids';
import { MeasuresServiceMock } from '~sq-server-commons/api/mocks/MeasuresServiceMock';
import { QualityGatesServiceMock } from '~sq-server-commons/api/mocks/QualityGatesServiceMock';
import { getMeasuresWithPeriodAndMetrics } from '~sq-server-commons/api/measures';
import { getQualityGateProjectStatus } from '~sq-server-commons/api/quality-gates';
import { mockPullRequest } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { mockQualityGateProjectStatus } from '~sq-server-commons/helpers/mocks/quality-gates';
import { mockLoggedInUser, mockMeasure } from '~sq-server-commons/helpers/testMocks';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { Feature } from '~sq-server-commons/types/features';
import PullRequestOverview from './PullRequestOverview';

jest.mock('~sq-server-commons/api/branches');
jest.mock('~sq-server-commons/api/measures');
jest.mock('~sq-server-commons/api/quality-gates');
jest.mock('~sq-server-commons/api/ce', () => ({
  getAnalysisStatus: jest.fn().mockResolvedValue({ component: { warnings: [] } }),
}));

const almHandler = new AlmSettingsServiceMock();
const branchesHandler = new BranchesServiceMock();
const measuresHandler = new MeasuresServiceMock();
const qualityGatesHandler = new QualityGatesServiceMock();

const pullRequest = mockPullRequest({
  analysisDate: '2026-09-20T10:00:00+0000',
  branch: 'feature/speed',
  key: '01',
  target: 'main',
  title: 'Faster scans',
  url: 'https://github.com/haembina/example/pull/1',
});

const newCodeMeasure = (metric: MetricKey, value: string) =>
  mockMeasure({ metric, period: { index: 1, value, bestValue: false } });

beforeEach(() => {
  almHandler.reset();
  branchesHandler.reset();
  branchesHandler.addPullRequest(pullRequest);
  measuresHandler.reset();
  measuresHandler.registerComponentMeasures({
    [PARENT_COMPONENT_KEY]: {
      [MetricKey.new_violations]: newCodeMeasure(MetricKey.new_violations, '3'),
      [MetricKey.new_accepted_issues]: newCodeMeasure(MetricKey.new_accepted_issues, '1'),
      [MetricKey.pull_request_fixed_issues]: mockMeasure({
        metric: MetricKey.pull_request_fixed_issues,
        value: '2',
        period: undefined,
      }),
      [MetricKey.new_lines]: newCodeMeasure(MetricKey.new_lines, '42'),
      [MetricKey.new_security_hotspots]: newCodeMeasure(MetricKey.new_security_hotspots, '1'),
    },
  });
  qualityGatesHandler.reset();
  qualityGatesHandler.setQualityGateProjectStatus(
    mockQualityGateProjectStatus({
      status: 'ERROR',
      conditions: [
        {
          actualValue: '3',
          comparator: 'GT',
          errorThreshold: '0',
          metricKey: MetricKey.new_violations,
          periodIndex: 1,
          status: 'ERROR',
        },
      ],
    }),
  );
});

function renderOverview(currentUser = mockLoggedInUser()) {
  return renderComponent(
    <PullRequestOverview
      component={mockComponent({ key: PARENT_COMPONENT_KEY })}
      pullRequest={pullRequest}
    />,
    `/dashboard?id=${PARENT_COMPONENT_KEY}&pullRequest=01`,
    { currentUser, featureList: [Feature.BranchSupport] },
  );
}

describe('capability pull-request-overview: read the quality gate and new-code measures of a pull request', () => {
  it('shows the failed gate, the failing condition and the new-code measures', async () => {
    renderOverview();

    expect(await screen.findByText('metric.level.ERROR')).toBeInTheDocument();
    expect(screen.getByText('overview.new_issues')).toBeInTheDocument();
    expect(screen.getByText('overview.accepted_issues')).toBeInTheDocument();
    expect(screen.getByText('overview.pull_request.fixed_issues')).toBeInTheDocument();
    expect(screen.getByTestId(`overview__measures-${MetricKey.new_violations}`)).toHaveTextContent(
      '3',
    );
    expect(screen.getAllByText(/42/).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/branch_like_navigation\.for_merge_into_x_from_y/)).not.toHaveLength(
      0,
    );
  });

  it('promotes SonarQube for IDE on a failed gate to a signed-in user', async () => {
    renderOverview();

    expect(await screen.findByText('overview.sonarlint_ad.header')).toBeInTheDocument();
  });

  it('shows a passed gate, and zero for measures a pull request never received', async () => {
    measuresHandler.registerComponentMeasures({ [PARENT_COMPONENT_KEY]: {} });
    qualityGatesHandler.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'OK',
        conditions: [
          {
            actualValue: '0',
            comparator: 'GT',
            errorThreshold: '0',
            metricKey: MetricKey.new_violations,
            periodIndex: 1,
            status: 'OK',
          },
        ],
      }),
    );
    renderComponent(
      <PullRequestOverview
        component={mockComponent({ key: PARENT_COMPONENT_KEY })}
        pullRequest={{ ...pullRequest, analysisDate: undefined }}
      />,
      `/dashboard?id=${PARENT_COMPONENT_KEY}&pullRequest=01`,
      { currentUser: mockLoggedInUser(), featureList: [Feature.BranchSupport] },
    );

    expect(await screen.findByText('metric.level.OK')).toBeInTheDocument();
    expect(screen.getAllByText('0').length).toBeGreaterThan(0);
    expect(screen.getByText('issue.type.SECURITY_HOTSPOT.plural')).toBeInTheDocument();
    expect(screen.queryByText('overview.sonarlint_ad.header')).not.toBeInTheDocument();
  });

  it('says how a passed issues condition was met, and warns of conditions it ignored', async () => {
    measuresHandler.registerComponentMeasures({
      [PARENT_COMPONENT_KEY]: {
        [MetricKey.new_violations]: newCodeMeasure(MetricKey.new_violations, '0'),
      },
    });
    qualityGatesHandler.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'OK',
        ignoredConditions: true,
        conditions: [
          {
            actualValue: '0',
            comparator: 'GT',
            errorThreshold: '0',
            metricKey: MetricKey.new_violations,
            periodIndex: 1,
            status: 'OK',
          },
        ],
      }),
    );
    renderOverview();

    expect(await screen.findByText('metric.level.OK')).toBeInTheDocument();
    expect(screen.getByText('overview.quality_gate.ignored_conditions')).toBeInTheDocument();
    expect(
      within(screen.getByTestId(`overview__measures-${MetricKey.new_violations}`)).getByText(
        /overview\.quality_gate\.required_x/,
      ),
    ).toBeInTheDocument();
  });

  it('shows the issues card without a count when the measures answer carries no measure list', async () => {
    jest.mocked(getMeasuresWithPeriodAndMetrics).mockResolvedValueOnce({
      component: { key: PARENT_COMPONENT_KEY, name: 'project', qualifier: 'TRK' },
      metrics: [],
    } as Awaited<ReturnType<typeof getMeasuresWithPeriodAndMetrics>>);
    renderOverview();

    expect(await screen.findByText('metric.level.ERROR')).toBeInTheDocument();
    const issuesCard = screen.getByTestId(`overview__measures-${MetricKey.new_violations}`);
    expect(issuesCard).toHaveTextContent('overview.new_issues');
    expect(issuesCard).not.toHaveTextContent(/\d/);
  });

  it('renders nothing when the pull request has no gate status yet', async () => {
    jest.mocked(getQualityGateProjectStatus).mockRejectedValue(new Error('not analyzed'));
    const { container } = renderOverview();

    await waitFor(() => {
      expect(container).toBeEmptyDOMElement();
    });
    jest.mocked(getQualityGateProjectStatus).mockImplementation(
      qualityGatesHandler.handleQualityGetProjectStatus,
    );
  });
});
