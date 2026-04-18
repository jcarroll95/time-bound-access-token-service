FROM ubuntu:latest
LABEL authors="jamescarroll"

ENTRYPOINT ["top", "-b"]

# build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests

# rundocker run -p 8080:8080 --env-file .env tbats
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]