# Tasks: Harden the inputs

**Input**: Design documents from `/specs/002-harden-api-inputs/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. The plan requires `*Test.java` and `*IT.java` to be written first and to fail before implementation. JUnit Jupiter and Mockito for domain and use cases. Integration uses the Testcontainers PostgreSQL that `IntegrationTestBase` already starts.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- One Maven module at the root of `cc-backend-challenge/`
- Code in `src/main/java/com/cc/brokerage/`
- Unit tests in `src/test/java/com/cc/brokerage/` with the `Test` suffix
- Integration tests in `src/test/java/com/cc/brokerage/` with the `IT` suffix
- The error body is still `{ "message": "..." }`. The texts are the ones in `specs/002-harden-api-inputs/contracts/inputs.md`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Strict reading of the document, with no new dependencies

- [x] T001 [P] Enable in `src/main/resources/application.yml` `spring.jackson.deserialization.fail-on-unknown-properties: true`, `spring.jackson.deserialization.fail-on-trailing-tokens: true`, `spring.jackson.read.strict-duplicate-detection: true` and `spring.jackson.datatype.enum.fail-on-numbers-for-enums: true`. Do not add dependencies to `pom.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Keep a scalar from turning into another type before any story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T002 Create `src/main/java/com/cc/brokerage/config/JsonMapperConfig.java` with a `JsonMapperBuilderCustomizer` that sets `CoercionAction.Fail` when an integer, a decimal, a text, a side or a type arrives as text, number, boolean, array or object of another shape. An empty body, an array at the root or a different content type gets the same rejection as an unreadable request

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Reject a request that cannot be interpreted (Priority: P1) 🎯 MVP

**Goal**: An order request that is unreadable, empty, wrongly typed, with an extra field or with a repeated field stores no order and returns neither the body nor an internal diagnosis

**Independent Test**: Send a body that cannot be read, an empty body, a number where text goes, text where a size goes, a field the operation does not define and the same field twice. No new order appears and the response does not quote the body sent

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T003 [US1] Create `src/test/java/com/cc/brokerage/adapter/in/web/HardenDocumentIT.java`, extending `IntegrationTestBase`, for `POST /api/brokerage-api/v1/accounts/10001/orders`. A body that is not a document, an empty body, a document that is not an object, and trailing tokens answer 400 with `The request cannot be read.`; a different content type answers 415, without the body sent. A field the operation does not define answers 400 with `The request contains a field that is not allowed.`, without the name of that key. A repeated `size` answers 400 with `The size field is duplicated.`. A numeric ticker or a size as text answer 400 with `The ticker field is not valid.` or `The size field is not valid.`. In every case no order is stored

### Implementation for User Story 1

- [x] T004 [US1] Change `src/main/java/com/cc/brokerage/adapter/in/web/ApiExceptionHandler.java` so that `HttpMessageNotReadableException` and a malformed path argument answer 400 with `ApiError`, and `HttpMediaTypeNotSupportedException` answers 415 with `ApiError`, never with `exception.getMessage()`. Unreadable, empty, not an object or with trailing tokens: `The request cannot be read.`. Unknown field: `The request contains a field that is not allowed.`. One of our fields repeated: `The size field is duplicated.` and the equivalent for ticker, side, type, amount or price. Wrong type: `The ticker field is not valid.` and the equivalent for side, type, size, amount or price. If the reader does not point to one of our fields, use `The request cannot be read.` (depends on T003)

**Checkpoint**: User Story 1 rejects the broken document without storing and without leaking the reader

---

## Phase 4: User Story 2 - Reject numbers that do not fit the model (Priority: P2)

**Goal**: Size, amount, price and order id out of range are not stored trimmed or with another sign. An amount overflow does not store even a business-rejected order

