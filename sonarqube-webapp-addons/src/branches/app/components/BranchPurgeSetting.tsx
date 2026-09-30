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

import styled from '@emotion/styled';
import { cssVar, Spinner } from '@sonarsource/echoes-react';
import { HelperHintIcon } from '~design-system';
import { Switch } from '~adapters/components/common/Switch';
import Tooltip from '~sq-server-commons/components/controls/Tooltip';
import { translate } from '~sq-server-commons/helpers/l10n';
import { useExcludeFromPurgeMutation } from '~sq-server-commons/queries/branch';
import { isMainBranch } from '~shared/helpers/branch-like';
import { Branch } from '~sq-server-commons/types/branch-like';
import { Component } from '~sq-server-commons/types/types';

interface Props {
  branch: Branch;
  component: Component;
}

/**
 * Switch that excludes a branch from automatic purge of inactive branches, saved at once via
 * `useExcludeFromPurgeMutation`. Disabled while saving and for the main branch, which is
 * never purged; a tooltip says why.
 */
export default function BranchPurgeSetting(props: Props) {
  const { branch, component } = props;
  const { mutate: excludeFromPurge, isPending } = useExcludeFromPurgeMutation();

  const handleOnChange = (exclude: boolean) => {
    excludeFromPurge({ component, key: branch.name, exclude });
  };

  const isTheMainBranch = isMainBranch(branch);
  const disabled = isTheMainBranch || isPending;
  // The column header names the setting; the switch's own name adds which branch it keeps.
  const keepWhenInactive = translate(
    'project_branch_pull_request.branch.auto_deletion.keep_when_inactive',
  );

  return (
    <>
      <SwitchBoundary>
        <Switch
          ariaLabel={`${keepWhenInactive}: ${branch.name}`}
          disabled={disabled}
          name={branch.name}
          onChange={handleOnChange}
          value={branch.excludedFromPurge}
        />
      </SwitchBoundary>
      <Spinner isLoading={isPending} className="sw-ml-1" />
      {isTheMainBranch && (
        <span className="sw-ml-1">
          <Tooltip
            content={translate(
              'project_branch_pull_request.branch.auto_deletion.main_branch_tooltip',
            )}
          >
            <HelperHintIcon aria-label={translate('help')} />
          </Tooltip>
        </span>
      )}
    </>
  );
}

// The switch draws its off track in the weak border color, 1.55:1 on the page; an inset edge in
// the bolder border color lets the off state meet WCAG 1.4.11's 3:1. A disabled switch is exempt.
const SwitchBoundary = styled.span`
  display: inline-flex;

  & [role='switch'][aria-checked='false']:not(:disabled) {
    box-shadow: inset 0 0 0 1px ${cssVar('color-border-bolder')};
  }
`;
