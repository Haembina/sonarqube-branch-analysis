#!/usr/bin/env bash
# Copyright (C) 2026 Haembina
# SPDX-License-Identifier: LGPL-3.0-or-later
#
# Installs or upgrades one release of this plugin on a SonarQube running in a Proxmox LXC container,
# in the manual-install layout: the jar in extensions/plugins, the two javaagent lines in
# conf/sonar.properties, and the release's webapp in place of web.
#
# Usage: scripts/install-proxmox-ct.sh <version> <proxmox ssh host> <container id> <sonarqube url>
# e.g. scripts/install-proxmox-ct.sh 27.0.0 proxmox 100 http://sonarqube.lan:9000
#
# Needs `gh` able to read this repository's releases and `ssh <proxmox ssh host>` as a user who may run
# `pct`. SonarQube lives in /opt/sonarqube inside the container, runs as the systemd unit `sonarqube`,
# and is stopped for the swap, so run this while no analysis is queued. The first run keeps the stock
# web directory and sonar.properties in /opt/sonarqube-backup.
# shellcheck disable=SC2029 # the jar, version and container names are meant to expand here, before ssh sends each command
set -euo pipefail

version="${1:?plugin version, without the v}"
host="${2:?the Proxmox host, as ssh reaches it}"
container="${3:?the LXC container id SonarQube runs in}"
sonarqube_url="${4:?the SonarQube base URL, to wait for it to come back}"
jar="haembina-branch-analysis-${version}.jar"
download=$(mktemp -d)
trap 'rm -rf "${download}"' EXIT

gh release download "v${version}" --repo Haembina/haembina-branch-analysis --dir "${download}"

scp -q "${download}/${jar}" "${download}/sonarqube-webapp.zip" "${host}:/tmp/"
ssh "${host}" "pct push ${container} /tmp/${jar} /tmp/${jar} && pct push ${container} /tmp/sonarqube-webapp.zip /tmp/sonarqube-webapp.zip && rm /tmp/${jar} /tmp/sonarqube-webapp.zip"

ssh "${host}" "pct exec ${container} -- bash -euo pipefail -c '
  cd /opt/sonarqube
  systemctl stop sonarqube
  mkdir -p /opt/sonarqube-backup
  ls -d /opt/sonarqube-backup/web*-stock >/dev/null 2>&1 || mv web /opt/sonarqube-backup/web-stock
  cp -n conf/sonar.properties /opt/sonarqube-backup/sonar.properties-before-branch-plugin
  rm -rf web && mkdir web && unzip -q /tmp/sonarqube-webapp.zip -d web
  rm -f extensions/plugins/sonarqube-community-branch-plugin-*.jar extensions/plugins/haembina-branch-analysis-*.jar
  install -m 644 /tmp/${jar} extensions/plugins/${jar}
  sed -i \"/^sonar\\.\\(web\\|ce\\)\\.javaAdditionalOpts=/d\" conf/sonar.properties
  printf \"%s\\n\" \"sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/${jar}=web\" \"sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/${jar}=ce\" >> conf/sonar.properties
  chown -R sonarqube:sonarqube web extensions/plugins conf/sonar.properties
  rm /tmp/${jar} /tmp/sonarqube-webapp.zip
  systemctl start sonarqube
'"

for _ in $(seq 1 60); do
  curl -s "${sonarqube_url}/api/system/status" | grep -q '"status":"UP"' && break
  sleep 5
done
curl -s "${sonarqube_url}/api/system/status"
echo
