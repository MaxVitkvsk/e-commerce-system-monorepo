FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY user-service/pom.xml user-service/pom.xml
RUN mvn -B -f user-service/pom.xml dependency:go-offline
COPY user-service/src user-service/src
RUN mvn -B -f user-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /application
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/user-service/target/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]