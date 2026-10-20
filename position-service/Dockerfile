# ========================================================
# Dockerfile: position-service (Spring Boot 3)
# ========================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S recongroup && adduser -S reconuser -G recongroup

COPY target/position-service-1.0.0-SNAPSHOT.jar app.jar

RUN chown -R reconuser:recongroup /app
USER reconuser

EXPOSE 8082

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
