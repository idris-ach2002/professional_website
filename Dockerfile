FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Tests are executed by CI (clean verify); the production image only compiles/packages main sources.
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
