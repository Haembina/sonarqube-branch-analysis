#!/bin/sh

set -eu

repository_root=$(CDPATH='' cd -- "$(dirname "$0")/.." && pwd)
test_root=$(mktemp -d)
trap 'rm -rf "${test_root}"' EXIT HUP INT TERM

mkdir -p "${test_root}/bin"
calls="${test_root}/calls"
password="${test_root}/password"
present="${test_root}/present"
report="${test_root}/report"
printf '%s\n' '#!/bin/sh' \
  "printf '%s\\n' \"\$*\" >> '${calls}'" \
  "if [ \"\$1\" = login ]; then cat > '${password}'; fi" \
  "if [ \"\$1\" = manifest ]; then grep -Fqx \"\$3\" '${present}'; fi" > "${test_root}/bin/docker"
chmod +x "${test_root}/bin/docker"

printf '%s\n' 'ghcr.io/haembina/haembina-branch-analysis:27.0.2' > "${present}"
PATH="${test_root}/bin:${PATH}" VERSION=27.0.2 ACTOR=octocat GHCR_TOKEN=secret-token \
  sh "${repository_root}/docker/check-release-image.sh" > "${report}"

test "$(cat "${password}")" = "secret-token"
test "$(sed -n 1p "${calls}")" = "login ghcr.io --username octocat --password-stdin"
test "$(sed -n 2p "${calls}")" = "manifest inspect ghcr.io/haembina/haembina-branch-analysis:27.0.2"
test "$(wc -l < "${calls}" | tr -d ' ')" = "2"
if grep -Fq 'secret-token' "${calls}"; then exit 1; fi
if grep -Fq '::error::' "${report}"; then exit 1; fi

: > "${calls}"
if PATH="${test_root}/bin:${PATH}" VERSION=27.1.0 ACTOR=octocat GHCR_TOKEN=secret-token \
  sh "${repository_root}/docker/check-release-image.sh" > "${report}"; then
  exit 1
fi
test "$(sed -n 2p "${calls}")" = "manifest inspect ghcr.io/haembina/haembina-branch-analysis:27.1.0"
grep -Fq '::error::27.1.0 was released with no container image' "${report}"
grep -Fq 'ghcr.io/haembina/haembina-branch-analysis:27.1.0 is not in the registry' "${report}"

: > "${calls}"
if PATH="${test_root}/bin:${PATH}" ACTOR=octocat GHCR_TOKEN=secret-token \
  sh "${repository_root}/docker/check-release-image.sh" 2> /dev/null; then
  exit 1
fi
test ! -s "${calls}"

: > "${calls}"
printf '%s\n' 'registry.example/plugin:27.0.2' > "${present}"
PATH="${test_root}/bin:${PATH}" VERSION=27.0.2 ACTOR=octocat GHCR_TOKEN=secret-token \
  IMAGE=registry.example/plugin sh "${repository_root}/docker/check-release-image.sh" > /dev/null
test "$(sed -n 2p "${calls}")" = "manifest inspect registry.example/plugin:27.0.2"

workflow="${repository_root}/.github/workflows/build.yml"
job=$(sed -n '/^  image-published:$/,/^  [a-z-]*:$/p' "${workflow}")
printf '%s\n' "${job}" | grep -Fq 'needs: [snapshot, release, image]'
printf '%s\n' "${job}" | grep -Fq "needs.snapshot.outputs.release == 'true'"
printf '%s\n' "${job}" | grep -Fq "needs.release.result == 'success'"
printf '%s\n' "${job}" | grep -Fq '!cancelled()'
printf '%s\n' "${job}" | grep -Fq 'packages: read'
printf '%s\n' "${job}" | grep -Fq 'run: sh docker/check-release-image.sh'
