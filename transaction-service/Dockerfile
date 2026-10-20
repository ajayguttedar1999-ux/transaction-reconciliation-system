# ========================================================
# Dockerfile: transaction-service (Spring Boot 3)
# ========================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for enterprise banking security
RUN addgroup -S recongroup && adduser -S reconuser -G recongroup

# Copy compiled jar
COPY target/transaction-service-1.0.0-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R reconuser:recongroup /app
USER reconuser

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
