import js from '@eslint/js';
import json from '@eslint/json';
import sonarjs from 'eslint-plugin-sonarjs';
import yml from 'eslint-plugin-yml';
import tseslint from 'typescript-eslint';
import { basePreset, jsonPreset, typescriptPreset, yamlPreset } from '@haembina/config';

const ADDONS = ['sonarqube-webapp-addons/src/**/*.{ts,tsx}'];

export default [
  ...basePreset({
    js,
    sonarjs,
    files: ['**/*.mjs', ...ADDONS],
    ignores: ['build/**', 'sonarqube-lib/**', 'sonarqube-webapp/**', '.scannerwork/**', '.claude/**'],
  }),
  ...typescriptPreset({ tseslint, files: ADDONS }),
  ...jsonPreset({ json }),
  ...yamlPreset({ yml }),
];
