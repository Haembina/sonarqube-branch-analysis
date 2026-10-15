#!/usr/bin/env bash

set -Eeuo pipefail

CURRENT_DIR=$(pwd)
WEBAPP_DIR=${1:-"${CURRENT_DIR}/sonarqube-webapp"}
ADDONS_DIR=${2:-"${CURRENT_DIR}/sonarqube-webapp-addons"}

function override_vite_config() {
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
    cd "${WEBAPP_DIR}"
    echo "Copying the addons into sonarqube-webapp/libs/sq-server-addons"
    rm -rf ./libs/sq-server-addons ./.nx/workspace-data
    cp -R "${ADDONS_DIR}" ./libs/sq-server-addons
    override_vite_config
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
