
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -pl eureka-server -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/eureka-server/target/*.jar app.jar

EXPOSE 8762
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8762} -jar app.jar"]
