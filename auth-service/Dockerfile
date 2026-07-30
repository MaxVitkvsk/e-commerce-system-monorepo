FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
RUN apt-get update && apt-get install -y maven \
 && mvn -B -ntp -f auth-service/pom.xml dependency:go-offline -Dmaven.test.skip=true || true
COPY auth-service/src ./auth-service/src
RUN mvn -B -ntp -f auth-service/pom.xml package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]