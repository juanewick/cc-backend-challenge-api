# Tasks: Consulta de la cuenta

**Input**: Design documents from `/specs/003-consulta-cuenta-ordenes/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/consulta.md, quickstart.md

**Tests**: Incluidos, como en las features 001 y 002. Primero el caso de uso o el `*IT.java`, después el código que lo hace pasar. `mvn verify`.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

## Path Conventions

- Un módulo Maven en la raíz de `cc-backend-challenge/`
- Código en `src/main/java/com/cc/brokerage/`
- Tests en `src/test/java/com/cc/brokerage/`
- Sin tablas ni columnas nuevas. Sin idempotencia. Los códigos de error son los de `contracts/consulta.md`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dejar un solo lugar para el cierre vigente, sin dependencias nuevas

- [x] T001 Crear `src/main/java/com/cc/brokerage/domain/service/LatestPrices.java` que, de una lista de `MarketPrice`, se quede con la de mayor `date` y, a igual fecha, mayor `id`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: El cuerpo de error lleva código y mensaje. Bloquea las historias que rechazan

- [x] T002 Crear `src/main/java/com/cc/brokerage/adapter/in/web/ErrorCodes.java` con el mapa de mensajes estables a los códigos de `contracts/consulta.md`
- [x] T003 Cambiar `src/main/java/com/cc/brokerage/adapter/in/web/ApiError.java` a `code` y `message`, y hacer que `src/main/java/com/cc/brokerage/adapter/in/web/ApiExceptionHandler.java` lo complete sin repetir el valor enviado

**Checkpoint**: Un 404 de cuenta inexistente responde `ACCOUNT_NOT_FOUND` y el mensaje de siempre

---

## Phase 3: User Story 1 - Ver las órdenes y la reserva (Priority: P1)

**Goal**: Listar y consultar órdenes, y mostrar liquidado y reservado

**Independent Test**: En `10001`, el portfolio informa `753000.00` y `125500.00`, el listado trae la compra `NEW` de 50 `LOMA` a `710.00`, y otra cuenta no puede leerla

### Tests for User Story 1

- [x] T004 [P] [US1] Afirmar liquidado y reservado en `src/test/java/com/cc/brokerage/domain/service/PortfolioCalculatorTest.java` y en `src/test/java/com/cc/brokerage/adapter/in/web/PortfolioControllerIT.java`
- [x] T005 [P] [US1] Cubrir listado, consulta propia, ajena e inexistente en `src/test/java/com/cc/brokerage/adapter/in/web/OrderQueryIT.java`

### Implementation for User Story 1

- [x] T006 [US1] Publicar `settledCash` y `reservedCash` calculados en `src/main/java/com/cc/brokerage/domain/model/Portfolio.java`, `src/main/java/com/cc/brokerage/domain/service/PortfolioCalculator.java` y `src/main/java/com/cc/brokerage/adapter/in/web/PortfolioResponse.java`. No guardarlos
- [x] T007 [US1] Usar `LatestPrices` en `src/main/java/com/cc/brokerage/application/GetPortfolioService.java`
- [x] T008 [US1] Agregar `src/main/java/com/cc/brokerage/application/ListOrdersService.java` y `src/main/java/com/cc/brokerage/application/GetOrderService.java`, ordenando por fecha de alta e id ascendente, y exponer `GET` en `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java`. Orden ajena: `ORDER_NOT_OWNED`. Inexistente: `ORDER_NOT_FOUND`

**Checkpoint**: User Story 1 se prueba sola

---

## Phase 4: User Story 2 - Entender el resultado del alta (Priority: P2)

**Goal**: El alta informa importe y, si quedó rechazada, el motivo. La consulta posterior no trae el motivo

**Independent Test**: Comprar de más `GGAL` en `10001` responde `REJECTED`, `INSUFFICIENT_CASH` y el importe. El portfolio de referencia no cambia

### Tests for User Story 2

- [x] T009 [P] [US2] Afirmar motivo e importe en `src/test/java/com/cc/brokerage/adapter/in/web/RejectOrderIT.java` y el encabezado `Location` en `src/test/java/com/cc/brokerage/adapter/in/web/PlaceMarketOrderIT.java`

### Implementation for User Story 2

- [x] T010 [US2] Crear `src/main/java/com/cc/brokerage/domain/model/RejectionReason.java` y devolver importe y motivo desde `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` sin persistirlos. Motivos: `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES`, `NO_MARKET_PRICE`
- [x] T011 [US2] Publicar `amount` y `rejectionReason` solo en la respuesta del alta, y `Location` hacia el `GET` de esa orden, en `src/main/java/com/cc/brokerage/adapter/in/web/OrderResponse.java` y `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java`

**Checkpoint**: User Stories 1 y 2 funcionan. Repetir el alta crea otra orden

---

## Phase 5: User Story 3 - Operar el ticker que la búsqueda encuentra (Priority: P3)

**Goal**: `ggal` opera `GGAL`. `PESOS` y `ARS` no son accionables. Hay cotización por ticker

**Independent Test**: Alta con `ggal` y búsquedas `PESOS` y `ARS`

### Tests for User Story 3

- [x] T012 [P] [US3] Cubrir `ggal`, `PESOS`, `ARS` y `GET /instruments/PAMP` en `src/test/java/com/cc/brokerage/adapter/in/web/InstrumentControllerIT.java` y `src/test/java/com/cc/brokerage/adapter/in/web/PlaceMarketOrderIT.java`

### Implementation for User Story 3

- [x] T013 [US3] Resolver el ticker sin distinguir mayúsculas en `src/main/java/com/cc/brokerage/adapter/out/persistence/InstrumentJpaRepository.java` y `src/main/java/com/cc/brokerage/adapter/out/persistence/PersistenceAdapter.java`. La respuesta muestra el ticker del catálogo
- [x] T014 [US3] Excluir `MONEDA` en `src/main/java/com/cc/brokerage/application/SearchInstrumentsService.java` y agregar `src/main/java/com/cc/brokerage/application/GetInstrumentQuoteService.java` con `GET /instruments/{ticker}` en `src/main/java/com/cc/brokerage/adapter/in/web/InstrumentController.java`. Moneda: `INSTRUMENT_NOT_TRADABLE`

**Checkpoint**: User Story 3 se prueba sola

---

## Phase 6: User Story 4 - Leer un error y el contrato (Priority: P4)

**Goal**: Cada rechazo trae código estable. El OpenAPI generado explica errores, importes y que `0.48` es 0,48 %

**Independent Test**: Cuenta inexistente, ticker inválido y cancelación de una orden que no está abierta traen código y el mensaje de siempre. `/v3/api-docs` describe las unidades

### Tests for User Story 4

- [x] T015 [P] [US4] Afirmar códigos en los `*IT.java` de error ya existentes y la descripción del porcentaje en `src/test/java/com/cc/brokerage/adapter/in/web/GeneratedOpenApiIT.java`

### Implementation for User Story 4

- [x] T016 [US4] Anotar operaciones, `ApiError` y los campos de importe y porcentaje en los records de `src/main/java/com/cc/brokerage/adapter/in/web/` para que springdoc publique `400`, `404`, `409`, el texto a dos decimales y la unidad `0,48 %`

**Checkpoint**: El contrato generado coincide con `contracts/consulta.md`

---

## Phase 7: User Story 5 - Usar un solo cierre vigente (Priority: P5)

**Goal**: Portfolio, búsqueda, cotización y orden de mercado usan el mismo cierre

**Independent Test**: Buscar `PAMP`, pedir su cotización y comprar 1 a mercado comparten el precio `925.85`

### Tests for User Story 5

- [x] T017 [P] [US5] Afirmar el mismo precio de `PAMP` en búsqueda, cotización y alta en `src/test/java/com/cc/brokerage/adapter/in/web/InstrumentControllerIT.java` y `src/test/java/com/cc/brokerage/adapter/in/web/PlaceMarketOrderIT.java`

### Implementation for User Story 5

- [x] T018 [US5] Hacer que `src/main/java/com/cc/brokerage/application/SearchInstrumentsService.java`, `src/main/java/com/cc/brokerage/application/GetInstrumentQuoteService.java` y `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` elijan el cierre solo con `LatestPrices`. Alta y cancelación de un activo existente devuelven su ticker, nunca vacío, en `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java`

**Checkpoint**: Las cuatro lecturas comparten la regla de `research.md`

---

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T019 Correr `mvn verify` según `specs/003-consulta-cuenta-ordenes/quickstart.md` y dejar en verde la regresión de `10001`, `PAMP` y un alta válida

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: arranca ya
- **Foundational (Phase 2)**: depende del setup y bloquea los rechazos con código
- **User Stories**: US1 puede arrancar después del setup. US2 depende de US1 para el `Location` hacia el `GET`. US3 y US5 comparten `LatestPrices`. US4 depende de que los códigos de la fase 2 existan
- **Polish**: depende de las historias

### User Story Dependencies

- **User Story 1 (P1)**: después de T001
- **User Story 2 (P2)**: el `GET` de US1 tiene que existir para `Location`
- **User Story 3 (P3)**: independiente del listado, usa el mismo catálogo
- **User Story 4 (P4)**: después de T002 y T003
- **User Story 5 (P5)**: después de T001 y de la cotización de US3

### Parallel Opportunities

- T004 y T005 pueden escribirse juntos
- T009 y T012 tocan IT distintos y pueden avanzar en paralelo hasta que editen el mismo controller

### Parallel Example: User Story 1

```text
T004 PortfolioCalculatorTest y PortfolioControllerIT
T005 OrderQueryIT
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T001
2. T004 a T008
3. Parar y validar el portfolio de `10001` y el listado de `LOMA`

### Incremental Delivery

1. Setup y fundación de errores
2. US1 listado y reserva
3. US2 motivo, importe y `Location`
4. US3 ticker y cotización
5. US4 contrato
6. US5 un solo cierre
7. `mvn verify`

---

## Notes

- No se crea ninguna tabla ni columna
- Repetir un alta crea otra orden
- El motivo no se guarda y no vuelve en el `GET` de la orden
- [P] marca archivos distintos
