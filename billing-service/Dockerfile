FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Cache dependencies in their own layer
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre AS runner

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=builder /app/target/billing-service-*.jar ./app.jar

USER app

EXPOSE 4001
EXPOSE 9001

ENTRYPOINT ["java", "-jar", "app.jar"]
