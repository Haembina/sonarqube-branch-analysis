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

import { getBranchLikeActionsId } from './BranchLikeRow';

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
