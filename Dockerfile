FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package && mvn -B dependency:copy -Dartifact=org.postgresql:postgresql:42.7.5 -DoutputDirectory=/build/driver
FROM quay.io/wildfly/wildfly:35.0.1.Final-jdk17
COPY --from=build --chown=jboss:root /build/driver/postgresql-42.7.5.jar /opt/jboss/wildfly/modules/org/postgresql/main/postgresql.jar
COPY --chown=jboss:root docker/module.xml /opt/jboss/wildfly/modules/org/postgresql/main/module.xml
COPY --chown=jboss:root docker/configure.cli /tmp/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure.cli
COPY --from=build --chown=jboss:root /build/target/movie-lab.war /opt/jboss/wildfly/standalone/deployments/movie-lab.war
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
