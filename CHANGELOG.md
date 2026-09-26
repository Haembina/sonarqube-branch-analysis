# Changelog

All notable changes to this project are documented here.

This file is written by `npm run release`, never by hand. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres
to [Semantic Versioning](https://semver.org/).

## 27.0.0 (2026-09-26)

### BREAKING CHANGES

* SonarQube loads this as a new plugin. Settings under com.github.mc1arke.sonarqube.plugin.branch.* are not read; set them again under com.haembina.branchanalysis.*, and point both javaagent lines at the new jar.

### Features

* **BREAKING** rename the plugin Haembina Branch Analysis

## 26.9.1 (2026-09-25)

### Features

* a server setting that switches off branch detection from CI
