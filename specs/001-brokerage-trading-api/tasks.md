# Tasks: Portfolio, búsqueda y órdenes

**Input**: Design documents from `/specs/001-brokerage-trading-api/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md. El contrato HTTP lo genera springdoc en `/v3/api-docs`; no hay una copia en esta carpeta.

**Tests**: Incluidos. El pedido fija TDD: JUnit Jupiter y Mockito en servicios y cálculo; `*IT.java` con Testcontainers y LocalStack para el camino feliz y los errores.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Un módulo Maven en la raíz de `cc-backend-challenge/`
- Código en `src/main/java/com/cc/brokerage/`
- Tests unitarios en `src/test/java/com/cc/brokerage/` con sufijo `Test`
- Integración en `src/test/java/com/cc/brokerage/` con sufijo `IT`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Proyecto Java 25 y Spring Boot 4.1.2, más el arranque Docker

- [x] T001 [P] Crear `pom.xml` con parent `spring-boot-starter-parent` 4.1.2, `java.version` 25, y dependencias `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `org.postgresql:postgresql`, `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, Testcontainers BOM, `org.testcontainers:postgresql` y `org.testcontainers:localstack`. Surefire ejecuta `*Test.java` y Failsafe ejecuta `*IT.java` dentro de `mvn verify`
- [x] T002 [P] Crear `src/main/java/com/cc/brokerage/BrokerageApplication.java` con la clase de arranque de Spring Boot
- [x] T003 [P] Crear `src/main/resources/application.yml` con `spring.jpa.hibernate.ddl-auto: none`, dialecto PostgreSQL, y URL, usuario y clave leídos de variables de entorno
- [x] T004 [P] Crear `Dockerfile` de dos etapas con `eclipse-temurin:25-jdk` para compilar y `eclipse-temurin:25-jre` para ejecutar, exponiendo el puerto 8080
- [x] T005 [P] Crear `compose.yaml` con el servicio `postgres` en la imagen `postgres:18.7` y el servicio `app` que espera su healthcheck y publica 8080. Si el tag `18.7` no existe en Docker Hub, usar el patch 18 publicado más nuevo y anotarlo en `README.txt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Esquema, modelo y adaptadores compartidos. Ninguna historia empieza antes de esta fase

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Copiar el SQL entregado, sin cambiar su significado, a `src/main/resources/db/database.sql` y montarlo en `compose.yaml` como init de Postgres
- [x] T007 [P] Crear `src/main/java/com/cc/brokerage/adapter/out/persistence/UserEntity.java` sobre la tabla `users`, con `id` identidad, `email` texto y `accountnumber` texto de hasta 20 caracteres
- [x] T008 [P] Crear `src/main/java/com/cc/brokerage/adapter/out/persistence/InstrumentEntity.java` sobre `instruments`, con `ticker` texto de hasta 10 caracteres, `name` texto y `type` texto de hasta 10 caracteres cuyos valores de referencia son `ACCIONES` o `MONEDA`
- [x] T009 [P] Crear `src/main/java/com/cc/brokerage/adapter/out/persistence/MarketPriceEntity.java` sobre `marketdata`, con `high`, `low` y `open` decimal (10,2) que pueden ser nulos, `close` y `previousclose` decimal (10,2), y `date` de calendario
- [x] T010 [P] Crear `src/main/java/com/cc/brokerage/adapter/out/persistence/OrderEntity.java` sobre `orders`, con `size` entero, `price` decimal (10,2), `type` `MARKET` o `LIMIT`, `side` `BUY`, `SELL`, `CASH_IN` o `CASH_OUT`, `status` `NEW`, `FILLED`, `REJECTED` o `CANCELLED`, y `datetime`
- [x] T011 [P] Crear los registros de dominio `User`, `Instrument`, `MarketPrice` y `Order` en `src/main/java/com/cc/brokerage/domain/model/`, con los mismos límites y enumeraciones que las entidades, sin anotaciones de JPA
- [x] T012 [P] Crear `src/main/java/com/cc/brokerage/adapter/in/web/ApiError.java` y `src/main/java/com/cc/brokerage/adapter/in/web/ApiExceptionHandler.java`. El cuerpo de error es `{ "message": "..." }` según el contrato que genera springdoc
- [x] T013 [P] Crear `src/test/java/com/cc/brokerage/IntegrationTestBase.java` que levante PostgreSQL y LocalStack con Testcontainers, cargue `database.sql` y deje LocalStack solo como contenedor sano. Los escenarios de negocio no llaman a AWS
- [x] T014 [P] Crear `src/main/java/com/cc/brokerage/config/ClockConfig.java` con un `Clock` inyectable para el `datetime` de las órdenes nuevas
- [x] T015 Crear los repositorios Spring Data `UserJpaRepository`, `InstrumentJpaRepository`, `MarketPriceJpaRepository` y `OrderJpaRepository` en `src/main/java/com/cc/brokerage/adapter/out/persistence/` (depends on T007–T010)
- [x] T016 Crear los puertos de salida `UserRepository`, `InstrumentRepository`, `MarketPriceRepository` y `OrderRepository` en `src/main/java/com/cc/brokerage/domain/port/out/` (depends on T011)
- [x] T017 Implementar `src/main/java/com/cc/brokerage/adapter/out/persistence/PersistenceAdapter.java`, que traduzca entre entidades y dominio, resuelva el precio vigente por mayor `date` y, a igualdad, mayor `id`, y bloquee la fila del usuario al modificar sus órdenes (depends on T015, T016)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Consultar el portfolio (Priority: P1) 🎯 MVP

