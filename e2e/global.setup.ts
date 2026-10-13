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

import { randomBytes } from 'node:crypto';
import { request, type APIResponse, type FullConfig } from '@playwright/test';
import { PROJECT } from './sonarqube.helper';

export default async function globalSetup(config: FullConfig): Promise<void> {
  const { baseURL, storageState } = config.projects[0].use;
  const admin = await request.newContext({
    baseURL,
    httpCredentials: {
      username: 'admin',
      password: process.env.SONARQUBE_ADMIN_PASSWORD ?? 'admin',
      send: 'always',
    },
  });

  const login = `e2e-${randomBytes(4).toString('hex')}`;
  const password = `E2e-${randomBytes(12).toString('hex')}!`;

  await expectOk(admin.post('/api/users/create', { form: { login, name: login, password } }));
  await expectOk(
    admin.post('/api/permissions/add_user', {
      form: { login, permission: 'admin', projectKey: PROJECT },
    }),
  );
  await admin.dispose();

  const account = await request.newContext({
    baseURL,
    httpCredentials: { username: login, password, send: 'always' },
  });
  await expectOk(account.post('/api/users/dismiss_notice', { form: { notice: 'sonarlintAd' } }));
  await account.dispose();

  const user = await request.newContext({ baseURL });
  await expectOk(user.post('/api/authentication/login', { form: { login, password } }));
  await user.storageState({ path: storageState as string });
  await user.dispose();
}

async function expectOk(pending: Promise<APIResponse>): Promise<void> {
  const response = await pending;
  if (!response.ok()) {
    throw new Error(`${response.url()} answered ${response.status()}: ${await response.text()}`);
  }
}
