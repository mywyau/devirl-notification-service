# Use a minimal JRE runtime
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Copy in the staged app from sbt-native-packager
COPY target/docker/stage/opt /opt
COPY target/docker/stage/bin /bin
COPY target/docker/stage/conf /conf

# Expose service port
EXPOSE 8080

# Environment variable for prod config
ENV APP_ENV=prod

# Default command (native-packager generates this script)
CMD ["/bin/devirl-notification-service"]
