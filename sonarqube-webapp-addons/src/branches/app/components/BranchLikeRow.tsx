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

import {
  ButtonIcon,
  ButtonVariety,
  DropdownMenu,
  IconMoreVertical,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { ActionCell, Badge, ContentCell, TableRowInteractive } from '~design-system';
import BranchLikeIcon from '~sq-server-commons/components/icon-mappers/BranchLikeIcon';
import DateFromNow from '~shared/components/intl/DateFromNow';
import QualityGateStatus from '~sq-server-commons/components/nav/QualityGateStatus';
import { getBranchLikeDisplayName } from '~sq-server-commons/helpers/branch-like';
import { translate } from '~sq-server-commons/helpers/l10n';
import {
  isBranch,
  isMainBranch,
  isPullRequest,
} from '~shared/helpers/branch-like';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import { Component } from '~sq-server-commons/types/types';
import BranchPurgeSetting from './BranchPurgeSetting';

/**
 * The id of a row's actions menu, from its branch or pull request display name. Echoes names
 * the menu's trigger button with this id and a `-trigger` suffix.
 */
export function getBranchLikeActionsId(displayName: string): string {
  return `branch-settings-action-${displayName}`;
}

/** Props for {@link BranchLikeRow}; the three callbacks open the matching action dialog. */
export interface BranchLikeRowProps {
  branchLike: BranchLike;
  component: Component;
  displayPurgeSetting?: boolean;
  onDelete: () => void;
  onRename: () => void;
  onSetAsMain: () => void;
}

/**
 * One table row for a branch or pull request: name, quality gate status, last analysis date,
 * the keep-when-inactive switch (branches only, when `displayPurgeSetting` is set) and an
 * actions menu. The main branch offers rename only; every other entry offers delete.
 */
function BranchLikeRow(props: BranchLikeRowProps) {
  const { branchLike, component, displayPurgeSetting } = props;
  const intl = useIntl();
  const branchLikeDisplayName = getBranchLikeDisplayName(branchLike);

  return (
    <TableRowInteractive>
      <ContentCell>
        <BranchLikeIcon branchLike={branchLike} className="sw-mr-1" />
        <span title={branchLikeDisplayName}>{branchLikeDisplayName}</span>
        <span>
          {isMainBranch(branchLike) && (
            <Badge className="sw-ml-2">{translate('branches.main_branch')}</Badge>
          )}
        </span>
      </ContentCell>
      <ContentCell>
        <QualityGateStatus
          branchLike={branchLike}
          className="sw-flex sw-items-center sw-w-24"
          showStatusText
        />
      </ContentCell>
      <ContentCell>{<DateFromNow date={branchLike.analysisDate} />}</ContentCell>
      {displayPurgeSetting && isBranch(branchLike) && (
        <ContentCell>
          <BranchPurgeSetting branch={branchLike} component={component} />
        </ContentCell>
      )}
      <ActionCell>
        <DropdownMenu
          id={getBranchLikeActionsId(branchLikeDisplayName)}
          items={
            <>
              {isBranch(branchLike) && !isMainBranch(branchLike) && (
                <DropdownMenu.ItemButton onClick={props.onSetAsMain}>
                  {translate('project_branch_pull_request.branch.set_main')}
                </DropdownMenu.ItemButton>
              )}

              {isMainBranch(branchLike) ? (
                <DropdownMenu.ItemButton onClick={props.onRename}>
                  {translate('project_branch_pull_request.branch.rename')}
                </DropdownMenu.ItemButton>
              ) : (
                <DropdownMenu.ItemButtonDestructive onClick={props.onDelete}>
                  {translate(
                    isPullRequest(branchLike)
                      ? 'project_branch_pull_request.pull_request.delete'
                      : 'project_branch_pull_request.branch.delete',
                  )}
                </DropdownMenu.ItemButtonDestructive>
              )}
            </>
          }
        >
          <ButtonIcon
            Icon={IconMoreVertical}
            ariaLabel={intl.formatMessage(
              { id: 'project_branch_pull_request.branch.actions_label' },
              { branchName: branchLikeDisplayName },
            )}
            variety={ButtonVariety.Default}
          />
        </DropdownMenu>
      </ActionCell>
    </TableRowInteractive>
  );
}

export default React.memo(BranchLikeRow);
