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

export const FRESHNESS_URL = '/api/analysis_freshness/list';
export const MAX_PROJECTS_PER_REQUEST = 100;

export interface Analysis {
  analysisDate?: string;
  name: string;
  revision?: string;
}

export type MainState = 'CURRENT' | 'MOVED' | 'UNKNOWN';

export interface ProjectFreshness {
  key: string;
  main?: Analysis;
  mainState: MainState;
  newestBranch?: Analysis;
  newestPullRequest?: Analysis;
}

interface Waiting {
  reject: (error: unknown) => void;
  resolve: (freshness: ProjectFreshness | null) => void;
}

let pending = new Map<string, Waiting[]>();

async function request(keys: string[]): Promise<Map<string, ProjectFreshness>> {
  const { projects } = await getJSON<{ projects: ProjectFreshness[] }>(FRESHNESS_URL, {
    projects: keys.join(','),
  });
  return new Map(projects.map((project) => [project.key, project]));
}

async function flush(): Promise<void> {
  const batch = pending;
  pending = new Map();
  const keys = [...batch.keys()];
  const slices = Array.from({ length: Math.ceil(keys.length / MAX_PROJECTS_PER_REQUEST) }, (_, index) =>
    keys.slice(index * MAX_PROJECTS_PER_REQUEST, (index + 1) * MAX_PROJECTS_PER_REQUEST),
  );
  await Promise.all(
    slices.map(async (slice) => {
      try {
        const answers = await request(slice);
        slice.forEach((key) => batch.get(key)?.forEach(({ resolve }) => resolve(answers.get(key) ?? null)));
      } catch (error) {
        slice.forEach((key) => batch.get(key)?.forEach(({ reject }) => reject(error)));
      }
    }),
  );
}

export function loadProjectFreshness(key: string): Promise<ProjectFreshness | null> {
  return new Promise((resolve, reject) => {
    if (pending.size === 0) {
      queueMicrotask(() => {
        void flush();
      });
    }
    pending.set(key, [...(pending.get(key) ?? []), { reject, resolve }]);
  });
}
