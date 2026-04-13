# Time-Bound Access Token Service

## Overview

A small but realistic backend service that issues temporary access grants and automatically expires them. This is a simplified precursor to a PAM (Privileged Access Management) system, directly rehearsing concepts like authentication, authorization, time-bounded permissions, and revocation.

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

The service follows a layered Spring Boot architecture where dependencies flow strictly downward: controllers handle HTTP translation, services contain all business logic, and repositories manage data access. Authentication is handled as a cross-cutting concern via a JWT security filter that runs before any controller logic, decoupling identity verification from the business layer. See [docs/architecture.md](docs/architecture.md) for a high-level overview.

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

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL
- Docker (local database)
- Maven

## Operational Concerns

- Environment configuration via `application.yml` profiles
- Structured logging
- Basic error handling with meaningful error responses
- Database migrations (Flyway)
- Background job using Spring's `@Scheduled` to periodically invalidate expired grants

## Testing Strategy (Integrated From the Start)

- **Repository layer:** `@DataJpaTest` for JPA entity and query validation
- **Service layer:** Unit tests with mocked dependencies
- **Controller layer:** `@WebMvcTest` with MockMvc for endpoint behavior
- **Integration:** `@SpringBootTest` for end-to-end flows against a test database

## How to Run Locally

### Prerequisites  
- Java 21  
- Docker  

### Environment variables  
Export these before running. The app fails to start if either is missing.  

export JWT_SECRET="yoursecret"  
export SPRING_DATASOURCE_PASSWORD="yourpw"  
export SPRING_PROFILES_ACTIVE="local"  

### Run  
    docker compose up -d  
    ./mvnw spring-boot:run  

### Seeded users (local profile only)
- testuser / user123 (USER)
- admin / admin123 (ADMIN)

## Dependency Management

Spring Boot 4.0.5 is the latest patch release at time of writing and includes
current security patches for Spring Framework, Spring Security, Jackson, and
Tomcat. IntelliJ's Package Checker reports no known CVEs against the resolved
dependency tree.

## Testing

- Unit tests for security utilities and core logic — runnable with `./mvnw test`
- Integration tests authored for full lifecycle and expiration paths
  (currently `@Disabled` — see `docs/integration-tests.md`)

## Bridge to PAM

Once the MVP is achieved, the next project iteration will develop privileged access  
management features including:

- JIT access approvals
- RBAC policies
- Audit logs
- Revocation workflows
- Queue-based revocation
