#!/bin/sh

set -eu

: "${VERSION:?VERSION names the released version}"
: "${ACTOR:?ACTOR names the user that logs in to ghcr.io}"
: "${GHCR_TOKEN:?GHCR_TOKEN is the token that may read packages}"

image="${IMAGE:-ghcr.io/haembina/haembina-branch-analysis}:${VERSION}"

printf '%s' "${GHCR_TOKEN}" | docker login ghcr.io --username "${ACTOR}" --password-stdin

if ! docker manifest inspect "${image}" > /dev/null; then
  echo "::error::${VERSION} was released with no container image: ${image} is not in the registry"
  exit 1
fi

echo "${image} is in the registry"
