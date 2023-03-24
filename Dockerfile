FROM --platform=$BUILDPLATFORM gradle:7.2-jdk17 AS builder

ENV GRADLE_OPTS "-Dorg.gradle.daemon=false -Dorg.gradle.vfs.watch=false"

WORKDIR /src

COPY settings.gradle.kts /src/
COPY build.gradle.kts /src/

# Dependencies
RUN gradle classes

COPY src /src/src

RUN gradle shadowJar

FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app
COPY --from=builder /src/build/libs/*-full.jar /app/Fusion.jar

ENTRYPOINT ["java", "-jar", "Fusion.jar"]
