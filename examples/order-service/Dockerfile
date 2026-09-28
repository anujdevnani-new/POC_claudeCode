# Use OpenJDK 21 as the base image
FROM openjdk:21

ARG JAR_FILE_PATH
ARG JAR_FILE_NAME

# Set environment variables for runtime
ENV MICRO_SERVICE_NAME="order-service"
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

# Expose the application port
EXPOSE 9000

# Set the working directory
WORKDIR ${APP_HOME}

# Use shell form of ENTRYPOINT so variables are expanded
ENTRYPOINT java -jar /opt/${MICRO_SERVICE_NAME}/${JAR_FILE_NAME}
