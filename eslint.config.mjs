// ESLint flat config, read by `npm run lint:es`, which `npm run lint` runs.
//
// The webapp addons compile only inside SonarSource's `sonarqube-webapp`
// workspace, where `tsconfig.json` and every `~sq-server-commons` import
// resolve. Outside it there is no program to type-check against, so the
// TypeScript rules are the syntactic sets: typed rules would fail on every
// unresolved import rather than report anything about the code.
import js from '@eslint/js';
import json from '@eslint/json';
import sonarjs from 'eslint-plugin-sonarjs';
import tseslint from 'typescript-eslint';

export default [
  {
    // SonarSource's webapp is a submodule and not ours to lint. npm writes
    // package-lock.json and nobody edits it, so a finding there is about npm.
    ignores: [
      'build/**',
      'node_modules/**',
      'sonarqube-lib/**',
      'sonarqube-webapp/**',
      '.scannerwork/**',
      '.claude/**',
      'package-lock.json',
    ],
  },
  { files: ['**/*.mjs'], ...js.configs.recommended },
  { files: ['**/*.mjs'], ...sonarjs.configs.recommended },
  ...tseslint.configs.recommended.map((config) => ({
    ...config,
    files: ['sonarqube-webapp-addons/src/**/*.{ts,tsx}'],
  })),
  {
    files: ['sonarqube-webapp-addons/src/**/*.{ts,tsx}'],
    ...sonarjs.configs.recommended,
  },
  {
    // A tunable number lives in a named constant. The TypeScript form
    // replaces the base rule so an enum member or a literal type is not one.
    files: ['sonarqube-webapp-addons/src/**/*.{ts,tsx}'],
    rules: {
      'no-magic-numbers': 'off',
      '@typescript-eslint/no-magic-numbers': [
        'error',
        {
          ignore: [-1, 0, 1],
          ignoreEnums: true,
          ignoreNumericLiteralTypes: true,
          ignoreReadonlyClassProperties: true,
        },
      ],
    },
  },
  {
    // The same for the build scripts.
    files: ['**/*.mjs'],
    rules: { 'no-magic-numbers': ['error', { ignore: [-1, 0, 1] }] },
  },
  {
    // Every JSON file, package.json and the addons' tsconfig.json and
    // project.json among them. Every reader downstream refuses a malformed
    // file; none refuses a well-formed one that names a key twice, which is
    // what this catches.
    files: ['**/*.json'],
    language: 'json/json',
    ...json.configs.recommended,
  },
  {
    // markdownlint's config carries comments, which strict JSON rejects.
    files: ['**/*.jsonc'],
    language: 'json/jsonc',
    ...json.configs.recommended,
  },
];
