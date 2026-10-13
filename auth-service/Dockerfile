FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Resolve dependencies first so this layer is cached until the pom changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre AS runner

RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app

COPY --from=builder --chown=app:app /app/target/auth-service-*.jar ./app.jar

USER app

EXPOSE 4005

ENTRYPOINT ["java", "-jar", "app.jar"]
