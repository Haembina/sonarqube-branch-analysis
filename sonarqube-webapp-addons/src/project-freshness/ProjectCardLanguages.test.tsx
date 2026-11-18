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

import { act, fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderComponent } from '~sq-server-commons/helpers/testReactTestingUtils';
import { ProjectCardLanguages } from './ProjectCardLanguages';

const LANGUAGES = {
  css: { key: 'css', name: 'CSS' },
  docker: { key: 'docker', name: 'Docker' },
  js: { key: 'js', name: 'JavaScript' },
  ts: { key: 'ts', name: 'TypeScript' },
};

const DISTRIBUTION = 'css=900;docker=40;js=2400;ts=500;py=10;<null>=3';

const LINE_WIDTH = 120;

let textWidth = LINE_WIDTH;

beforeEach(() => {
  textWidth = LINE_WIDTH;
  jest.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockImplementation(() => LINE_WIDTH);
  jest.spyOn(HTMLElement.prototype, 'scrollWidth', 'get').mockImplementation(() => textWidth);
});

afterEach(() => {
  jest.restoreAllMocks();
});

function renderLanguages(distribution?: string) {
  return renderComponent(<ProjectCardLanguages distribution={distribution} languages={LANGUAGES} />);
}

describe('capability project-card-languages: read every language a project holds on its card', () => {
  it('lists every language by lines of code, naming one the server does not know by its key', () => {
    renderLanguages(DISTRIBUTION);

    expect(screen.getByText('JavaScript, CSS, TypeScript, Docker, py, unknown')).toBeInTheDocument();
  });

  it('offers no tooltip while the whole line fits', async () => {
    const user = userEvent.setup();
    renderLanguages(DISTRIBUTION);

    await user.hover(screen.getByText(/^JavaScript, CSS/));

    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  it('offers the full list in a tooltip when the line is cut', async () => {
    const user = userEvent.setup();
    textWidth = LINE_WIDTH * 2;
    renderLanguages(DISTRIBUTION);

    await user.hover(screen.getByText(/^JavaScript, CSS/));

    expect(await screen.findByRole('tooltip')).toHaveTextContent('JavaScriptCSSTypeScriptDockerpyunknown');
  });

  it('measures the line again when the window is resized', async () => {
    const user = userEvent.setup();
    renderLanguages(DISTRIBUTION);

    textWidth = LINE_WIDTH * 2;
    act(() => {
      fireEvent(window, new Event('resize'));
    });
    await user.hover(screen.getByText(/^JavaScript, CSS/));

    expect(await screen.findByRole('tooltip')).toBeInTheDocument();
  });

  it('draws nothing for a project with no language distribution', () => {
    const { container } = renderLanguages();

    expect(container).toBeEmptyDOMElement();
  });
});
