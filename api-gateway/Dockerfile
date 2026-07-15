# Use a lightweight JRE image for running the app
FROM eclipse-temurin:17-jre-jammy

# Define the location of the compiled jar file
ARG JAR_FILE=target/*.jar

# Copy the jar file into the container as app.jar
COPY target/api-gateway-*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]