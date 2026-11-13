# Tasks: Endurecer las entradas

**Input**: Design documents from `/specs/002-harden-api-inputs/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluidos. El plan exige que `*Test.java` y `*IT.java` se escriban primero y fallen antes de la implementación. JUnit Jupiter y Mockito en dominio y casos de uso. La integración usa el PostgreSQL de Testcontainers que ya levanta `IntegrationTestBase`.

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
- El cuerpo de error sigue siendo `{ "message": "..." }`. Los textos son los de `specs/002-harden-api-inputs/contracts/entradas.md`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Lectura estricta del documento, sin dependencias nuevas

- [x] T001 [P] Activar en `src/main/resources/application.yml` `spring.jackson.deserialization.fail-on-unknown-properties: true`, `spring.jackson.deserialization.fail-on-trailing-tokens: true`, `spring.jackson.read.strict-duplicate-detection: true` y `spring.jackson.datatype.enum.fail-on-numbers-for-enums: true`. No agregar dependencias a `pom.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Impedir que un escalar se convierta en otro tipo antes de cualquier historia

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T002 Crear `src/main/java/com/cc/brokerage/config/JsonMapperConfig.java` con un `JsonMapperBuilderCustomizer` que ponga `CoercionAction.Fail` cuando un entero, un decimal, un texto, un lado o un tipo llegue como texto, número, booleano, arreglo u objeto de otra forma. Un cuerpo vacío, un arreglo en la raíz o un tipo de contenido distinto queda para el mismo rechazo que un pedido ilegible

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Rechazar un pedido que no se puede interpretar (Priority: P1) 🎯 MVP

**Goal**: Un alta ilegible, vacía, con un tipo distinto, con un campo de más o con un campo repetido no graba una orden y no devuelve el cuerpo ni un diagnóstico interno

**Independent Test**: Enviar un cuerpo que no se puede leer, un cuerpo vacío, un número donde va un texto, un texto donde va una cantidad, un campo que la operación no define y el mismo campo dos veces. No aparece una orden nueva y la respuesta no cita el cuerpo enviado

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T003 [US1] Crear `src/test/java/com/cc/brokerage/adapter/in/web/HardenDocumentIT.java`, que extienda `IntegrationTestBase`, para `POST /api/brokerage-api/v1/accounts/10001/orders`. Un cuerpo que no es un documento, un cuerpo vacío, un documento que no es un objeto, un tipo de contenido distinto y tokens de más responden 400 con `El pedido no se puede leer`, sin el cuerpo enviado. Un campo que la operación no define responde 400 con `El pedido tiene un campo que no corresponde`, sin el nombre de esa clave. `size` repetido responde 400 con `El campo cantidad está repetido`. Un ticker numérico o una cantidad en texto responden 400 con `El campo ticker no es válido` o `El campo cantidad no es válido`. En todos los casos no se graba una orden

### Implementation for User Story 1

- [x] T004 [US1] Cambiar `src/main/java/com/cc/brokerage/adapter/in/web/ApiExceptionHandler.java` para que `HttpMessageNotReadableException`, `HttpMediaTypeNotSupportedException` y un argumento de ruta mal formado respondan 400 con `ApiError` y nunca con `exception.getMessage()`. Pedido ilegible, vacío, que no es un objeto, con tokens de más o con otro tipo de contenido: `El pedido no se puede leer`. Campo desconocido: `El pedido tiene un campo que no corresponde`. Campo nuestro repetido: `El campo cantidad está repetido` y el equivalente para ticker, lado, tipo, monto o precio. Tipo distinto: `El campo ticker no es válido` y el equivalente para lado, tipo, cantidad, monto o precio. Si el lector no indica un campo nuestro, usar `El pedido no se puede leer` (depends on T003)

**Checkpoint**: User Story 1 rechaza el documento roto sin grabar y sin filtrar el lector

---

## Phase 4: User Story 2 - Rechazar números que no entran en el modelo (Priority: P2)

**Goal**: Cantidad, monto, precio e identificador de orden fuera de rango no se guardan recortados ni con otro signo. El desborde de un monto no graba ni una orden rechazada por negocio

