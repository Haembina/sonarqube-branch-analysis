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

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mockBranch, mockMainBranch } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { BranchLike } from '~sq-server-commons/types/branch-like';
import { BranchLikeRow, getBranchLikeActionsId } from './BranchLikeRow';

const DAY = 86_400_000;

const ANALYSED_DAYS_AGO = 3;

function renderRow(branchLike: BranchLike, onSelect = jest.fn()) {
  renderComponent(
    <table>
      <tbody>
        <BranchLikeRow
          branchLike={branchLike}
          component={mockComponent()}
          onDelete={jest.fn()}
          onRename={jest.fn()}
          onSelect={onSelect}
          onSetAsMain={jest.fn()}
        />
      </tbody>
    </table>,
  );
}

describe('BranchLikeRow', () => {
  it('shows how long ago the branch was last analysed', () => {
    renderRow(mockBranch({ analysisDate: new Date(Date.now() - ANALYSED_DAYS_AGO * DAY).toISOString() }));

    expect(screen.getByText(`${ANALYSED_DAYS_AGO} days ago`)).toBeInTheDocument();
  });

  it('offers a branch for selection, and reports the choice', async () => {
    const user = userEvent.setup();
    const onSelect = jest.fn();
    renderRow(mockBranch({ name: 'stale' }), onSelect);

    await user.click(screen.getByRole('checkbox', { name: 'branch_analysis.select.stale' }));

    expect(onSelect).toHaveBeenCalledWith(true);
  });

  it('offers no selection for the main branch', () => {
    renderRow(mockMainBranch({ name: 'main' }));

    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });
});

describe('getBranchLikeActionsId', () => {
  it('gives each branch one id an ARIA reference can name, whatever its name holds', () => {
    const ids = ['main', 'feature/integration', 'feature-integration', '1 – feature/pull-request', 'a.b', 'a_b', 'a_2eb'].map(
      getBranchLikeActionsId,
    );

    expect(ids.every((id) => /^[A-Za-z0-9_-]+$/.test(id))).toBe(true);
    expect(new Set(ids).size).toBe(ids.length);
    expect(ids[0]).toBe('branch-settings-action-main');
    expect(ids[1]).toBe('branch-settings-action-feature_2fintegration');
  });
});
