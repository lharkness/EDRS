# Multi-stage build for EDRS services
# This is a template Dockerfile - each service should have its own

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM and common module first (for dependency caching)
COPY pom.xml .
COPY common/pom.xml ./common/
COPY common/src ./common/src

# Copy service-specific files
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/pom.xml ./${SERVICE_NAME}/
COPY ${SERVICE_NAME}/src ./${SERVICE_NAME}/src

# Build the project
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
ARG SERVICE_NAME
COPY --from=build /app/${SERVICE_NAME}/target/${SERVICE_NAME}-*.jar app.jar

# Expose port (will be overridden by docker-compose)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
