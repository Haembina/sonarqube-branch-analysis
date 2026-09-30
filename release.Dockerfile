ARG SONARQUBE_VERSION="community"

FROM alpine:3.23 AS downloader
ARG PLUGIN_VERSION
ENV PLUGIN_VERSION=${PLUGIN_VERSION}

ARG DOWNLOAD_BASE_URL=https://github.com/Haembina/sonarqube-branch-analysis/releases/download/v${PLUGIN_VERSION}

ADD ${DOWNLOAD_BASE_URL}/sonarqube-webapp.zip /tmp/sonarqube-webapp.zip
ADD ${DOWNLOAD_BASE_URL}/haembina-branch-analysis-${PLUGIN_VERSION}.jar /tmp/haembina-branch-analysis.jar

RUN apk update && \
    apk add unzip && \
    mkdir -p /opt/sonarqube/web && \
    unzip /tmp/sonarqube-webapp.zip -d /opt/sonarqube/web

FROM sonarqube:${SONARQUBE_VERSION}
ARG PLUGIN_VERSION

USER root
RUN rm -rf /opt/sonarqube/web/*

COPY --from=downloader --chown=sonarqube:root /tmp/haembina-branch-analysis.jar /opt/sonarqube/lib/haembina-branch-analysis/haembina-branch-analysis-${PLUGIN_VERSION}.jar
COPY --chmod=755 docker/branch-analysis-entrypoint.sh /opt/sonarqube/docker/branch-analysis-entrypoint.sh
COPY --from=downloader --chmod=550 --chown=sonarqube:root /opt/sonarqube/web /opt/sonarqube/web


USER sonarqube
ENV PLUGIN_VERSION=${PLUGIN_VERSION}
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${PLUGIN_VERSION}.jar=ce"
ENTRYPOINT ["/opt/sonarqube/docker/branch-analysis-entrypoint.sh"]
