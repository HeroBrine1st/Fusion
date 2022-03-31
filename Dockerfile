FROM gradle:7.2.0-jdk16 AS builder

ENV GRADLE_OPTS "-Dorg.gradle.daemon=false"

WORKDIR /src

COPY settings.gradle.kts /src/
COPY build.gradle.kts /src/

# Dependencies
RUN gradle classes

COPY src /src/src

RUN gradle shadowJar

FROM openjdk:16.0.2 AS runner

RUN mkdir /app
COPY --from=builder /src/build/libs/*.jar /app/Fusion.jar
WORKDIR /app

CMD ["java", "-jar", "Fusion.jar"]
