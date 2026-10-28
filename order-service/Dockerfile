
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -pl order-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/order-service/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8083} -jar app.jar"]
