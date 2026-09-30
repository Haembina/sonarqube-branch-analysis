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

// The addons' unit and component suites, run by the sonarqube-webapp workspace's own Jest
// setup: its Babel transform, jsdom environment, Testing Library, jest-axe and the
// fail-on-console guard. Run it from this directory with `npm run test:addons`, after
// `yarn install` in the submodule.
//
// rootDir is the workspace, so every `<rootDir>` path SonarSource's config names resolves
// there, and Babel finds the workspace's babel.config.js. The suites are crawled from the
// addons' real path rather than through setup.sh's link, because Jest does not follow a
// symlink, and the process runs from this directory so Istanbul instruments them and the
// lcov names them as `sonarqube-webapp-addons/src/...`.

const path = require('node:path');

const webapp = path.join(__dirname, 'sonarqube-webapp');
const addons = path.join(__dirname, 'sonarqube-webapp-addons', 'src');
const common = require('./sonarqube-webapp/apps/sq-server/jest.config.common.js');

/** Coverage floor, in percent, the run fails under. */
const FLOOR = 100;

/**
 * Branch floor, in percent. The one branch left is the type guard in BranchLikeTabs' set-as-main
 * handler, which the row menu offers only for a branch, never a pull request.
 */
const BRANCH_FLOOR = 99;

module.exports = {
  ...common,
  rootDir: webapp,
  // The workspace's manual mocks of react-intl, lodash and @emotion, which Jest reads from a
  // `__mocks__` directory under a root.
  roots: [addons, path.join(webapp, 'apps', 'sq-server', '__mocks__')],
  // A file under the real addons path looks for packages from there upward, which never
  // reaches the workspace's node_modules.
  modulePaths: [path.join(webapp, 'node_modules')],
  moduleNameMapper: {
    ...common.moduleNameMapper,
    '~sq-server-addons/(.+)': `${addons}/$1`,
  },
  // JestPreprocess.js's Babel options, with Babel's cwd moved to the workspace so its
  // plugins resolve there. Istanbul keeps Jest's cwd, this directory, as its own.
  transform: {
    '^.+\\.(mjs|[jt]sx?)$': [
      'babel-jest',
      {
        cwd: webapp,
        rootMode: 'upward',
        presets: ['@babel/preset-env'],
        plugins: ['babel-plugin-twin', 'babel-plugin-macros'],
      },
    ],
  },
  testRegex: String.raw`\.test\.tsx?$`,
  collectCoverage: true,
  collectCoverageFrom: [
    '../sonarqube-webapp-addons/src/**/*.{ts,tsx}',
    '!../sonarqube-webapp-addons/src/**/*.test.{ts,tsx}',
  ],
  coverageDirectory: path.join(__dirname, 'coverage', 'addons'),
  coverageReporters: [['lcovonly', { projectRoot: __dirname }], 'text-summary'],
  coverageThreshold: {
    global: { branches: BRANCH_FLOOR, functions: FLOOR, lines: FLOOR, statements: FLOOR },
  },
  maxWorkers: '50%',
  reporters: ['default'],
};
