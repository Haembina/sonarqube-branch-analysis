# Haembina Branch Analysis

Supports **SonarQube Community Build 26.9.0.129388**, the `sonarqube:26.9.0.129388-community` image. A release is built
and tested against that one build: install it on that build, and move to a newer build only with the release that
names it, because the webapp's branch pages are built against the same build's frontend.

Branch analysis and pull request decoration for SonarQube Community Build. It is built with Java 21 and Gradle against
the SonarQube plugin API, and ships a patched SonarQube webapp whose branch pages are TypeScript and React in
[`sonarqube-webapp-addons`](sonarqube-webapp-addons/README.md). SonarQube is a trademark of SonarSource; this plugin is
not made or supported by SonarSource.

It is derived from
[mc1arke/sonarqube-community-branch-plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin)
by Michael Clarke, and kept on one SonarQube Community Build release, `sonarqubeVersion` in `build.gradle`.

Every setting key starts `com.haembina.branchanalysis.`. One setting is Haembina's own,
`com.haembina.branchanalysis.auto-configuration.enabled` (Administration, General, Branches and Pull Requests; default
`true`). Set to `false`, a scan that names no branch or pull request analyses the main branch instead of reading its CI
environment, so CI that scans pull requests onto main keeps doing so.

## Getting started

The build needs JDK 21. Gradle downloads the SonarQube distribution it compiles against into `sonarqube-lib` on the
first build:

```bash
./gradlew build
```

The linters and the test runner are npm scripts, on the Node version in [`.nvmrc`](.nvmrc). `npm run lint` also needs
`shellcheck` on the `PATH`, and `npm test` needs `bash` and `git`:

```bash
npm ci
```

The webapp builds inside SonarSource's `sonarqube-webapp`, a git submodule. Its setup is in
[`sonarqube-webapp-addons/README.md`](sonarqube-webapp-addons/README.md).

## Commands

| Command | What it does |
| --- | --- |
| `./gradlew build` | Compiles with Error Prone and javac's lint as errors, runs the suite, fails under the JaCoCo floor, builds the jar |
| `./gradlew test` | The Java suite and its JaCoCo report in `build/reports/jacoco/test` |
| `npm test` | The Java suite with its JaCoCo floor, and the jars the SonarQube scan resolves types against |
| `npm run test:addons` | The webapp addons' Jest suite, with its coverage floor and `coverage/addons/lcov.info` |
| `./gradlew pitest` | PIT's mutation run over the Java, reported in `build/reports/pitest` |
| `bash scripts/integration-test.sh <jar> <webapp dir>` | Boots SonarQube with the jar and webapp and runs a main, branch and pull request analysis |
| `bash scripts/integration-test.sh <jar> <webapp dir> npm run test:e2e` | The same, then the Playwright suite over the branch pages against that server |
| `npm run lint` | shellcheck over the shell scripts, ESLint over the TypeScript, JavaScript and JSON, npm-groovy-lint over the Gradle scripts, Error Prone and javac over the Java |
| `npm run lint:md` | markdownlint over every document |

## Layout

| Path | Holds |
| --- | --- |
| `src/main/java` | The plugin: the Java agent, the compute-engine and web-server extensions, one client per ALM |
| `src/main/resources/static` | The images pull request decorations link to |
| `src/test` | The JUnit 5 suite and its fixtures |
| `sonarqube-webapp-addons` | The branch and pull request pages, linked into `sonarqube-webapp` at build time |
| `sonarqube-webapp` | SonarSource's webapp, a submodule pinned to one Community Build tag |
| `docker` | The Docker image's entrypoint and its upgrade test |
| `scripts` | The integration test and the suite `npm test` runs |
| `e2e` | The Playwright suite over the branch pages, run against the integration test's server |
| `docs` | How the agent, the extensions and the webapp fit together |

## Common tasks

### Switch off branch detection from CI

1. Set the setting to `false` with an administrator token:

   ```bash
   curl -u "$SONAR_ADMIN_TOKEN:" -X POST "$SONAR_HOST_URL/api/settings/set" -d key=com.haembina.branchanalysis.auto-configuration.enabled -d value=false
   ```

### Upgrade from the Community Branch Plugin

SonarQube reads Haembina Branch Analysis as a different plugin: its key, its jar name and every setting key changed.

1. Note the value of each `com.github.mc1arke.sonarqube.plugin.branch.*` setting on the server.
2. Stop SonarQube, delete `sonarqube-community-branch-plugin-*.jar` from `extensions/plugins`, and install this
   plugin's jar and webapp as in [Manual install](#manual-install), with both `javaagent` lines naming the new jar.
3. Start SonarQube and set each value again under `com.haembina.branchanalysis.*`, with the same suffix.
4. Scanners that pass `com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId` pass
   `com.haembina.branchanalysis.pullrequest.gitlab.pipelineId` instead.

## Testing

