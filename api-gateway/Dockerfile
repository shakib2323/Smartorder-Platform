
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -pl api-gateway -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/api-gateway/target/*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8084} -jar app.jar"]
