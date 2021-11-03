FROM gradle:7.2.0-jdk17 AS builder

ENV GRADLE_OPTS "-Dorg.gradle.daemon=false"

RUN mkdir /src/
COPY gradle /src/
COPY settings.gradle.kts /src/
COPY build.gradle.kts /src/
COPY src /src/src
WORKDIR /src
RUN gradle shadowJar

FROM openjdk:17.0.1 AS runner

RUN mkdir /app
COPY --from=builder /src/build/libs/*.jar /app/Fusion.jar
WORKDIR /app

CMD ["java", "-jar", "Fusion.jar"]