`./gradlew build` runs the JUnit 5 suite and fails under the JaCoCo floor in `build.gradle`. Both suites fail on
any uncovered line; the branch floor sits below 100 only for arms no test can reach, each named beside the floor in
`build.gradle` and `jest.config.cjs`. A new uncovered branch is tested, or deleted with the proof in its commit.
The webapp addons' suites sit beside the files they cover as `*.test.tsx` and run in the submodule's own Jest setup,
once its dependencies are installed; the run fails under the same floor:

```bash
npm run test:addons
```

The integration test boots the stock SonarQube image with the built jar and webapp, then runs a main, branch and pull
request analysis. Without Docker, `jq` and `curl`, it cannot start:

```bash
bash scripts/integration-test.sh build/libs/haembina-branch-analysis-*.jar path/to/unzipped/webapp
```

PIT measures whether the suite's assertions notice a change, which coverage cannot. It never fails a build: equivalent
mutants make a threshold noise. On 2026-09-26 it killed 785 of 988 mutants (79%), with a test strength of 86% over the
916 a test covers. Run it with:

```bash
./gradlew pitest
```

### End-to-end suite

A command after the integration test's two paths runs against its server once its checks pass. The Playwright suite
in `e2e` runs this way: it signs in as a user it creates, drives the branch list, the switcher, the pull request
overview, the branch new code settings and the rename, set-as-main and delete dialogs, each also by keyboard alone, and
needs Chromium from `npx playwright install chromium`. On each page and in each open dialog it runs an axe scan for
WCAG 2.2 A and AA, and any violation fails the spec; the nodes SonarSource's webapp draws outside the addons, with what
axe reports about each, are listed in `e2e/sonarqube.helper.ts`. The `e2e/wcag-*.test.ts` specs hold the WCAG 2.2 AA
criteria axe cannot measure, each titled with its criterion number, over the nodes `e2e/wcag.helper.ts` names as the
addons' own. A contrast is read once the control's transitions have finished, since a switch read mid-fade paints a
blend of its two states:

```bash
bash scripts/integration-test.sh build/libs/haembina-branch-analysis-*.jar path/to/unzipped/webapp npm run test:e2e
```

Pointed at a server by hand, the suite reads `SONARQUBE_URL`, `SONARQUBE_PROJECT` and, when the admin password is not
`admin`, `SONARQUBE_ADMIN_PASSWORD`; the project must hold the integration test's analyses, and the dialog specs
rename, re-point and delete them. A failure leaves its report in `build/reports/e2e`.

`npm test` checks the `sonarqube-webapp` submodule out for the addons' suite and removes it again afterwards, so a
dependency audit reads this repository's lockfiles: the submodule's dependencies are SonarSource's to fix. It builds
with `NX_DAEMON=false`, because Nx's daemon outlives the build and holds the submodule open, and on Windows the
removal then fails with `Could not remove submodule work tree`.

## Versioning

The version follows SemVer, derived from Conventional Commits. It is the plugin's own number;
the SonarQube Community Build it is built and tested against is `sonarqubeVersion` in `build.gradle` and the
`sonarqube-webapp` submodule's tag. A level is what a SonarQube administrator notices:

- **Major**: upgrading needs more than swapping the jar and the webapp. A renamed setting or plugin key, a changed
  `javaagent` path, or a SonarQube build no longer supported.
- **Minor**: something new to use: a setting, an ALM, a decoration, or a newer SonarQube build.
- **Patch**: a fix, with nothing to change on the server.

## Deploying

Take the plugin jar and `sonarqube-webapp.zip` from a release in this repository.

### Manual install

1. Copy the plugin JAR file to the `extensions/plugins/` directory of your SonarQube instance
2. Add `-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=web` to
   the `sonar.web.javaAdditionalOpts` property in your SonarQube installation's `conf/sonar.properties` file,
   e.g. `sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=web`
   where ${version} is the version of the plugin being worked with.
3. Add `-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=ce` to
   the `sonar.ce.javaAdditionalOpts` property in your SonarQube installation's `conf/sonar.properties` file,
   e.g. `sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=ce`
4. Replace the contents of the `web` directory in your SonarQube installation with the contents of the
   sonarqube-webapp zip archive
5. Start SonarQube, and accept the warning about using third-party plugins

### Docker

`Dockerfile` builds an image from this source, and `release.Dockerfile` one from a release. If you set the
`SONAR_WEB_JAVAADDITIONALOPTS` or `SONAR_CE_JAVAADDITIONALOPTS` environment variables in your container launch, add the
`javaagent` configuration to your overrides to match what's in the provided Dockerfile.

### Docker Compose

`docker-compose.yml` reads `SONARQUBE_VERSION`, `DOCKERFILE` and `PLUGIN_VERSION` from `.env`. To build and run a
container with the plugin and modified frontend code:

```bash
docker compose up --build
```

### Kubernetes with the official Helm chart

When using the
[SonarQube official Helm Chart](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube),
add the following settings to your helm values, where `${version}` should be replaced with the plugin version.

