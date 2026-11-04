#!/usr/bin/env bash

set -Eeuo pipefail

addons_root=$(CDPATH='' cd -- "$(dirname "$0")" && pwd)
test_root=$(mktemp -d)
trap 'rm -rf "${test_root}"' EXIT HUP INT TERM

webapp="${test_root}/webapp"
addons="${test_root}/addons"
original="export const baseViteConfig = { build: {} };"
mkdir -p "${webapp}/libs/sq-server-addons" "${webapp}/.nx/workspace-data" "${webapp}/.nx/cache" "${addons}/src"
touch "${webapp}/libs/sq-server-addons/stub.ts" "${webapp}/.nx/workspace-data/project-graph.json" "${webapp}/.nx/cache/kept"
printf '%s\n' "${original}" > "${webapp}/vite.config.base.ts"
printf '%s\n' "export const page = 'first';" > "${addons}/src/index.ts"

bash "${addons_root}/setup.sh" "${webapp}" "${addons}" > /dev/null

test ! -L "${webapp}/libs/sq-server-addons"
grep -Fq "'first'" "${webapp}/libs/sq-server-addons/src/index.ts"
test ! -e "${webapp}/libs/sq-server-addons/stub.ts"
test ! -e "${webapp}/.nx/workspace-data"
test -e "${webapp}/.nx/cache/kept"
test "$(cat "${webapp}/vite.config.src.ts")" = "${original}"
grep -Fq "from './vite.config.src'" "${webapp}/vite.config.base.ts"
grep -Fq "outDir: process.env.BUILD_OUTPUT_DIR || 'build/webapp'," "${webapp}/vite.config.base.ts"

printf '%s\n' "export const page = 'second';" > "${addons}/src/index.ts"
bash "${addons_root}/setup.sh" "${webapp}" "${addons}" > /dev/null

grep -Fq "'second'" "${webapp}/libs/sq-server-addons/src/index.ts"
test "$(cat "${webapp}/vite.config.src.ts")" = "${original}"
grep -Fq "from './vite.config.src'" "${webapp}/vite.config.base.ts"

printf '%s\n' "${original}" > "${webapp}/vite.config.base.ts"
bash "${addons_root}/setup.sh" "${webapp}" "${addons}" > /dev/null

test "$(cat "${webapp}/vite.config.src.ts")" = "${original}"
grep -Fq "from './vite.config.src'" "${webapp}/vite.config.base.ts"
