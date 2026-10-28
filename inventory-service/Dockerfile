
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -pl inventory-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/inventory-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8082} -jar app.jar"]
