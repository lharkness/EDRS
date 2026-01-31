# Multi-stage build for Inventory Service
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM and common module
COPY pom.xml .
COPY common/pom.xml ./common/
COPY common/src ./common/src

# Copy service files
COPY inventory-service/pom.xml ./inventory-service/
COPY inventory-service/src ./inventory-service/src

# Build
RUN mvn clean package -pl inventory-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR
COPY --from=build /app/inventory-service/target/inventory-service-*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