**Independent Test**: Cantidad `-1`, cantidad `0`, cantidad mayor que `2147483647`, monto o precio negativo, monto o precio con más de 8 enteros después del redondeo, e identificador `0` o negativo. Ninguno graba una orden ni cambia el portfolio. La cantidad `2147483647` no se rechaza por tamaño

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T005 [P] [US2] Crear `src/test/java/com/cc/brokerage/domain/model/MoneyMathTest.java`. Después de redondear half-up a 2, `99999999.994` entra como `99999999.99` y `99999999.995` se rechaza por superar 8 dígitos enteros. Un cociente mayor que `2147483647` no devuelve un entero truncado. El cociente `0` sigue siendo `0`. El cociente `2147483647` se acepta
- [x] T006 [P] [US2] Ampliar `src/test/java/com/cc/brokerage/application/PlaceMarketOrderServiceTest.java` y `src/test/java/com/cc/brokerage/application/PlaceLimitOrderServiceTest.java`. Un monto cuyo cociente supera `2147483647` no llama a `save`. Un monto que redondea a más de 8 enteros, un monto no positivo y un precio límite no positivo tampoco graban. Un monto chico cuyo cociente es `0`, y una orden de mercado sin cierre, se siguen guardando `REJECTED`
- [x] T007 [P] [US2] Crear `src/test/java/com/cc/brokerage/adapter/in/web/HardenNumbersIT.java`, que extienda `IntegrationTestBase`. Cantidad `0`, `-1` o mayor que `2147483647` responde 400 y no graba. La cantidad `2147483647` no responde 400 por tamaño. Monto o precio negativo, no numérico o con 9 enteros tras el redondeo responde 400. `POST /api/brokerage-api/v1/accounts/10001/orders/0/cancellation` y un id que no es un entero de 1 a 2147483647 responden 400 con `La orden no es válida` y no cancelan otra orden. Una orden inexistente bien formada responde 404 con `La orden no existe`, sin el id

### Implementation for User Story 2

- [x] T008 [P] [US2] Cambiar `src/main/java/com/cc/brokerage/domain/model/MoneyMath.java` para redondear half-up a 2 y recién ahí exigir un valor mayor que cero con como máximo 8 dígitos enteros. `sharesFromAmount` calcula el cociente entero y, si es mayor que `2147483647`, lanza `InvalidRequestException` con `La cantidad debe ser un entero de 1 a 2147483647`. El cociente `0` se devuelve como `0`. No usar `intValue()` (depends on T005)
- [x] T009 [P] [US2] Crear `src/main/java/com/cc/brokerage/domain/model/InputText.java` con el identificador de orden: entero de 1 a 2147483647. Cero, negativo, texto, espacios y un número más grande lanzan `InvalidRequestException` con `La orden no es válida`. `0001` es la orden 1 (depends on T007)
- [x] T010 [US2] Cambiar `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` para usar el redondeo y el cociente de `MoneyMath` antes de `save`. Cantidad fuera de 1 a 2147483647: `La cantidad debe ser un entero de 1 a 2147483647`. Monto no positivo: `El monto debe ser mayor que cero`. Monto por encima de 8 enteros: `El monto supera el máximo permitido`. Precio límite ausente o no positivo: `El precio es obligatorio y debe ser mayor que cero`. Precio por encima del máximo: `El precio supera el máximo permitido`. Se mantienen `Debe indicarse exactamente una forma de cantidad: acciones o monto`, `El lado debe ser BUY o SELL`, `El tipo de orden es obligatorio` y `El tipo debe ser MARKET o LIMIT`. El desborde no graba; el cociente `0` y la orden de mercado sin cierre se siguen guardando `REJECTED` (depends on T006, T008)
- [x] T011 [US2] Cambiar `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java` para recibir `{orderId}` como texto, interpretarlo con `InputText` y no llamar a la cancelación si no entra en 1 a 2147483647. Cambiar `src/main/java/com/cc/brokerage/domain/OrderNotFoundException.java` para que el mensaje sea `La orden no existe`, sin el id (depends on T007, T009)

**Checkpoint**: User Story 2 rechaza números fuera de rango sin truncar y sin cambiar el rechazo de negocio ya existente

---

## Phase 5: User Story 3 - Tratar el texto como dato y no como instrucción (Priority: P3)

**Goal**: Cuenta, ticker y búsqueda tienen formato cerrado o texto acotado. Un texto que parece una instrucción no se ejecuta, no elige otro registro y no devuelve un error interno

