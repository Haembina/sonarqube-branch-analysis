<p align="center">
  <img src="icons/icon.png" alt="Haembina Branch Analysis logo" width="128">
</p>

# Haembina Branch Analysis

Branch analysis and pull request decoration for **SonarQube Community Build 26.9.0.129388**
(`sonarqube:26.9.0.129388-community`). Java 21 and Gradle against the SonarQube plugin API, plus a patched SonarQube
webapp whose branch pages are TypeScript and React in [`sonarqube-webapp-addons`](sonarqube-webapp-addons/README.md).
Each release supports only the SonarQube build it names, `sonarqubeVersion` in `build.gradle`.

A fork of [mc1arke/sonarqube-community-branch-plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin)
by Michael Clarke. SonarQube is a trademark of SonarSource; SonarSource neither makes nor supports this plugin.

## What it does

SonarQube Community Build analyses one branch per project and decorates nothing. This plugin adds:

- Analysis of every branch and pull request, each with its own quality gate, issues and new code, plus the pages to
  list, rename, delete and set the main branch.
- Pull request decoration: GitHub check runs with annotations and an optional summary comment, GitLab merge
  request discussions and a summary comment, Bitbucket Cloud and Server Code Insights reports, Azure DevOps threads
  and a quality gate status.
- Branch and pull request parameters read from Azure Pipelines, Bitbucket Pipelines, Cirrus CI, Codemagic, GitHub
  Actions, GitLab CI and Jenkins, so the scanner needs no `sonar.branch.*` or `sonar.pullrequest.*` there.

## What this fork adds

| | mc1arke's plugin | Haembina Branch Analysis |
| --- | --- | --- |
| CI branch detection | Always on | Off switch: `com.haembina.branchanalysis.auto-configuration.enabled` |
| Java coverage | Reported | Build fails under 100% of lines and branches |
| Branch page tests | None | Jest suite, same floor; Playwright suite against a running SonarQube |
| Accessibility | Unchecked | axe WCAG 2.2 A and AA scan of every branch page and dialog, keyboard-only specs, and a spec per criterion axe cannot measure |
| Release gate | The build | Jar and webapp publish only after a real SonarQube runs a main, branch and pull request analysis with them |
| Mutation testing | PIT configured | PIT weekly in CI |
| Java compiler | javac warnings | Error Prone and javac lint as errors |

Branch page fixes: long branch names wrap, the pull request overview reflows on a narrow screen, focus returns to a
row's actions button when its dialog closes, controls carry accessible names, and an uncaught error shows as a toast.

Merged from pull requests to mc1arke's repository: SonarQube 26.7, 26.8 and 26.9 support, the project key in Azure
DevOps quality gate status names for monorepos, Bitbucket Cloud OAuth credentials kept out of debug logs, GitLab
summary comments deleted on resolved discussions, and plugin upgrades in release images.

## Getting started

JDK 21. The first build downloads the SonarQube distribution into `sonarqube-lib`:

```bash
./gradlew build
```

Node from [`.nvmrc`](.nvmrc). `npm run lint` needs `shellcheck`; `npm run test:coverage` needs `bash` and `git`:

```bash
npm ci
```

The webapp builds inside the `sonarqube-webapp` submodule; setup is in
[`sonarqube-webapp-addons/README.md`](sonarqube-webapp-addons/README.md).

## Commands

| Command | What it does |
| --- | --- |
| `./gradlew build` | Compiles with Error Prone and javac lint as errors, runs the suite under the JaCoCo floor, builds the jar |
| `./gradlew test` | The Java suite; JaCoCo report in `build/reports/jacoco/test` |
| `npm test` | The Java suite, without its floor |
| `npm run test:coverage` | The Java suite with its floor, then the addons' suite, and the jars the SonarQube scan resolves types against. What CI runs |
| `npm run test:addons` | The addons' Jest suite with its floor; `coverage/addons/lcov.info` |
| `./gradlew pitest` | PIT mutation run; report in `build/reports/pitest` |
| `bash scripts/integration-test.sh <jar> <webapp dir>` | Boots SonarQube with both and runs a main, branch and pull request analysis |
| `bash scripts/integration-test.sh <jar> <webapp dir> npm run test:e2e` | The same, then the Playwright suite against that server |
| `npm run lint` | shellcheck, ESLint (TypeScript, JavaScript, JSON), npm-groovy-lint (Gradle), Error Prone and javac (Java) |
| `npm run lint:md` | markdownlint over every document |
| `npm run lint:docker` | hadolint over both Dockerfiles, in Docker |

