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

import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import type { APIRequestContext } from '@playwright/test';

const ADDON_SOURCES = 'sonarqube-webapp-addons/src';

const SOURCE = /\.tsx?$/u;

const MESSAGE_KEY = /(?:\bid\s*[:=]\s*\{?|\btranslate\(\s*)['"]([\w.-]+)['"]/gu;

function sourcesUnder(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name);

    if (entry.isDirectory()) return sourcesUnder(path);

    return SOURCE.test(entry.name) && !entry.name.includes('.test.') ? [path] : [];
  });
}

export function addonMessageKeys(root = ADDON_SOURCES): Set<string> {
  return new Set(sourcesUnder(root).flatMap((path) => [...readFileSync(path, 'utf8').matchAll(MESSAGE_KEY)].map(([, key]) => key)));
}

export async function addonLanguageTables(request: APIRequestContext): Promise<Record<string, Record<string, string>>> {
  const response = await request.get('/api/l10n/index?locale=en');
  const { messages } = (await response.json()) as { messages: Record<string, string> };
  const keys = addonMessageKeys();

  return { en: Object.fromEntries(Object.entries(messages).filter(([key]) => keys.has(key))) };
}
