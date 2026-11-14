# Feature Specification: Account queries

**Feature Branch**: `003-account-order-queries`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "Complete the account without changing the delivered model. The orders of an account and a single order can be queried. The portfolio also reports settled and reserved cash. Placing an order says how to get back to that order, explains a rejection and gives the amount. The ticker is traded the same way it is searched. The currency is not offered as a share. Each rejection is a problem with a type taken from the OpenAPI definition, a title, the HTTP status, a human-readable detail and a stable code. invalid_params comes only on an API validation. The published contract explains those errors, the amounts and the percentages. Portfolio, search and market order use the same current close."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the orders and the reservation (Priority: P1)

A person queries an account and sees, besides the portfolio, the orders that account already has. They can open a single order and, if it is still open, cancel it. The portfolio shows how much cash is settled, how much is reserved and how much is available. Those amounts are computed from the orders already stored. They are not stored again.

**Why this priority**: Placement and cancellation already exist, but whoever loses the placement response cannot find the order or explain why the available balance dropped.

**Independent Test**: With account `10001` and no new orders, list its orders, open the open limit buy of 50 `LOMA` at `710.00`, cancel it from that data and see the available balance grow by `35500.00`.

**Acceptance Scenarios**:

1. **Given** account `10001` and its reference orders, **When** its orders are listed, **Then** all of them appear, oldest first and, at the same moment, by increasing id. The open limit buy of 50 `LOMA` at `710.00` is in the list, with its ticker, side, type, size, price, status and date.
2. **Given** that list, **When** that order is queried within account `10001`, **Then** the result is that same order.
3. **Given** an order of account `10001`, **When** another account queries it, **Then** it is not shown and the problem is the same as for a missing order (`404`, `BRK-003`), so as not to reveal that the id exists. The order does not change.
4. **Given** an id that does not exist, **When** it is queried, **Then** the problem says the order does not exist, without repeating the id.
5. **Given** account `10001` before any new order, **When** the portfolio is queried, **Then** the total value stays at `889756.00`, available at `627500.00`, settled cash is `753000.00` and reserved cash is `125500.00`.
6. **Given** an accepted placement, **When** the response is read, **Then** it says how to get that order, and that query returns the order just created.

---

### User Story 2 - Understand the placement result (Priority: P2)

Whoever sends an order sees in that response whether it was filled, left open or rejected, why it was rejected and which amount it committed.

**Why this priority**: Today a rejection is stored and answers created, without saying whether pesos, shares, a whole share or the close were missing.

**Independent Test**: Try to buy more `GGAL` shares than the available balance of `10001` covers and read in the response the insufficient-cash reason and the amount. The reference portfolio does not change.

**Acceptance Scenarios**:

1. **Given** account `10001`, **When** a buy that needs more pesos than are available is sent, **Then** the order is `REJECTED` and the response says cash is insufficient, together with the amount of size times the applicable price.
2. **Given** account `10001` with 40 available `PAMP` shares, **When** 100 are sold at market, **Then** the response says shares are insufficient.
3. **Given** the `GGAL` close, **When** a buy is placed for an amount that does not buy a whole share, **Then** the response says the resulting size is zero.
4. **Given** an instrument without a close, **When** a market order is sent, **Then** the response says there is no current price.
5. **Given** a `FILLED` or `NEW` placement, **When** the response is read, **Then** it carries the computed amount, size times price already rounded to two decimals. A later query shows the order as stored. The rejection reason is explained only in the placement response and is not added to the order.

---

### User Story 3 - Trade the ticker the search finds (Priority: P3)

A person searches an instrument without minding case and then trades it with the same text. The peso currency does not appear as if it were a share that can be bought.

**Why this priority**: The search already accepts `ggal`. Placing with that same text today says the instrument does not exist.

**Independent Test**: Send a buy of `ggal` on account `10001` and check that it trades `GGAL`. Search `PESOS` and `ARS` and check that no tradable share is offered.

**Acceptance Scenarios**:

1. **Given** the reference catalogue, **When** an order with ticker `ggal` is sent, **Then** the order is on `GGAL` and the response shows that ticker.
2. **Given** the reference catalogue, **When** `PESOS` or `ARS` is searched, **Then** the currency does not come as a tradable instrument. A new deposit or withdrawal still cannot be recorded.
3. **Given** a ticker of the catalogue, **When** its quote is requested, **Then** that single instrument comes back, with latest price and daily return, or unavailable if there is no close. A text search still returns the ticker or name matches.

---

### User Story 4 - Read a problem and the contract (Priority: P4)

Whoever integrates the account tells a rejection apart by a `BRK-xxx` code and by the problem type published in the contract. The detail and the titles are in English. If the rejection is an API validation, the response names the invalid parameters. If it is not, that list does not come.

**Why this priority**: Today the client can only compare the message text. A `0.48` return can be read as 0.48 % or as 48 %.

**Independent Test**: Cause an invalid ticker, a missing account and a cancellation of an order that is not open. The invalid ticker carries `invalid_params`. The missing account and the order that is not open carry their code and detail, without `invalid_params` and without the value sent. The contract describes those problems and the units.

