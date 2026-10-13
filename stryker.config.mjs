import { delimiter, join } from 'node:path';
import process from 'node:process';

import { strykerPreset } from '@haembina/config';

process.env.NODE_PATH = [join(import.meta.dirname, 'sonarqube-webapp', 'node_modules'), process.env.NODE_PATH]
  .filter(Boolean)
  .join(delimiter);

export default {
  ...strykerPreset({
    runner: {
      testRunner: 'jest',
      jest: {
        configFile: 'jest.config.cjs',
        enableFindRelatedTests: true,
      },
    },
    mutate: ['sonarqube-webapp-addons/src/**/*.{ts,tsx}', '!sonarqube-webapp-addons/src/**/*.test.{ts,tsx}'],
  }),
  inPlace: true,
  incrementalFile: 'reports/stryker-incremental.json',
  timeoutMS: 120_000,
};
