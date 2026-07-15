FROM eclipse-temurin:25-jdk
WORKDIR /app

# Install Allure
RUN apt-get update && apt-get install -y wget unzip \
    && wget https://github.com/allure-framework/allure2/releases/download/2.32.0/allure-2.32.0.tgz \
    && tar -zxvf allure-2.32.0.tgz -C /opt/ \
    && ln -s /opt/allure-2.32.0/bin/allure /usr/bin/allure \
    && rm allure-2.32.0.tgz \
    && apt-get clean


COPY target/*.jar app.jar
COPY src/test/resources/features /app/features
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
