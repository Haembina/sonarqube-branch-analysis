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

import { FormattedMessage, useIntl } from 'react-intl';
import { SeparatorCircleIcon } from '~design-system';
import DateFromNow from '~shared/components/intl/DateFromNow';
import DateTimeFormatter from '~shared/components/intl/DateTimeFormatter';
import { Analysis } from './project-freshness.adapter';
import { useProjectFreshness } from './use-project-freshness.hook';

export interface ProjectFreshnessProps {
  projectKey: string;
}

function AnalysisAge({ analysis, messageId }: Readonly<{ analysis?: Analysis; messageId: string }>) {
  const { formatMessage } = useIntl();
  if (!analysis?.analysisDate) {
    return null;
  }
  const { analysisDate, name } = analysis;
  return (
    <>
      <SeparatorCircleIcon className="sw:mx-1" />
      <DateTimeFormatter date={analysisDate}>
        {(formattedDate) => (
          <span title={formatMessage({ id: 'branch_analysis.freshness.title' }, { date: formattedDate, name })}>
            <FormattedMessage id={messageId} values={{ date: <DateFromNow date={analysisDate} /> }} />
          </span>
        )}
      </DateTimeFormatter>
    </>
  );
}

export default function ProjectFreshness({ projectKey }: Readonly<ProjectFreshnessProps>) {
  const { data } = useProjectFreshness(projectKey);

  if (!data) {
    return null;
  }

  return (
    <>
      <AnalysisAge analysis={data.newestBranch} messageId="branch_analysis.freshness.branch" />
      <AnalysisAge analysis={data.newestPullRequest} messageId="branch_analysis.freshness.pull_request" />
      {data.mainState === 'MOVED' && (
        <>
          <SeparatorCircleIcon className="sw:mx-1" />
          <span className="sw:typo-semibold">
            <FormattedMessage id="branch_analysis.freshness.main_moved" values={{ main: data.main?.name }} />
          </span>
        </>
      )}
    </>
  );
}
