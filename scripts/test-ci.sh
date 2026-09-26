#!/usr/bin/env bash
# Copyright (C) 2026 Haembina
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# Every suite with its coverage floor, writing the reports the conventions gate and the SonarQube scan read:
# the Java suite's JaCoCo XML and classes, then the webapp addons' Jest lcov in coverage/addons/lcov.info.
# This is `npm test`, which verify.yml runs before its reports stage and its scan.
#
# The addons' suite runs inside SonarSource's sonarqube-webapp workspace, so this checks the submodule out,
# links the addons into it with setup.sh, installs its dependencies and builds sq-server once, because the
# workspace's Jest setup reads the Nx project graph that build writes. Without Java 21 or Node on the PATH,
# it cannot start.
set -euo pipefail

root=$(CDPATH='' cd -- "$(dirname "$0")/.." && pwd)
cd "${root}"

sh gradlew test jacocoTestCoverageVerification sonarLibraries

git submodule update --init --depth 1 sonarqube-webapp
./sonarqube-webapp-addons/setup.sh
(cd sonarqube-webapp && corepack enable && yarn install --no-immutable && yarn nx run sq-server:build)

npm run test:addons

# The submodule is SonarSource's webapp at a pinned tag: its dependencies are theirs to fix, so the audit and the
# scan that follow in verify.yml read this repository's own tree and not its lockfile.
git submodule deinit --force sonarqube-webapp
