
FROM maven:3.8.4-openjdk-17 AS build

WORKDIR /app

COPY backend/pom.xml .
RUN mvn dependency:go-offline

COPY backend/src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/cloud-storage-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]