# SonarQube with this plugin and its webapp, built from source.
#
# SONARQUBE_VERSION and SONARQUBE_DIGEST name one image and change together, as in .env. PLUGIN_VERSION is the
# jar's version, which the release writes here from package.json; the build names the jar after it and the
# entrypoint copies it by that name.
ARG SONARQUBE_VERSION="26.9.0.129388-community"
ARG SONARQUBE_DIGEST="sha256:c0f1160bccfa435db4168c2d7df69af3c3ea7bfeb87395613014048fb33a68c1"
ARG PLUGIN_VERSION="27.0.2"
ARG WORKDIR="/home/build/project"

# The Gradle the wrapper pins, on the JDK the plugin targets, running the suite before it packages the jar.
FROM eclipse-temurin:21-jdk-noble@sha256:70898f0f893a6b772a0f29834d8b022e3ac20b6a0c33a922973cf66342ef56be AS builder
ARG PLUGIN_VERSION
ARG WORKDIR

WORKDIR ${WORKDIR}
COPY gradlew build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src
RUN sh gradlew build -Pversion="${PLUGIN_VERSION}" --no-daemon


# bash, which setup.sh needs, ships in the Debian image.
FROM node:22.16-bookworm-slim@sha256:048ed02c5fd52e86fda6fbd2f6a76cf0d4492fd6c6fee9e2c463ed5108da0e34 AS webapp-builder

WORKDIR /home/build
COPY sonarqube-webapp ./sonarqube-webapp
COPY sonarqube-webapp-addons ./sonarqube-webapp-addons
RUN bash sonarqube-webapp-addons/setup.sh

WORKDIR /home/build/sonarqube-webapp
RUN yarn install && yarn nx run sq-server:build


FROM sonarqube:${SONARQUBE_VERSION}@${SONARQUBE_DIGEST}
ARG PLUGIN_VERSION
ARG WORKDIR

COPY --from=builder --chown=sonarqube:root ${WORKDIR}/build/libs/haembina-branch-analysis-${PLUGIN_VERSION}.jar /opt/sonarqube/lib/haembina-branch-analysis/
COPY --chown=sonarqube:root --chmod=755 docker/branch-analysis-entrypoint.sh /opt/sonarqube/docker/branch-analysis-entrypoint.sh

RUN chmod -R 770 /opt/sonarqube/web && rm -rf /opt/sonarqube/web/*
COPY --from=webapp-builder --chown=sonarqube:root --chmod=550 /home/build/sonarqube-webapp/apps/sq-server/build/webapp /opt/sonarqube/web

ENV PLUGIN_VERSION=${PLUGIN_VERSION}
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/haembina-branch-analysis-${PLUGIN_VERSION}.jar=ce"
ENTRYPOINT ["/opt/sonarqube/docker/branch-analysis-entrypoint.sh"]
