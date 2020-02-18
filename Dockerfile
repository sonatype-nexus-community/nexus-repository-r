# declaration of NEXUS_VERSION must appear before first FROM command
# see: https://docs.docker.com/engine/reference/builder/#understand-how-arg-and-from-interact
ARG NEXUS_VERSION=latest

FROM maven:3-jdk-8-alpine AS build

COPY . /nexus-repository-r/
RUN cd /nexus-repository-r/; \
    mvn clean package -PbuildKar;

FROM sonatype/nexus3:$NEXUS_VERSION

ARG DEPLOY_DIR=/opt/sonatype/nexus/deploy/
USER root
COPY --from=build /nexus-repository-r/nexus-repository-r/target/nexus-repository-r-*-bundle.kar ${DEPLOY_DIR}
USER nexus
