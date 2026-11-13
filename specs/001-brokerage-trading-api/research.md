# Research: Portfolio, search and orders

## Spring Boot 4.1 and Java 25

**Decision**: Java 25 and Spring Boot 4.1.2, with Maven and the `spring-boot-starter-parent` parent.

**Rationale**: The request fixes Java 25 (LTS) and Spring Boot 4.1.x. The 4.1 line published as of 2026-11-12 is 4.1.2 (October 22, 2026). It needs Java 17 at least and supports Java 25. The web starter of this generation is `spring-boot-starter-webmvc`; the MVC test one is `spring-boot-starter-webmvc-test`.

**Alternatives considered**: Spring Boot 4.1.0 and 4.1.1 are behind on patches. Spring Boot 4.2 is still a release candidate. Gradle also works; Maven is the parent's default and is enough for a single module.

## The JUnit that Spring Boot 4.1 brings

**Decision**: Use JUnit Jupiter 6.0.3 and Mockito 5.23.0, the versions `spring-boot-starter-test` 4.1.2 manages. Service tests use `@ExtendWith(MockitoExtension.class)` and mocks of the ports. Pure calculation tests need no mocks.

**Rationale**: The request names JUnit 5. Spring Boot 4.1 no longer manages Jupiter 5.x: the test starter brings `junit-jupiter` 6.0.3, the next line of the same API (`@Test`, `@BeforeEach`, assertions and `MockitoExtension`). Pinning Jupiter 5 would clash with the BOM and with Boot 4.1 test support.

**Alternatives considered**: Moving the project down to Spring Boot 3.5 to keep the JUnit 5 artifact. That breaks the Spring Boot 4.1.x requirement.

## Data access

**Decision**: Spring Data JPA only as an outbound adapter. Hibernate does not generate the schema (`ddl-auto: none`). The delivered SQL is loaded as is. Portfolio, reservation and share-count calculation lives in a pure domain service, not in scattered queries.

**Rationale**: The request allows Spring Data or plain SQL. The schema already exists, is small and must be respected. JPA maps the four tables and the domain stays testable without a database. Unquoted PostgreSQL columns end up lower case (`accountnumber`, `instrumentid`, `previousclose`); the mapping uses those real names.

**Alternatives considered**: `JdbcTemplate` or jOOQ for everything. They work, but JPA covers order and user CRUD with less code, and the calculation stays outside the ORM anyway. A Hibernate-generated schema would change the delivered model.

## PostgreSQL in Docker

**Decision**: Official image pinned to `postgres:18.6`. Patch 18.7 came out on 2026-11-12, but that tag was not on Docker Hub yet. `latest` is not used.

**Rationale**: PostgreSQL 18.7 came out on 2026-11-12, with that date's security patches. The `latest` image seen the same day still pointed to 18.6. The request asks for the latest patched stable version, not a floating tag.

**Alternatives considered**: PostgreSQL 17.x is patched, but it is not the latest stable. Leaving `postgres:18` also floats between patches.

## Standalone application

**Decision**: `compose.yaml` starts two services: `postgres` and `app`. The app is built with a two-stage Dockerfile (`eclipse-temurin:25-jdk` to build, `eclipse-temurin:25-jre` to run). The reference SQL comes in through `docker-entrypoint-initdb.d`. The app waits for the Postgres healthcheck and publishes port 8080.

**Rationale**: The request asks for a Dockerized app that starts by itself, with the database and the data, without installing Java or PostgreSQL on the host.

**Alternatives considered**: Only the database in Docker and the app on the host. It does not meet the single start. A single image with Postgres and Java inside mixes two processes and worsens the lifecycle.

## LocalStack and Testcontainers

**Decision**: The `*IT.java` classes use Testcontainers. The PostgreSQL container is the real database for happy and error scenarios. The same harness starts LocalStack with the Testcontainers module and checks that it answers; no business operation calls AWS.

**Rationale**: The request names both tools. LocalStack emulates AWS, and this application has no S3, queues or any other AWS service. Using it as the database would be wrong. Leaving it out would break the written request. Starting it and checking it meets the requirement without inventing a product dependency.

**Alternatives considered**: Leaving LocalStack out. Storing orders in S3 just to have something to test against LocalStack.

## Generated contract and Actuator

**Decision**: springdoc-openapi 3.2.0, artifact `springdoc-openapi-starter-webmvc-ui`. The OpenAPI is served at `/v3/api-docs` and `/v3/api-docs.yaml`, with the UI at `/swagger-ui.html`. No copy stays in `specs`. Actuator exposes only `health` and `info` (`management.endpoints.web.exposure.include=health,info`). Info publishes `info.app.name` with the application name. Health shows no components.

**Rationale**: The springdoc matrix assigns 3.2.x to Spring Boot 4.1.x. 3.2.0 is built on Spring Boot 4.1.2, the same version as this service. 3.0.x is Spring Boot 4.0 and 2.x is Spring Boot 3. The generated document describes the controllers that actually answer. A hand-written YAML drifts. Health and info are enough to know whether the process and the database answer and whether the process is `brokerage-api`. The rest of Actuator (`env`, `beans`, `mappings`, `heapdump`) stays closed because there is no authentication.

**Alternatives considered**: Keeping `specs/001-brokerage-trading-api/contracts/http-api.yaml` as the source and generating code from it at build time. It duplicates the contract. Exposing every Actuator endpoint with `include: "*"`. It publishes configuration and heap dumps on an API without authentication.

## Identity and HTTP contract

**Decision**: The account is identified by `accountNumber` in the path. The instrument, by `ticker`. The stored order, by its id. There is no authentication and no Spring Security.

**Rationale**: That is how the specification defined it. The ticker is what the search returns. The order id exists in the model and makes cancellation unique.

**Alternatives considered**: Identifying the user by the internal numeric id. It leaks the model more, since the account number is already the business identifier.

## Concurrency of the available balance

**Decision**: Placing or cancelling an order runs in a transaction that locks the user row before computing the available balance.

**Rationale**: Two simultaneous buys must not use the same cash twice. The reference volume is four accounts, so one lock per account is enough.

**Alternatives considered**: Optimistic locking without a user row. It makes it easier to miss a race case in the integration test.
