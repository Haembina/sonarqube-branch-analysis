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

const path = require('node:path');

const webapp = path.join(__dirname, 'sonarqube-webapp');
const addons = path.join(__dirname, 'sonarqube-webapp-addons', 'src');
const common = require('./sonarqube-webapp/apps/sq-server/jest.config.common.js');

const FLOOR = 100;

module.exports = {
  ...common,
  rootDir: webapp,
  roots: [addons, path.join(webapp, 'apps', 'sq-server', '__mocks__')],
  modulePaths: [path.join(webapp, 'node_modules')],
  moduleNameMapper: {
    ...common.moduleNameMapper,
    '~sq-server-addons/(.+)': `${addons}/$1`,
  },
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
    global: { branches: FLOOR, functions: FLOOR, lines: FLOOR, statements: FLOOR },
  },
  maxWorkers: '50%',
  reporters: ['default'],
};