**Goal**: Consultar una cuenta y obtener valor total, pesos disponibles y posiciones con cantidad, valor de mercado, rendimiento total y rendimiento diario

**Independent Test**: `GET /api/accounts/10001/portfolio` devuelve disponible `627500.00`, total `889756.00`, y las posiciones BMA (−10, `-15028.00`, `1.78`, `-1.15`), METR (500, `114750.00`, `-8.20`, `-1.08`) y PAMP (40, `37034.00`, `-0.45`, `0.44`)

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T018 [P] [US1] Crear `src/test/java/com/cc/brokerage/domain/service/PortfolioCalculatorTest.java` con la cuenta `10001`: efectivo liquidado `753000.00`, disponible `627500.00`, total `889756.00`, posiciones ordenadas por ticker, moneda `ARS` excluida y cantidad neta cero omitida. El rendimiento total es `(cierre − costo promedio) / costo promedio × 100` en una posición larga y `(precio promedio de venta − cierre) / precio promedio de venta × 100` en una corta. El rendimiento diario es `(cierre − cierre anterior) / cierre anterior × 100`. Ambos con dos decimales, half-up
- [x] T019 [P] [US1] Crear `src/test/java/com/cc/brokerage/application/GetPortfolioServiceTest.java` con Mockito sobre los puertos: cuenta inexistente no devuelve portfolio; cuenta sin órdenes devuelve total `0.00`, disponible `0.00` y posiciones vacías
- [x] T020 [P] [US1] Crear `src/test/java/com/cc/brokerage/adapter/in/web/PortfolioControllerIT.java`, que extienda `IntegrationTestBase`, para `GET /api/accounts/10001/portfolio` (200 con el oráculo), una cuenta sin órdenes (200 en cero) y una cuenta inexistente (404, sin portfolio)

### Implementation for User Story 1

- [x] T021 [US1] Implementar `src/main/java/com/cc/brokerage/domain/service/PortfolioCalculator.java` con las fórmulas de `data-model.md`: reserva de compras `NEW`, cantidad neta solo con `FILLED`, valor total igual al efectivo liquidado más las posiciones con cierre, y posición sin cierre listada con valores nulos y fuera del total (depends on T018)
- [x] T022 [US1] Implementar el puerto `src/main/java/com/cc/brokerage/domain/port/in/GetPortfolioUseCase.java` y `src/main/java/com/cc/brokerage/application/GetPortfolioService.java` (depends on T019, T021)
- [x] T023 [US1] Implementar `src/main/java/com/cc/brokerage/adapter/in/web/PortfolioController.java` y `src/main/java/com/cc/brokerage/adapter/in/web/PortfolioResponse.java` para `GET /api/accounts/{accountNumber}/portfolio` según el contrato que genera springdoc. Importes y porcentajes viajan como texto con dos decimales (depends on T020, T022)

**Checkpoint**: User Story 1 se consulta sola, sin enviar órdenes nuevas

---

## Phase 4: User Story 2 - Buscar activos (Priority: P2)

**Goal**: Devolver, sin duplicados y ordenados por ticker, los activos cuyo ticker o nombre contienen el texto, sin distinguir mayúsculas

