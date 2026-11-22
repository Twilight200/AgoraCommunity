FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-noble
WORKDIR /app

RUN groupadd --system agora \
    && useradd --system --gid agora --create-home agora

COPY --from=build --chown=agora:agora \
    /workspace/target/agora-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER agora
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]