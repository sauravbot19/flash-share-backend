# --- Stage 1: Build Stage ---
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# 1. Copy the parent pom.xml from the root context
COPY pom.xml .

# 2. Copy the entire rider-service submodule folder
COPY rider-service ./rider-service

# 3. Build the specific submodule using the file flag (-f)
RUN mvn -f rider-service/pom.xml clean package -DskipTests

# --- Stage 2: Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the compiled JAR from the updated target location inside the build container
COPY --from=build /app/rider-service/target/rider-service-1.0.0-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8081

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]