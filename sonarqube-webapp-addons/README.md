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

There are no tests for this project. The `integration-test` job in `.github/workflows/build.yml` loads the built webapp
against a running SonarQube, which is what catches a broken import here.

## Where this bites

- Without `setup.sh`, `yarn nx run sq-server:build` builds the stock webapp: the branch pages are missing and nothing
  fails. Check the link exists with `ls -l sonarqube-webapp/libs/sq-server-addons`.
- The submodule pins one SonarQube Community Build tag (`git submodule status`). A webapp from another tag loads against
  a server that does not match it, and the branch pages fail at runtime.
