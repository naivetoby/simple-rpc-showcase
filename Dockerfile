FROM maven:3.9-eclipse-temurin-21 AS maven

FROM maven AS jvm-build
WORKDIR /project
COPY . .
ARG MODULE
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp -pl "$MODULE" -am package \
    && cp "$MODULE"/target/*.jar /app.jar

FROM eclipse-temurin:21-jre AS jvm
COPY --from=jvm-build /app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]

FROM ghcr.io/graalvm/native-image-community:25-ol9 AS native-build
COPY --from=maven /usr/share/maven /usr/share/maven
WORKDIR /project
COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    /usr/share/maven/bin/mvn -B -ntp -Pnative package

FROM oraclelinux:9-slim AS native
ARG MODULE
COPY --from=native-build /project/${MODULE}/target/${MODULE} /app
ENTRYPOINT ["/app"]
