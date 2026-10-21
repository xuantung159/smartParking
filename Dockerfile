FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B package


FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system parking \
    && useradd --system --gid parking parking

COPY --from=build \
    --chown=parking:parking \
    /build/target/parking-0.0.1-SNAPSHOT.jar \
    app.jar

USER parking

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
