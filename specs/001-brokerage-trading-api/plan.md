# Implementation Plan: Portfolio, búsqueda y órdenes

**Branch**: `001-brokerage-trading-api` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-brokerage-trading-api/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Una cuenta de inversión en pesos se consulta, se buscan activos por ticker o nombre, y se envían compras y ventas de mercado o límite. El portfolio y las reservas se calculan desde las órdenes ya guardadas. La aplicación es un servicio HTTP en Java 25 y Spring Boot 4.1.2, con arquitectura hexagonal, PostgreSQL 18.7 y un arranque único por Docker Compose. Los tests de cálculo y de servicios van primero; la integración corre contra contenedores reales.

## Technical Context

**Language/Version**: Java 25 (LTS)

**Primary Dependencies**: Spring Boot 4.1.2 (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`). Contrato generado con springdoc-openapi 3.2.0 (`springdoc-openapi-starter-webmvc-ui`), la línea 3.2.x construida sobre Spring Boot 4.1.2. Test: `spring-boot-starter-test` (JUnit Jupiter 6.0.3, Mockito 5.23.0), `spring-boot-starter-webmvc-test`, Testcontainers PostgreSQL y LocalStack.

**Storage**: PostgreSQL 18.6, imagen oficial fijada (18.7 todavía no estaba en Docker Hub). El esquema y los datos salen del SQL entregado, sin que Hibernate lo genere.

**Testing**: JUnit Jupiter y Mockito para `*Test.java` de dominio y servicios. `*IT.java` con Testcontainers para el camino feliz y los errores. `ActuatorIT` tiene `@Order(1)` y corre antes que el resto de las clases de integración: comprueba health, info y que ningún otro endpoint de Actuator esté publicado. Maven Surefire corre las unitarias y Failsafe las de integración, ambas desde `mvn verify`.

**Target Platform**: Linux en contenedor. El host solo necesita Docker para el arranque standalone. El puerto público es 8080.

**Project Type**: Servicio HTTP, un solo módulo Maven, en la raíz de `cc-backend-challenge`.

**Performance Goals**: La búsqueda sobre el catálogo de referencia responde en menos de 1 segundo. El resto de las operaciones de la spec se resuelven en la misma petición.

**Constraints**: Sin autenticación. Sin simulación de mercado. Sin fracciones de acciones. Sin ingresos ni egresos nuevos. Esquema de base fijo. Precios en pesos. Decisiones de diseño no fijadas por la spec se escriben en `README.txt`.

**Scale/Scope**: 4 cuentas, 66 instrumentos y dos días de precios. Una transacción por cuenta alcanza para no gastar dos veces el mismo disponible.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` sigue siendo la plantilla sin principios ratificados. No hay gates de proyecto que evaluar. El gate pasa.

Las restricciones que sí obligan son las del pedido y las de [research.md](research.md): Java 25, Spring Boot 4.1.2, hexagonal, PostgreSQL en Docker, TDD, tests de servicio con Mockito, integración `*IT.java` con Testcontainers y LocalStack, sin autenticación, y arranque standalone con Docker.

Revisión posterior al diseño: el modelo y el quickstart cumplen esas restricciones. El contrato HTTP no se guarda en el repositorio: lo genera springdoc al arrancar. La única complejidad extra es LocalStack, justificada abajo. El gate sigue en pasa.

## Project Structure

### Documentation (this feature)

```text
specs/001-brokerage-trading-api/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md              # lo crea /speckit-tasks, no este comando
```

### Source Code (repository root)

```text
cc-backend-challenge/
├── pom.xml
├── Dockerfile
├── compose.yaml
├── README.txt
├── src/main/java/com/cc/brokerage/
│   ├── BrokerageApplication.java
│   ├── domain/
│   │   ├── model/
│   │   ├── port/in/
│   │   ├── port/out/
│   │   └── service/          # cálculo puro de portfolio, reservas y cantidades
│   ├── application/          # casos de uso: consultar, buscar, ordenar, cancelar
│   └── adapter/
│       ├── in/web/           # controladores y traducción HTTP
│       └── out/persistence/  # entidades JPA y repositorios
├── src/main/resources/
│   ├── application.yml
│   └── db/database.sql       # el SQL entregado, sin cambios de significado
├── src/test/java/com/cc/brokerage/
│   ├── domain/service/       # *Test.java sin mocks
│   ├── application/          # *Test.java con Mockito sobre los puertos
│   └── adapter/in/web/       # *IT.java
└── postman/                  # opcional, no bloquea el comportamiento
```

**Structure Decision**: Un módulo Maven en la raíz del repositorio. El dominio no depende de Spring ni de JPA. Los casos de uso dependen de puertos. REST y JPA son adaptadores. Así la arquitectura hexagonal pedida queda visible y los tests de servicio no levantan la base.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| LocalStack en los tests de integración, sin un servicio AWS en el producto | El pedido nombra LocalStack junto con Testcontainers | Usarlo como base sería falso: no emula PostgreSQL. Omitirlo deja el pedido escrito sin cumplir. Se arranca y se verifica el contenedor; las afirmaciones de negocio van contra la API y Postgres. |
