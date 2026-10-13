# Infrastructure

AWS CDK (Java 25) stack that deploys the patient-management services to LocalStack:
VPC, one Postgres instance each for auth, billing and patient services, an MSK cluster,
an ECS Fargate cluster (auth, billing, analytics, patient, api-gateway) and an ALB in front of the gateway.

## Synthesize

Requires JDK 25 and Node.js (used by the CDK jsii runtime). No Docker or LocalStack needed.

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
./mvnw compile exec:java
```

This writes `cdk.out/localstack.template.json` (git-ignored).

## Deploy to LocalStack

Start LocalStack (edge port 4566), build and tag the service images locally
(`auth-service`, `billing-service`, `analytics-service`, `patient-service`, `api-gateway`), then:

```sh
./localstack-deploy.sh          # LOCALSTACK_ENDPOINT overrides http://localhost:4566
```

The script deletes any existing `patient-management` stack (ignored if absent), deploys the template
and prints the load balancer DNS name.

## Secrets and environment

- `JWT_SECRET` is not stored in source. The stack creates a Secrets Manager secret
  (`patient-management/jwt-secret`) with a generated 64-char alphanumeric value
  (base64-decodable, 384 bits) and injects it into auth-service as an ECS secret.
- Database credentials are generated per instance (`admin_user`) and passed to each service via
  `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`.
- api-gateway runs with `SPRING_PROFILES_ACTIVE=prod` and `AUTH_SERVICE_URL`.
- Services get `SPRING_KAFKA_BOOTSTRAP_SERVERS` pointing at LocalStack's MSK ports.
- Fargate tasks are sized 512 CPU / 1024 MiB for the JVM services.
