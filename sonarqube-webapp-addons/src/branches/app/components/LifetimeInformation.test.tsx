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

import { screen, waitFor } from '@testing-library/react';
import SettingsServiceMock from '~sq-server-commons/api/mocks/SettingsServiceMock';
import { getValue } from '~sq-server-commons/api/settings';
import { mockAppState } from '~sq-server-commons/helpers/testMocks';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { SettingsKey } from '~sq-server-commons/types/settings';
import LifetimeInformation from './LifetimeInformation';

jest.mock('~sq-server-commons/api/settings');

const settingsHandler = new SettingsServiceMock();

beforeEach(() => {
  settingsHandler.reset();
});

it('tells a user who cannot administer how long branches live, with no settings link', async () => {
  settingsHandler.set(SettingsKey.DaysBeforeDeletingInactiveBranchesAndPRs, '14');
  renderComponent(<LifetimeInformation />, '/', { appState: mockAppState({ canAdmin: false }) });

  expect(
    await screen.findByText(/project_branch_pull_request\.lifetime_information\.14/),
  ).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'settings.page' })).not.toBeInTheDocument();
});

it('shows nothing when the setting cannot be read', async () => {
  jest.mocked(getValue).mockRejectedValueOnce(new Error('forbidden'));
  const { container } = renderComponent(<LifetimeInformation />);

  await waitFor(() => {
    expect(getValue).toHaveBeenCalled();
  });
  expect(container).not.toHaveTextContent('project_branch_pull_request.lifetime_information');
});

it('ignores an answer that arrives after the page is gone', async () => {
  let answer: ((value: { key: string; value: string }) => void) | undefined;
  jest.mocked(getValue).mockReturnValueOnce(
    new Promise((resolve) => {
      answer = resolve;
    }),
  );
  const { unmount } = renderComponent(<LifetimeInformation />);

  unmount();
  answer?.({ key: SettingsKey.DaysBeforeDeletingInactiveBranchesAndPRs, value: '30' });

  await waitFor(() => {
    expect(getValue).toHaveBeenCalled();
  });
  expect(document.body).not.toHaveTextContent('project_branch_pull_request.lifetime_information');
});

it('ignores a failure that arrives after the page is gone', async () => {
  let fail: ((reason: Error) => void) | undefined;
  jest.mocked(getValue).mockReturnValueOnce(
    new Promise((_, reject) => {
      fail = reject;
    }),
  );
  const { unmount } = renderComponent(<LifetimeInformation />);

  unmount();
  fail?.(new Error('forbidden'));

  await waitFor(() => {
    expect(getValue).toHaveBeenCalled();
  });
  expect(document.body.textContent).toBe('');
});
