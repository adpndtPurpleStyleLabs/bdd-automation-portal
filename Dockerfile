FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY target/*.jar app.jar
COPY src/test/resources/features /app/features
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
