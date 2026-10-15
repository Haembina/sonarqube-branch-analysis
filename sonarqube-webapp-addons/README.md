# sq-server-addons

This library reimplements removed features from the SonarQube Community Build frontend to support Haembina Branch Analysis.

Most code is refactored from the [`SonarSource/sonarqube-webapp@2025.1.0.10869`](https://github.com/SonarSource/sonarqube-webapp/tree/2025.1.0.10869).

## Setup

Clone the `sonarqube-webapp` submodule:

```bash
git submodule update --init --recursive
```

Copy this directory into the submodule as `libs/sq-server-addons`, and point its Vite config at it. Run it again
after each change:

```bash
./sonarqube-webapp-addons/setup.sh
```

Install the webapp's dependencies:

```bash
cd sonarqube-webapp && yarn install
```

## Local Development

By default, the UI targets SonarQube at `http://localhost:9000`. Set the `PROXY` environment variable to target another
server.

```bash
cd sonarqube-webapp && PROXY=http://my-sonarqube.org yarn start-sqs
```

## Building

Run the [setup script](#setup) before each build; the Docker build runs it too.

- Without the copy, the build makes the stock webapp: the branch pages are missing and nothing fails.
  `ls sonarqube-webapp/libs/sq-server-addons/setup.sh` shows whether it is there.

```bash
cd sonarqube-webapp && yarn nx run sq-server:build
```

The distribution files are generated in the `sonarqube-webapp/apps/sq-server/build/webapp` directory.

The submodule pins one SonarQube Community Build tag (`git submodule status`). A webapp built from another tag fails at
runtime against the server.

## Testing

- Each file's suite sits beside it as `<File>.test.tsx`, written with the workspace's Testing Library helpers and service
  mocks.
- `jest.config.cjs` at the repository root runs them in the workspace's Jest setup, measures every file under `src`,
  and fails under the coverage floor.
- The suite runs the submodule's Jest, so run `yarn install` in the submodule first. Then, from the repository root:

```bash
npm run test:addons
```

The report is `coverage/addons/lcov.info`.

Neither Jest nor the Vite build type-checks the addons. `tsc` does, through the copy, so each import resolves to the
webapp module it names:

```bash
npm run typecheck:addons
```
