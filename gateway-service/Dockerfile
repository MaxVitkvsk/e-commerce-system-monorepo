FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY gateway-service/pom.xml gateway-service/
RUN mvn -B -f gateway-service/pom.xml dependency:go-offline
COPY gateway-service/src gateway-service/src
RUN mvn -B -f gateway-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /application
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/gateway-service/target/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]