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

import {
  Badge,
  IconBranch,
  IconGitBranch,
  IconPullrequest,
  LinkStandalone,
  Text,
} from '@sonarsource/echoes-react';
import { ReactNode } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import DateFromNow from '~shared/components/intl/DateFromNow';
import DateTimeFormatter from '~shared/components/intl/DateTimeFormatter';
import { getProjectOverviewUrl } from '~shared/helpers/urls';
import { getBranchUrl, getPullRequestUrl } from '~sq-server-commons/helpers/urls';
import { Analysis } from './project-freshness.adapter';
import { useProjectFreshness } from './use-project-freshness.hook';

export interface ProjectAnalysesProps {
  analysisDate?: string;
  projectKey: string;
}

interface AnalysisRowProps {
  analysis: Analysis;
  children?: ReactNode;
  icon: ReactNode;
  label: ReactNode;
  to: ReturnType<typeof getProjectOverviewUrl>;
}

function AnalysisRow({ analysis, children, icon, label, to }: Readonly<AnalysisRowProps>) {
  const { formatMessage } = useIntl();
  const { analysisDate, name } = analysis;
  if (!analysisDate) {
    return null;
  }
  return (
    <DateTimeFormatter date={analysisDate}>
      {(formattedDate) => (
        <div className="sw:flex sw:items-center sw:gap-2 sw:min-w-0">
          <LinkStandalone
            className="sw:min-w-0 sw:truncate"
            highlight="current-color"
            iconLeft={icon}
            title={formatMessage({ id: 'branch_analysis.freshness.title' }, { date: formattedDate, name })}
            to={to}
          >
            {label}
          </LinkStandalone>
          <Text className="sw:whitespace-nowrap" isSubtle>
            <DateFromNow date={analysisDate} />
          </Text>
          {children}
        </div>
      )}
    </DateTimeFormatter>
  );
}

export default function ProjectAnalyses({ analysisDate, projectKey }: Readonly<ProjectAnalysesProps>) {
  const { data } = useProjectFreshness(projectKey);
  const main = data?.main ?? { analysisDate, name: '' };

  return (
    <div className="sw:flex sw:flex-col sw:gap-1 sw:min-w-0">
      <AnalysisRow
        analysis={main}
        icon={<IconBranch />}
        label={main.name || <FormattedMessage id="branch_analysis.freshness.main" />}
        to={getProjectOverviewUrl(projectKey)}
      >
        {data?.mainState === 'MOVED' && (
          <Badge variety="warning">
            <FormattedMessage id="branch_analysis.freshness.main_moved" />
          </Badge>
        )}
      </AnalysisRow>
      {data?.newestBranch && (
        <AnalysisRow
          analysis={data.newestBranch}
          icon={<IconGitBranch />}
          label={data.newestBranch.name}
          to={getBranchUrl(projectKey, data.newestBranch.name)}
        />
      )}
      {data?.newestPullRequest && (
        <AnalysisRow
          analysis={data.newestPullRequest}
          icon={<IconPullrequest />}
          label={
            <FormattedMessage
              id="branch_analysis.freshness.pull_request_name"
              values={{ key: data.newestPullRequest.name }}
            />
          }
          to={getPullRequestUrl(projectKey, data.newestPullRequest.name)}
        />
      )}
    </div>
  );
}
