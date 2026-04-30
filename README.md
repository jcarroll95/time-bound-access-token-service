# Time-Bound Access Token Service

[![CI](https://github.com/jcarroll95/time-bound-access-token-service/actions/workflows/ci.yml/badge.svg)](https://github.com/jcarroll95/time-bound-access-token-service/actions/workflows/ci.yml)
[![CD](https://github.com/jcarroll95/time-bound-access-token-service/actions/workflows/cd.yml/badge.svg)](https://github.com/jcarroll95/time-bound-access-token-service/actions/workflows/cd.yml)

## Live Demo: [https://api.loadbearing.dev/](https://api.loadbearing.dev/)

Use the test user credentials to log in and create timed access grants.

- **testuser / user123** (USER role)
- **admin / admin123** (ADMIN role)

![Demo access grant management](./docs/tbats-demo.gif)

## Overview

A small but realistic backend service that issues temporary access grants and automatically expires them. This is a  
simplified precursor to a PAM (Privileged Access Management) system, directly rehearsing concepts like authentication,  
authorization, time-bounded permissions, and revocation.

## Objectives

1. REST API design and controllers
2. Persistence with JPA/Hibernate and SQL
3. Authentication via JWT
4. Clear separation of concerns and defined boundaries
5. Testing at every layer

## Core Behavior

- Users request access to a protected resource.
- If approved, the system issues a token valid for a limited duration.
- The system tracks and revokes expired grants.

## Architecture

<img src="docs/access_token_service_architecture.svg" width="600" alt="Access Token Service Architecture">

The service follows a layered Spring Boot architecture where dependencies flow strictly downward: controllers handle HTTP  
translation, services contain all business logic, and repositories manage data access. Authentication is handled as a  
cross-cutting concern via a JWT security filter that runs before any controller logic, decoupling identity verification
from the business layer. See [docs/architecture.md](docs/architecture.md) for a high-level overview.

## API Surface (6 Endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | Authenticate and receive a JWT |
| POST | `/grants` | Request a new access grant |
| GET | `/grants/{id}` | Retrieve a specific grant |
| GET | `/grants/active` | List all currently active grants |
| DELETE | `/grants/{id}` | Revoke a specific grant |
| GET | `/health` | Application health check |

See [docs/api-contracts.md](docs/api-contracts.md) for contract definitions.

## Domain Model (3 Entities)

User and AccessGrant are the primary entities. See [docs/entity-relationships.md](docs/entity-relationships.md).

## Tech Stack

**Application**
- Java 21
- Spring Boot 4.0.5
- Spring Security (JWT authentication, BCrypt password hashing)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Flyway (database migrations)
- Bucket4j (token-bucket rate limiting)
- Maven

**Infrastructure & Deployment**
- Docker (multi-stage build)
- AWS ECS Fargate (compute)
- AWS RDS PostgreSQL (managed database)
- AWS ECR (container registry)
- AWS ALB (load balancer, TLS termination)
- AWS ACM (SSL/TLS certificate, auto-renewing)
- AWS Secrets Manager (credential injection)
- GitHub Actions (CI/CD with OIDC authentication to AWS)

## Operational Concerns

- HTTPS with TLS termination at the ALB, HTTP-to-HTTPS redirect via 301; ACM-managed certificate with auto-renewal
- ALB target group health checks tuned for Spring Boot startup time (`/health`, 10s interval, 2/2 thresholds, 120s grace period). This prevents the slow-booting Spring app from being killed before accumulating enough health checks.
- Rolling deployments with `minimumHealthyPercent: 100`, `maximumPercent: 200`. This way the new task starts and passes ALB health checks before the old task drains, so requests are never interrupted
- Rate limiting on authentication and grant creation endpoints (Bucket4j token-bucket)
- Security group chaining: ECS tasks only accept traffic through the ALB, not directly from the internet
- GitHub Actions authenticates to AWS via OIDC federation with short-lived STS credentials per workflow run, no long-lived access keys stored as repo secrets, full CloudTrail audit trail
- CD pipeline gates on `wait-for-service-stability: true` README.md CD badge going green means ECS confirmed a healthy rollout, not just that the image was pushed
- CI runs tests on every PR to `main`; CD runs the full test suite, builds, pushes to ECR, and deploys only on merge
- Environment configuration via `application.yml` profiles (`local`, `demo`)
- Structured logging
- Error handling with consistent JSON error responses
- Database migrations managed by Flyway
- Background job using Spring's `@Scheduled` to periodically revoke expired grants

## Testing Strategy (Integrated From the Start)

- **Repository layer:** `@DataJpaTest` for JPA entity and query validation
- **Service layer:** Unit tests with mocked dependencies
- **Controller layer:** `@WebMvcTest` with MockMvc for endpoint behavior
- **Integration:** `@SpringBootTest` with Testcontainers for full lifecycle and expiration flows against a real PostgreSQL instance

## How to Run Locally

### Setup

Copy `example.env` to `.env` and fill in values:

```bash
cp example.env .env
```

#### Option A: Run app on host machine, Postgres in Docker

Start PostgreSQL:

```bash
docker compose up -d
```

Export environment variables and run the application with the `local` profile
(which loads seeded test users):

```bash
set -a && source .env && set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The service listens on `http://localhost:8080`.

#### Option B: Run app + Postgres both in Docker (recommended for parity)

Use Docker Compose to run both services:

```bash
docker compose up --build
```

The app container uses `DB_HOST=postgres` and `DB_PORT=5432` internally so it can
reach the Postgres service over the Docker network.

#### Option C: Run app image with `docker run` against compose Postgres

If Postgres is started by compose and the app is started separately with `docker run`,
you must attach the app container to the same compose network and use the Postgres
service name on the container port:

```bash
docker compose up -d postgres 
docker run --rm -p 8080:8080 --env-file .env \
  --network tbats_default \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  tbats
```

Why: inside a container, `localhost` points to that same container, not to your Mac
and not to the Postgres container.

### Deployment

The live demo runs on AWS ECS Fargate behind an Application Load Balancer at `api.loadbearing.dev`. The CD pipeline handles deployment automatically on merge to `main`:

1. Tests run against a Testcontainers PostgreSQL instance where full schema and lifecycle are exercised before any image is built.
2. AWS credentials are obtained via [GitHub OIDC federation](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services). No long-lived AWS access keys live in repo secrets; each workflow run gets short-lived STS credentials scoped to a single deploy role.
3. Docker image is built (`--platform linux/amd64`) and pushed to ECR, tagged with both the commit SHA and `latest`.
4. Task definition is rendered using [`amazon-ecs-render-task-definition`](https://github.com/aws-actions/amazon-ecs-render-task-definition) which substitutes the new image tag into the committed `task-definition.json`, so everything else (env vars, secrets, IAM roles, CPU/memory) stays in version control alongside the code.
5. Service is updated using [`amazon-ecs-deploy-task-definition`](https://github.com/aws-actions/amazon-ecs-deploy-task-definition) with `wait-for-service-stability: true`. The workflow only succeeds after ECS confirms the new task is healthy and registered with the ALB target group.
6. ECS performs a rolling deployment that overlaps the new task with the old one. The old task only drains once the new one is passing ALB health checks.

Required environment variables at deploy time:

- `JWT_SECRET`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `DB_HOST` (RDS endpoint)
- `DB_PORT` (usually `5432`)
- `SPRING_PROFILES_ACTIVE` (`demo` for the live instance)

Secrets are stored in AWS Secrets Manager and injected into the ECS task definition. Non-secret environment variables are set directly on the task definition.

> Note: `SPRING_DATASOURCE_USER` is not a Spring Boot datasource property.
> Use `POSTGRES_USER` or `SPRING_DATASOURCE_USERNAME`.

### Example requests

The same API backing the demo site can be called directly: 

Log in as the seeded test user and capture the JWT:

```bash
TOKEN=$(curl -s -X POST https://api.loadbearing.dev/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"user123"}' \
  | jq -r .token)
```

(If you don't have jq installed, just curl POST the credentials and copy the token from the response.)

Create a grant:

```bash
curl -X POST https://api.loadbearing.dev/grants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"resourceName":"production-db","durationMinutes":60}'
```

List active grants:

```bash
curl https://api.loadbearing.dev/grants/active \
  -H "Authorization: Bearer $TOKEN"
```

Revoke a grant (use the `id` returned from the create call):

```bash
curl -X DELETE https://api.loadbearing.dev/grants/{id} \
  -H "Authorization: Bearer $TOKEN"
```

For local development, replace `https://api.loadbearing.dev` with `http://localhost:8080`.

### Seeded users

| Username | Password | Role | Available in |
|----------|----------|------|-------------|
| testuser | user123 | USER | `local`, `demo` |
| admin | admin123 | ADMIN | `local`, `demo` |

## Dependency Management

Spring Boot 4.0.5 is the latest patch release at time of writing and includes
current security patches for Spring Framework, Spring Security, Jackson, and
Tomcat. IntelliJ's Package Checker reports no known CVEs against the resolved
dependency tree.

## Known Limitations

These are deliberate scope decisions for an MVP-grade portfolio service. Each is something a production deployment of this system would tighten:

- **Single-AZ task placement.** The service runs tasks in one subnet (`us-east-1e`) even though the ALB spans two AZs. Acceptable at `desiredCount: 1`; multi-AZ would mean adding the second subnet to the service's network configuration.
- **Tasks share a security group with RDS.** Currently the default VPC security group. Should be a dedicated SG whose only inbound rule is from the ALB SG on port 8080.
- **Deployment circuit breaker disabled.** `deploymentCircuitBreaker.enable: false`. With it enabled and `rollback: true`, failed deployments would auto-revert to the prior task definition.
- **No autoscaling.** `desiredCount` is fixed at 1. A real workload would use target-tracking on CPU or request count.
- **Tasks have public IPs.** Required for ECR pull and outbound calls from public subnets. A private-subnet deployment with a NAT gateway, or VPC endpoints for ECR, Secrets Manager, CloudWatch, is a more locked-down posture.
- **No WAF in front of the ALB.** Adding AWS WAF with the AWS-managed rule sets would be the next layer of defense.

## Bridge to PAM

Once the MVP is achieved, the next project iteration will develop privileged access  
management features including:

- JIT access approvals
- RBAC policies
- Audit logs
- Revocation workflows
- Queue-based revocation