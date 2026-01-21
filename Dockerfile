####
# This Dockerfile is used to build a JVM-based container image for the Quarkus application.
####
FROM eclipse-temurin:17-jre-alpine

# Set the working directory
WORKDIR /work/

# Copy the application JAR
COPY build/quarkus-app/lib/ /work/lib/
COPY build/quarkus-app/*.jar /work/
COPY build/quarkus-app/app/ /work/app/
COPY build/quarkus-app/quarkus/ /work/quarkus/

# Set the user to non-root for security
RUN addgroup -g 1001 quarkus && \
    adduser -u 1001 -G quarkus -s /bin/sh -D quarkus && \
    chown -R quarkus:quarkus /work

USER quarkus

# Expose the application port
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/q/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/work/quarkus-run.jar"]
