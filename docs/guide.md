# Guide

How to install Haembina Branch Analysis on a SonarQube Community Build, and
configure it. Docker and Kubernetes installs are in the [README](../README.md).

## Install

Take the jar and `sonarqube-webapp.zip` from one release. A jar and a webapp
from different releases load, and the branch pages then fail at runtime.

1. Copy the jar into `extensions/plugins/`.
2. In `conf/sonar.properties`, set
   `sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=web`.
3. In the same file, set
   `sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=ce`.
4. Replace the contents of SonarQube's `web` directory with the webapp zip's.
5. Start SonarQube and accept the third-party plugin warning.

Without both `javaagent` lines, SonarQube starts with the plugin listed and no
branch support. This shows whether they are there:

```bash
grep javaAdditionalOpts conf/sonar.properties
```

Each release supports only the Community Build that `sonarqubeVersion` in
`build.gradle` names. A newer build may load the plugin and break a page,
because the webapp was built against that one.

## Configuration

Set
[`sonar.core.serverBaseURL`](https://docs.sonarsource.com/sonarqube-server/instance-administration/server-base-url),
or the links in decorations break. Every setting key of this plugin starts
`com.haembina.branchanalysis.`.

### Decorate a pull request

- Analyse the target branch first.
- CI that the plugin detects needs no branch parameters.
- Otherwise the target branch takes `sonar.branch.name`, and the pull request
  takes the properties below, following
  [SonarQube's guide](https://docs.sonarsource.com/sonarqube-server/discovering/code-analysis/pull-request-analysis#decoration).
- Any `sonar.branch.*` set beside them makes SonarQube analyse a branch and
  decorate nothing.

```properties
sonar.pullrequest.key = 100
sonar.pullrequest.branch = feature/TICKET-123
sonar.pullrequest.base = main
```

A GitHub pull request also needs `sonar.scm.revision`, for example with
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

SonarQube serves the images decorations show from this plugin. When the ALM
cannot reach SonarQube, copy `src/main/resources/static` somewhere it can, and
set `Images base URL` under General, Pull Request.

### Switch off branch detection from CI

- With detection on, a CI scan of a pull request lands on that pull request,
  and main stops moving when CI meant to scan main.
- With it off, a scan naming no branch or pull request analyses main.
- Set it under Administration, General, Branches and Pull Requests, or:

```bash
curl -u "$SONAR_ADMIN_TOKEN:" -X POST "$SONAR_HOST_URL/api/settings/set" -d key=com.haembina.branchanalysis.auto-configuration.enabled -d value=false
```

## Upgrade

A release's level says what an administrator does:

- **Major**: more than swapping the jar and webapp, such as a renamed setting
  or plugin key, a changed `javaagent` path, or a SonarQube build dropped.
- **Minor**: something new, such as a setting, an ALM, a decoration, or a newer
  SonarQube build.
- **Patch**: a fix, with nothing to change on the server.

### From the Community Branch Plugin

The plugin key, jar name and every setting key changed, so SonarQube reads this
as a different plugin.

1. Note each `com.github.mc1arke.sonarqube.plugin.branch.*` setting on the
   server.
2. Stop SonarQube, delete `sonarqube-community-branch-plugin-*.jar` from
   `extensions/plugins`, and [install](#install) this jar and webapp, with both
   `javaagent` lines naming the new jar.
3. Start SonarQube and set each value again under
   `com.haembina.branchanalysis.*`, same suffix.
4. Scanners passing
   `com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId`
   pass `com.haembina.branchanalysis.pullrequest.gitlab.pipelineId` instead.

## Support

- Open issues on this repository only; SonarSource and its forum close requests
  about this plugin.
- There is no migration path to a commercial edition.
- Migrating data analysed with this plugin is untested, and may lose some or
  all of it.
- SonarQube is a trademark of SonarSource, which neither makes nor supports
  this plugin.
