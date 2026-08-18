FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY order-service/pom.xml order-service/pom.xml
RUN mvn -B -f order-service/pom.xml dependency:go-offline
COPY order-service/src order-service/src
RUN mvn -B -f order-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /application
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/order-service/target/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]