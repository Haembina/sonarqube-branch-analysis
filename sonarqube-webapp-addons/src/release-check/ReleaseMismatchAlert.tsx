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

import { Layout, MessageCallout } from '@sonarsource/echoes-react';
import { useQuery } from '@tanstack/react-query';
import { FormattedMessage } from 'react-intl';
import { StaleTime } from '~shared/queries/common';
import { getInstalledPlugins } from '~sq-server-commons/api/plugins';
import { useCurrentUser } from '~sq-server-commons/context/current-user/CurrentUserContext';
import { isLoggedIn } from '~sq-server-commons/types/users';

declare const BRANCH_ANALYSIS_RELEASE: string;

export const PLUGIN_KEY = 'haembinabranchanalysis';

async function installedPluginVersion(): Promise<string | null> {
  const plugins = await getInstalledPlugins();
  return plugins.find(({ key }) => key === PLUGIN_KEY)?.version ?? null;
}

export interface ReleaseMismatchAlertProps {
  isGlobalBanner?: boolean;
}

export default function ReleaseMismatchAlert({ isGlobalBanner }: Readonly<ReleaseMismatchAlertProps>) {
  const { currentUser } = useCurrentUser();
  const { data: pluginRelease } = useQuery({
    queryKey: ['branch-analysis', 'plugin-release'],
    queryFn: installedPluginVersion,
    enabled: isLoggedIn(currentUser),
    staleTime: StaleTime.NEVER,
  });

  if (!pluginRelease || pluginRelease === BRANCH_ANALYSIS_RELEASE) {
    return null;
  }

  const message = (
    <FormattedMessage
      id="branch_analysis.release_mismatch"
      values={{ plugin: pluginRelease, webapp: BRANCH_ANALYSIS_RELEASE }}
    />
  );

  return isGlobalBanner ? (
    <Layout.Banner variety="warning">{message}</Layout.Banner>
  ) : (
    <MessageCallout variety="warning">{message}</MessageCallout>
  );
}
