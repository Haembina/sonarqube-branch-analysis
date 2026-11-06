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

import { SearchInput } from '@sonarsource/echoes-react';
import { screen } from '@testing-library/react';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { SearchInputTarget } from './SearchInputTarget';

function rulesOver(element: Element) {
  const selectors = [...element.classList].map((name) => `.${name} button`);
  return [...document.styleSheets]
    .flatMap((sheet) => [...sheet.cssRules])
    .filter((rule): rule is CSSStyleRule => rule instanceof CSSStyleRule)
    .filter((rule) => selectors.includes(rule.selectorText));
}

it('gives the clear button of a search box a 24px target', () => {
  renderComponent(
    <SearchInputTarget data-testid="target">
      <SearchInput ariaLabel="Search" onChange={jest.fn()} value="stale" />
    </SearchInputTarget>,
  );

  const target = screen.getByTestId('target');
  expect(target).toContainElement(screen.getByRole('button'));
  const [rule] = rulesOver(target);
  expect(rule.style.getPropertyValue('--button-height')).toBe('var(--echoes-dimension-height-600)');
  expect(rule.style.getPropertyValue('--button-width')).toBe('var(--echoes-dimension-width-300)');
});
