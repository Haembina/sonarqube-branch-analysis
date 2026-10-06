#!/bin/sh

# Runs docker/push-release-image.sh as build.yml's image job runs it, with a docker on PATH that
# records what it was asked instead of reaching a registry, and checks the job that calls it.

set -eu

repository_root=$(CDPATH='' cd -- "$(dirname "$0")/.." && pwd)
test_root=$(mktemp -d)
trap 'rm -rf "${test_root}"' EXIT HUP INT TERM

mkdir -p "${test_root}/bin"
calls="${test_root}/calls"
password="${test_root}/password"
printf '%s\n' '#!/bin/sh' \
  "printf '%s\\n' \"\$*\" >> '${calls}'" \
  "if [ \"\$1\" = login ]; then cat > '${password}'; fi" > "${test_root}/bin/docker"
chmod +x "${test_root}/bin/docker"

PATH="${test_root}/bin:${PATH}" VERSION=27.0.2 ACTOR=octocat GHCR_TOKEN=secret-token \
  sh "${repository_root}/docker/push-release-image.sh"

test "$(cat "${password}")" = "secret-token"
test "$(sed -n 1p "${calls}")" = "login ghcr.io --username octocat --password-stdin"
test "$(sed -n 2p "${calls}")" = "build --file ${repository_root}/release.Dockerfile --build-arg PLUGIN_VERSION=27.0.2 --tag ghcr.io/haembina/haembina-branch-analysis:27.0.2 ${repository_root}"
test "$(sed -n 3p "${calls}")" = "push ghcr.io/haembina/haembina-branch-analysis:27.0.2"
test "$(wc -l < "${calls}" | tr -d ' ')" = "3"
if grep -Fq 'secret-token' "${calls}"; then exit 1; fi

# Without a version nothing is built or pushed.
: > "${calls}"
if PATH="${test_root}/bin:${PATH}" ACTOR=octocat GHCR_TOKEN=secret-token \
  sh "${repository_root}/docker/push-release-image.sh" 2> /dev/null; then
  exit 1
fi
test ! -s "${calls}"

# The job runs on a v-tag only, after the mirror's release, with the token allowed to write packages.
workflow="${repository_root}/.github/workflows/build.yml"
job=$(sed -n '/^  image:$/,/^  [a-z-]*:$/p' "${workflow}")
printf '%s\n' "${job}" | grep -Fq 'needs: [snapshot, mirror]'
printf '%s\n' "${job}" | grep -Fq "if: needs.snapshot.outputs.release == 'true'"
printf '%s\n' "${job}" | grep -Fq 'packages: write'
printf '%s\n' "${job}" | grep -Fq 'run: sh docker/push-release-image.sh'
grep -Eq "^      - 'v\\*'$" "${workflow}"
