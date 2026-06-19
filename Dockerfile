FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

RUN mkdir -p /root/.m2 && echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/central</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/pharmacy-service.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "pharmacy-service.jar"]