```yaml
community:
  enabled: true

plugins:
  install:
    - https://github.com/Haembina/sonarqube-branch-analysis/releases/download/v${version}/haembina-branch-analysis-${version}.jar
sonarProperties:
  sonar.web.javaAdditionalOpts: "-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${version}.jar=web"
  sonar.ce.javaAdditionalOpts: "-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${version}.jar=ce"

extraVolumes:
  - name: webapp
    emptyDir:
      sizeLimit: 50Mi
extraVolumeMounts:
  - name: webapp
    mountPath: /opt/sonarqube/web
extraInitContainers:
  - name: download-webapp
    image: busybox:1.37
    volumeMounts:
      - name: webapp
        mountPath: /web
    command:
      - sh
      - -c
      - wget -O /tmp/sonarqube-webapp.zip https://github.com/Haembina/sonarqube-branch-analysis/releases/download/v${version}/sonarqube-webapp.zip &&
        unzip -o /tmp/sonarqube-webapp.zip -d /web &&
        chmod -R 755 /web &&
        chown -R 1000:0 /web &&
        rm -f /tmp/sonarqube-webapp.zip
```

## Configuration

### Global configuration

Make sure `sonar.core.serverBaseURL` in SonarQube
[/admin/settings](https://docs.sonarsource.com/sonarqube-server/instance-administration/server-base-url) is properly set
in order for the links in the comment to work.

Set all other properties that you can define globally for all of your projects.

### How to decorate a pull request

In order to decorate your Pull Request's source branch, you need to analyze your target branch first.

If the scan is being run from a CI supporting auto-configuration then the scanner can be launched without any branch
parameters. Otherwise, the analysis needs the following setting:
`sonar.branch.name = branch_name (e.g master)`

For the pull request branch, read the official SonarQube guide for
[pull request decoration](https://docs.sonarsource.com/sonarqube-server/discovering/code-analysis/pull-request-analysis#decoration).
Unless your CI supports auto-configuration, set these properties:

```properties
sonar.pullrequest.key = pull_request_id (e.g. 100)
sonar.pullrequest.branch = source_branch_name (e.g feature/TICKET-123)
sonar.pullrequest.base = target_branch_name (e.g master)
```

There must not be any `sonar.branch` properties like `sonar.branch.name` set when you analyze a pull request. These
properties tell SonarQube a branch is being analyzed rather than a pull request, so no decoration runs.

If you are scanning a GitHub pull request, you will also need to set the `sonar.scm.revision` argument. For example,
using the official [SonarQube Scan](https://github.com/marketplace/actions/official-sonarqube-scan) on GitHub Actions:

```yaml
- name: SonarQube Scan
  uses: sonarsource/sonarqube-scan-action@<action version>
  with:
    args: >
      -Dsonar.scm.revision=${{ github.event.pull_request.head.sha }}
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ vars.SONAR_HOST_URL }}
```

### Serving images for pull request decoration

By default, images for decoration are served as static resources on the SonarQube server as part of this plugin.

If your SonarQube server sits behind a firewall, or the ALM cannot reach it, change the `Images base URL` property in
`General > Pull Request` settings to a location the ALM can reach, and copy the files under
`src/main/resources/static` there.

## Support

This plugin is not maintained or supported by SonarSource and has no official upgrade path for migrating from the
SonarQube Community Build to any of the commercial editions (Developer, Enterprise, or Data Center Edition). Support
for any problems is only available through issues on this repository. Any attempt to request support for this plugin
from SonarSource or an affiliated channel (e.g. the Sonar Community forum) is likely to result in your request being
closed or ignored.

If you plan on migrating your SonarQube data to a commercial edition after using this plugin, be aware that this may
result in some or all of your data being lost, as the compatibility of this plugin with the official branch features is
untested.

## Where this bites

- A jar and a webapp from different releases load, and the branch pages then fail at runtime. Install both from the
  same release.
- Without both `javaagent` lines in `conf/sonar.properties`, SonarQube starts with the plugin listed and no branch
  support. `grep javaAdditionalOpts conf/sonar.properties` shows whether they are there.
- With CI detection left on, a CI scan of a pull request lands on that pull request instead of main, and main stops
  moving.
- `npm-groovy-lint` pins an `axios` that four GitHub advisories name, which npm's own audit does not list yet, so
  `overrides` in `package.json` holds `axios` at a fixed release. Remove the override once `npm-groovy-lint` takes a
  fixed `axios`; `npm ls axios` shows which one resolves.

## License

The plugin is licensed under the GNU Lesser General Public License, version 3 ([LICENSE](LICENSE)), which adds
permissions to the GNU General Public License, version 3 ([COPYING](COPYING)). It is derived from
[mc1arke/sonarqube-community-branch-plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin),
Copyright (C) 2020-2026 Michael Clarke, and modified by Haembina since 2026-09-25; [NOTICE](NOTICE) says who holds
what.

## Further reading

- [Architecture](docs/architecture.md): how the agent, the extensions and the webapp fit together
- [Webapp addons](sonarqube-webapp-addons/README.md): building the branch pages
- [Changelog](CHANGELOG.md)
- [License](LICENSE) and [Notice](NOTICE)
- [Security policy](SECURITY.md): reporting a vulnerability
