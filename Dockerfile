FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Use the repository-pinned Maven Wrapper so Docker and GitHub compile with
# the same Maven distribution. Tests run in CI; the runtime image packages main sources only.
RUN chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

RUN mkdir -p /app/uploads && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /app/target/*.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
