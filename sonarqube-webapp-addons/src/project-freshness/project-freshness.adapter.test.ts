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

import { getJSON } from '~adapters/helpers/request';
import {
  FRESHNESS_URL,
  loadProjectFreshness,
  MAX_PROJECTS_PER_REQUEST,
  ProjectFreshness,
} from './project-freshness.adapter';

jest.mock('~adapters/helpers/request');

function freshness(key: string): ProjectFreshness {
  return { key, mainState: 'CURRENT' };
}

function answerEveryKey() {
  jest.mocked(getJSON).mockImplementation((_url, data) =>
    Promise.resolve({ projects: String(data?.projects).split(',').map(freshness) }),
  );
}

beforeEach(() => {
  jest.mocked(getJSON).mockReset();
});

it('asks once for every card drawn in the same pass', async () => {
  answerEveryKey();

  const answers = await Promise.all([loadProjectFreshness('one'), loadProjectFreshness('two'), loadProjectFreshness('one')]);

  expect(answers).toEqual([freshness('one'), freshness('two'), freshness('one')]);
  expect(getJSON).toHaveBeenCalledTimes(1);
  expect(getJSON).toHaveBeenCalledWith(FRESHNESS_URL, { projects: 'one,two' });
});

it('splits a page of more projects than one request takes', async () => {
  answerEveryKey();
  const keys = Array.from({ length: MAX_PROJECTS_PER_REQUEST + 1 }, (_, index) => `project-${index}`);

  const answers = await Promise.all(keys.map(loadProjectFreshness));

  expect(answers).toEqual(keys.map(freshness));
  expect(getJSON).toHaveBeenCalledTimes(2);
});

it('answers nothing for a project the server leaves out', async () => {
  jest.mocked(getJSON).mockResolvedValue({ projects: [] });

  await expect(loadProjectFreshness('hidden')).resolves.toBeNull();
});

it('fails every card of a request the server refuses', async () => {
  jest.mocked(getJSON).mockRejectedValue(new Error('refused'));

  const answers = await Promise.allSettled([loadProjectFreshness('one'), loadProjectFreshness('two')]);

  expect(answers.map(({ status }) => status)).toEqual(['rejected', 'rejected']);
});
