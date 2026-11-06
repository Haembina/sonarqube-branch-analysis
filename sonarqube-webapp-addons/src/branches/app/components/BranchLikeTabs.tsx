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

import { IconGitBranch, IconPullrequest } from '@sonarsource/echoes-react';
import { useEffect, useState } from 'react';
import { ToggleButton, getTabId, getTabPanelId } from '~design-system';
import { PullRequest } from '~shared/types/branch-like';
import {
  getBranchLikeDisplayName,
  getBranchLikeKey,
  sortBranches,
  sortPullRequests,
} from '~sq-server-commons/helpers/branch-like';
import { translate, translateWithParameters } from '~sq-server-commons/helpers/l10n';
import { useProjectBranchesQuery } from '~adapters/queries/branch';
import {
  isBranch,
  isMainBranch,
  isPullRequest,
} from '~shared/helpers/branch-like';
import { Branch, BranchLike } from '~sq-server-commons/types/branch-like';
import { Component } from '~sq-server-commons/types/types';
import { getBranchLikeActionsId } from './BranchLikeRow';
import BranchLikeTable from './BranchLikeTable';
import BranchLikeToolbar, { BRANCH_LIKE_FILTER_ID, DELETE_SELECTED_ID } from './BranchLikeToolbar';
import DeleteBranchesModal from './DeleteBranchesModal';
import DeleteBranchModal from './DeleteBranchModal';
import RenameBranchModal from './RenameBranchModal';
import SetAsMainBranchModal from './SetAsMainBranchModal';

interface Props {
  component: Component;
  fetchComponent: () => Promise<void>;
}

export enum Tabs {
  Branch,
  PullRequest,
}

const TABS = [
  {
    key: Tabs.Branch,
    value: Tabs.Branch,
    label: (
      <>
        <IconGitBranch />
        <span className="sw-ml-2">{translate('project_branch_pull_request.tabs.branches')}</span>
      </>
    ),
  },
  {
    key: Tabs.PullRequest,
    value: Tabs.PullRequest,
    label: (
      <>
        <IconPullrequest />
        <span className="sw-ml-2">
          {translate('project_branch_pull_request.tabs.pull_requests')}
        </span>
      </>
    ),
  },
];

const triggerIdOf = (branchLike: BranchLike) =>
  `${getBranchLikeActionsId(getBranchLikeDisplayName(branchLike))}-trigger`;

const keysOf = (branchLikes: BranchLike[]) => branchLikes.map((branchLike) => getBranchLikeKey(branchLike));

function namedLike(branchLikes: BranchLike[], filter: string): BranchLike[] {
  const query = filter.trim().toLocaleLowerCase();
  return branchLikes.filter((branchLike) =>
    getBranchLikeDisplayName(branchLike).toLocaleLowerCase().includes(query),
  );
}

export default function BranchLikeTabs(props: Props) {
  const { component, fetchComponent } = props;
  const [currentTab, setCurrentTab] = useState<Tabs>(Tabs.Branch);
  const [renaming, setRenaming] = useState<BranchLike>();
  const [settingAsMain, setSettingAsMain] = useState<Branch>();
  const [deleting, setDeleting] = useState<BranchLike>();
  const [deletingSelected, setDeletingSelected] = useState(false);
  const [filter, setFilter] = useState('');
  const [selectedKeys, setSelectedKeys] = useState<ReadonlySet<string>>(new Set());

  const [returnFocusTo, setReturnFocusTo] = useState<string>();

  useEffect(() => {
    if (returnFocusTo !== undefined) {
      document.getElementById(returnFocusTo)?.focus();
      setReturnFocusTo(undefined);
    }
  }, [returnFocusTo]);

  const handleClose = () => {
    const open = renaming ?? deleting ?? settingAsMain;
    setReturnFocusTo(open && triggerIdOf(open));
    setRenaming(undefined);
    setDeleting(undefined);
    setSettingAsMain(undefined);
  };

  const handleSetAsMainBranch = () => {
    handleClose();
    void fetchComponent();
  };

  const handleSetAsMainBranchOption = (branchLike: BranchLike) => {
    if (isBranch(branchLike)) {
      setSettingAsMain(branchLike);
    }
  };

  const handleTabChange = (tab: Tabs) => {
    setCurrentTab(tab);
    setSelectedKeys(new Set());
  };

  const handleSelect = (branchLikes: BranchLike[], selected: boolean) => {
    const keys = keysOf(branchLikes);
    setSelectedKeys(
      new Set(
        selected
          ? [...selectedKeys, ...keys]
          : [...selectedKeys].filter((key) => !keys.includes(key)),
      ),
    );
  };

  const handleDeleteSelectedClose = () => {
    setDeletingSelected(false);
    setReturnFocusTo(DELETE_SELECTED_ID);
  };

  const handleDeleteSelectedDone = (notDeleted: BranchLike[]) => {
    setSelectedKeys(new Set(keysOf(notDeleted)));
    if (notDeleted.length === 0) {
      setDeletingSelected(false);
      setReturnFocusTo(BRANCH_LIKE_FILTER_ID);
    }
  };

  const { data: branchLikes = [] } = useProjectBranchesQuery(component);

  const isBranchMode = currentTab === Tabs.Branch;
  const sorted: BranchLike[] = isBranchMode
    ? sortBranches(branchLikes.filter(isBranch) as Branch[])
    : sortPullRequests(branchLikes.filter(isPullRequest) as PullRequest[]);
  const branchLikesToDisplay = namedLike(sorted, filter);
  const selected = branchLikesToDisplay.filter((branchLike) =>
    selectedKeys.has(getBranchLikeKey(branchLike)),
  );
  const title = translate(
    isBranchMode
      ? 'project_branch_pull_request.table.branch'
      : 'project_branch_pull_request.table.pull_request',
  );

  return (
    <>
      <ToggleButton
        onChange={handleTabChange}
        value={currentTab}
        options={TABS}
        role="tablist"
      />

      <BranchLikeToolbar
        filter={filter}
        onDeleteSelected={() => setDeletingSelected(true)}
        onFilterChange={setFilter}
        selectedCount={selected.length}
      />

      <div role="tabpanel" id={getTabPanelId(currentTab)} aria-labelledby={getTabId(currentTab)}>
        <BranchLikeTable
          branchLikes={branchLikesToDisplay}
          component={component}
          displayPurgeSetting={isBranchMode}
          emptyText={filter === '' ? undefined : translateWithParameters('no_results_for_x', filter)}
          onDelete={setDeleting}
          onRename={setRenaming}
          onSelect={handleSelect}
          onSetAsMain={handleSetAsMainBranchOption}
          selectedKeys={selectedKeys}
          title={title}
        />
      </div>

      {deleting && (
        <DeleteBranchModal branchLike={deleting} component={component} onClose={handleClose} />
      )}

      {deletingSelected && selected.length > 0 && (
        <DeleteBranchesModal
          branchLikes={selected}
          component={component}
          onClose={handleDeleteSelectedClose}
          onDone={handleDeleteSelectedDone}
        />
      )}

      {renaming && isMainBranch(renaming) && (
        <RenameBranchModal branch={renaming} component={component} onClose={handleClose} />
      )}

      {settingAsMain && !isMainBranch(settingAsMain) && (
        <SetAsMainBranchModal
          component={component}
          branch={settingAsMain}
          onClose={handleClose}
          onSetAsMain={handleSetAsMainBranch}
        />
      )}
    </>
  );
}
