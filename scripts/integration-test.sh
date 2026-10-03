#!/usr/bin/env bash
# Copyright (C) 2026 Haembina
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# End-to-end test of a built plugin jar and webapp against a real SonarQube.
#
# Installs the artifacts the way a manual (non-Docker) installation does: the jar in
# extensions/plugins, the javaagent options for the web and compute engine processes in
# conf/sonar.properties, and the patched webapp over /opt/sonarqube/web. They are copied
# into the running server and it is restarted, so no host path is bind-mounted and the
# daemon may be remote, as a CI service container's is. It then runs a main, a branch and a
# pull request analysis, and a GitHub Actions pull request scan with CI detection on and
# then switched off on the server. It fails when any compute engine task fails, when
# SonarQube lists the wrong branches or pull requests, or when a server log carries an ERROR.
#
# Usage: scripts/integration-test.sh <plugin jar> <webapp directory> [command...]
# Needs docker, curl and jq. Reads SONARQUBE_VERSION and SONARQUBE_DIGEST from .env unless they are set.
# With SONARQUBE_CONTAINER set, it installs into that running SonarQube, such as a workflow's
# `services:` container, publishing port SONARQUBE_PORT; without it, it starts and removes its own.
#
# A command after the two paths runs once every check has passed, while the server is still up,
# with SONARQUBE_URL and SONARQUBE_PROJECT exported; its failure fails the script. The
# Playwright suite runs this way, with these analyses as its fixture.

set -euo pipefail

plugin_jar=$(realpath "${1:?plugin jar}")
webapp_dir=$(realpath "${2:?webapp directory}")
shift 2
repository_root=$(cd -- "$(dirname "$0")/.." && pwd)

SONARQUBE_VERSION="${SONARQUBE_VERSION:-$(sed -n 's/^SONARQUBE_VERSION=//p' "${repository_root}/.env")}"
SONARQUBE_DIGEST="${SONARQUBE_DIGEST:-$(sed -n 's/^SONARQUBE_DIGEST=//p' "${repository_root}/.env")}"
SCANNER_IMAGE="${SCANNER_IMAGE:-sonarsource/sonar-scanner-cli:12.2.0.4256_8.1.0}"
SONARQUBE_PORT="${SONARQUBE_PORT:-9000}"
SONARQUBE_URL="http://localhost:${SONARQUBE_PORT}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-300}"
CE_TIMEOUT_SECONDS="${CE_TIMEOUT_SECONDS:-180}"
POLL_SECONDS=5

container="${SONARQUBE_CONTAINER:-}"
owned_container=""
project_key="branch-analysis-it"
project_dir=$(mktemp -d)
# The scanner container runs as its own user and writes .scannerwork into the project
chmod 777 "${project_dir}"
plugin_path="/opt/sonarqube/extensions/plugins/$(basename "${plugin_jar}")"

failure=""
failed_line=""
failed_command=""
trap 'failed_line=${LINENO}; failed_command=${BASH_COMMAND}' ERR

# Says why the test failed and stops it; cleanup repeats the reason after the server's logs.
fail() {
  failure="$*"
  echo "$*" >&2
  exit 1
}

cleanup() {
  status=$?
  if [ "${status}" -ne 0 ] && [ -n "${container}" ]; then
    echo "::group::SonarQube logs"
    docker logs "${container}" 2>&1 | tail -300 || true
    echo "::endgroup::"
  fi
  # Last, after the server's logs, so the line a failed job ends on says which step failed
  if [ -n "${failure}" ]; then
    echo "integration-test.sh failed: ${failure}" >&2
  elif [ "${status}" -ne 0 ]; then
    echo "integration-test.sh failed with exit ${status} at line ${failed_line:-?}: ${failed_command:-?}" >&2
  fi
  [ -z "${owned_container}" ] || docker rm -f "${owned_container}" >/dev/null 2>&1 || true
  rm -rf "${project_dir}"
  exit "${status}"
}
trap cleanup EXIT

