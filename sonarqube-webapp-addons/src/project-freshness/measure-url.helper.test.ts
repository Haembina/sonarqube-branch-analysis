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

import { Path } from 'react-router';
import { MetricKey } from '~shared/types/metrics';
import { measureUrl } from './measure-url.helper';

function query(metricKey: string) {
  const { pathname, search } = measureUrl('project', metricKey) as Partial<Path>;
  return { pathname, params: Object.fromEntries(new URLSearchParams(search)) };
}

it('opens the open issues of the quality a count measures', () => {
  expect(query(MetricKey.software_quality_security_issues)).toEqual({
    pathname: '/project/issues',
    params: expect.objectContaining({ id: 'project', impactSoftwareQualities: 'SECURITY', issueStatuses: 'OPEN,CONFIRMED' }),
  });
});

it('opens only the new code issues for a new code count', () => {
  expect(query(MetricKey.new_software_quality_reliability_issues).params).toEqual(
    expect.objectContaining({ impactSoftwareQualities: 'RELIABILITY', inNewCodePeriod: 'true' }),
  );
});

it('opens the hotspots, on new code when the card shows new code', () => {
  expect(query(MetricKey.security_hotspots_reviewed)).toEqual({ pathname: '/security_hotspots', params: { id: 'project' } });
  expect(query(MetricKey.new_security_hotspots_reviewed).params).toEqual({ id: 'project', inNewCodePeriod: 'true' });
});

it('opens the measure page of anything else', () => {
  expect(query(MetricKey.coverage)).toEqual({
    pathname: '/component_measures',
    params: expect.objectContaining({ id: 'project', metric: 'coverage' }),
  });
});
