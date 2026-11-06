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

import { Button, ButtonVariety, SearchInput, SearchInputWidth } from '@sonarsource/echoes-react';
import { translate, translateWithParameters } from '~sq-server-commons/helpers/l10n';

export const BRANCH_LIKE_FILTER_ID = 'branch-like-filter';

export const DELETE_SELECTED_ID = 'branch-like-delete-selected';

export interface BranchLikeToolbarProps {
  filter: string;
  onDeleteSelected: () => void;
  onFilterChange: (filter: string) => void;
  selectedCount: number;
}

export default function BranchLikeToolbar(props: Readonly<BranchLikeToolbarProps>) {
  const { filter, onDeleteSelected, onFilterChange, selectedCount } = props;

  return (
    <div className="sw-mt-6 sw-flex sw-flex-wrap sw-items-center sw-justify-between sw-gap-4">
      <SearchInput
        id={BRANCH_LIKE_FILTER_ID}
        onChange={onFilterChange}
        placeholderLabel={translate('search.search_by_name')}
        value={filter}
        width={SearchInputWidth.Medium}
      />
      <Button
        id={DELETE_SELECTED_ID}
        isDisabled={selectedCount === 0}
        onClick={onDeleteSelected}
        variety={ButtonVariety.DangerOutline}
      >
        {translateWithParameters('branch_analysis.delete_selected', selectedCount)}
      </Button>
    </div>
  );
}
