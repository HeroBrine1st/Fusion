FROM gradle:7.2.0-jdk17

COPY . /src/
WORKDIR /src
ENV GRADLE_OPTS "-Dorg.gradle.daemon=false"
RUN gradle classes

CMD ["gradle", "run"]