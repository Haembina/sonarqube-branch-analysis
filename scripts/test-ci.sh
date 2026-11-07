#!/usr/bin/env bash
# Copyright (C) 2026 Haembina
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# Every suite with its coverage floor, writing the reports a SonarQube scan reads: the Java suite's
# JaCoCo XML and classes, then the webapp addons' Jest lcov in coverage/addons/lcov.info.
# This is `npm run test:coverage`.
#
# The addons' suite runs inside SonarSource's sonarqube-webapp workspace, so this prepares it with
# fetch-webapp.sh and builds sq-server once, because the workspace's Jest setup reads the Nx project graph
# that build writes. Without Java 21 or Node on the PATH, it cannot start.
set -euo pipefail

root=$(CDPATH='' cd -- "$(dirname "$0")/.." && pwd)
cd "${root}"

sh gradlew test jacocoTestCoverageVerification

export NX_DAEMON=false

bash scripts/fetch-webapp.sh
(cd sonarqube-webapp && "${root}/node_modules/.bin/corepack" yarn nx run sq-server:build)

# Workaround: SWC refuses every cache root inside verify's network-less user namespace #84
SWC_NATIVE_BINDING_CACHE_SKIP_SECURITY_CHECK=1 npm run test:addons

git submodule deinit --force sonarqube-webapp
