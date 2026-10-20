# ========================================================
# Dockerfile: position-cache-service (Quarkus 3.15 Fast-JAR)
# ========================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S recongroup && adduser -S reconuser -G recongroup

# Quarkus fast-jar packaging structure
COPY target/quarkus-app/lib/ /app/lib/
COPY target/quarkus-app/*.jar /app/
COPY target/quarkus-app/app/ /app/app/
COPY target/quarkus-app/quarkus/ /app/quarkus/

RUN chown -R reconuser:recongroup /app
USER reconuser

EXPOSE 8085

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/quarkus-run.jar"]