api() {
  curl --silent --show-error --fail -u "${auth}" "$@"
}

wait_until_up() {
  local deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
  until curl --silent "${SONARQUBE_URL}/api/system/status" | jq -e '.status == "UP"' >/dev/null 2>&1; do
    if (( SECONDS >= deadline )); then
      fail "SonarQube was not UP after ${STARTUP_TIMEOUT_SECONDS}s"
    fi
    sleep "${POLL_SECONDS}"
  done
}

if [ -z "${container}" ]; then
  echo "Starting sonarqube:${SONARQUBE_VERSION}"
  owned_container="branch-analysis-it-$$"
  container="${owned_container}"
  docker run -d --name "${container}" -p "${SONARQUBE_PORT}:9000" -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
    "sonarqube:${SONARQUBE_VERSION}@${SONARQUBE_DIGEST}" >/dev/null
fi

# A restart while the first start is still migrating its database can corrupt it, so the plain server comes up
# before anything is installed.
wait_until_up
echo "Installing the candidate plugin and webapp into ${container}"
docker exec -u root "${container}" sh -c 'rm -rf /opt/sonarqube/web/*'
docker cp "${webapp_dir}/." "${container}:/opt/sonarqube/web"
docker cp "${plugin_jar}" "${container}:${plugin_path}"
docker exec -u root "${container}" sh -c "chmod -R a+rX /opt/sonarqube/web '${plugin_path}' && chmod u+w /opt/sonarqube/conf/sonar.properties && printf '%s\n' \
  'sonar.web.javaAdditionalOpts=-javaagent:${plugin_path}=web' \
  'sonar.ce.javaAdditionalOpts=-javaagent:${plugin_path}=ce' >> /opt/sonarqube/conf/sonar.properties"
docker restart --time 60 "${container}" >/dev/null
wait_until_up

auth="admin:admin"
api "${SONARQUBE_URL}/api/plugins/installed" | jq -e '.plugins | any(.key == "haembinabranchanalysis")' >/dev/null \
  || fail "haembinabranchanalysis is not installed"
api "${SONARQUBE_URL}/api/features/list" | jq -e 'index("monorepo") != null' >/dev/null \
  || fail "the plugin's monorepo feature is not registered"
curl --silent --show-error --fail "${SONARQUBE_URL}/" | grep -q 'id="content"' \
  || fail "the webapp does not serve its index page"

api -X POST "${SONARQUBE_URL}/api/projects/create" -d "project=${project_key}" -d "name=${project_key}" >/dev/null
token=$(api -X POST "${SONARQUBE_URL}/api/user_tokens/generate" -d "name=integration-test" | jq -r .token)

cat > "${project_dir}/sonar-project.properties" <<EOF
sonar.projectKey=${project_key}
sonar.sources=.
sonar.exclusions=sonar-project.properties
EOF
printf '%s\n' 'function greet(name) {' '  return "Hello " + name;' '}' 'console.log(greet("integration test"));' \
  > "${project_dir}/greet.js"

# Extra `docker run` options for the scanner, such as the CI variables auto-configuration reads.
scanner_env=()

