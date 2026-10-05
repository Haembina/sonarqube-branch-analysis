// ESLint flat config, read by `npm run lint:es`, which `npm run lint` runs.
//
// The estate presets in `@haembina/config` carry the shared half: both
// recommended sets and S6's named numbers on the build scripts and the addons,
// typescript-eslint's set on the addons, and strict JSON and JSONC.
//
// The webapp addons compile only inside SonarSource's `sonarqube-webapp`
// workspace, where `tsconfig.json` and every `~sq-server-commons` import
// resolve. Outside it there is no program to type-check against, so the
// TypeScript rules are the syntactic set: typed rules would fail on every
// unresolved import rather than report anything about the code.
import js from '@eslint/js';
import json from '@eslint/json';
import sonarjs from 'eslint-plugin-sonarjs';
import tseslint from 'typescript-eslint';
import { basePreset, jsonPreset, typescriptPreset } from '@haembina/config';

/** The addons' TypeScript, the only TypeScript this repository ships. */
const ADDONS = ['sonarqube-webapp-addons/src/**/*.{ts,tsx}'];

export default [
  ...basePreset({
    js,
    sonarjs,
    files: ['**/*.mjs', ...ADDONS],
    // SonarSource's webapp is a submodule and not ours to lint, and the rest
    // is Gradle's, SonarQube's or the scanner's output.
    ignores: ['build/**', 'sonarqube-lib/**', 'sonarqube-webapp/**', '.scannerwork/**', '.claude/**'],
  }),
  ...typescriptPreset({ tseslint, files: ADDONS }),
  ...jsonPreset({ json }),
];
