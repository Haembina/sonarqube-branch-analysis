#!/usr/bin/env bash
# Copyright (C) 2026 Haembina
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# End-to-end test of a built plugin jar and webapp against a real SonarQube.
#
# Installs the artifacts the way a manual (non-Docker) installation does: the jar in
# extensions/plugins, the javaagent options for the web and compute engine processes,
# and the patched webapp over /opt/sonarqube/web. It then runs a main, a branch and a
# pull request analysis, and a GitHub Actions pull request scan with CI detection on and
# then switched off on the server. It fails when any compute engine task fails, when
# SonarQube lists the wrong branches or pull requests, or when a server log carries an ERROR.
#
# Usage: scripts/integration-test.sh <plugin jar> <webapp directory> [command...]
# Needs docker, curl and jq. Reads SONARQUBE_VERSION from .env unless it is set.
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
SCANNER_IMAGE="${SCANNER_IMAGE:-sonarsource/sonar-scanner-cli:12.2.0.4256_8.1.0}"
SONARQUBE_PORT="${SONARQUBE_PORT:-9000}"
SONARQUBE_URL="http://localhost:${SONARQUBE_PORT}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-300}"
CE_TIMEOUT_SECONDS="${CE_TIMEOUT_SECONDS:-180}"
POLL_SECONDS=5

container="branch-analysis-it-$$"
project_key="branch-analysis-it"
project_dir=$(mktemp -d)
# The scanner container runs as its own user and writes .scannerwork into the project
chmod 777 "${project_dir}"
plugin_path="/opt/sonarqube/extensions/plugins/$(basename "${plugin_jar}")"

cleanup() {
  status=$?
  if [ "${status}" -ne 0 ]; then
    echo "::group::SonarQube logs"
    docker logs "${container}" 2>&1 | tail -300 || true
    echo "::endgroup::"
  fi
  docker rm -f "${container}" >/dev/null 2>&1 || true
  rm -rf "${project_dir}"
  exit "${status}"
}
trap cleanup EXIT

api() {
  curl --silent --show-error --fail -u "${auth}" "$@"
}

echo "Starting sonarqube:${SONARQUBE_VERSION} with the candidate plugin"
docker run -d --name "${container}" -p "${SONARQUBE_PORT}:9000" \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  -e SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:${plugin_path}=web" \
  -e SONAR_CE_JAVAADDITIONALOPTS="-javaagent:${plugin_path}=ce" \
  -v "${plugin_jar}:${plugin_path}:ro" \
  -v "${webapp_dir}:/opt/sonarqube/web:ro" \
  "sonarqube:${SONARQUBE_VERSION}" >/dev/null

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
until curl --silent "${SONARQUBE_URL}/api/system/status" | jq -e '.status == "UP"' >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "SonarQube was not UP after ${STARTUP_TIMEOUT_SECONDS}s" >&2
    exit 1
  fi
  sleep "${POLL_SECONDS}"
done

auth="admin:admin"
api "${SONARQUBE_URL}/api/plugins/installed" | jq -e '.plugins | any(.key == "haembinabranchanalysis")' >/dev/null \
  || { echo "haembinabranchanalysis is not installed" >&2; exit 1; }
api "${SONARQUBE_URL}/api/features/list" | jq -e 'index("monorepo") != null' >/dev/null \
  || { echo "the plugin's monorepo feature is not registered" >&2; exit 1; }
curl --silent --show-error --fail "${SONARQUBE_URL}/" | grep -q 'id="content"' \
  || { echo "the webapp does not serve its index page" >&2; exit 1; }

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
# success once the report is uploaded, before the plugin has processed anything.
analyse() {
  local output task_id task_status
  output=$(docker run --rm --network host "${scanner_env[@]}" -v "${project_dir}:/usr/src" "${SCANNER_IMAGE}" \
    -Dsonar.host.url="${SONARQUBE_URL}" -Dsonar.token="${token}" "$@" 2>&1) \
    || { echo "${output}" >&2; return 1; }
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
  || { echo "feature/integration is not listed as a branch" >&2; exit 1; }
assert_quality_gate "branch=feature/integration"

echo "3/5 pull request 1"
analyse -Dsonar.pullrequest.key=1 -Dsonar.pullrequest.branch=feature/pull-request -Dsonar.pullrequest.base=main
api "${SONARQUBE_URL}/api/project_pull_requests/list?project=${project_key}" \
  | jq -e '.pullRequests | any(.key == "1")' >/dev/null \
  || { echo "pull request 1 is not listed" >&2; exit 1; }
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
pull_request_listed 7 || { echo "CI detection did not turn the scan into pull request 7" >&2; exit 1; }

echo "5/5 GitHub Actions pull request 8 with CI detection switched off on the server"
api -X POST "${SONARQUBE_URL}/api/settings/set" \
  -d "key=com.haembina.branchanalysis.auto-configuration.enabled" -d "value=false"
github_pull_request 8
analyse
scanner_env=()
if pull_request_listed 8; then
  echo "the scan became pull request 8 although CI detection was switched off" >&2
  exit 1
fi

errors=$(docker exec "${container}" sh -c 'grep -h " ERROR " /opt/sonarqube/logs/web.log /opt/sonarqube/logs/ce.log || true')
if [ -n "${errors}" ]; then
  echo "${errors}" >&2
  echo "SonarQube logged errors" >&2
  exit 1
fi

echo "Integration test passed: main, branch and pull request analyses and the CI detection switch, with no logged errors"

if [ "$#" -gt 0 ]; then
  echo "Running $* against ${SONARQUBE_URL}"
  SONARQUBE_URL="${SONARQUBE_URL}" SONARQUBE_PROJECT="${project_key}" "$@"
fi
