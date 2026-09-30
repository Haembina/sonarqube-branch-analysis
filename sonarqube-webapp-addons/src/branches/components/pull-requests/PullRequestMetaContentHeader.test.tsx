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
import { MetricKey } from '~shared/types/metrics';
import { mockPullRequest } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockMeasureEnhanced, mockMetric } from '~sq-server-commons/helpers/testMocks';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { PullRequestMetaContentHeader } from './PullRequestMetaContentHeader';

const pullRequest = mockPullRequest({
  analysisDate: '2026-09-20T10:00:00+0000',
  branch: 'feature/speed',
  target: 'main',
});

it('shows the last analysis, the new lines and the branches it merges', () => {
  renderComponent(
    <PullRequestMetaContentHeader
      measures={[
        mockMeasureEnhanced({
          metric: mockMetric({ key: MetricKey.new_lines }),
          period: { bestValue: false, index: 1, value: '42' },
        }),
      ]}
      pullRequest={pullRequest}
    />,
  );

  expect(screen.getByText(/overview\.last_analysis_x/)).toBeInTheDocument();
  expect(screen.getByText('42 metric.new_lines.name')).toBeInTheDocument();
  expect(screen.getByText(/branch_like_navigation\.for_merge_into_x_from_y/)).toBeInTheDocument();
});

it('shows zero new lines and no analysis for a pull request never analyzed', () => {
  const { container } = renderComponent(
    <PullRequestMetaContentHeader
      measures={[]}
      pullRequest={{ ...pullRequest, analysisDate: undefined }}
    />,
  );

  expect(screen.queryByText(/overview\.last_analysis_x/)).not.toBeInTheDocument();
  expect(container).toHaveTextContent('0 metric.new_lines.name');
});
