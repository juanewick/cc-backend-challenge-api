# Implementation Plan: Consulta de la cuenta

**Branch**: `feat/add-various-improvements` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-consulta-cuenta-ordenes/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

La cuenta se puede leer completa sin cambiar el esquema entregado por Cocos. Se listan y se consultan las órdenes ya guardadas. El portfolio informa también el efectivo liquidado y el reservado, calculados al responder. El alta explica un rechazo y el importe en esa respuesta, e indica cómo volver a la orden. El ticker del alta no distingue mayúsculas. La moneda no sale como acción operable. Hay cotización por ticker. Los rechazos traen un código estable además del mensaje. Portfolio, búsqueda, cotización y orden de mercado usan el mismo cierre más reciente. Repetir un alta crea otra orden.

## Technical Context

**Language/Version**: Java 25, el mismo runtime de la feature 001.

**Primary Dependencies**: Spring Boot 4.1.2, Spring Data JPA y springdoc-openapi 3.2.0, ya presentes. No se agrega una librería. No hay Redis ni caché nueva.

**Storage**: Sin cambios de esquema. No se crean tablas ni columnas y no se modifican las existentes. `orders.size` sigue en `INT` y `orders.datetime` en `TIMESTAMP` sin zona. Liquidado, reservado, importe, motivo y código de error no se persisten.

**Testing**: JUnit Jupiter y Mockito, como en las features 001 y 002. Primero los casos de uso (`*Test.java`). Después `*IT.java` con el PostgreSQL de Testcontainers. `mvn verify`.

**Target Platform**: El mismo servicio HTTP en contenedor, puerto 8080, bajo `/api/brokerage-api/v1`.

**Project Type**: Servicio HTTP, un solo módulo Maven, arquitectura hexagonal ya existente.

**Performance Goals**: El cierre vigente de varios activos se elige en una sola pasada compartida, no en dos copias del mismo recorrido. El catálogo de referencia sigue cabiendo en una lectura.

**Constraints**: Sin sesiones, sin límite de intentos y sin idempotencia del alta. Sin copia escrita del OpenAPI: el contrato lo sigue generando la aplicación y esta feature lo anota. El cuerpo de error es un problema con `type`, `title`, `status`, `detail` y `code`. En un error de dominio, `code` es `BRK-xxx` y `detail` está en inglés. No repite el valor enviado. Una posición corta puede seguir con cantidad negativa.

**Scale/Scope**: Las operaciones ya entregadas más el listado de órdenes, la consulta de una orden, la cotización por ticker y los campos calculados del portfolio y del alta. El catálogo y las cuentas de referencia no cambian.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` sigue siendo la plantilla sin principios ratificados. No hay gates de proyecto que evaluar. El gate pasa.

Las restricciones que sí obligan son las de la feature 001, las de la 002 y las de esta spec: Java 25, Spring Boot 4.1.2, hexagonal, el esquema entregado sin columnas nuevas, el contrato generado por springdoc, y no guardar liquidado, reservado, importe, motivo ni código.

Revisión posterior al diseño: [research.md](research.md), [data-model.md](data-model.md), [contracts/consulta.md](contracts/consulta.md) y [quickstart.md](quickstart.md) no agregan tablas ni columnas. El gate sigue en pasa.

## Project Structure

### Documentation (this feature)

```text
specs/003-consulta-cuenta-ordenes/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── consulta.md
└── tasks.md              # lo crea /speckit-tasks, no este comando
```

### Source Code (repository root)

```text
cc-backend-challenge/
├── src/main/java/com/cc/brokerage/
│   ├── domain/model/Portfolio.java
│   ├── domain/model/RejectionReason.java
│   ├── domain/service/LatestPrice.java
│   ├── domain/service/PortfolioCalculator.java
│   ├── application/GetPortfolioService.java
│   ├── application/SearchInstrumentsService.java
│   ├── application/PlaceOrderService.java
│   ├── application/ListOrdersService.java
│   ├── application/GetOrderService.java
│   ├── application/GetInstrumentQuoteService.java
│   ├── adapter/in/web/PortfolioResponse.java
│   ├── adapter/in/web/OrderResponse.java
│   ├── adapter/in/web/OrderController.java
│   ├── adapter/in/web/InstrumentController.java
│   ├── adapter/in/web/ApiError.java
│   ├── adapter/in/web/ApiExceptionHandler.java
│   └── adapter/out/persistence/PersistenceAdapter.java
└── src/test/java/com/cc/brokerage/
    ├── application/*ServiceTest.java
    └── adapter/in/web/*IT.java
```

**Structure Decision**: Se queda el módulo único y los paquetes hexagonales. El liquidado y el reservado ya salen de `PortfolioCalculator.Holdings` y solo se publican. El cierre vigente se elige en un solo lugar del dominio y lo usan portfolio, búsqueda, cotización y alta de mercado. El motivo del rechazo nace en `PlaceOrderService` y viaja solo en la respuesta del alta.

## Complexity Tracking

Sin violaciones. Esta feature no agrega una base, una caché ni una columna. Agrega lecturas sobre las órdenes y los activos que ya existen.
