#!/bin/sh

set -eu

: "${VERSION:?VERSION names the released version}"
: "${ACTOR:?ACTOR names the user that logs in to ghcr.io}"
: "${GHCR_TOKEN:?GHCR_TOKEN is the token that may write packages}"

repository_root=$(CDPATH='' cd -- "$(dirname "$0")/.." && pwd)
image="${IMAGE:-ghcr.io/haembina/haembina-branch-analysis}:${VERSION}"

printf '%s' "${GHCR_TOKEN}" | docker login ghcr.io --username "${ACTOR}" --password-stdin
docker build --file "${repository_root}/release.Dockerfile" --build-arg "PLUGIN_VERSION=${VERSION}" \
  --tag "${image}" "${repository_root}"
docker push "${image}"
