# Integration Tests

This project has two integration tests written in `src/test/java/com/jcarroll95/tbats/`:

- `GrantLifecycleIntegrationTest` — login, create grant, list active, revoke, 
   verify removed.  
- `GrantExpirationIntegrationTest` — verifies the scheduled `GrantExpirationJob` 
   flips expired grants to revoked.  

Both use `@SpringBootTest(webEnvironment = RANDOM_PORT)` and exercise the full
HTTP → security filter → controller → service → JPA → Postgres path.

## Runtime status: disabled

Tests are currently `@Disabled` due to runtime environment incompatibilities
encountered with multiple test database approaches:

**Testcontainers attempt.** Initial design used Testcontainers PostgreSQL for
real-Postgres fidelity. Docker Desktop on Apple Silicon returned a stub socket
to Testcontainers' Java client, producing repeated `BadRequestException: Status 400`
failures. Switching to Colima resolved socket discovery but exposed a docker-java
client API version mismatch (Testcontainers 1.20.x bundles a client that
negotiates Docker API 1.32, but modern daemons require 1.44+). Upgrading
Testcontainers to 2.x resolved the mismatch but introduced a JUnit 4/5
compatibility regression with IntelliJ.

**Zonky embedded Postgres attempt.** Switching from Testcontainers to Zonky
embedded-database-spring-test removed the Docker dependency, but Zonky's
autoconfiguration interaction with Spring Boot 4's Flyway autoconfiguration
produced bean wiring failures even with the documented setup.

**Resolution.** Integration tests are disabled for now.
Re-enabling will follow the next Spring Boot patch release, ecosystem catch-up
from Testcontainers/Zonky, or my follow-on project's more robust infrastructure
build-out. 