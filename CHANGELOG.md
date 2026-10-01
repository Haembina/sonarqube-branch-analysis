# Changelog

All notable changes to this project are documented here.

This file is written by `npm run release`, never by hand. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres
to [Semantic Versioning](https://semver.org/).

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
