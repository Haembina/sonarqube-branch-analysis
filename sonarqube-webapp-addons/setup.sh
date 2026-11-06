#!/usr/bin/env bash

set -Eeuo pipefail

CURRENT_DIR=$(pwd)
WEBAPP_DIR=${1:-"${CURRENT_DIR}/sonarqube-webapp"}
ADDONS_DIR=${2:-"${CURRENT_DIR}/sonarqube-webapp-addons"}

function release_version() {
    local version="${PLUGIN_VERSION:-}"
    if [[ -z "${version}" && -f "${ADDONS_DIR}/../gradle.properties" ]]; then
        version=$(sed -n 's/^version=//p' "${ADDONS_DIR}/../gradle.properties")
    fi
    if [[ ! "${version}" =~ ^[0-9A-Za-z.+-]+$ ]]; then
        echo "No release version: set PLUGIN_VERSION, or keep gradle.properties beside the addons" >&2
        return 1
    fi
    echo "${version}"
}

function override_vite_config() {
    local version=$1
    if [[ -f ./vite.config.src.ts ]] && grep -q "from './vite.config.src'" ./vite.config.base.ts; then
        echo "vite.config.base.ts already wraps vite.config.src.ts, rewriting the wrapper"
    else
        echo "Overriding config vite.config.base.ts"
        mv ./vite.config.base.ts ./vite.config.src.ts
    fi
    cat <<EOF >vite.config.base.ts
import { resolve } from 'node:path';
import { baseViteConfig as config } from './vite.config.src';

export * from './vite.config.src';
export const baseViteConfig = {
  ...config,
  build: {
    ...config.build,
    outDir: process.env.BUILD_OUTPUT_DIR || 'build/webapp',
  },
  define: {
    ...config.define,
    BRANCH_ANALYSIS_RELEASE: JSON.stringify('${version}'),
  },
  resolve: {
    ...config.resolve,
    alias: {
      ...config.resolve?.alias,
      '~sq-server-commons': resolve(__dirname, 'libs/sq-server-commons/src'),
    },
  },
};
EOF
}

function main() {
    local version
    version=$(release_version)
    cd "${WEBAPP_DIR}"
    echo "Copying the addons into sonarqube-webapp/libs/sq-server-addons"
    rm -rf ./libs/sq-server-addons ./.nx/workspace-data
    cp -R "${ADDONS_DIR}" ./libs/sq-server-addons
    override_vite_config "${version}"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
