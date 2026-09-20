FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn --batch-mode --no-transfer-progress clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:+UseSerialGC"

RUN mkdir -p /app/uploads && chown -R 10001:10001 /app

COPY --from=build --chown=10001:10001 /app/target/*.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]