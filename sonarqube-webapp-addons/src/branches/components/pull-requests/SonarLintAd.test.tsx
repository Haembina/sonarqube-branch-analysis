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
import { mockCurrentUser, mockLoggedInUser } from '~sq-server-commons/helpers/testMocks';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import SonarLintAd from './SonarLintAd';

beforeEach(() => {
  localStorage.clear();
});

it('shows a signed-in user the promotion on a failed gate, until dismissed for good', async () => {
  const user = userEvent.setup();
  const { unmount } = renderComponent(<SonarLintAd status="ERROR" />, '/', {
    currentUser: mockLoggedInUser(),
  });

  expect(screen.getByRole('heading', { name: 'overview.sonarlint_ad.header' })).toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: /overview\.sonarlint_ad\.learn_more/ }),
  ).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'overview.sonarlint_ad.close_promotion' }));
  expect(screen.queryByText('overview.sonarlint_ad.header')).not.toBeInTheDocument();

  unmount();
  renderComponent(<SonarLintAd status="ERROR" />, '/', { currentUser: mockLoggedInUser() });
  expect(screen.queryByText('overview.sonarlint_ad.header')).not.toBeInTheDocument();
});

it.each([
  ['a passed gate', 'OK' as const, mockLoggedInUser()],
  ['an anonymous user', 'ERROR' as const, mockCurrentUser()],
  ['a user already in connected mode', 'ERROR' as const, mockLoggedInUser({
    usingSonarLintConnectedMode: true,
  })],
])('stays hidden for %s', (_, status, currentUser) => {
  const { container } = renderComponent(<SonarLintAd status={status} />, '/', { currentUser });

  expect(container).toBeEmptyDOMElement();
});
