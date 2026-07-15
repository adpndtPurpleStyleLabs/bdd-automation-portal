FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
COPY target/open-source-test-automation.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
