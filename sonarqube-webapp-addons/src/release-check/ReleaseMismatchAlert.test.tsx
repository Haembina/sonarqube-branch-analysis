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

import { act, screen, waitFor } from '@testing-library/react';
import { getInstalledPlugins } from '~sq-server-commons/api/plugins';
import { mockInstalledPlugin } from '~sq-server-commons/helpers/mocks/plugins';
import { mockCurrentUser, mockLoggedInUser } from '~sq-server-commons/helpers/testMocks';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { CurrentUser } from '~sq-server-commons/types/users';
import ReleaseMismatchAlert, { PLUGIN_KEY } from './ReleaseMismatchAlert';

jest.mock('~sq-server-commons/api/plugins');

beforeEach(() => {
  jest.mocked(getInstalledPlugins).mockReset();
});

const WEBAPP_RELEASE = 'test-release';

const OTHER_RELEASE = '1.0.0';

const MISMATCH = `branch_analysis.release_mismatch.${OTHER_RELEASE}.${WEBAPP_RELEASE}`;

function answerPlugins(...versions: Array<{ key: string; version: string }>) {
  jest.mocked(getInstalledPlugins).mockResolvedValue(versions.map((plugin) => mockInstalledPlugin(plugin)));
}

function renderAlert(isGlobalBanner?: boolean, currentUser: CurrentUser = mockLoggedInUser()) {
  renderComponent(<ReleaseMismatchAlert isGlobalBanner={isGlobalBanner} />, '/', { currentUser });
}

describe('capability release-mismatch: say when the plugin jar and the webapp come from different releases', () => {
  it('names both releases in a global banner when they differ', async () => {
    answerPlugins({ key: 'other', version: WEBAPP_RELEASE }, { key: PLUGIN_KEY, version: OTHER_RELEASE });
    renderAlert(true);

    expect(await screen.findByText(MISMATCH)).toBeInTheDocument();
  });

  it('names both releases on a page of its own when they differ', async () => {
    answerPlugins({ key: PLUGIN_KEY, version: OTHER_RELEASE });
    renderAlert(false);

    expect(await screen.findByText(MISMATCH)).toBeInTheDocument();
  });

  it.each([
    ['both come from one release', { key: PLUGIN_KEY, version: WEBAPP_RELEASE }],
    ['the plugin is not installed', { key: 'other', version: OTHER_RELEASE }],
  ])('says nothing when %s', async (_, plugin) => {
    answerPlugins(plugin);
    renderAlert(true);

    await waitFor(() => expect(getInstalledPlugins).toHaveBeenCalled());
    await act(async () => {
      await jest.mocked(getInstalledPlugins).mock.results[0].value;
    });
    expect(screen.queryByText(/branch_analysis\.release_mismatch/)).not.toBeInTheDocument();
  });

  it('asks nothing for an anonymous visitor, who may not read the plugin list', () => {
    renderAlert(true, mockCurrentUser());

    expect(getInstalledPlugins).not.toHaveBeenCalled();
  });
});
