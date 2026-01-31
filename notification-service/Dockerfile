# Multi-stage build for Notification Service
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM and common module
COPY pom.xml .
COPY common/pom.xml ./common/
COPY common/src ./common/src

# Copy service files
COPY notification-service/pom.xml ./notification-service/
COPY notification-service/src ./notification-service/src

# Build
RUN mvn clean package -pl notification-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR
COPY --from=build /app/notification-service/target/notification-service-*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
