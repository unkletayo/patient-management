###### Copyright © 2025 Code Jackal | Original Course Material by Chris Blakely

---
# Join the Discord Community

This source code is for the Java/Spring microservices course available on my 
YouTube channel. You can join the discord for help and discussion here:

https://discord.gg/nCrDnfCE



# Patient Management

Java microservices for managing patients, their billing accounts and an event stream.
Java 25 (LTS), Spring Boot 4.0.8, Spring Cloud 2025.1.3, Spring gRPC, PostgreSQL 17, Kafka.

| Service | Port | Role |
|---|---|---|
| `api-gateway` | 4004 | Single public entry point. Routes, validates the JWT, adds `X-User-Email` and `X-User-Role`. |
| `auth-service` | 4005 | Login and token validation. |
| `patient-service` | 4000 | Patient CRUD and search. Calls billing over gRPC, publishes Kafka events. |
| `billing-service` | 4001 REST, 9001 gRPC | Billing accounts. |
| `analytics-service` | 4002 | Kafka consumer, with a dead-letter topic. |
| `infrastructure` | | AWS CDK stack (also deployable to LocalStack). |
| `integration-tests` | | Black-box tests that call the running stack through the gateway. |

## Run locally

Needs Docker. Each service builds with its own Maven wrapper, so no local Maven or JDK is needed for this.

```bash
cp .env.example .env            # then set JWT_SECRET, e.g. openssl rand -base64 48
export JWT_SECRET=$(openssl rand -base64 48)
docker compose up --build -d
```

The gateway is then on http://localhost:4004. Cold JVMs are slow on small machines; the first
create-patient call can time out until billing and Kafka have warmed up.

Seed user: `testuser@test.com` / `password123`.

```bash
TOKEN=$(curl -s localhost:4004/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"testuser@test.com","password":"password123"}' | jq -r .responseBody.token)
curl -s localhost:4004/api/patients -H "Authorization: Bearer $TOKEN"
```

Ready-made requests are in `api-requests/` (set `{{token}}` by running login first).

## API

Every response uses one envelope:

```json
{ "requestSuccessful": true, "responseMessage": "success", "responseCode": "0", "responseBody": {} }
```

Errors set `requestSuccessful` to `false` and `responseCode` to the HTTP status as a string.
Validation errors (400) return a field to message map in `responseBody`.

Lists are paginated: `page` (0-based, default 0), `size` (default 20, max 100) and `sort=field,asc|desc`
(allow-listed fields only, otherwise 400). `responseBody` is:

```json
{ "content": [], "pageNumber": 0, "pageSize": 20, "totalElements": 0,
  "totalPages": 0, "first": true, "last": true, "empty": true }
```

| Method | Path | Notes |
|---|---|---|
| POST | `/auth/login` | Public. Returns a JWT. |
| GET | `/auth/validate` | Public route; the auth service checks the bearer token itself. |
| GET | `/api/patients` | Paginated. Filters: `name`, `email` (case-insensitive contains), `registeredFrom`, `registeredTo` (`yyyy-MM-dd`, inclusive). |
| GET | `/api/patients/{id}` | 404 if unknown. |
| POST | `/api/patients` | `registeredDate` optional (defaults to today). 409 on duplicate email. Creates the billing account. |
| PUT | `/api/patients/{id}` | 404 unknown, 409 duplicate email. |
| DELETE | `/api/patients/{id}` | 404 if unknown. |
| GET | `/api/billing-accounts` | Paginated. |
| GET | `/api/billing-accounts/{patientId}` | 404 if unknown. |
| PATCH | `/api/billing-accounts/{patientId}/status` | `{"status":"SUSPENDED"}`. ACTIVE to SUSPENDED or CLOSED, SUSPENDED to ACTIVE or CLOSED. CLOSED is final (409). |

OpenAPI JSON is served through the gateway at `/api-docs/patients` and `/api-docs/auth`.

## Configuration

| Variable | Service | Purpose |
|---|---|---|
| `JWT_SECRET` | auth | Required. Base64 signing key, at least 32 bytes. |
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | auth, patient, billing | Postgres. Falls back to in-memory H2 when unset. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | auth, patient, billing | Default `update`. |
| `SPRING_SQL_INIT_MODE` | auth, patient, billing | `always` runs `schema.sql` and `data.sql` (seed data). |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | patient, analytics | Kafka brokers. |
| `BILLING_SERVICE_ADDRESS`, `BILLING_SERVICE_GRPC_PORT` | patient | Billing gRPC target. |
| `BILLING_GRPC_DEADLINE` | patient | gRPC deadline. Compose sets `15s` for cold starts. |
| `AUTH_SERVICE_URL`, `PATIENT_SERVICE_URL`, `BILLING_SERVICE_URL` | gateway | Upstream URLs. |

## Build and test

```bash
cd <service> && ./mvnw verify     # unit and slice tests (needs JAVA_HOME on a JDK 25)
```

Integration tests need the compose stack running, otherwise they skip:

```bash
cd integration-tests && ./mvnw verify        # or: ./mvnw -Pit verify from the repo root
# other gateway: -Dgateway.url=https://host or GATEWAY_URL
```

## Deploy

- **Docker host:** `docker compose up --build -d` on any VM with Docker, behind a reverse proxy on port 4004.
- **AWS:** `infrastructure/` is a CDK app (Fargate, RDS, MSK). `cdk bootstrap` once, then `cdk deploy`.
  It also runs on LocalStack for local experiments. It has not been deployed end to end yet.

## Known limitations

- Creating a patient calls billing synchronously. If the patient transaction fails after billing
  succeeds, an orphan billing account remains. There is no compensation or outbox yet.
- The `patient.DLT` topic relies on broker auto-creation; the CDK stack does not provision it.
- Roles are forwarded by the gateway but not enforced by any service.
