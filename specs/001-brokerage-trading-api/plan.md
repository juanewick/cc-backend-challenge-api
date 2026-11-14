# Implementation Plan: Portfolio, search and orders

**Branch**: `001-brokerage-trading-api` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-brokerage-trading-api/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

An investment account in pesos can be queried, instruments can be searched by ticker or name, and market or limit buys and sells can be sent. The portfolio and the reservations are computed from the orders already stored. The application is an HTTP service in Java 25 and Spring Boot 4.1.2, with hexagonal architecture, PostgreSQL 18.7 and a single start through Docker Compose. Calculation and service tests come first; integration runs against real containers.

## Technical Context

**Language/Version**: Java 25 (LTS)

**Primary Dependencies**: Spring Boot 4.1.2 (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`). Contract generated with springdoc-openapi 3.2.0 (`springdoc-openapi-starter-webmvc-ui`), the 3.2.x line built on Spring Boot 4.1.2. Test: `spring-boot-starter-test` (JUnit Jupiter 6.0.3, Mockito 5.23.0), `spring-boot-starter-webmvc-test`, Testcontainers PostgreSQL.

**Storage**: PostgreSQL 18.6, official image pinned (18.7 was not on Docker Hub yet). Schema and data come from the delivered SQL; Hibernate does not generate it.

**Testing**: JUnit Jupiter and Mockito for domain and service `*Test.java`. `*IT.java` with Testcontainers for the happy path and the errors. `ActuatorIT` has `@Order(1)` and runs before the other integration classes: it checks health, info and that no other Actuator endpoint is published. Maven Surefire runs the unit tests and Failsafe the integration ones, both from `mvn verify`.

**Target Platform**: Linux in a container. The host only needs Docker for the standalone start. The public port is 8080.

**Project Type**: HTTP service, a single Maven module, at the root of `cc-backend-challenge`.

**Performance Goals**: Search over the reference catalogue answers in under 1 second. The other operations of the spec are resolved within the same request.

**Constraints**: No authentication. No market simulation. No fractional shares. No new deposits or withdrawals. Fixed database schema. Prices in pesos. Design decisions the spec does not fix are written in `README.md`.

**Scale/Scope**: 4 accounts, 66 instruments and two days of prices. One transaction per account is enough to never spend the same available balance twice.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is still the template without ratified principles. There are no project gates to evaluate. The gate passes.

The constraints that do bind are those of the request and of [research.md](research.md): Java 25, Spring Boot 4.1.2, hexagonal, PostgreSQL in Docker, TDD, service tests with Mockito, `*IT.java` integration with Testcontainers, no authentication, and standalone start with Docker.

Post-design review: the model and the quickstart meet those constraints. The HTTP contract is not stored in the repository: springdoc generates it at startup. The gate still passes.

## Project Structure

### Documentation (this feature)

```text
specs/001-brokerage-trading-api/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md              # created by /speckit-tasks, not by this command
```

### Source Code (repository root)

```text
cc-backend-challenge/
├── pom.xml
├── Dockerfile
├── compose.yaml
├── README.md
├── src/main/java/com/cc/brokerage/
│   ├── BrokerageApplication.java
│   ├── domain/
│   │   ├── model/
│   │   ├── port/in/
│   │   ├── port/out/
│   │   └── service/          # pure calculation of portfolio, reservations and quantities
│   ├── application/          # use cases: get, search, place, cancel
│   └── adapter/
│       ├── in/web/           # controllers and HTTP translation
│       └── out/persistence/  # JPA entities and repositories
├── src/main/resources/
│   ├── application.yml
│   └── db/database.sql       # the delivered SQL, with no change of meaning
├── src/test/java/com/cc/brokerage/
│   ├── domain/service/       # *Test.java without mocks
│   ├── application/          # *Test.java with Mockito over the ports
│   └── adapter/in/web/       # *IT.java
└── postman/                  # optional, does not block the behaviour
```

**Structure Decision**: One Maven module at the repository root. The domain does not depend on Spring or JPA. Use cases depend on ports. REST and JPA are adapters. That keeps the requested hexagonal architecture visible, and service tests do not start the database.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | | |
