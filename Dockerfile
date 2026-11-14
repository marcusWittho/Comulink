# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test \
    && find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:25-jre-noble AS runtime

RUN groupadd --system shortener \
    && useradd --system --gid shortener --home-dir /app --shell /usr/sbin/nologin shortener

WORKDIR /app
COPY --from=build --chown=shortener:shortener /workspace/app.jar /app/app.jar

USER shortener
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
