# SonarQube with a released jar and webapp, downloaded from the public mirror's release of PLUGIN_VERSION.
#
# SONARQUBE_VERSION and SONARQUBE_DIGEST name one image and change together, as in .env. The release writes
# PLUGIN_VERSION here from package.json.
ARG SONARQUBE_VERSION="26.11.0.134175-community"
ARG SONARQUBE_DIGEST="sha256:65541accbdb92e27fe770fc6ba99988a3dd5a0c71ffafd67f88abfad366996fa"
ARG PLUGIN_VERSION="27.4.4"

# busybox's unzip unpacks the webapp, so nothing is installed.
FROM alpine:3.24@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf AS downloader
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
