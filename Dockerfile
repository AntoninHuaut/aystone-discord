FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY build.gradle.kts gradle.properties settings.gradle.kts ./
COPY gradle/ gradle/

RUN gradle dependencies --no-daemon

COPY src/ src/
RUN gradle shadowJar --no-daemon


FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S aystonediscord && adduser -S aystonediscord -G aystonediscord

WORKDIR /app

COPY --from=build /app/build/libs/* ./bot.jar

RUN apk add --no-cache tzdata

RUN chown -R aystonediscord:aystonediscord /app

USER aystonediscord

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD pgrep java || exit 1

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -cp bot.jar fr.maner.aystonediscord || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "bot.jar"]