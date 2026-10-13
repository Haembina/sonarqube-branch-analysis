# SonarQube with a released jar and webapp, downloaded from the public mirror's release of PLUGIN_VERSION.
#
# SONARQUBE_VERSION and SONARQUBE_DIGEST name one image and change together, as in .env. The release writes
# PLUGIN_VERSION here from package.json.
ARG SONARQUBE_VERSION="26.10.0.133078-community"
ARG SONARQUBE_DIGEST="sha256:2e378f7852b67d43507d3d383ee8fb5a522b8b0f920be9803f74f72952fce8f1"
ARG PLUGIN_VERSION="27.0.2"

# busybox's unzip unpacks the webapp, so nothing is installed.
FROM alpine:3.24@sha256:294b683cb724975bec92580e1e685676bd4b50bda910ddb8c51d4cabeaec77e6 AS downloader
ARG PLUGIN_VERSION

ADD https://github.com/Haembina/sonarqube-branch-analysis/releases/download/v${PLUGIN_VERSION}/sonarqube-webapp.zip /tmp/sonarqube-webapp.zip
ADD https://github.com/Haembina/sonarqube-branch-analysis/releases/download/v${PLUGIN_VERSION}/haembina-branch-analysis-${PLUGIN_VERSION}.jar /tmp/haembina-branch-analysis.jar

RUN mkdir -p /opt/sonarqube/web && unzip -q /tmp/sonarqube-webapp.zip -d /opt/sonarqube/web


FROM sonarqube:${SONARQUBE_VERSION}@${SONARQUBE_DIGEST}
ARG PLUGIN_VERSION

COPY --from=downloader --chown=sonarqube:root /tmp/haembina-branch-analysis.jar /opt/sonarqube/lib/haembina-branch-analysis/haembina-branch-analysis-${PLUGIN_VERSION}.jar
COPY --chown=sonarqube:root --chmod=755 docker/branch-analysis-entrypoint.sh /opt/sonarqube/docker/branch-analysis-entrypoint.sh

RUN chmod -R 770 /opt/sonarqube/web && rm -rf /opt/sonarqube/web/*
COPY --from=downloader --chown=sonarqube:root --chmod=550 /opt/sonarqube/web /opt/sonarqube/web

ENV PLUGIN_VERSION=${PLUGIN_VERSION}
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${PLUGIN_VERSION}.jar=ce"
ENTRYPOINT ["/opt/sonarqube/docker/branch-analysis-entrypoint.sh"]
