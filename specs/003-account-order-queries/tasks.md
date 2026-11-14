# Tasks: Account queries

**Input**: Design documents from `/specs/003-account-order-queries/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/queries.md, quickstart.md

**Tests**: Included, as in features 001 and 002. First the use case or the `*IT.java`, then the code that makes it pass. `mvn verify`.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

## Path Conventions

- One Maven module at the root of `cc-backend-challenge/`
- Code in `src/main/java/com/cc/brokerage/`
- Tests in `src/test/java/com/cc/brokerage/`
- No new tables or columns. No idempotency. The error codes are those of `contracts/queries.md`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: A single place for the current close, with no new dependencies

- [x] T001 Create `src/main/java/com/cc/brokerage/domain/service/LatestPrices.java` which, from a list of `MarketPrice`, keeps the one with the highest `date` and, on the same date, the highest `id`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The error body carries a code and a message. Blocks the stories that reject

- [x] T002 Create `src/main/java/com/cc/brokerage/adapter/in/web/ErrorCodes.java` with the map from stable messages to the codes of `contracts/queries.md`
- [x] T003 Change `src/main/java/com/cc/brokerage/adapter/in/web/ApiError.java` to `code` and `message`, and make `src/main/java/com/cc/brokerage/adapter/in/web/ApiExceptionHandler.java` fill it without repeating the value sent

**Checkpoint**: A 404 for a missing account answers `ACCOUNT_NOT_FOUND` and the usual message

---

## Phase 3: User Story 1 - See the orders and the reservation (Priority: P1)

**Goal**: List and query orders, and show settled and reserved cash

**Independent Test**: On `10001`, the portfolio reports `753000.00` and `125500.00`, the list carries the `NEW` buy of 50 `LOMA` at `710.00`, and another account cannot read it

### Tests for User Story 1

- [x] T004 [P] [US1] Assert settled and reserved in `src/test/java/com/cc/brokerage/domain/service/PortfolioCalculatorTest.java` and in `src/test/java/com/cc/brokerage/adapter/in/web/PortfolioControllerIT.java`
- [x] T005 [P] [US1] Cover the list, an own query, another account's query and a missing one in `src/test/java/com/cc/brokerage/adapter/in/web/OrderQueryIT.java`

### Implementation for User Story 1

- [x] T006 [US1] Publish the computed `settledCash` and `reservedCash` in `src/main/java/com/cc/brokerage/domain/model/Portfolio.java`, `src/main/java/com/cc/brokerage/domain/service/PortfolioCalculator.java` and `src/main/java/com/cc/brokerage/adapter/in/web/PortfolioResponse.java`. Do not store them
- [x] T007 [US1] Use `LatestPrices` in `src/main/java/com/cc/brokerage/application/GetPortfolioService.java`
- [x] T008 [US1] Add `src/main/java/com/cc/brokerage/application/ListOrdersService.java` and `src/main/java/com/cc/brokerage/application/GetOrderService.java`, sorted by placement date and ascending id, and expose `GET` in `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java`. The order is looked up by id and account together: another account's order or a missing one answers `ORDER_NOT_FOUND`

**Checkpoint**: User Story 1 is tested on its own

---

## Phase 4: User Story 2 - Understand the placement result (Priority: P2)

**Goal**: Placement reports the amount and, if rejected, the reason. A later query does not carry the reason

**Independent Test**: Buying too much `GGAL` on `10001` answers `REJECTED`, `INSUFFICIENT_CASH` and the amount. The reference portfolio does not change

### Tests for User Story 2

- [x] T009 [P] [US2] Assert reason and amount in `src/test/java/com/cc/brokerage/adapter/in/web/RejectOrderIT.java` and the `Location` header in `src/test/java/com/cc/brokerage/adapter/in/web/PlaceMarketOrderIT.java`

### Implementation for User Story 2

- [x] T010 [US2] Create `src/main/java/com/cc/brokerage/domain/model/RejectionReason.java` and return amount and reason from `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` without persisting them. Reasons: `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES`, `NO_MARKET_PRICE`
- [x] T011 [US2] Publish `amount` and `rejectionReason` only in the placement response, and `Location` pointing to the `GET` of that order, in `src/main/java/com/cc/brokerage/adapter/in/web/OrderResponse.java` and `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java`

**Checkpoint**: User Stories 1 and 2 work. Repeating the placement creates another order

---

## Phase 5: User Story 3 - Trade the ticker the search finds (Priority: P3)

**Goal**: `ggal` trades `GGAL`. `PESOS` and `ARS` are not tradable. There is a quote by ticker

**Independent Test**: Placement with `ggal` and searches `PESOS` and `ARS`

### Tests for User Story 3

- [x] T012 [P] [US3] Cover `ggal`, `PESOS`, `ARS` and `GET /instruments/PAMP` in `src/test/java/com/cc/brokerage/adapter/in/web/InstrumentControllerIT.java` and `src/test/java/com/cc/brokerage/adapter/in/web/PlaceMarketOrderIT.java`

### Implementation for User Story 3

- [x] T013 [US3] Resolve the ticker case-insensitively in `src/main/java/com/cc/brokerage/adapter/out/persistence/InstrumentJpaRepository.java` and `src/main/java/com/cc/brokerage/adapter/out/persistence/PersistenceAdapter.java`. The response shows the catalogue ticker
- [x] T014 [US3] Exclude `MONEDA` in `src/main/java/com/cc/brokerage/application/SearchInstrumentsService.java` and add `src/main/java/com/cc/brokerage/application/GetInstrumentQuoteService.java` with `GET /instruments/{ticker}` in `src/main/java/com/cc/brokerage/adapter/in/web/InstrumentController.java`. Currency: `INSTRUMENT_NOT_TRADABLE`

**Checkpoint**: User Story 3 is tested on its own

---

## Phase 6: User Story 4 - Read an error and the contract (Priority: P4)

**Goal**: Every rejection carries a stable code. The generated OpenAPI explains errors, amounts and that `0.48` is 0.48 %

**Independent Test**: A missing account, an invalid ticker and a cancellation of an order that is not open carry a code and the usual message. `/v3/api-docs` describes the units

### Tests for User Story 4

- [x] T015 [P] [US4] Assert codes in the existing error `*IT.java` and the percentage description in `src/test/java/com/cc/brokerage/adapter/in/web/GeneratedOpenApiIT.java`

### Implementation for User Story 4

- [x] T016 [US4] Annotate operations, `ApiError` and the amount and percentage fields in the records of `src/main/java/com/cc/brokerage/adapter/in/web/` so springdoc publishes `400`, `404`, `409`, the two-decimal text and the `0.48 %` unit

**Checkpoint**: The generated contract matches `contracts/queries.md`

---

## Phase 7: User Story 5 - Use a single current close (Priority: P5)

**Goal**: Portfolio, search, quote and market order use the same close

**Independent Test**: Searching `PAMP`, requesting its quote and buying 1 at market share the price `925.85`

### Tests for User Story 5

- [x] T017 [P] [US5] Assert the same `PAMP` price in search, quote and placement in `src/test/java/com/cc/brokerage/adapter/in/web/InstrumentControllerIT.java` and `src/test/java/com/cc/brokerage/adapter/in/web/PlaceMarketOrderIT.java`

### Implementation for User Story 5

- [x] T018 [US5] Make `src/main/java/com/cc/brokerage/application/SearchInstrumentsService.java`, `src/main/java/com/cc/brokerage/application/GetInstrumentQuoteService.java` and `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` pick the close only through `LatestPrices`. Placement and cancellation of an existing instrument return its ticker, never empty, in `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java`

**Checkpoint**: The four reads share the rule of `research.md`

---

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T019 Run `mvn verify` per `specs/003-account-order-queries/quickstart.md` and leave the regression of `10001`, `PAMP` and a valid placement green

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: starts right away
- **Foundational (Phase 2)**: depends on setup and blocks the rejections with a code
- **User Stories**: US1 can start after setup. US2 depends on US1 for the `Location` pointing to the `GET`. US3 and US5 share `LatestPrices`. US4 depends on the codes of phase 2 existing
- **Polish**: depends on the stories

### User Story Dependencies

- **User Story 1 (P1)**: after T001
- **User Story 2 (P2)**: the `GET` of US1 has to exist for `Location`
- **User Story 3 (P3)**: independent of the list, uses the same catalogue
- **User Story 4 (P4)**: after T002 and T003
- **User Story 5 (P5)**: after T001 and the quote of US3

### Parallel Opportunities

- T004 and T005 can be written together
- T009 and T012 touch different ITs and can move in parallel until they edit the same controller

### Parallel Example: User Story 1

```text
T004 PortfolioCalculatorTest and PortfolioControllerIT
T005 OrderQueryIT
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T001
2. T004 to T008
3. Stop and validate the portfolio of `10001` and the `LOMA` listing

### Incremental Delivery

1. Setup and error foundation
2. US1 list and reservation
3. US2 reason, amount and `Location`
4. US3 ticker and quote
5. US4 contract
6. US5 single close
7. `mvn verify`

---

## Notes

- No table or column is created
- Repeating a placement creates another order
- The reason is not stored and does not come back on the order `GET`
- [P] marks different files
