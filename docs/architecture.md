# Architecture

SonarQube Community Build ships the branch and pull request machinery of the commercial editions, switched off by an
edition check. This plugin switches it on in three places: a Java agent that rewrites the edition check as SonarQube's
classes load, plugin extensions that implement the branch interfaces the core leaves empty, and a webapp whose branch
pages the Community Build's frontend no longer carries.

## The Java agent

`CommunityBranchAgent` is the jar's `Premain-Class`. SonarQube starts it through the two `-javaagent` lines in
`conf/sonar.properties`, with `web` or `ce` as its argument. It retransforms a handful of core classes with Javassist:
`PlatformEditionProvider` answers Developer, `MultipleAlmFeature` and the plugin's own bootstrap answer available, and
the new-code-period web services get a Developer edition provider.

The agent runs before any plugin loads, which is why it lives in the same jar: it is the only code here that can change
a class SonarQube has already decided to use.

## The plugin

`CommunityBranchPluginBootstrap` is the `Plugin-Class`. On the web server and the compute engine it checks the agent
ran, and fails the start with a message naming the missing `javaagent` line if it did not. On the scanner it loads
`CommunityBranchPlugin` through `ElevatedClassLoaderFactory`, a class loader whose parent is SonarQube's core loader,
so the scanner-side classes can see the core types a plugin loader hides.

| Package | Runs in | Does |
| --- | --- | --- |
| `scanner` | The scanner | Reads `sonar.branch.*` and `sonar.pullrequest.*`, or detects them from CI (`scanner/autoconfiguration`), and loads the project's existing branches |
| `server` | The web server | The branch feature, component keys per branch, and the web services the webapp's pull request settings call |
| `ce` | The compute engine | Loads the branch an analysis belongs to, and after a pull request analysis decorates it (`ce/pullrequest`) |
| `almclient` | The compute engine and web server | One HTTP client per ALM: Azure DevOps, Bitbucket, GitHub and GitLab |

A pull request decoration is `PullRequestPostAnalysisTask`: it builds `AnalysisDetails` from the finished analysis and
hands them to the decorator for the project's ALM binding, which posts a summary, inline comments and a status.

## The webapp

The Community Build's frontend dropped the branch and pull request pages. `sonarqube-webapp-addons` reimplements them
as an Nx library, and `setup.sh` links it into SonarSource's `sonarqube-webapp` submodule as `libs/sq-server-addons`
before the build. The release publishes the built webapp as `sonarqube-webapp.zip`, which replaces SonarQube's `web`
directory. The submodule is pinned to the Community Build tag the plugin compiles against, so a jar and a webapp from
one release always match.

SonarQube's own webapp installs no page-wide error handler. `error-boundary.ts` adds one when the webapp first loads the
addon registry: an error or a rejected promise nothing caught shows as SonarQube's error toast, carrying the error's
message, a thrown string as written, or anything else as JSON. A rejected `Response` is skipped, because SonarQube's
API layer has already shown it.

## Build and release

Gradle builds a shaded jar: the plugin's own dependencies are relocated inside it, and SonarQube's jars are
`compileOnly`, taken from the distribution Gradle downloads into `sonarqube-lib`.

`package.json` holds the version and the npm tooling around the Gradle build.

## Where this bites

- The agent's transformations name SonarQube classes and methods by string. A Community Build that renames one fails
  at startup with the bootstrap's message, not at compile time. The integration test is what catches it:
  `bash scripts/integration-test.sh <jar> <webapp dir>`.
- A SonarQube build newer than `sonarqubeVersion` in `build.gradle` may load the plugin and break a page, because the
  webapp was built against the submodule's tag. Move the submodule and `sonarqubeVersion` together.