**Independent Test**: `q=banco` devuelve `BBAR`, `BHIP`, `BMA` y `BPAT`. `q=pamp` devuelve una vez `PAMP` con último precio `925.85` y rendimiento diario `0.44`. `q=YPF` incluye `YPFD`. `q=zzzz` devuelve una lista vacía. El texto vacío responde 400

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T024 [P] [US2] Crear `src/test/java/com/cc/brokerage/application/SearchInstrumentsServiceTest.java` con Mockito: coincidencia por contenido sin distinguir mayúsculas, sin duplicados, orden por ticker, y precio o rendimiento diario nulos cuando no hay cierre o el cierre anterior es nulo o cero
- [x] T025 [P] [US2] Crear `src/test/java/com/cc/brokerage/adapter/in/web/InstrumentControllerIT.java` para `GET /api/instruments`: `banco`, `pamp`, `YPF`, `zzzz` y `q` vacío o en blanco (400, sin listado)

### Implementation for User Story 2

- [x] T026 [US2] Implementar `src/main/java/com/cc/brokerage/domain/port/in/SearchInstrumentsUseCase.java` y `src/main/java/com/cc/brokerage/application/SearchInstrumentsService.java`. El texto debe coincidir por contenido en ticker o nombre; escapar `%` y `_` para que no actúen como comodines (depends on T024)
- [x] T027 [US2] Implementar `src/main/java/com/cc/brokerage/adapter/in/web/InstrumentController.java` y `src/main/java/com/cc/brokerage/adapter/in/web/InstrumentQuoteResponse.java` para `GET /api/instruments` según el contrato que genera springdoc (depends on T025, T026)

**Checkpoint**: La búsqueda funciona sin crear órdenes

---

## Phase 5: User Story 3 - Enviar una orden de mercado (Priority: P3)

**Goal**: Comprar o vender al cierre vigente, por cantidad entera o por monto, y guardar la orden `FILLED` con ese precio

**Independent Test**: Comprar 1 `GGAL` a mercado en la cuenta `10001` queda `FILLED`, cantidad `1`, precio `885.80`, efectivo liquidado `752114.20` y disponible `626614.20`

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T028 [P] [US3] Crear `src/test/java/com/cc/brokerage/application/PlaceMarketOrderServiceTest.java` con Mockito: compra de 1 `GGAL` a `885.80`; monto `2000` que resulta en cantidad `2` y descuenta `1771.60`; venta de 1 `PAMP` a `925.85`; un precio enviado en `MARKET` se ignora; cantidad y monto juntos, o ninguno, no guardan orden; cantidad no entera positiva, monto no positivo, ticker inexistente o cuenta inexistente no guardan orden; `CASH_IN` y `CASH_OUT` no guardan orden
- [x] T029 [P] [US3] Crear `src/test/java/com/cc/brokerage/adapter/in/web/PlaceMarketOrderIT.java` para `POST /api/accounts/{accountNumber}/orders` con los mismos casos. Una orden guardada responde 201. Una solicitud inválida responde 400 o 404 y no inserta fila

### Implementation for User Story 3

- [x] T030 [US3] Implementar `src/main/java/com/cc/brokerage/domain/port/in/PlaceOrderUseCase.java` y el camino `MARKET` de `src/main/java/com/cc/brokerage/application/PlaceOrderService.java`. El precio guardado es el cierre vigente. Si llega un monto, `size = floor(monto / precio)` y el efectivo que mueve es `size * precio`. `size` queda entero y positivo (depends on T028)
- [x] T031 [US3] Implementar `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java`, `PlaceOrderRequest.java` y `OrderResponse.java` para `POST /api/accounts/{accountNumber}/orders`. `side` aceptado en alta: `BUY` o `SELL`. `type` `MARKET` no exige `price` (depends on T029, T030)

**Checkpoint**: Una orden de mercado válida queda ejecutada y el portfolio siguiente la refleja

---

## Phase 6: User Story 4 - Enviar una orden límite (Priority: P4)

**Goal**: Dejar una compra o venta al precio elegido en estado `NEW`, reservando pesos o acciones, sin ejecutarla contra el cierre

**Independent Test**: Comprar 10 `GGAL` a `800.00` queda `NEW`, no crea posición, no cambia el efectivo liquidado y baja el disponible `8000.00`

