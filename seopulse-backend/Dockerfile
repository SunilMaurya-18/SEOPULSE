# syntax=docker/dockerfile:1

# ---- Build ------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependencies change far less often than sources, so they get their own layer
# (also reused from the GitHub Actions layer cache).
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY lombok.config ./
COPY src src
RUN ./mvnw -B -q package -DskipTests \
    && cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Runtime ----------------------------------------------------------------
FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system seopulse \
    && useradd --system --gid seopulse --no-create-home --shell /usr/sbin/nologin seopulse

WORKDIR /application

# Least to most frequently changing, so image pulls only fetch the app layer.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8082 \
    MANAGEMENT_PORT=8081

USER seopulse
EXPOSE 8082 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=4 \
    CMD curl -fsS "http://localhost:${MANAGEMENT_PORT}/actuator/health/liveness" > /dev/null || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "application.jar"]
