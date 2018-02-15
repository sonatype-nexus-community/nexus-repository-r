ARG NEXUS_VERSION=3.8.0

FROM maven:3-jdk-8-alpine AS build
ARG NEXUS_VERSION=3.8.0
ARG NEXUS_BUILD=02

COPY . /nexus-repository-r/
RUN cd /nexus-repository-r/; sed -i "s/3.8.0-02/${NEXUS_VERSION}-${NEXUS_BUILD}/g" pom.xml; \
    mvn;

FROM sonatype/nexus3:$NEXUS_VERSION
ARG NEXUS_VERSION=3.8.0
ARG NEXUS_BUILD=02
# Will not seem to work in sed without some magick
ARG R_VERSION=1.0.1
ARG TARGET_DIR=/opt/sonatype/nexus/system/org/sonatype/nexus/plugins/nexus-repository-r/${R_VERSION}/
ARG FEATURES_XML_TARGET=/opt/sonatype/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/nexus-oss-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml
USER root
RUN mkdir -p ${TARGET_DIR}; \
    sed -i 's@nexus-repository-npm</feature>@nexus-repository-npm</feature>\n        <feature prerequisite="false" dependency="false">nexus-repository-r</feature>@g' ${FEATURES_XML_TARGET}; \
    sed -i 's@<feature name="nexus-repository-npm"@<feature name="nexus-repository-r" description="org.sonatype.nexus.plugins:nexus-repository-r" version="1.0.1">\n        <details>org.sonatype.nexus.plugins:nexus-repository-r</details>\n        <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-r/1.0.1</bundle>\n    </feature>\n    <feature name="nexus-repository-npm"@g' ${FEATURES_XML_TARGET};
COPY --from=build /nexus-repository-r/target/nexus-repository-r-${R_VERSION}.jar ${TARGET_DIR}
USER nexus