## Layout

| Path | Holds |
| --- | --- |
| `src/main/java` | The Java agent, the compute-engine and web-server extensions, one client per ALM |
| `src/main/resources/static` | Images that pull request decorations link to |
| `src/test` | The JUnit 5 suite and fixtures |
| `sonarqube-webapp-addons` | The branch and pull request pages, linked into `sonarqube-webapp` at build time |
| `sonarqube-webapp` | SonarSource's webapp, a submodule pinned to one Community Build tag |
| `docker` | The image's entrypoint and its upgrade test |
| `scripts` | The integration test and the suites `npm run test:coverage` runs |
| `e2e` | The Playwright suite |
| `docs` | How the agent, the extensions and the webapp fit together |

## Common tasks

### Switch off branch detection from CI

With it off, a scan naming no branch or pull request analyses main, so CI that scans pull requests onto main keeps
doing so. Set it under Administration, General, Branches and Pull Requests, or:

```bash
curl -u "$SONAR_ADMIN_TOKEN:" -X POST "$SONAR_HOST_URL/api/settings/set" -d key=com.haembina.branchanalysis.auto-configuration.enabled -d value=false
```

### Upgrade from the Community Branch Plugin

The plugin key, jar name and every setting key changed, so SonarQube reads this as a different plugin.

