/**
 * How the addons' TypeScript measures whether its suites assert what they execute.
 *
 * Stryker is not a dependency: `npm run mutation` installs it for the run and the next
 * `npm ci` prunes it. Jest lives in the sonarqube-webapp workspace rather than here, so the run
 * needs `yarn install` in the submodule and `sonarqube-webapp-addons/setup.sh` first, as
 * `npm run test:addons` does.
 *
 * **Never `npx stryker`**: the bare name resolves to an abandoned registry package. Inside an npm
 * script the local binary is already on PATH.
 */
import { delimiter, join } from 'node:path';
import process from 'node:process';

import { strykerPreset } from '@haembina/config';

// The jest runner resolves `jest` from the working directory, and this checkout's own
// node_modules holds none. Stryker's test runners are child processes started after this file
// is read, so they inherit the workspace's node_modules as a module path.
process.env.NODE_PATH = [join(import.meta.dirname, 'sonarqube-webapp', 'node_modules'), process.env.NODE_PATH]
  .filter(Boolean)
  .join(delimiter);

/**
 * The shared settings (per-test coverage, incremental runs, eight workers, the reporters) are
 * `strykerPreset`'s; the preset names no Jest runner, so the runner is spelled out here.
 */
export default {
  ...strykerPreset({
    runner: {
      testRunner: 'jest',
      jest: {
        configFile: 'jest.config.cjs',
        // Only the suites importing a mutated file rerun.
        enableFindRelatedTests: true,
      },
    },
    mutate: ['sonarqube-webapp-addons/src/**/*.{ts,tsx}', '!sonarqube-webapp-addons/src/**/*.test.{ts,tsx}'],
  }),
  // In place rather than in a sandbox: a sandbox leaves out every node_modules, and the
  // workspace's is where Jest, Babel and jsdom live. Stryker restores every file when the run
  // ends, interrupted or not.
  inPlace: true,
  incrementalFile: 'reports/stryker-incremental.json',
  // A timeout counts as killed, so a tight one inflates the score. The jsdom suites take tens of
  // seconds each on a cold transform cache, past the preset's minute.
  timeoutMS: 120_000,
};