**Acceptance Scenarios**:

1. **Given** an API parameter that fails validation, **When** the operation is rejected, **Then** the response is a problem with `type` equal to the URI of the `InvalidParameters` schema of the OpenAPI definition, `title` "Invalid parameters", `status` equal to HTTP `400`, `detail` in English, `code` `BRK-xxx` and `invalid_params`. Each item of `invalid_params` carries `field` and `message`. Neither `detail` nor `message` repeats the value sent. The code does not change if the wording is adjusted.
2. **Given** an invalid ticker, **When** the placement is rejected, **Then** the problem is the invalid-parameters one, `code` is `BRK-009` and `invalid_params` points at the `ticker` field with the detail "The ticker is not valid.".
3. **Given** an account, instrument or order that does not exist, or an order of another account, **When** the query is rejected, **Then** `type` is the URI of the `NotFound` schema, `title` is "Not found", `status` is `404`, `code` is `BRK-004`, `BRK-002` or `BRK-003` depending on the resource, and `invalid_params` does not come.
4. **Given** an order that is not open, **When** the cancellation is rejected, **Then** `type` is the URI of the `Conflict` schema, `title` is "Conflict", `status` is `409`, `code` is `BRK-005`, and `invalid_params` does not come.
5. **Given** a business rule that is not a parameter validation, such as trading the currency or recording a new deposit or withdrawal, **When** it is rejected, **Then** `type` is the URI of the `BadRequest` schema, `title` is "Bad request", `status` is `400`, `code` is `BRK-006` or `BRK-007`, and `invalid_params` does not come.
6. **Given** a body with a field that does not belong, **When** it is rejected, **Then** it is an API validation and carries `invalid_params`, but it does not name that field or quote the body.
7. **Given** the contract published by the application, **When** it is read, **Then** it publishes the `InvalidParameters`, `BadRequest`, `NotFound` and `Conflict` schemas, describes the rejections of these operations, presents amounts as text with two decimals and states that a return of `0.48` means 0.48 %. The `type` of each problem is the URI of its schema in that definition.
8. **Given** account `10001`, a `PAMP` search and a valid placement, **When** they are called as before, **Then** they still complete. A body that is unreadable, empty, with an extra, repeated or wrongly typed field still stores nothing and does not quote the body.

---

### User Story 5 - Use a single current close (Priority: P5)

Portfolio, search, quote and market order take the same latest close of each instrument: the one of the most recent observation. Placement and cancellation show the ticker of that instrument.

**Why this priority**: If each read picks a different close, the price of the search and that of the market order stop being the same number.

**Independent Test**: Search `PAMP`, request its quote and buy 1 share at market on account `10001`. The price in the three responses is the same current close.

**Acceptance Scenarios**:

1. **Given** an instrument with more than one close, **When** the portfolio is queried, it is searched, its quote is requested or a market order is sent, **Then** all four use the close of the most recent observation. On the same date, the newest observation wins.
2. **Given** a placement or cancellation of an instrument that exists, **When** the response is read, **Then** the ticker is the instrument's. It is not empty.
3. **Given** account `10001` before new orders, **When** the portfolio is queried, **Then** quantities, values and returns of `BMA`, `METR` and `PAMP` are still those of the query already delivered, with the total return on average cost.

---

### Edge Cases

