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

import { renderHook, waitFor } from '@testing-library/react';
import { getContextWrapper } from '~adapters/helpers/test-utils';
import { EntitlementCheckFeatureKey } from '~shared/types/billing';
import {
  useEntitlementCheckQuery,
  useEntitlementChecksQuery,
  usePurchasableFeature,
} from './entitlements';

const wrapper = getContextWrapper();

const notEntitled = (featureKey: EntitlementCheckFeatureKey) => ({
  consumption: null,
  entitled: false,
  excludedValues: [],
  featureKey,
  value: null,
});

it('answers one feature as not entitled, without asking a server', async () => {
  const { result } = renderHook(
    () => useEntitlementCheckQuery({ featureKey: EntitlementCheckFeatureKey.LinesOfCode }),
    { wrapper },
  );

  await waitFor(() => {
    expect(result.current.data).toEqual(notEntitled(EntitlementCheckFeatureKey.LinesOfCode));
  });
});

it('answers several features at once, skipping empty keys', async () => {
  const { result } = renderHook(
    () =>
      useEntitlementChecksQuery([
        EntitlementCheckFeatureKey.LinesOfCode,
        '' as EntitlementCheckFeatureKey,
      ]),
    { wrapper },
  );

  await waitFor(() => {
    expect(result.current.isPending).toBe(false);
  });
  expect(result.current.data).toEqual([notEntitled(EntitlementCheckFeatureKey.LinesOfCode)]);
});

it('finds no purchasable feature', async () => {
  const { result } = renderHook(() => usePurchasableFeature('branches'), { wrapper });

  await waitFor(() => {
    expect(result.current.isSuccess).toBe(true);
  });
  expect(result.current.data).toBeUndefined();
});
