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
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { PRMessage } from './PRMessage';

it('uses the generic terms when the project is bound to no platform', () => {
  renderComponent(
    <span>
      <PRMessage almKey={undefined} message="Open {pull_requests}, one {pull_request} at a time" />
    </span>,
  );

  expect(
    screen.getByText('Open pull_requests, one pull_request at a time'),
  ).toBeInTheDocument();
});

it("uses the platform's own terms, and hands the text to a render function", () => {
  renderComponent(
    <PRMessage almKey="gitlab" message="{PULL_REQUESTS}">
      {(text) => <strong>{text}</strong>}
    </PRMessage>,
  );

  expect(screen.getByText('pull_requests.gitlab')).toContainHTML('strong');
});
