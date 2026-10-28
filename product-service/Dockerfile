
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -pl product-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/product-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8081} -jar app.jar"]