- A position whose net quantity is zero is not shown. The short `BMA` position is still shown, with negative quantity and market value.
- An instrument without a latest close stays in the position with value and returns unavailable, and is left out of the total value. Its ticker is listed in `unpricedTickers`, so the total value does not look complete.
- If the previous close is zero or missing, the daily return is unavailable.
- If there is no average cost (no `FILLED` buys on a long, no `FILLED` sells on a short), the total return is unavailable.
- The currency is still not a position.
- Whoever knows the account number trades. This feature adds no sessions, permissions or attempt limit.
- The types already defined for the order size and its date do not change. No cancellation date is added.
- No table or column is created, and no existing column is modified. The rejection reason, settled cash, reserved cash, the placement amount and the problem of an error are computed in the response. They are not stored.
- A validation that names a known field, such as the account, the ticker, the order id, the search, the side, the type, the size, the amount or the price, carries that field in `invalid_params`. If both forms of quantity are missing, or both come, the list points at size and amount.
- An unreadable request is a `BadRequest` problem and carries no `invalid_params`.
- A request with a content type other than `application/json` is an `UnsupportedMediaType` problem (`415`) and carries no `invalid_params`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST list the orders of an existing account and MUST allow querying one by its id. An order of another account MUST NOT be shown and MUST answer the same as a missing order. A missing order MUST be reported without repeating the id.
- **FR-002**: The list MUST include ticker, side, type, size, price, status and date, oldest first and, at the same moment, by increasing id.
- **FR-003**: The portfolio MUST report the total value, settled cash, reserved cash and available cash. Settled and reserved MUST be computed from the orders already stored and MUST NOT be stored again. Reserved MUST be the sum of the open buys. Open sells MUST still reserve shares.
- **FR-004**: An accepted placement, or a rejected one that is stored, MUST say how to query that order. That query MUST return the same order.
- **FR-005**: The placement response MUST include the amount computed at that moment, size times the applicable price already rounded. That amount MUST NOT be stored. If the status is `REJECTED`, it MUST include a stable reason among insufficient cash, insufficient shares, zero resulting size and no current price. The reason MUST NOT be stored and MUST NOT be added to the order.
- **FR-006**: A query of the order MUST show the status and the data the order already stores. It MUST NOT require the rejection reason.
- **FR-007**: Placement MUST resolve the ticker case-insensitively. The response MUST show the catalogue ticker.
- **FR-008**: A search MUST NOT offer the currency as a tradable instrument. A new peso deposit or withdrawal MUST still not be stored.
- **FR-009**: It MUST be possible to request the quote of a ticker. If there is no close, the price and the daily return MUST be reported as unavailable.
- **FR-010**: Every domain rejection MUST answer a problem with `type`, `title`, `status`, `detail` and `code`. `type` MUST be the URI of the matching schema in the OpenAPI definition published by the application. `status` MUST be the HTTP status declared for that error. `detail` MUST be the English text of that error and MUST NOT repeat the value sent or an internal detail. `code` MUST be the `BRK-xxx` of that error, MUST exist only in the response and MUST NOT be stored. The code does not change if the wording is adjusted. An unreadable request, an extra field or a wrongly typed one MUST stay outside that catalogue, with `code` `UNREADABLE`, `UNKNOWN_FIELD` or `INVALID_FIELD` and the detail in English. A repeated field is not checked: the last value wins.
- **FR-011**: `invalid_params` MUST come only when the rejection is an API validation. Each item MUST carry `field` and `message`, and `message` MUST NOT repeat the value sent. A field that does not belong MUST be treated as an API validation without naming that field. On a missing resource, a conflict or a business rule, `invalid_params` MUST NOT come.
- **FR-012**: The published contract MUST include the `InvalidParameters`, `BadRequest`, `NotFound` and `Conflict` schemas, MUST describe the rejections, MUST present amounts as text with two decimals and MUST state that a return reported as `0.48` is 0.48 %.
- **FR-013**: Portfolio, search, quote and market order MUST use the close of the most recent observation of the instrument. On the same date, the newest observation MUST be used.
- **FR-014**: Placement and cancellation of an existing instrument MUST return its ticker.
- **FR-015**: Account `10001`, the `PAMP` search and a valid placement MUST keep completing as before this feature. The reference positions, available cash and total value MUST be kept. Repeating that placement MUST create another order.
- **FR-016**: This feature MUST NOT create tables or columns, and MUST NOT modify existing columns. Orders, accounts, instruments and prices MUST still be stored only where they already are.

### Key Entities

- **Order**: The stored order of an account, with the data it already has: ticker, side, type, size, price, status and placement date. Listing or querying it adds no data.
- **Account reservation**: Settled cash, cash reserved by open buys and available cash. Computed when answering, from the existing orders. Not stored.
- **Placement result**: What comes back when placing. The amount and, if it was rejected, the reason are computed in that response and not stored.
- **Quote**: The read of an instrument that already exists, with its latest price and daily return, or without them if there is no close. No new quote is stored.
- **Problem**: The rejection of an operation. It carries the type published in the contract, an English title, the HTTP status, the English detail and a `BRK-xxx` code, except for the unreadable request and the extra, repeated or wrongly typed field. The list of invalid parameters is part of the problem only on an API validation. Not stored.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the reference accounts can list their orders and open one of their own. 100% of the attempts to read another account's order or a missing one show no data of that order and do not modify it.
- **SC-002**: On account `10001`, before new orders, the portfolio reports `753000.00` settled, `125500.00` reserved, `627500.00` available and `889756.00` total value.
- **SC-003**: 100% of the stored placement rejections carry one of the four defined reasons and the amount. A buy with `ggal` trades `GGAL`. Searching `PESOS` or `ARS` returns no tradable share.
- **SC-004**: 100% of the rejections carry `type`, `title`, `status`, `detail` and `code`, without the value sent. `invalid_params` is in 100% of the API validations and in 0% of the other rejections. The published contract describes those problems and the unit of the percentages. The price of `PAMP` in the search, in the quote and in a market buy is the same.

## Assumptions

- This feature builds on the portfolio, search, placement and cancellation already delivered, and on the input hardening. It does not reopen those rules except where a scenario of this feature says so.
- Repeating the same request creates another order, as until now. No sessions, permissions or attempt cap are added.
- No table or column is created, and no column is modified. The order size and date keep the types of the reference model. The date is still the placement date. No cancellation date is added.
- Whoever queries the order sees the `REJECTED` status and the data that order already had. The reason is understood from the placement response.
- Whoever knows the account number trades, as before.
- The detail of each domain rejection is the English message of its `BRK-xxx` code. The problem adds the type, the title, the status and that code. Adjusting the wording does not change the code.
- `type` is not another company's address. It is the URI of the schema inside the OpenAPI definition this application publishes.
- The titles are "Invalid parameters", "Bad request", "Not found", "Conflict" and "Unsupported media type", one per schema.
