# sq-server-addons

This library reimplements removed features from the SonarQube Community Build frontend to support Haembina Branch Analysis.

Most code is refactored from the [`SonarSource/sonarqube-webapp@2025.1.0.10869`](https://github.com/SonarSource/sonarqube-webapp/tree/2025.1.0.10869).

## Setup

Clone the `sonarqube-webapp` submodule:

```bash
git submodule update --init --recursive
```

Link this directory into the submodule as `libs/sq-server-addons`, and point its Vite config at it:

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

If you are building locally, run the [setup script](#setup) first to create the symlink. The Docker build overwrites the
directory instead.

```bash
cd sonarqube-webapp && yarn nx run sq-server:build
```

The distribution files are generated in the `sonarqube-webapp/apps/sq-server/build/webapp` directory.

## Testing

Each file's suite sits beside it as `<File>.test.tsx`, written with the workspace's Testing Library helpers and service
mocks. `jest.config.cjs` at the repository root runs them in the workspace's Jest setup, measures every file under
`src`, and fails under the coverage floor. After `yarn install` in the submodule, from the repository root:

```bash
npm run test:addons
```

The report is `coverage/addons/lcov.info`.

## Where this bites

- `npm run test:addons` runs the submodule's Jest, so before `yarn install` there it stops at
  `Cannot find module .../sonarqube-webapp/node_modules/jest/bin/jest.js`.
- Without `setup.sh`, `yarn nx run sq-server:build` builds the stock webapp: the branch pages are missing and nothing
  fails. Check the link exists with `ls -l sonarqube-webapp/libs/sq-server-addons`.
- Nx hashes none of the linked addons, so after a change to them `yarn nx run sq-server:build` can replay the previous
  build from its cache and ship the old pages. Build with `--skip-nx-cache` after changing the addons.
- The submodule pins one SonarQube Community Build tag (`git submodule status`). A webapp from another tag loads against
  a server that does not match it, and the branch pages fail at runtime.
