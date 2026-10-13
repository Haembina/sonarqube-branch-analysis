<div align="center">

<img src="../icons/icon.png" alt="" width="48">

# Contributing

How a change lands, setting up a checkout, and what to do when.

[README](../README.md) · [Architecture](architecture.md) · **Contributing** ·
[undefined](guide.md) · [Security](security.md) · [Testing](testing.md)

</div>

---

## How a change lands

```mermaid
flowchart LR
  s0["Fork"]
  s1["Branch"]
  s2["npm run gate"]
  s3["Pull request"]
  s4["Review"]
  s5["Merged on green"]
  s0 --> s1
  s1 --> s2
  s2 --> s3
  s3 --> s4
  s4 --> s5
```

## Setup

1. Download the jar and sonarqube-webapp.zip from the same release: a jar and a
   webapp from different releases break the branch pages.
2. Copy the jar into SonarQube's extensions/plugins directory.
3. In conf/sonar.properties, set
   sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=web.
4. In the same file, set
   sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/haembina-branch-analysis-${version}.jar=ce.
5. Replace the contents of SonarQube's web directory with the webapp zip's.
6. Start SonarQube and accept the third-party plugin warning.
7. Set the server base URL and decorate pull requests as docs/guide.md
   describes.

## When you

| | When you | Do |
| --- | --- | --- |
| <img src="../icons/readme/bug.svg" width="18" alt=""> | Find a bug | Open an issue with the SonarQube build, the plugin version, the steps that reproduce it, and what you expected |
| <img src="../icons/readme/lock.svg" width="18" alt=""> | Find a vulnerability | Report it privately, as docs/security.md says, never in a public issue |
| <img src="../icons/readme/git-merge.svg" width="18" alt=""> | Open a pull request | A maintainer lands it in the source repository, then closes yours with a link to the commit it landed as |
| <img src="../icons/readme/code.svg" width="18" alt=""> | Change the Java code | Keep ./gradlew build green: Error Prone and javac warnings are errors, and the suite holds its coverage floor |
| <img src="../icons/readme/device-desktop.svg" width="18" alt=""> | Change the branch pages | Run setup.sh, then npm run test:addons and npm run typecheck:addons, and build with --skip-nx-cache so Nx cannot replay old pages |
| <img src="../icons/readme/refresh.svg" width="18" alt=""> | Move to a newer SonarQube build | Move the sonarqube-webapp submodule, sonarqubeVersion in build.gradle, .env and both Dockerfiles together |
| <img src="../icons/readme/test-pipe.svg" width="18" alt=""> | Change what the agent rewrites | Run the integration test: the agent names SonarQube classes by string, so only a real server catches a rename |
