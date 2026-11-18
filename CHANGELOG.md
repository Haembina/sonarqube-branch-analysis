# Changelog

All notable changes to this project are documented here.

This file is written by `npm run release`, never by hand. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres
to [Semantic Versioning](https://semver.org/).

## 27.4.4 (2026-11-18)

### Bug Fixes

* Marketplace, System, Code and bubble chart layouts keep their half widths on SonarQube 26.11

## 27.4.3 (2026-11-18)

### Bug Fixes

* keep a project card's main row clear of the favorite star
* a project card lists every language that fits, close under its title

## 27.4.2 (2026-11-18)

### Maintenance

* internal housekeeping

## 27.4.1 (2026-11-17)

### Maintenance

* internal housekeeping

## 27.4.0 (2026-11-17)

### Features

* open main, the newest branch, the newest pull request or any measure straight from its project card
* show each project's newest branch and pull request analysis, and when main has moved, on the projects page
* report when each project's main, branches and pull requests were last analyzed
* delete pull request analyses after merge or close, and branch analyses whose branch is gone

### Bug Fixes

* screen readers skip a project card's coverage and duplication charts, which repeat the value beside them
* build the image from source again now that setup.sh applies the webapp patches
* delete branches and pull requests left without an analysis for the configured days

## 27.3.1 (2026-11-15)

### Bug Fixes

* update the libraries it is built on

## 27.3.0 (2026-11-14)

### Features

* branch analysis runs on SonarQube Community Build 26.11

### Bug Fixes

* the image leaves an installed jar alone when it is already current
* an Azure DevOps request gives up after 30 seconds
* the GitLab pipeline status reports total coverage
* the branch screens keep their layout on SonarQube 26.11
* the Docker images start from SonarQube Community Build 26.11
* update the libraries it is built on

## 27.2.0 (2026-11-06)

### Features

* post each new issue as a GitHub review comment on its line, resolved once fixed
* name the missing javaagent line when SonarQube refuses to start without it
* name both releases in a banner when the jar and webapp do not match
* filter the branches page by name and delete several branches at once
* show a branch analysis's quality gate as a check on GitHub and a status on GitLab
* decorate Gitea and Forgejo pull requests with a commit status and a summary comment
* let a project skip the clean-pass summary comment and GitHub annotations
* detect branches and pull requests from Woodpecker, Buildkite, CircleCI, TeamCity and Drone

### Bug Fixes

* clear the seven SonarQube issues the batch's new code raised
* give the search boxes' clear button a 24px target
* edit the GitHub summary comment in place instead of reposting it each push

## 27.1.8 (2026-10-30)

### Bug Fixes

* validate_binding declares its empty answer, so SonarQube stops warning it has no response example

## 27.1.7 (2026-10-30)

### Maintenance

* internal housekeeping

## 27.1.6 (2026-10-30)

### Bug Fixes

* the pull request list web service documents its response with an example

## 27.1.5 (2026-10-25)

### Maintenance

* internal housekeeping

## 27.1.4 (2026-10-24)

### Maintenance

* internal housekeeping

## 27.1.3 (2026-10-24)

### Bug Fixes

* draw the pull request button's platform icon the webapp serves
* a cached webapp build rebuilds when the branch pages change and restores the webapp it built
* update the libraries it is built on

## 27.1.2 (2026-10-14)

### Maintenance

* internal housekeeping

## 27.1.1 (2026-10-14)

### Bug Fixes

* BitbucketClient imports nothing its Javadoc only names
* clear the eleven issues SonarQube's new S9383, S9397 and S9398 raise

## 27.1.0 (2026-10-13)

### Features

* branch analysis runs on SonarQube Community Build 26.10

### Bug Fixes

* the branch pages show focus, selection and an empty name without relying on color
* each branch's actions menu has an id an ARIA reference can name
* a reused checkout keeps the add-ons' Vite override
* verifySonarqubeVersion holds both Dockerfiles to the SonarQube tag and digest .env pins (#24)
* the SonarQube images build from the wrapper's Gradle on pinned bases, and CI lints and builds them (#24)

## 27.0.2 (2026-10-01)

### Bug Fixes

* take jackson-datatype-jsr310 2.22.3
* build the webapp without Nx's daemon, so npm test removes the submodule again
* reflow the pull request overview on a narrow screen
* edge the off keep-when-inactive switch at 3:1
* close the class name in the agent's transform-failure log (#18)
* wrap long branch names instead of cutting them off
* return focus to a row's actions button when its dialog closes
* show a rejected object as JSON rather than [object Object]
* show an uncaught error or rejection as an error toast

## 27.0.1 (2026-09-26)

### Bug Fixes

* name the keep-when-inactive switch and the branch switcher (#13)
* clear the quality gate on the batch's new code
* draw the pull request overview's accepted-issues and checklist icons in their theme colors (#6)
* name each branch's actions button after its branch (#12)

## 27.0.0 (2026-09-26)

### BREAKING CHANGES

* SonarQube loads this as a new plugin. Settings under com.github.mc1arke.sonarqube.plugin.branch.* are not read; set them again under com.haembina.branchanalysis.*, and point both javaagent lines at the new jar.

### Features

* **BREAKING** rename the plugin Haembina Branch Analysis

## 26.9.1 (2026-09-25)

### Features

* a server setting that switches off branch detection from CI
