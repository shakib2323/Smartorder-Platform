
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -pl auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/auth-service/target/*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8085} -jar app.jar"]
