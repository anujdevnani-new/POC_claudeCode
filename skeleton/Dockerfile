# Use OpenJDK 21 as the base image
FROM openjdk:21

ARG JAR_FILE_PATH
ARG JAR_FILE_NAME

# Set environment variables for runtime
ENV MICRO_SERVICE_NAME="example-service"
ENV MAINTAINER="maintaineri@agilery.ai"
ENV JAR_FILE_NAME=${JAR_FILE_NAME}
ENV JAR_FILE_PATH=${JAR_FILE_PATH}
ENV APP_HOME=/opt/${MICRO_SERVICE_NAME}

# Add maintainer metadata
LABEL maintainer="${MAINTAINER}"

# Create the application directory
RUN mkdir -p ${APP_HOME}

# Copy the JAR file to the application directory
COPY ${JAR_FILE_PATH} ${APP_HOME}/

# ── OTel agent ──────────────────────────────────────────────────────────────
# Pin to a specific version for reproducibility. Check latest at:
# https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases
ARG OTEL_AGENT_VERSION=2.26.1
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar \
    /otel/opentelemetry-javaagent.jar
RUN chmod 444 /otel/opentelemetry-javaagent.jar
# ────────────────────────────────────────────────────────────────────────────

# Copy JMX files
ENV JMX_ACCESS_FILE_PATH=${APP_HOME}/jmx/jmxremote.access
ENV JMX_PASSWORD_FILE_PATH=${APP_HOME}/jmx/jmxremote.password
COPY "src/main/resources/jmx/jmxremote.access" ${JMX_ACCESS_FILE_PATH}
COPY "src/main/resources/jmx/jmxremote.password" ${JMX_PASSWORD_FILE_PATH}

# Set JMX env variables
RUN chmod 400 ${APP_HOME}/jmx/jmxremote.*


# Expose the application port
EXPOSE 9000

# Set the working directory
WORKDIR ${APP_HOME}

# Use shell form of ENTRYPOINT so variables are expanded
ENTRYPOINT sh -c "java \
-javaagent:/otel/opentelemetry-javaagent.jar \
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9010 \
-Dcom.sun.management.jmxremote.rmi.port=9010 \
-Dcom.sun.management.jmxremote.authenticate=true \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.password.file=${JMX_PASSWORD_FILE_PATH} \
-Dcom.sun.management.jmxremote.access.file=${JMX_ACCESS_FILE_PATH} \
-Djava.rmi.server.hostname=localhost \
-jar ${APP_HOME}/${JAR_FILE_NAME}"
