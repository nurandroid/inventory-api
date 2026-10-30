FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/inventory-api-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]