**Independent Test**: Size `-1`, size `0`, size greater than `2147483647`, negative amount or price, amount or price with more than 8 integer digits after rounding, and id `0` or negative. None stores an order or changes the portfolio. Size `2147483647` is not rejected for its size

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T005 [P] [US2] Create `src/test/java/com/cc/brokerage/domain/model/MoneyMathTest.java`. After rounding half-up to 2, `99999999.994` is accepted as `99999999.99` and `99999999.995` is rejected for exceeding 8 integer digits. A quotient greater than `2147483647` does not return a truncated integer. Quotient `0` stays `0`. Quotient `2147483647` is accepted
- [x] T006 [P] [US2] Extend `src/test/java/com/cc/brokerage/application/PlaceMarketOrderServiceTest.java` and `src/test/java/com/cc/brokerage/application/PlaceLimitOrderServiceTest.java`. An amount whose quotient exceeds `2147483647` does not call `save`. An amount that rounds to more than 8 integer digits, a non-positive amount and a non-positive limit price do not store either. A small amount whose quotient is `0`, and a market order without a close, are still stored `REJECTED`
- [x] T007 [P] [US2] Create `src/test/java/com/cc/brokerage/adapter/in/web/HardenNumbersIT.java`, extending `IntegrationTestBase`. Size `0`, `-1` or greater than `2147483647` answers 400 and does not store. Size `2147483647` does not answer 400 for its size. A negative, non-numeric amount or price, or one with 9 integer digits after rounding, answers 400. `POST /api/brokerage-api/v1/accounts/10001/orders/0/cancellation` and an id that is not an integer from 1 to 2147483647 answer 400 with `The order is not valid.` and do not cancel another order. A well-formed missing order answers 404 with `Order not found.`, without the id

### Implementation for User Story 2

- [x] T008 [P] [US2] Change `src/main/java/com/cc/brokerage/domain/model/MoneyMath.java` to round half-up to 2 and only then require a value greater than zero with at most 8 integer digits. `sharesFromAmount` computes the integer quotient and, if it is greater than `2147483647`, throws `InvalidRequestException` with `Size must be an integer from 1 to 2147483647.`. Quotient `0` is returned as `0`. Do not use `intValue()` (depends on T005)
- [x] T009 [P] [US2] Create `src/main/java/com/cc/brokerage/domain/model/InputText.java` with the order id: an integer from 1 to 2147483647. Zero, negative, text, spaces and a larger number throw `InvalidRequestException` with `The order is not valid.`. `0001` is order 1 (depends on T007)
- [x] T010 [US2] Change `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` to use the rounding and the quotient of `MoneyMath` before `save`. Size outside 1 to 2147483647: `Size must be an integer from 1 to 2147483647.`. Non-positive amount: `Amount must be greater than zero.`. Amount above 8 integer digits: `Amount exceeds the maximum allowed.`. Missing or non-positive limit price: `Price is required and must be greater than zero.`. Price above the maximum: `Price exceeds the maximum allowed.`. `Exactly one quantity is required: shares or amount.`, `Side must be BUY or SELL.` and `Order type is required.` stay. The overflow does not store; quotient `0` and the market order without a close are still stored `REJECTED` (depends on T006, T008)
- [x] T011 [US2] Change `src/main/java/com/cc/brokerage/adapter/in/web/OrderController.java` to take `{orderId}` as text, parse it with `InputText` and not call the cancellation if it does not fit 1 to 2147483647. Change `src/main/java/com/cc/brokerage/domain/OrderNotFoundException.java` so the message is `Order not found.`, without the id (depends on T007, T009)

**Checkpoint**: User Story 2 rejects out-of-range numbers without truncating and without changing the existing business rejection

---

## Phase 5: User Story 3 - Treat text as data, not as an instruction (Priority: P3)

**Goal**: Account, ticker and search have a closed format or a bounded text. A text that looks like an instruction is not executed, does not pick another record and returns no internal error

