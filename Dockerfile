FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=target/dependency

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app
COPY --from=build --chown=app:app /workspace/target/core-banking-ledger-1.0.0.jar /app/app.jar
COPY --from=build --chown=app:app /workspace/target/dependency/ /app/dependency/

USER app
ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/dependency/*", "com.bank.Main"]
