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

import { mockComponent } from '~sq-server-commons/helpers/mocks/component';
import { AlmType, getComponentAlmKey, isAzure, isBitbucket, sanitizeAlmId } from './alm-integrations';

describe('sanitizeAlmId', () => {
  it.each([
    ['bitbucket', AlmType.Bitbucket],
    ['bitbucketcloud', AlmType.Bitbucket],
    ['azure', 'microsoft'],
    ['microsoft', 'microsoft'],
    ['github', AlmType.GitHub],
    ['gitlab', AlmType.GitLab],
  ])('maps %s to the documentation key %s', (key, expected) => {
    expect(sanitizeAlmId(key)).toBe(expected);
  });
});

describe('isBitbucket and isAzure', () => {
  it('answer false for an absent key', () => {
    expect(isBitbucket()).toBe(false);
    expect(isAzure()).toBe(false);
  });

  it('recognize only their own platform', () => {
    expect(isBitbucket('bitbucketcloud')).toBe(true);
    expect(isBitbucket('github')).toBe(false);
    expect(isAzure('azure')).toBe(true);
    expect(isAzure('gitlab')).toBe(false);
  });
});

describe('getComponentAlmKey', () => {
  it('is undefined for a component with no binding', () => {
    expect(getComponentAlmKey(mockComponent())).toBeUndefined();
  });

  it('sanitizes the key of a bound component', () => {
    const bound = mockComponent({ alm: { key: 'bitbucketcloud', url: 'https://bitbucket.org' } });

    expect(getComponentAlmKey(bound)).toBe(AlmType.Bitbucket);
  });
});
