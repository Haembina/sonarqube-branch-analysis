#!/bin/sh

# Builds the release image for one released version from release.Dockerfile and pushes it to GHCR.
# build.yml's image job runs it on a v-tag once the mirror holds that version's release.
#
# Environment: VERSION (the released version, without the v), ACTOR (the GitHub user that logs in)
# and GHCR_TOKEN (a token with packages: write), all required. IMAGE overrides the repository the
# image is pushed to. The token reaches docker on stdin, never on a command line.

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