**Independent Test**: Portfolio de `10001' OR '1'='1`, alta con ticker `PAMP'; DROP TABLE orders;--` y búsqueda `pamp' OR 1=1`. La cuenta y el ticker se rechazan. La búsqueda no modifica datos. `10001` y `PAMP` siguen respondiendo como antes

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T012 [P] [US3] Crear `src/test/java/com/cc/brokerage/domain/model/InputTextTest.java`. La cuenta es de 1 a 20 dígitos, sin recorte: los espacios al inicio o al final se rechazan y los ceros a la izquierda se conservan. El ticker es de 1 a 10 letras o dígitos, sin recorte y sin cambiar mayúsculas. La búsqueda recorta solo los extremos y, después, admite de 1 a 100 caracteres Unicode, sin controles ISO ni carácter nulo
- [x] T013 [P] [US3] Ampliar `src/test/java/com/cc/brokerage/application/SearchInstrumentsServiceTest.java`. Búsqueda vacía: `La búsqueda no tiene texto`. Más de 100 caracteres: `La búsqueda supera el máximo permitido`. Un control o el carácter nulo: `La búsqueda no es válida`. `PAMP` y `pamp` encuentran el mismo activo. `pamp' OR 1=1` no agrega filas. La comparación no depende del idioma de la máquina
- [x] T014 [P] [US3] Crear `src/test/java/com/cc/brokerage/adapter/in/web/HardenTextIT.java`, que extienda `IntegrationTestBase`. Cuenta con letras, espacios, signos o más de 20 dígitos responde 400 con `La cuenta no es válida` en portfolio, alta y cancelación, y no devuelve otra cuenta. Ticker vacío, con espacios, con signos o con más de 10 letras o dígitos responde 400 con `El ticker no es válido` y no graba. Una cuenta bien formada que no existe responde 404 con `La cuenta no existe`. Un ticker de formato válido que no existe responde 404 con `El activo no existe`. Esos 404 no repiten el valor enviado. `GET /api/brokerage-api/v1/accounts/10001/portfolio` y `GET /api/brokerage-api/v1/instruments?q=PAMP` siguen respondiendo como antes

### Implementation for User Story 3

- [x] T015 [US3] Completar `src/main/java/com/cc/brokerage/domain/model/InputText.java` con la cuenta (1 a 20 dígitos, sin recorte; si no entra, `La cuenta no es válida`), el ticker (1 a 10 letras o dígitos, sin recorte y sin cambiar mayúsculas; si no entra, `El ticker no es válido`) y la búsqueda (extremos recortados, 1 a 100 caracteres Unicode, sin controles ISO ni carácter nulo). Vacía: `La búsqueda no tiene texto`. Demasiado larga: `La búsqueda supera el máximo permitido`. Con un control: `La búsqueda no es válida` (depends on T012)
- [x] T016 [P] [US3] Cambiar `src/main/java/com/cc/brokerage/application/SearchInstrumentsService.java` para aceptar el texto con `InputText` antes de leer el catálogo y compararlo en memoria con `Locale.ROOT`, sin distinguir mayúsculas y sin concatenarlo en una consulta (depends on T013, T015)
- [x] T017 [P] [US3] Validar la cuenta con `InputText` al inicio de `src/main/java/com/cc/brokerage/application/GetPortfolioService.java`, `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` y `src/main/java/com/cc/brokerage/application/CancelOrderService.java`, y el ticker al inicio de `PlaceOrderService`, antes de cualquier repositorio. Cambiar `src/main/java/com/cc/brokerage/domain/AccountNotFoundException.java` a `La cuenta no existe` y `src/main/java/com/cc/brokerage/domain/InstrumentNotFoundException.java` a `El activo no existe`, sin el valor enviado (depends on T014, T015)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cerrar la regresión de lo ya válido

- [x] T018 [P] Revisar los `*IT.java` de `src/test/java/com/cc/brokerage/adapter/in/web/` y actualizar solo las aserciones que exijan el valor enviado dentro de `message`
- [x] T019 Ejecutar `mvn verify` desde `cc-backend-challenge` y los curl de `specs/002-harden-api-inputs/quickstart.md`. No agregar un OpenAPI escrito a mano: el contrato sigue siendo el que genera springdoc en `/v3/api-docs`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - No depende de US1 para el dominio. El HTTP de un número que no entra en el entero usa el traductor de US1
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Amplía `InputText.java`, que US2 crea en T009. El portfolio y la búsqueda se prueban sin la alta

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 2

```bash
# Tests de US2, archivos distintos:
Task: "Crear src/test/java/com/cc/brokerage/domain/model/MoneyMathTest.java"
Task: "Ampliar src/test/java/com/cc/brokerage/application/PlaceMarketOrderServiceTest.java y src/test/java/com/cc/brokerage/application/PlaceLimitOrderServiceTest.java"
Task: "Crear src/test/java/com/cc/brokerage/adapter/in/web/HardenNumbersIT.java"

# Implementación en paralelo después de esos tests:
Task: "Cambiar src/main/java/com/cc/brokerage/domain/model/MoneyMath.java"
Task: "Crear src/main/java/com/cc/brokerage/domain/model/InputText.java"
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
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
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
- US3 edita `InputText.java` y `PlaceOrderService.java` después de US2. No los tomes en paralelo con T009, T010 y T011
