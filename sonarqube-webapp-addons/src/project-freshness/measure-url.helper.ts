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

import { getComponentIssuesUrl } from '~shared/helpers/urls';
import { MetricKey } from '~shared/types/metrics';
import { isIssueMeasure, propsToIssueParams } from '~sq-server-commons/components/shared/utils';
import { getComponentDrilldownUrl } from '~sq-server-commons/helpers/urls';
import { getComponentSecurityHotspotsUrl } from '~sq-server-commons/sonar-aligned/helpers/urls';

const NEW_CODE_PREFIX = 'new_';

const HOTSPOT_METRICS = new Set<string>([
  MetricKey.security_hotspots_reviewed,
  MetricKey.new_security_hotspots_reviewed,
]);

export function measureUrl(componentKey: string, metricKey: string) {
  const inNewCodePeriod = metricKey.startsWith(NEW_CODE_PREFIX);
  if (isIssueMeasure(metricKey)) {
    return getComponentIssuesUrl(componentKey, propsToIssueParams(metricKey, inNewCodePeriod));
  }
  if (HOTSPOT_METRICS.has(metricKey)) {
    return getComponentSecurityHotspotsUrl(componentKey, undefined, {
      inNewCodePeriod: inNewCodePeriod ? String(inNewCodePeriod) : undefined,
    });
  }
  return getComponentDrilldownUrl({ componentKey, metric: metricKey });
}
