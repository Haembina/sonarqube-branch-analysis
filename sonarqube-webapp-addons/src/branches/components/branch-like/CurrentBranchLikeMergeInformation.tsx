/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { PullRequest } from '~shared/types/branch-like';

/** Props for {@link CurrentBranchLikeMergeInformation}. */
export interface CurrentBranchLikeMergeInformationProps {
  pullRequest: PullRequest;
}

/**
 * A line naming the target and source branches of a pull request ("for merge into X from Y"),
 * wrapping inside 400px, or inside what the header row leaves it, rather than cutting a long
 * branch name off, with the full text as its hover title.
 */
export function CurrentBranchLikeMergeInformation({
  pullRequest,
}: Readonly<CurrentBranchLikeMergeInformationProps>) {
  const intl = useIntl();

  return (
    // The header row sets nowrap, and a branch name has no spaces to wrap at, so the line wraps
    // again here and only the names break between any two letters.
    <span
      className="sw-inline-block sw-min-w-0 sw-max-w-[400px] sw-whitespace-normal"
      title={intl.formatMessage(
        { id: 'branch_like_navigation.for_merge_into_x_from_y.title' },
        {
          target: pullRequest.target,
          branch: pullRequest.branch,
        },
      )}
    >
      <FormattedMessage
        id="branch_like_navigation.for_merge_into_x_from_y"
        values={{
          target: <strong className="sw-break-all">{pullRequest.target}</strong>,
          branch: <strong className="sw-break-all">{pullRequest.branch}</strong>,
        }}
      />
    </span>
  );
}

export default React.memo(CurrentBranchLikeMergeInformation);