### Tests for User Story 4 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T032 [P] [US4] Crear `src/test/java/com/cc/brokerage/application/PlaceLimitOrderServiceTest.java` con Mockito: compra de 10 `GGAL` a `800.00` queda `NEW` y reserva `8000.00`; sin precio, o con precio no mayor que cero, no guarda orden; monto `2500` a `800.00` guarda cantidad `3` y reserva `2400.00`; la orden sigue `NEW` aunque el cierre sea otro; una venta límite de 10 `PAMP` a `1000.00` reserva 10 acciones y no cambia el efectivo
- [x] T033 [P] [US4] Crear `src/test/java/com/cc/brokerage/adapter/in/web/PlaceLimitOrderIT.java` para esos casos sobre `POST /api/accounts/{accountNumber}/orders`

### Implementation for User Story 4

- [x] T034 [US4] Extender `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` para `LIMIT`: guardar `NEW` con el precio enviado, reservar `size * price` en compras y `size` acciones en ventas, y no pasar la orden a `FILLED` (depends on T032)
- [x] T035 [US4] Extender `src/main/java/com/cc/brokerage/adapter/in/web/PlaceOrderRequest.java` para exigir `price` mayor que cero cuando `type` es `LIMIT`, y responder 400 sin guardar la orden si falta (depends on T033, T034)

**Checkpoint**: Las órdenes límite quedan abiertas y el portfolio reserva su disponible

---

## Phase 7: User Story 5 - Rechazar órdenes que no se pueden cubrir (Priority: P5)

**Goal**: Guardar como `REJECTED` la compra que supera los pesos disponibles o la venta que supera las acciones disponibles, sin cambiar la cuenta

**Independent Test**: Comprar `1000000` de `GGAL` o vender `100` de `PAMP` en la cuenta `10001` deja la orden `REJECTED` y el portfolio igual que en la historia 1

### Tests for User Story 5 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T036 [P] [US5] Crear `src/test/java/com/cc/brokerage/application/RejectOrderServiceTest.java` con Mockito: compra de `1000000` `GGAL` rechazada; venta de `100` `PAMP` rechazada; monto `100` sobre `GGAL` a `885.80` da cantidad `0` y se guarda `REJECTED`; el disponible ya reservado no se reutiliza ni se recorta la orden; dos órdenes que juntas superan el disponible dejan la segunda `REJECTED`
- [x] T037 [P] [US5] Crear `src/test/java/com/cc/brokerage/adapter/in/web/RejectOrderIT.java`. La orden rechazada responde 201 con estado `REJECTED`. El `GET` del portfolio de `10001` sigue en `627500.00` y `889756.00`

### Implementation for User Story 5

- [x] T038 [US5] Completar en `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` el rechazo por pesos o acciones insuficientes, por cantidad `0` y por mercado sin cierre. La fila se guarda `REJECTED` y no cambia tenencia, efectivo ni reservas. La transacción bloquea al usuario antes de decidir (depends on T036, T017)

**Checkpoint**: Ninguna orden aceptada deja el disponible en negativo

---

## Phase 8: User Story 6 - Cancelar una orden abierta (Priority: P6)

**Goal**: Pasar a `CANCELLED` una orden `NEW` del titular y devolver la reserva. No cancelar `FILLED`, `REJECTED`, `CANCELLED` ni una orden de otra cuenta

**Independent Test**: Cancelar la compra límite `NEW` de 50 `LOMA` a `710.00` de la cuenta `10001` deja el disponible en `663000.00` y el efectivo liquidado igual

### Tests for User Story 6 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T039 [P] [US6] Crear `src/test/java/com/cc/brokerage/application/CancelOrderServiceTest.java` con Mockito: cancelar la compra de 50 `LOMA` a `710.00` libera `35500.00`; cancelar `FILLED`, `REJECTED` o `CANCELLED` no cambia el estado; otra cuenta no cancela la orden y la ve como inexistente; un id inexistente no cancela nada; cancelar una venta límite de 10 `PAMP` devuelve esas 10 acciones y no cambia el efectivo
- [x] T040 [P] [US6] Crear `src/test/java/com/cc/brokerage/adapter/in/web/CancelOrderIT.java` para `POST /api/accounts/{accountNumber}/orders/{orderId}/cancellation`. En una base recién cargada, la orden `LOMA` tiene id `5`. Éxito responde 200. Estado no cancelable responde 409. Id inexistente o de otra cuenta responde 404

### Implementation for User Story 6

