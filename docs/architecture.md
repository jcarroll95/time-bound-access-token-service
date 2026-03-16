# Architecture

## Overview

The Time-Bound Access Token Service is a backend service that issues temporary access grants to protected resources and automatically expires them. It follows a standard Spring Boot layered architecture where each layer has a single responsibility and dependencies flow strictly downward.

## System Architecture

Requests flow through five layers, each separated by a clear boundary.

### Security filter boundary

Every incoming HTTP request passes through the JWT authentication filter before reaching any controller. The filter extracts the token from the `Authorization` header, validates the signature and expiration, and populates Spring's `SecurityContext` with the authenticated user's identity. If the token is missing or invalid, the request is rejected with a 401 before any business logic executes.

Public endpoints (`/auth/login`, `/health`) are excluded from this filter via the `SecurityFilterChain` configuration.

### Controller boundary (REST API)

Controllers translate between HTTP and application logic. They extract input from requests, delegate to a single service method, and translate the result into an HTTP response with the appropriate status code. Controllers do not contain business logic.

**AuthController** handles `POST /auth/login`. It receives credentials, delegates to AuthService, and returns a JWT.

**GrantController** handles all `/grants` endpoints. It extracts the authenticated user from the SecurityContext (never from client input), delegates to GrantService, and returns grant data or status codes.

**HealthController** handles `GET /health`. It returns application status with no authentication required.

### Service boundary (business logic)

Services contain all domain rules and are the only layer where business decisions are made.

**AuthService** verifies credentials against stored password hashes using Spring Security's `PasswordEncoder` and delegates token creation to a JWT utility class. It does not validate tokens on subsequent requests — that is the security filter's responsibility.

**GrantService** is the core of the application. It handles grant creation (computing `issuedAt` and `expiresAt` from the requested duration), retrieval (defining what "active" means: not revoked and not expired), revocation (soft delete via the `revoked` flag for audit trail purposes), and bulk expiration (marking all past-due grants as revoked). The scheduled background job calls into GrantService rather than containing expiration logic itself.

### Repository boundary (data access)

Repositories handle all database interaction through Spring Data JPA. Services call repository methods — they never write SQL or know which database is backing the application.

**UserRepository** supports lookup by username for the authentication flow.

**GrantRepository** supports lookup by ID, querying active grants by user, and finding expired but non-revoked grants for the scheduled job.

### Infrastructure boundary

PostgreSQL stores all application data. Docker provides the local database instance. Flyway manages database schema migrations. Application configuration is handled through `application.yml` profiles.

## Background Job

A Spring `@Scheduled` task periodically calls `GrantService.revokeExpiredGrants()` to mark expired grants as revoked. The job itself contains no business logic — it is a timer that triggers a service method.

## Key Design Decisions

**Soft deletes for revocation.** Revoking a grant sets `revoked = true` rather than deleting the row. This preserves an audit trail of who had access to what and when access was revoked, which is a core requirement for the eventual PAM system.

**User identity derived from token, not client input.** Endpoints never accept a userId in the request body. The server extracts identity from the JWT in the SecurityContext, preventing users from impersonating others.

**UUIDs for entity IDs.** Auto-incrementing integers allow clients to guess valid IDs by incrementing. UUIDs prevent enumeration, which matters for a security-adjacent application.

## Tech Stack

- Java 17 or 21
- Spring Boot
- Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL
- Docker (local database)
- Flyway (database migrations)
- Maven or Gradle
