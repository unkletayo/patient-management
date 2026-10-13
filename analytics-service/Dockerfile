FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:25-jre AS runner

RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app

COPY --from=builder --chown=app:app /app/target/*.jar ./app.jar

USER app

EXPOSE 4002

ENTRYPOINT ["java", "-jar", "app.jar"]
