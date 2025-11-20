# --- Base image with Java 21 ---
FROM eclipse-temurin:21-jdk-alpine

# --- Set working directory ---
WORKDIR /app

# --- Copy the built jar from target ---
COPY target/statistic-server-0.0.1-SNAPSHOT.jar app.jar

# --- Expose server port ---
EXPOSE 9090

# --- Run the application ---
ENTRYPOINT ["java", "-jar", "/app/app.jar"]