# SonarQube with this plugin and its webapp, built from source.
#
# SONARQUBE_VERSION and SONARQUBE_DIGEST name one image and change together, as in .env. PLUGIN_VERSION is the
# jar's version, which the release writes here from package.json; the build names the jar after it and the
# entrypoint copies it by that name.
ARG SONARQUBE_VERSION="26.11.0.134175-community"
ARG SONARQUBE_DIGEST="sha256:65541accbdb92e27fe770fc6ba99988a3dd5a0c71ffafd67f88abfad366996fa"
ARG PLUGIN_VERSION="27.4.4"
ARG WORKDIR="/home/build/project"

# The Gradle the wrapper pins, on the JDK gradle-daemon-jvm.properties names, running the suite before it packages the jar.
# verifySonarqubeVersion compares .env, README.md and these Dockerfiles, none of which this stage copies;
# CI's Gradle build runs it.
FROM eclipse-temurin:25-jdk-noble@sha256:80f67fb3d6f76e2d817d98a4cc2a9cf05f054d4e6adc5e2144bde671d722e63f AS builder
ARG PLUGIN_VERSION
ARG WORKDIR

WORKDIR ${WORKDIR}
COPY gradlew build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src
RUN sh gradlew build -x verifySonarqubeVersion -Pversion="${PLUGIN_VERSION}" --no-daemon


# bash and git, which setup.sh needs to copy the addons and apply their patches, ship in the full Debian image.
# setup.sh names PLUGIN_VERSION, the jar's version, as the webapp's release.
FROM node:22.23-bookworm@sha256:e13726ca604184d7a91fcacab1fc09eefd642dc28e79ac34e85540dd93b78cfb AS webapp-builder
ARG PLUGIN_VERSION

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
