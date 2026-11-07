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

import { Checkbox } from '@sonarsource/echoes-react';
import * as React from 'react';
import { ActionCell, ContentCell, HelperHintIcon, Table, TableRow } from '~design-system';
import { isMainBranch } from '~shared/helpers/branch-like';
import Tooltip from '~sq-server-commons/components/controls/Tooltip';
import { getBranchLikeKey } from '~sq-server-commons/helpers/branch-like';
import { translate } from '~sq-server-commons/helpers/l10n';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import { Component } from '~sq-server-commons/types/types';
import BranchLikeRow from './BranchLikeRow';

const COLUMN_WIDTHS_WITH_PURGE_SETTING = ['3%', 'auto', '10%', '15%', '15%', '5%'];
const COLUMN_WIDTHS_WITHOUT_PURGE_SETTING = ['3%', 'auto', '10%', '15%', '5%'];

export interface BranchLikeTableProps {
  branchLikes: BranchLike[];
  component: Component;
  displayPurgeSetting?: boolean;
  emptyText?: string;
  onDelete: (branchLike: BranchLike) => void;
  onRename: (branchLike: BranchLike) => void;
  onSelect: (branchLikes: BranchLike[], selected: boolean) => void;
  onSetAsMain: (branchLike: BranchLike) => void;
  selectedKeys: ReadonlySet<string>;
  title: string;
}

function selectAllState(deletable: BranchLike[], selectedKeys: ReadonlySet<string>) {
  const selected = deletable.filter((branchLike) => selectedKeys.has(getBranchLikeKey(branchLike)));
  if (selected.length === 0) {
    return false;
  }
  return selected.length === deletable.length ? true : 'indeterminate';
}

export function BranchLikeTable(props: BranchLikeTableProps) {
  const { branchLikes, component, displayPurgeSetting, emptyText, selectedKeys, title } = props;
  const deletable = branchLikes.filter((branchLike) => !isMainBranch(branchLike));
  const allSelected = selectAllState(deletable, selectedKeys);
  const columnWidths = displayPurgeSetting
    ? COLUMN_WIDTHS_WITH_PURGE_SETTING
    : COLUMN_WIDTHS_WITHOUT_PURGE_SETTING;

  const header = (
    <TableRow>
      <ContentCell>
        {deletable.length > 0 && (
          <Checkbox
            ariaLabel={translate(allSelected === true ? 'uncheck_all' : 'check_all')}
            checked={allSelected}
            onCheck={() => props.onSelect(deletable, allSelected !== true)}
          />
        )}
      </ContentCell>
      <ContentCell>{title}</ContentCell>
      <ContentCell>{translate('status')}</ContentCell>
      <ContentCell>{translate('project_branch_pull_request.last_analysis_date')}</ContentCell>
      {displayPurgeSetting && (
        <ContentCell>
          <div className="sw-flex sw-items-center">
            <span>
              {translate('project_branch_pull_request.branch.auto_deletion.keep_when_inactive')}
            </span>
            <span className="sw-ml-1">
              <Tooltip
                content={translate(
                  'project_branch_pull_request.branch.auto_deletion.keep_when_inactive.tooltip',
                )}
              >
                <HelperHintIcon />
              </Tooltip>
            </span>
          </div>
        </ContentCell>
      )}

      <ActionCell>{translate('actions')}</ActionCell>
    </TableRow>
  );

  return (
    <div className="sw-mt-6">
      <Table columnCount={columnWidths.length} columnWidths={columnWidths} header={header}>
        {branchLikes.map((branchLike) => (
          <BranchLikeRow
            branchLike={branchLike}
            component={component}
            displayPurgeSetting={displayPurgeSetting}
            isSelected={selectedKeys.has(getBranchLikeKey(branchLike))}
            key={getBranchLikeKey(branchLike)}
            onDelete={() => props.onDelete(branchLike)}
            onRename={() => props.onRename(branchLike)}
            onSelect={(selected) => props.onSelect([branchLike], selected)}
            onSetAsMain={() => props.onSetAsMain(branchLike)}
          />
        ))}
      </Table>
      {branchLikes.length === 0 && emptyText !== undefined && (
        <p className="sw-mt-4">{emptyText}</p>
      )}
    </div>
  );
}

export default React.memo(BranchLikeTable);
