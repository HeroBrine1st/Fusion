FROM gradle:7.2.0-jdk17 AS builder

COPY . /src/
WORKDIR /src
ENV GRADLE_OPTS "-Dorg.gradle.daemon=false"
RUN gradle shadowJar

FROM openjdk:17.0.1 AS runner

RUN mkdir /app
COPY --from=builder /src/build/libs/*.jar /app/Fusion.jar
WORKDIR /app

CMD ["java", "-jar", "Fusion.jar"]