- [x] T041 [US6] Implementar `src/main/java/com/cc/brokerage/domain/port/in/CancelOrderUseCase.java` y `src/main/java/com/cc/brokerage/application/CancelOrderService.java`. Solo el titular cancela una orden `NEW`; el estado pasa a `CANCELLED` y se libera la reserva de pesos o de acciones (depends on T039)
- [x] T042 [US6] Agregar la cancelación en `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java` según el contrato que genera springdoc (depends on T040, T041)

**Checkpoint**: Solo las órdenes abiertas se cancelan, y el disponible vuelve

---

## Phase 9: User Story 7 - Identificar el servicio y su ruta pública (Priority: P7)

**Goal**: El arranque se identifica como `brokerage-api` y todas las operaciones HTTP se atienden bajo `/api/brokerage-api/v1`

**Independent Test**: `GET /api/brokerage-api/v1/accounts/10001/portfolio` coincide con la historia 1. `GET /api/accounts/10001/portfolio` no se atiende. El banner y el nombre de la aplicación son `brokerage-api`

### Tests for User Story 7 ⚠️

- [x] T047 [P] [US7] Crear `src/test/java/com/cc/brokerage/adapter/in/web/ApplicationIdentityIT.java`: el nombre de la aplicación es `brokerage-api`, el banner contiene `brokerage-api`, y las rutas sin `/api/brokerage-api` o sin `/v1` responden 404
- [x] T048 [US7] Prefijar con `API_V1` (`/api/brokerage-api/v1`) todos los `*IT.java` existentes que invocan HTTP (depends on T047)

### Implementation for User Story 7

- [x] T049 [US7] Crear `src/main/resources/banner.txt` con `brokerage-api` y fijar `spring.application.name` en `brokerage-api` (depends on T047)
- [x] T050 [US7] Crear `src/main/java/com/cc/brokerage/adapter/in/web/ApiBase.java` (`/api/brokerage-api`) y prefijar cada controller con `ApiBase.PATH + "/v1/..."` (depends on T048)
- [x] T051 [US7] Actualizar `quickstart.md` y `postman/brokerage.postman_collection.json` para que la base sea `http://localhost:8080/api/brokerage-api` y cada path lleve `/v1`. El contrato escrito a mano se retiró: lo genera springdoc (depends on T050)

**Checkpoint**: Quien invoca el servicio usa `/api/brokerage-api/v1` para todas las operaciones

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Arranque standalone y ejemplo de invocación. No cambia las reglas de negocio

- [x] T043 [P] Crear la colección opcional `postman/brokerage.postman_collection.json` con portfolio, búsqueda, orden de mercado, orden límite, rechazo y cancelación
- [x] T044 Revisar `README.txt` y anotar cualquier decisión de diseño nueva que no esté ya en la spec o en `research.md`
- [x] T045 Ejecutar la validación de `specs/001-brokerage-trading-api/quickstart.md` con `docker compose up --build` y los `curl` de la cuenta `10001`
- [x] T046 Ejecutar `mvn verify` y dejar en verde los `*Test.java` y los `*IT.java`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4 → P5 → P6)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independiente de US1; comparte instrumentos y precios
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - El portfolio de US1 demuestra el efecto, pero la orden se prueba sola
- **User Story 4 (P4)**: Depends on el alta de órdenes de US3, porque extiende `PlaceOrderService` y `OrderController`
- **User Story 5 (P5)**: Depends on US3 y US4, porque el rechazo cubre mercado y límite sobre el mismo servicio
- **User Story 6 (P6)**: Can start after Foundational (Phase 2) usando la orden `LOMA` ya cargada. No necesita US4 para el caso de referencia

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, US1, US2, US3 y US6 can start in parallel
- US4 espera el alta de US3. US5 espera US3 y US4
- All tests for a user story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Tests de la historia 1, juntos, y deben fallar antes de implementar:
Task: "PortfolioCalculatorTest en src/test/java/com/cc/brokerage/domain/service/PortfolioCalculatorTest.java"
Task: "GetPortfolioServiceTest en src/test/java/com/cc/brokerage/application/GetPortfolioServiceTest.java"
Task: "PortfolioControllerIT en src/test/java/com/cc/brokerage/adapter/in/web/PortfolioControllerIT.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add User Story 4, luego User Story 5, y User Story 6 en cualquier momento después de la base
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3 y, al terminar, User Story 4 y User Story 5
   - Developer D: User Story 6
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