1. Note each `com.github.mc1arke.sonarqube.plugin.branch.*` setting on the server.
2. Stop SonarQube, delete `sonarqube-community-branch-plugin-*.jar` from `extensions/plugins`, and
   [install](#manual-install) this jar and webapp, with both `javaagent` lines naming the new jar.
3. Start SonarQube and set each value again under `com.haembina.branchanalysis.*`, same suffix.
4. Scanners passing `com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId` pass
   `com.haembina.branchanalysis.pullrequest.gitlab.pipelineId` instead.

## Testing

Both unit suites fail on any uncovered line or branch; a new uncovered branch is tested or deleted with proof.
The addons' suites sit beside their files as `*.test.tsx` and run in the submodule's Jest setup.

The integration test needs Docker, `jq` and `curl`:

```bash
bash scripts/integration-test.sh build/libs/haembina-branch-analysis-*.jar path/to/unzipped/webapp
```

A failed run prints the server's last 300 log lines, then one `integration-test.sh failed:` line naming the check or
command that failed, so a CI job's last lines say why it is red.

PIT shows whether the assertions notice a change, which coverage cannot. It sets no threshold, since equivalent
mutants make one noise.

### End-to-end suite

A command after the integration test's two paths runs against its server once its checks pass. The Playwright suite
signs in as a user it creates and drives the branch list, the switcher, the pull request overview, the new code
settings and the rename, set-as-main and delete dialogs, each also by keyboard alone. Each page and open dialog gets an
axe scan for WCAG 2.2 A and AA; `e2e/sonarqube.helper.ts` lists the nodes SonarSource draws outside the addons. The
`e2e/wcag-*.test.ts` specs cover the criteria axe cannot measure, over the nodes `e2e/wcag.helper.ts` names. It needs
`npx playwright install chromium`:

```bash
bash scripts/integration-test.sh build/libs/haembina-branch-analysis-*.jar path/to/unzipped/webapp npm run test:e2e
```

Against a server of your own, set `SONARQUBE_URL`, `SONARQUBE_PROJECT` and, when the admin password is not `admin`,
`SONARQUBE_ADMIN_PASSWORD`. The project must hold the integration test's analyses; the dialog specs rename, re-point
and delete them. Failures leave a report in `build/reports/e2e`.

## Versioning

SemVer, derived from Conventional Commits, independent of the SonarQube build. A level is what an administrator
notices:

- **Major**: upgrading needs more than swapping the jar and webapp, such as a renamed setting or plugin key, a changed
  `javaagent` path, or a SonarQube build dropped.
- **Minor**: something new: a setting, an ALM, a decoration, or a newer SonarQube build.
- **Patch**: a fix, with nothing to change on the server.

## Deploying

Take the jar and `sonarqube-webapp.zip` from one release.

### Manual install

1. Copy the jar into `extensions/plugins/`.
2. In `conf/sonar.properties`, set
   `sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=web`.
3. In the same file, set
   `sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=ce`.
4. Replace the contents of SonarQube's `web` directory with the webapp zip's.
5. Start SonarQube and accept the third-party plugin warning.

### Docker

`Dockerfile` builds an image from source with the Gradle wrapper, running the suite; `release.Dockerfile` downloads a
release from the public mirror. Both base images are pinned by digest, and `SONARQUBE_VERSION` and `SONARQUBE_DIGEST`
change together. A tag pushes the release image to `ghcr.io/haembina/haembina-branch-analysis:<version>`. Overriding
`SONAR_WEB_JAVAADDITIONALOPTS` or `SONAR_CE_JAVAADDITIONALOPTS` drops the `javaagent` settings unless the override
repeats them. `docker-compose.yml` reads `SONARQUBE_VERSION`, `SONARQUBE_DIGEST`, `DOCKERFILE`, `PLUGIN_VERSION` and
`SONARQUBE_PORT` from `.env`:

```bash
docker compose up --build
```

### Kubernetes with the official Helm chart

Helm values for the
[SonarQube chart](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube), with `${version}`
replaced:

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

Set [`sonar.core.serverBaseURL`](https://docs.sonarsource.com/sonarqube-server/instance-administration/server-base-url)
or the links in decorations break. Every setting key starts `com.haembina.branchanalysis.`.

### Decorate a pull request

Analyse the target branch first. CI that supports auto-configuration needs no branch parameters. Otherwise the target
branch takes `sonar.branch.name`, and the pull request takes these, following
[SonarQube's guide](https://docs.sonarsource.com/sonarqube-server/discovering/code-analysis/pull-request-analysis#decoration).
Any `sonar.branch.*` set alongside makes SonarQube analyse a branch and decorate nothing:

```properties
sonar.pullrequest.key = 100
sonar.pullrequest.branch = feature/TICKET-123
sonar.pullrequest.base = main
```

A GitHub pull request also needs `sonar.scm.revision`, e.g. with
[SonarQube Scan](https://github.com/marketplace/actions/official-sonarqube-scan):

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

### Decoration images

SonarQube serves them from this plugin. When the ALM cannot reach SonarQube, copy `src/main/resources/static` somewhere
it can and set `Images base URL` under General, Pull Request.

## Support

Issues on this repository only; SonarSource and its forum close requests about this plugin. There is no migration path
to a commercial edition, and migrating data analysed with this plugin is untested and may lose some or all of it.

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
- `npm run test:coverage` checks the `sonarqube-webapp` submodule out and removes it afterwards, so a dependency audit reads only
  this repository's lockfiles. It builds with `NX_DAEMON=false`: Nx's daemon holds the submodule open, and on Windows
  the removal fails with `Could not remove submodule work tree`.

## License

LGPL-3.0 ([LICENSE](LICENSE)), which adds permissions to GPL-3.0 ([COPYING](COPYING)). Derived from
[mc1arke/sonarqube-community-branch-plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin),
Copyright (C) 2020-2026 Michael Clarke, and modified by Haembina since 2026-09-25; [NOTICE](NOTICE) says who holds
what.

## Further reading

- [Architecture](docs/architecture.md): how the agent, the extensions and the webapp fit together
- [Webapp addons](sonarqube-webapp-addons/README.md): building the branch pages
- [Changelog](CHANGELOG.md)
- [License](LICENSE) and [Notice](NOTICE)
- [Security policy](SECURITY.md): reporting a vulnerability