**Independent Test**: Portfolio of `10001' OR '1'='1`, order with ticker `PAMP'; DROP TABLE orders;--` and search `pamp' OR 1=1`. The account and the ticker are rejected. The search modifies no data. `10001` and `PAMP` keep answering as before

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T012 [P] [US3] Create `src/test/java/com/cc/brokerage/domain/model/InputTextTest.java`. The account is 1 to 20 digits, untrimmed: leading or trailing spaces are rejected and leading zeros are kept. The ticker is 1 to 10 letters or digits, untrimmed and with its case unchanged. The search trims only the ends and then allows 1 to 100 Unicode characters, without ISO controls or the null character
- [x] T013 [P] [US3] Extend `src/test/java/com/cc/brokerage/application/SearchInstrumentsServiceTest.java`. Empty search: `The search has no text.`. More than 100 characters: `The search exceeds the maximum length.`. A control or the null character: `The search is not valid.`. `PAMP` and `pamp` find the same instrument. `pamp' OR 1=1` adds no rows. The comparison does not depend on the machine locale
- [x] T014 [P] [US3] Create `src/test/java/com/cc/brokerage/adapter/in/web/HardenTextIT.java`, extending `IntegrationTestBase`. An account with letters, spaces, signs or more than 20 digits answers 400 with `The account is not valid.` on portfolio, placement and cancellation, and does not return another account. A ticker that is empty, has spaces, signs or more than 10 letters or digits answers 400 with `The ticker is not valid.` and does not store. A well-formed account that does not exist answers 404 with `Account not found.`. A ticker of valid format that does not exist answers 404 with `Instrument not found.`. Those 404 do not repeat the value sent. `GET /api/brokerage-api/v1/accounts/10001/portfolio` and `GET /api/brokerage-api/v1/instruments?q=PAMP` keep answering as before

### Implementation for User Story 3

- [x] T015 [US3] Complete `src/main/java/com/cc/brokerage/domain/model/InputText.java` with the account (1 to 20 digits, untrimmed; otherwise `The account is not valid.`), the ticker (1 to 10 letters or digits, untrimmed and with its case unchanged; otherwise `The ticker is not valid.`) and the search (ends trimmed, 1 to 100 Unicode characters, without ISO controls or the null character). Empty: `The search has no text.`. Too long: `The search exceeds the maximum length.`. With a control: `The search is not valid.` (depends on T012)
- [x] T016 [P] [US3] Change `src/main/java/com/cc/brokerage/application/SearchInstrumentsService.java` to accept the text through `InputText` before reading the catalogue and compare it in memory with `Locale.ROOT`, case-insensitively and without concatenating it into a query (depends on T013, T015)
- [x] T017 [P] [US3] Validate the account with `InputText` at the start of `src/main/java/com/cc/brokerage/application/GetPortfolioService.java`, `src/main/java/com/cc/brokerage/application/PlaceOrderService.java` and `src/main/java/com/cc/brokerage/application/CancelOrderService.java`, and the ticker at the start of `PlaceOrderService`, before any repository. Change `src/main/java/com/cc/brokerage/domain/AccountNotFoundException.java` to `Account not found.` and `src/main/java/com/cc/brokerage/domain/InstrumentNotFoundException.java` to `Instrument not found.`, without the value sent (depends on T014, T015)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Close the regression of what was already valid

- [x] T018 [P] Review the `*IT.java` in `src/test/java/com/cc/brokerage/adapter/in/web/` and update only the assertions that expect the value sent inside `message`
- [x] T019 Run `mvn verify` from `cc-backend-challenge` and the curl calls of `specs/002-harden-api-inputs/quickstart.md`. Do not add a hand-written OpenAPI: the contract is still the one springdoc generates at `/v3/api-docs`

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
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Does not depend on US1 for the domain. The HTTP side of a number that does not fit the integer uses the translator from US1
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Extends `InputText.java`, which US2 creates in T009. Portfolio and search are tested without placement

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
# US2 tests, different files:
Task: "Create src/test/java/com/cc/brokerage/domain/model/MoneyMathTest.java"
Task: "Extend src/test/java/com/cc/brokerage/application/PlaceMarketOrderServiceTest.java and src/test/java/com/cc/brokerage/application/PlaceLimitOrderServiceTest.java"
Task: "Create src/test/java/com/cc/brokerage/adapter/in/web/HardenNumbersIT.java"

# Implementation in parallel after those tests:
Task: "Change src/main/java/com/cc/brokerage/domain/model/MoneyMath.java"
Task: "Create src/main/java/com/cc/brokerage/domain/model/InputText.java"
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
- US3 edits `InputText.java` and `PlaceOrderService.java` after US2. Do not take them in parallel with T009, T010 and T011
