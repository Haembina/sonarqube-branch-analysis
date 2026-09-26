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
import routes from './app/routes';
import Menu from './components/branch-like/Menu';
import PRLink from './components/branch-like/PRLink';
import BranchListSection from './components/branch-list/BranchListSection';
import PullRequestOverview from './components/pull-requests/PullRequestOverview';
import ProjectBranchSelector from './components/project-branch-selector/ProjectBranchSelector';

const branches = { routes, Menu, PRLink, BranchListSection, PullRequestOverview, ProjectBranchSelector };

export default branches;