# Runs one analysis and waits for its compute engine task, because the scanner reports
# success once the report is uploaded, before the plugin has processed anything. The project
# is copied into the scanner's container rather than mounted, for the same reason the server's
# artifacts are.
analyse() {
  local scanner output task_id task_status
  scanner=$(docker create --network host "${scanner_env[@]}" "${SCANNER_IMAGE}" \
    -Dsonar.host.url="${SONARQUBE_URL}" -Dsonar.token="${token}" "$@")
  docker cp "${project_dir}/." "${scanner}:/usr/src"
  output=$(docker start --attach "${scanner}" 2>&1) \
    || { echo "${output}" >&2; docker rm "${scanner}" >/dev/null; return 1; }
  docker rm "${scanner}" >/dev/null
  task_id=$(sed -n 's|.*api/ce/task?id=\([A-Za-z0-9_-]*\).*|\1|p' <<<"${output}" | tail -1)
  [ -n "${task_id}" ] || { echo "${output}" >&2; echo "no compute engine task in the scanner output" >&2; return 1; }

  local deadline=$((SECONDS + CE_TIMEOUT_SECONDS))
  while (( SECONDS < deadline )); do
    task_status=$(api "${SONARQUBE_URL}/api/ce/task?id=${task_id}" | jq -r .task.status)
    case "${task_status}" in
      SUCCESS) echo "task ${task_id}: SUCCESS"; return 0 ;;
      FAILED|CANCELED)
        api "${SONARQUBE_URL}/api/ce/task?id=${task_id}&additionalFields=stacktrace" | jq -r '.task.errorMessage, .task.errorStacktrace' >&2
        return 1 ;;
    esac
    sleep "${POLL_SECONDS}"
  done
  echo "task ${task_id} did not finish within ${CE_TIMEOUT_SECONDS}s" >&2
  return 1
}

# A branch or pull request only has a quality gate once its analysis was processed.
assert_quality_gate() {
  api "${SONARQUBE_URL}/api/qualitygates/project_status?projectKey=${project_key}&$1" \
    | jq -e '.projectStatus.status | IN("OK", "ERROR")' >/dev/null \
    || { echo "no quality gate computed for $1" >&2; return 1; }
}

echo "1/5 main branch"
analyse

echo "2/5 branch feature/integration"
analyse -Dsonar.branch.name=feature/integration
api "${SONARQUBE_URL}/api/project_branches/list?project=${project_key}" \
  | jq -e '.branches | any(.name == "feature/integration")' >/dev/null \
  || fail "feature/integration is not listed as a branch"
assert_quality_gate "branch=feature/integration"

echo "3/5 pull request 1"
analyse -Dsonar.pullrequest.key=1 -Dsonar.pullrequest.branch=feature/pull-request -Dsonar.pullrequest.base=main
api "${SONARQUBE_URL}/api/project_pull_requests/list?project=${project_key}" \
  | jq -e '.pullRequests | any(.key == "1")' >/dev/null \
  || fail "pull request 1 is not listed"
assert_quality_gate "pullRequest=1"

# Pretends to be a GitHub Actions pull request run, which auto-configuration reads.
github_pull_request() {
  scanner_env=(-e GITHUB_ACTIONS=true -e "GITHUB_REF=refs/pull/$1/merge" -e GITHUB_HEAD_REF=feature/ci -e GITHUB_BASE_REF=main)
}
pull_request_listed() {
  api "${SONARQUBE_URL}/api/project_pull_requests/list?project=${project_key}" \
    | jq -e --arg key "$1" '.pullRequests | any(.key == $key)' >/dev/null
}

echo "4/5 GitHub Actions pull request 7, detected from CI by default"
github_pull_request 7
analyse
pull_request_listed 7 || fail "CI detection did not turn the scan into pull request 7"

echo "5/5 GitHub Actions pull request 8 with CI detection switched off on the server"
api -X POST "${SONARQUBE_URL}/api/settings/set" \
  -d "key=com.haembina.branchanalysis.auto-configuration.enabled" -d "value=false"
github_pull_request 8
analyse
scanner_env=()
if pull_request_listed 8; then
  fail "the scan became pull request 8 although CI detection was switched off"
fi

errors=$(docker exec "${container}" sh -c 'grep -h " ERROR " /opt/sonarqube/logs/web.log /opt/sonarqube/logs/ce.log || true')
if [ -n "${errors}" ]; then
  fail "SonarQube logged errors:
${errors}"
fi

echo "Integration test passed: main, branch and pull request analyses and the CI detection switch, with no logged errors"

if [ "$#" -gt 0 ]; then
  echo "Running $* against ${SONARQUBE_URL}"
  SONARQUBE_URL="${SONARQUBE_URL}" SONARQUBE_PROJECT="${project_key}" "$@"
fi
