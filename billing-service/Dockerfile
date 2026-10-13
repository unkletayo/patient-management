FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:25-jre AS runner

RUN useradd --system --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app

COPY --from=builder /app/target/*.jar ./app.jar

USER app

EXPOSE 4001 9001

ENTRYPOINT ["java", "-jar", "app.jar"]
