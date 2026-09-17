FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY payment-service/pom.xml payment-service/
RUN mvn -B -f payment-service/pom.xml dependency:go-offline
COPY payment-service/src payment-service/src
RUN mvn -B -f payment-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /application
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/payment-service/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]