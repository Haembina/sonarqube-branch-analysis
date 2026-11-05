#!/usr/bin/env bash
# Copyright (C) 2026 Haembina
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# Checks out SonarSource's sonarqube-webapp submodule, copies the addons into it with setup.sh and installs
# its dependencies into a Yarn cache outside the tree: everything the addons' suite fetches from the network.
# verify runs the suite in a namespace with no network, so its install step runs this and then deinits the
# submodule; test-ci.sh runs it again, and it checks out from the local objects and installs from the cache.
set -euo pipefail

root=$(CDPATH='' cd -- "$(dirname "$0")/.." && pwd)
cd "${root}"

export YARN_CACHE_FOLDER="${YARN_CACHE_FOLDER:-${HOME}/.cache/sonarqube-webapp-yarn}"

git submodule update --init --depth 1 sonarqube-webapp
./sonarqube-webapp-addons/setup.sh
(cd sonarqube-webapp && "${root}/node_modules/.bin/corepack" yarn install --no-immutable)
