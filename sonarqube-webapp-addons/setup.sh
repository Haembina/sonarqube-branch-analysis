#!/usr/bin/env bash

set -Eeuo pipefail

CURRENT_DIR=$(pwd)
WEBAPP_DIR=${1:-"${CURRENT_DIR}/sonarqube-webapp"}
ADDONS_DIR=${2:-"${CURRENT_DIR}/sonarqube-webapp-addons"}

function override_vite_config() {
    # Skipped only while the base still wraps the source. A reused checkout keeps the
    # untracked vite.config.src.ts while checkout restores the tracked base, so the
    # source alone existing is no sign the override is in place.
    if [[ -f ./vite.config.src.ts ]] && grep -q "from './vite.config.src'" ./vite.config.base.ts; then
        echo "vite.config.base.ts already wraps vite.config.src.ts, skipping override"
        return
    fi
    echo "Overriding config vite.config.base.ts"
    mv ./vite.config.base.ts ./vite.config.src.ts
    cat <<EOF >vite.config.base.ts
import { resolve } from 'node:path';
import { baseViteConfig as config } from './vite.config.src';

export * from './vite.config.src';
export const baseViteConfig = {
  ...config,
  resolve: {
    ...config.resolve,
    preserveSymlinks: true,
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
    echo "Creating symlink for sonarqube-webapp/libs/sq-server-addons"
    rm -rf ./libs/sq-server-addons
    ln -s "${ADDONS_DIR}" ./libs/sq-server-addons
    override_vite_config
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
