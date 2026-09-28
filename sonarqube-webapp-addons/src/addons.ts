/*
 * Copyright (C) 2020-2026 Michael Clarke and the contributors to mc1arke/sonarqube-community-branch-plugin
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
 *
 */
import branches from './branches';
import { installErrorBoundary } from './error-boundary';
import * as entitlements from './feature-license/entitlements';

// The webapp imports this registry once, at startup, so the boundary covers every page it draws.
installErrorBoundary();

const registry = { branches, entitlements };

/**
 * The registry's type: the addons this build ships, and any other addon the webapp asks for,
 * which is absent here and so reads as `undefined`.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any -- the webapp reads addons of other editions, each typed where it is called
export type AddonsType = { [key: string]: any } & typeof registry;

/**
 * The addon registry the SonarQube webapp loads: the branch and pull request UI, and the
 * entitlement hooks, which always answer "not entitled" in this community build.
 */
export const addons: AddonsType = registry;
