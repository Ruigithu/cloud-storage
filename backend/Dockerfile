 #Use an official Maven image to build the Spring Boot app
# 使用 Maven 3.8.4 + OpenJDK 17 作为构建环境
FROM maven:3.8.4-openjdk-17 AS build

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 并下载所有依赖（go-offline 让构建过程更快）
COPY pom.xml .
RUN mvn dependency:go-offline

# 复制源码并构建项目
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段，使用轻量级的 OpenJDK 17 镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制从构建阶段生成的 JAR 文件到当前阶段
COPY --from=build /app/target/cloud-storage-0.0.1-SNAPSHOT.jar app.jar

# 运行 Spring Boot 应用
CMD ["java", "-jar", "app.jar"]