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
import { mockPullRequest } from '~sq-server-commons/helpers/mocks/branch-like';
import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import PRLink from './PRLink';

/** Where the bound platform lives; the link never reads it. */
const ALM_URL = 'https://alm.example.com';

function renderLink(url: string | undefined, almKey?: string) {
  return renderComponent(
    <PRLink
      component={mockComponent(almKey ? { alm: { key: almKey, url: ALM_URL } } : {})}
      pullRequest={mockPullRequest({ url })}
    />,
  );
}

it('names the platform the project is bound to', () => {
  renderLink('https://git.example.com/pr/1', 'gitlab');

  const link = screen.getByRole('link', { name: /branches\.see_the_pr_on_x/ });
  expect(link).toHaveAttribute('href', 'https://git.example.com/pr/1');
  expect(screen.getByRole('img', { name: 'gitlab' })).toHaveAttribute(
    'src',
    '/images/alm/gitlab.svg',
  );
});

it.each([
  ['https://github.com/o/r/pull/1', 'github'],
  ['https://gitlab.example.com/o/r/-/merge_requests/1', 'gitlab'],
  ['https://bitbucket.example.com/projects/p/repos/r/pull-requests/1', 'bitbucket'],
  ['https://dev.azure.com/o/p/_git/r/pullrequest/1', 'azure'],
  ['https://o.visualstudio.com/p/_git/r/pullrequest/1', 'azure'],
])('guesses the platform of an unbound project from %s', (url, almKey) => {
  renderLink(url);

  expect(screen.getByRole('img', { name: almKey })).toBeInTheDocument();
});

it('links without a platform when the URL names none', () => {
  renderLink('https://review.example.com/1');

  expect(screen.getByRole('link', { name: 'branches.see_the_pr' })).toHaveAttribute(
    'href',
    'https://review.example.com/1',
  );
  expect(screen.queryByRole('img')).not.toBeInTheDocument();
});

it('renders nothing for a pull request without a URL', () => {
  const { container } = renderLink(undefined);

  expect(container).toBeEmptyDOMElement();
});
