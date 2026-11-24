FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /project
COPY . .
ARG MODULE
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl "$MODULE" -am package \
    && cp "$MODULE"/target/*.jar /app.jar

FROM eclipse-temurin:21-jre
COPY --from=build /app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
