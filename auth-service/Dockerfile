FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY auth-service/pom.xml .
COPY auth-service/src ./src
RUN apt-get update && apt-get install -y maven && mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]