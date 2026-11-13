# Feature Specification: Portfolio, search and orders

**Feature Branch**: `001-brokerage-trading-api`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "An investment account in pesos must be queryable (total value, pesos available to trade, and positions with quantity, market value and total return), it must be possible to search market instruments by ticker and/or name, and to send MARKET and LIMIT buy or sell orders. MARKET orders fill at once at the latest price; LIMIT orders stay open. If there is not enough cash or shares, the order is stored as rejected. Only open orders can be cancelled. Cash and holdings are rebuilt from the filled orders. Peso transfers are modelled as orders. There is no market simulation and no user authentication. The reference set of accounts, instruments, prices and orders is used as delivered."

## Clarifications

### Session 2026-11-12

- Q: How is the total return of each position computed? → A: (Latest close − average cost of the shares still held) / average cost. Revised on 2026-11-13: the first answer, based on net invested capital, inflated the result after partial sales and lost meaning when sells exceeded buys.
- Q: Do orders that are still open reserve the available cash or shares? → A: Open buys reserve pesos and open sells reserve shares
- Q: Can new peso deposits and withdrawals be recorded, or does the portfolio only use the ones already loaded? → A: No new ones are recorded; the portfolio only uses the deposits and withdrawals already loaded
- Q: Which instruments does a search return? → A: The instruments whose ticker or name contains the text, case-insensitively
- Q: Is the daily return shown in addition to the total return? → A: It is shown on every position and every instrument found, in addition to the total return of the position

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Get the portfolio (Priority: P1)

A person gives the account number and gets the state of that account: its total value, how many pesos are free to trade, and which instruments it holds, with the number of shares, the peso value of each position, its total return and its daily return.

**Why this priority**: It is the main information requested. With the reference data it can be shown without sending new orders.

**Independent Test**: Query account `10001` (`emiliano@test.com`) on the reference set and compare cash, total value and positions with the values of this story.

**Acceptance Scenarios**:

1. **Given** the reference set, **When** account `10001` is queried, **Then** the result shows exactly:
   - pesos available to trade: `627500.00`
   - total account value: `889756.00`
   - positions, sorted by ticker:

   | Ticker | Quantity | Market value | Total return | Daily return |
   |--------|----------|--------------|--------------|--------------|
   | BMA    | -10      | -15028.00    | 1.78%        | -1.15%       |
   | METR   | 500      | 114750.00    | -8.20%       | -1.08%       |
   | PAMP   | 40       | 37034.00     | -0.45%       | 0.44%        |

2. **Given** an existing account without orders, **When** its portfolio is queried, **Then** the total value is `0.00`, the available pesos are `0.00` and the list of positions is empty.
3. **Given** an account number that does not exist, **When** the portfolio is queried, **Then** there is no portfolio result and the response says the account does not exist.
4. **Given** account `10001`, **When** the portfolio is queried without identifying oneself, **Then** the query is served anyway: there is no access control.
5. **Given** the peso deposits and withdrawals already loaded on account `10001`, **When** its portfolio is queried, **Then** those movements are part of the `753000.00` of settled cash behind the available cash and the total value of this story. The currency does not appear as a position.
6. **Given** account `10001`, **When** someone tries to record a new peso deposit or withdrawal, **Then** no order is stored and the portfolio stays the same.

---

### User Story 2 - Search instruments (Priority: P2)

A person types a text and gets the market instruments whose ticker or name matches that text, without duplicates.

**Why this priority**: An instrument has to be found before trading, and it can be tested with the reference catalogue alone.

**Independent Test**: Search `banco` and `pamp` on the reference catalogue and check matches, absence of duplicates and price data.

**Acceptance Scenarios**:

1. **Given** the reference catalogue, **When** `banco` is searched, **Then** the result contains only, in this order, `BBAR` (Banco Frances), `BHIP` (Banco Hipotecario S.A.), `BMA` (Banco Macro S.A.) and `BPAT` (Banco Patagonia).
2. **Given** the reference catalogue, **When** `pamp` is searched, **Then** the result contains `PAMP` (Pampa Holding S.A.) once, with latest price `925.85` and daily return `0.44%`.
3. **Given** the reference catalogue, **When** `YPF` is searched, **Then** the result includes `YPFD` because the ticker contains that text.
4. **Given** the reference catalogue, **When** `zzzz` is searched, **Then** the result is an empty list.
5. **Given** an empty text or one made only of spaces, **When** it is searched, **Then** there is no list and the response says the search has no text.
6. **Given** an instrument without a price, **When** it appears in a search, **Then** it is identified anyway and its price and daily return are reported as unavailable.

---

### User Story 3 - Send a market order (Priority: P3)

A person buys or sells an instrument at the current price. They give an exact number of shares or an amount in pesos. The order fills at once, is recorded as filled, and the portfolio reflects the new holding and the new cash.

**Why this priority**: It is the operation that changes the account. It does not depend on leaving orders open.

**Independent Test**: Starting from the reference set, buy 1 share of `GGAL` at market on account `10001` and check status, price, cash and position.

**Acceptance Scenarios**:

1. **Given** account `10001` and the latest `GGAL` close at `885.80`, **When** a MARKET buy of 1 share is sent without a price, **Then** the order is `FILLED`, with size `1` and price `885.80`, settled cash becomes `752114.20`, available pesos become `626614.20`, and a `GGAL` position of 1 share appears with value `885.80` and total return `0.00%`.
2. **Given** account `10001`, **When** a MARKET buy of `GGAL` for `2000` pesos is sent, **Then** the size is `2` (integer part of `2000 / 885.80`), the stored price is `885.80`, the status is `FILLED` and cash drops by `1771.60`, not `2000`.
3. **Given** account `10001` with 40 available `PAMP` shares, **When** 1 share is sold at MARKET, **Then** the order is `FILLED` at the `925.85` close, the `PAMP` position becomes 39 shares and cash grows by `925.85`.
4. **Given** a MARKET order, **When** the request also carries a price, **Then** it is rejected with `400` and `BRK-027`, and the order is not stored. A price on a market order would read as a cap the order does not honour.
5. **Given** a MARKET request, **When** it gives both a number of shares and an amount in pesos, or neither, **Then** no order is stored and the response says exactly one of the two must be given.
6. **Given** a request, **When** the size is not a positive integer, the amount is not greater than zero, the instrument does not exist or the account does not exist, **Then** no order is stored and the response names the invalid data.

---

### User Story 4 - Send a limit order (Priority: P4)

A person places a buy or sell order at a chosen price. The order stays open. It never fills by itself. While it is open, the cash or shares it commits are no longer available to trade.

**Why this priority**: The request tells this order type apart from the market one, and the reference set already has open orders the portfolio must honour.

**Independent Test**: Send a new limit buy and check that it stays open, creates no position and only lowers the available pesos.

**Acceptance Scenarios**:

1. **Given** account `10001`, **When** a LIMIT buy of 10 `GGAL` shares at `800.00` is sent, **Then** the order is `NEW` with size `10` and price `800.00`, no new position exists for it, settled cash does not change and available pesos drop by `8000.00`.
2. **Given** a LIMIT order, **When** no price is sent, or the price is not greater than zero, **Then** no order is stored and the response says the price is required.
3. **Given** account `10001`, **When** a LIMIT buy of `GGAL` for `2500` pesos at `800.00` is sent, **Then** the order is `NEW` with size `3` and reserves `2400.00`.
4. **Given** a newly accepted LIMIT order, **When** the portfolio is queried again, **Then** the order is still `NEW`: there is no automatic fill against the latest price.
5. **Given** account `10001` with 40 available `PAMP` shares, **When** a LIMIT sell of 10 shares at `1000.00` is sent, **Then** the order is `NEW`, cash does not change, available `PAMP` shares become 30 and a later sell of 40 shares is rejected.

---

### User Story 5 - Reject orders that cannot be covered (Priority: P5)

If a buy needs more pesos than are available, or a sell needs more shares than are available, the order is not applied. It is recorded as rejected and the account stays the same.

**Why this priority**: It keeps the account from going overdrawn. It can be tested with a single invalid order on the reference data.

**Independent Test**: Try to buy more pesos than available and sell more shares than available; the portfolio of `10001` must stay identical to story 1.

**Acceptance Scenarios**:

1. **Given** account `10001` with `627500.00` available pesos, **When** a MARKET buy of `1000000` `GGAL` shares is sent, **Then** the order is stored `REJECTED` and the portfolio stays as in story 1.
2. **Given** account `10001` with 40 available `PAMP` shares, **When** `100` shares are sold at MARKET, **Then** the order is stored `REJECTED` and the `PAMP` position stays at 40.
3. **Given** the `GGAL` close at `885.80`, **When** a buy for `100` pesos is sent, **Then** the resulting whole size is `0`, the order is stored `REJECTED` and balances do not change.
4. **Given** pesos or shares already reserved by `NEW` orders, **When** a new order needs that same balance, **Then** it is rejected: reserved balance is not reused and the order is not trimmed to fit.
5. **Given** two orders that are valid on their own but together exceed what is available, **When** the first is accepted and the second is sent, **Then** the second is stored `REJECTED`.

---

### User Story 6 - Cancel an open order (Priority: P6)

A person cancels an order that is still open. The order becomes cancelled and what it reserved becomes available again. A filled, rejected or already cancelled order cannot be cancelled, nor can an order of another account.

**Why this priority**: The request requires it, and the reference set already has an identifiable open order, so no new one is needed.

**Independent Test**: Cancel the open limit buy of 50 `LOMA` shares at `710.00` on account `10001` and check the status and the available pesos.

**Acceptance Scenarios**:

1. **Given** account `10001` and its `NEW` limit buy of 50 `LOMA` at `710.00`, **When** the owner cancels it, **Then** the status becomes `CANCELLED`, settled cash does not change and available pesos grow by `35500.00`, to `663000.00`.
2. **Given** a `FILLED`, `REJECTED` or `CANCELLED` order, **When** someone tries to cancel it, **Then** the status does not change and the response says only open orders can be cancelled.
3. **Given** an open order of account `10001`, **When** another account cancels it, **Then** the status does not change and the response says the order does not exist, as with an id that does not exist.
4. **Given** an order that does not exist, **When** someone tries to cancel it, **Then** the response says the order does not exist.
5. **Given** a `NEW` LIMIT sell of 10 `PAMP`, **When** the owner cancels it, **Then** the status becomes `CANCELLED` and the available `PAMP` shares include those 10 again. Cash does not change.

---

### User Story 7 - Identify the service and its public path (Priority: P7)

Whoever starts or calls the service recognises it as `brokerage-api` and uses a single base path, `/api/brokerage-api`, with version `/v1` on every operation, to get the portfolio, search instruments and send or cancel orders.

**Why this priority**: It tells this service apart from others, gives it a stable address and allows another version to be published later without moving the base.

**Independent Test**: Start the service and check that it identifies itself as `brokerage-api`; get the portfolio of `10001`, search `pamp` and send an order using `/api/brokerage-api/v1`. The same call without that base, or with the base but without `/v1`, is not served.

**Acceptance Scenarios**:

1. **Given** the running application, **When** its startup identification is observed, **Then** the visible name is `brokerage-api`.
2. **Given** account `10001`, **When** the portfolio is queried at `/api/brokerage-api/v1/accounts/10001/portfolio`, **Then** it is served as in story 1.
3. **Given** the reference catalogue, **When** `pamp` is searched at `/api/brokerage-api/v1/instruments`, **Then** the result is the one in story 2.
4. **Given** a buy or sell order, **When** it is sent or cancelled under `/api/brokerage-api/v1`, **Then** the rules of stories 3 to 6 still hold.
5. **Given** the same operations, **When** they are called without `/api/brokerage-api` or without `/v1`, **Then** there is no business result: the portfolio is not queried and no order is stored.

---

### User Story 8 - Know whether the API is up (Priority: P8)

Whoever is about to trade, or has just changed the configuration, checks two fixed paths. Health says whether the process and the database are healthy. Info says the process is `brokerage-api`. The contract of the business operations is read from the OpenAPI the application itself generates at startup. There is no hand-written contract.

**Why this priority**: It does not change the portfolio calculation. It lets one detect, before trusting the other operations, that Actuator is exposed too much or too little and that the application answers.

**Independent Test**: `GET /actuator/health` returns `UP` without components. `GET /actuator/info` returns the name `brokerage-api`. `GET /actuator/env` is not served. `GET /v3/api-docs` returns an OpenAPI that includes the portfolio.

**Acceptance Scenarios**:

1. **Given** the running application and an available database, **When** `/actuator/health` is queried, **Then** the response is successful and the status is `UP`, without component details.
2. **Given** the running application, **When** `/actuator/info` is queried, **Then** the response is successful and identifies the application as `brokerage-api`.
3. **Given** the running application, **When** another Actuator endpoint, such as `/actuator/env`, is queried, **Then** it is not served.
4. **Given** the running application, **When** `/v3/api-docs` is requested, **Then** the document is an OpenAPI generated by the application and includes the portfolio, search and order operations under `/api/brokerage-api/v1`. The same description is at `/v3/api-docs.yaml` and can be read at `/swagger-ui.html`.

---

### Edge Cases

- A position whose net quantity is zero is not shown. The short `BMA` position on the reference account is shown, with negative quantity and market value.
- The peso currency is not a position, even though its movements are orders.
- An instrument without a latest close is listed with its quantity and with market value and return unavailable, and is left out of the total account value. The portfolio reports it in `unpricedTickers`.
- If the previous close is zero or missing, the daily return is unavailable.
- If a long position has no `FILLED` buys, or a short one has no `FILLED` sells, there is no average cost and the total return is unavailable.
- A partial sale does not change the average cost of the remaining shares, so it does not change the total return.
- A market order on an instrument without a latest close is stored `REJECTED` and does not change the account.
- What is left of an amount that does not buy another whole share stays as pesos; fractions are not bought.
- A rejected or cancelled order does not change holdings, settled cash or reservations.
- Cancelling an open limit sell returns the reserved shares, not pesos.
- Amounts are reported with two decimals and percentages with two decimals, rounding half-up.
- Whoever knows the account number can query it and trade on it. There are no sessions or permissions.
- An attempt to record a new peso deposit or withdrawal stores no order and does not change cash.
- Health and info live at `/actuator`, outside `/api/brokerage-api`. The generated OpenAPI lives at `/v3/api-docs`. Those paths are not portfolio, search or order operations.
- An Actuator endpoint other than health and info, including one added by a new version of the platform, is not published. No list is used that exposes everything and then excludes some.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow querying the portfolio of an existing account from its account number.
- **FR-002**: The portfolio MUST report the total account value, the pesos available to trade and the list of positions.
- **FR-003**: Each position MUST report the instrument, the net number of shares, the market value in pesos, the total return and the daily return, both as percentages.
- **FR-004**: All prices and monetary values MUST be expressed in pesos.
- **FR-005**: The net quantity of an instrument MUST be the sum of the sizes of its `FILLED` buys minus the sum of the sizes of its `FILLED` sells.
- **FR-006**: The market value of a position MUST be the net quantity times the most recent close of that instrument.
- **FR-007**: The average cost of an instrument MUST be the sum of size times price of its `FILLED` buys divided by the sum of their sizes. A short position uses, in the same way, the average price of its `FILLED` sells.
- **FR-008**: The total return MUST be, for a long position, `(close − average cost) / average cost`, where the average cost is the sum of size times price of the `FILLED` buys divided by the sum of their sizes; for a short position, `(average sale price − close) / average sale price`, with the average of the `FILLED` sells; expressed as a percentage.
- **FR-009**: The daily return MUST be `(close − previous close) / previous close`, expressed as a percentage, from the most recent price.
- **FR-010**: The current price of an instrument MUST be the close of its most recent date. If there is more than one price on that date, the one recorded last MUST be used.
- **FR-011**: Settled cash MUST add `FILLED` deposits, subtract `FILLED` withdrawals, subtract the amount of `FILLED` buys and add the amount of `FILLED` sells. The amount of a buy or sell is size times price.
- **FR-012**: Available pesos MUST be settled cash minus the amount reserved by `NEW` buys.
- **FR-013**: The available shares of an instrument MUST be its net quantity minus the size of its `NEW` sells.
- **FR-014**: The total account value MUST be settled cash plus the market value of the positions that have a close. Reserved cash is still part of the total value.
- **FR-015**: Positions with zero net quantity and the peso currency MUST be excluded from the list. Shown positions MUST be sorted by ticker.
- **FR-016**: A search MUST take a text and return, without duplicates and sorted by ticker, the instruments whose ticker or name contains that text, case-insensitively.
- **FR-017**: Each instrument found MUST report ticker, name, type, latest price and daily return when they exist.
- **FR-018**: A buy or sell order MUST give the account, the instrument, the side and exactly one form of quantity: whole shares or an amount in pesos.
- **FR-019**: When the order gives an amount, the size MUST be the integer part of `amount / applicable price`, never a fraction. The applicable price is the latest close on a market order and the price sent on a limit order.
- **FR-020**: A valid MARKET order MUST be stored `FILLED` at once, with the latest close as its price. The request MUST NOT carry a price: if it does, it is rejected without storing.
- **FR-021**: A valid LIMIT order MUST be stored `NEW` with the price sent and MUST stay that way: the system does not simulate the market or fill it later.
- **FR-022**: A buy whose amount exceeds the available pesos, or a sell whose size exceeds the available shares, MUST be stored `REJECTED`. The same MUST happen when the size resulting from an amount is zero or when a market order has no close.
- **FR-023**: A `REJECTED` or `CANCELLED` order MUST leave holdings, settled cash and reservations unchanged.
- **FR-024**: A `FILLED` order MUST show in the next portfolio query.
- **FR-025**: Only the owner MUST be able to cancel an order, and only if its status is `NEW`. The status becomes `CANCELLED` and the reservation of pesos or shares is released.
- **FR-026**: Peso deposits and withdrawals already stored MUST count towards cash. The system MUST NOT accept a new deposit or withdrawal: no order is stored and cash does not change.
- **FR-027**: Every stored order MUST keep side, size, price, type, status and date-time. Sides are `BUY`, `SELL`, `CASH_IN` and `CASH_OUT`. Types are `MARKET` and `LIMIT`. Statuses are `NEW`, `FILLED`, `REJECTED` and `CANCELLED`.
- **FR-028**: The system MUST work on the reference set as delivered, without changing its meaning: historical `FILLED` orders count even if they are limit orders, and historical `NEW`, `CANCELLED` and `REJECTED` orders follow these same rules.
- **FR-029**: The system MUST NOT require authentication or restrict access to an account.
- **FR-030**: A request with invalid data, or a new peso deposit or withdrawal, MUST NOT store an order. The business rejection of FR-022 MUST be stored.
- **FR-031**: The application MUST start with Docker, together with its database and the reference data, and MUST serve portfolio, search and orders without installing the platform or the database separately.
- **FR-032**: At startup, the application MUST identify itself as `brokerage-api`.
- **FR-033**: Every portfolio, search and order operation MUST be served under the base path `/api/brokerage-api` and the version `/v1` of each operation. The same operations without that base or that version MUST NOT be served.
- **FR-034**: The system MUST expose only two Actuator endpoints over HTTP: `GET /actuator/health` and `GET /actuator/info`. Health MUST report `UP` when the application and its database answer, and MUST NOT publish component details. Info MUST identify the application as `brokerage-api`. Any other Actuator endpoint MUST NOT be exposed.
- **FR-035**: The business API contract MUST be the OpenAPI that springdoc-openapi 3.2.0 generates at startup. It MUST be served at `/v3/api-docs` and `/v3/api-docs.yaml`, and MUST be readable at `/swagger-ui.html`. The repository MUST NOT keep a hand-written copy of that contract.

### Key Entities *(include if feature involves data)*

- **Account**: The person who trades. Identified by the account number and has an email. Reference account `10001` belongs to `emiliano@test.com`.
- **Instrument**: A market instrument, with ticker, name and type. The reference types are shares and currency. The currency is `ARS` (pesos) and is not shown as a position.
- **Order**: A movement of an account on an instrument. It has a whole size, a price in pesos, a side, a type, a status and a date-time. It is the only source for rebuilding holdings, cash and reservations.
- **Price**: Daily quote of an instrument, with open, high, low, close, previous close and date. The close of the most recent date is the latest price. In the delivered set, quotes are dated by calendar day.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The query of reference account `10001` matches 100% the available cash, the total value and the three positions defined in story 1, including rounding to two decimals.
- **SC-002**: On the reference catalogue, a search returns 100% of the instruments whose ticker or name contains the text, and 0% of those that do not, in under 1 second.
- **SC-003**: The result of a valid market order already carries the filled status and the applied price, and an immediate portfolio query shows the updated holding and cash.
- **SC-004**: 100% of the buys that exceed the available pesos and of the sells that exceed the available shares are rejected, and a later query shows the account unchanged.
- **SC-005**: 100% of valid limit orders stay open, and 100% of valid cancellations make them cancelled and return the reserved balance before the next query.
- **SC-006**: With the reference set, a person completes the three core tasks on the first try: read the portfolio of `10001`, find `PAMP` by name or ticker and record a buy or sell order with the right status.
- **SC-007**: With Docker available, a single start leaves the application and its reference data ready, without installing the platform or the database separately, and the query of account `10001` matches story 1.
- **SC-008**: 100% of portfolio, search and order operations are called under the base path `/api/brokerage-api` and the version `/v1`, and the name `brokerage-api` is recognisable at startup.
- **SC-009**: With the database available, `/actuator/health` reports `UP` without components and `/actuator/info` reports `brokerage-api`. No other Actuator endpoint answers. `/v3/api-docs` describes the operations of stories 1 to 6.

## Assumptions

- The account number is the public identifier. No session is needed.
- A negative net quantity is kept: the reference set leaves `BMA` short on account `10001` (20 shares bought and 30 sold, all filled).
- If the amount buys more shares than the available balance can cover, the order is rejected. It is not silently reduced to the covered maximum. "Maximum quantity" is read only as the maximum number of whole shares that amount can buy or sell at the applicable price.
- A market order that carries a price is rejected. It is not silently ignored.
- The full data definition delivered prevails when the short description differs: the price has a calendar date, and the order has a date and time.
- Out of scope: authentication, market simulation, partial fills, fractional shares, other currencies, creating or changing accounts and instruments, and recording new peso deposits or withdrawals.
- A catalogue of examples to call each operation is desirable but not needed to consider the behaviour complete.
- The build constraints that came with the original request do not change this behaviour. Planning must apply them as requested, and the design decisions this specification does not fix must be written down for whoever reviews the solution.
- The application starts by itself with Docker, together with the database and the reference data. The Java code lives in this same project folder.
- springdoc-openapi 3.2.0 is the version of that library for Spring Boot 4.1.x. It is built on Spring Boot 4.1.2. The 3.0.x line belongs to Spring Boot 4.0 and 2.x to Spring Boot 3, so they do not apply to this service. The artifact is `springdoc-openapi-starter-webmvc-ui`, the Spring MVC one with the reading UI.
- Health does not publish components or the database it connects to. Without authentication, that detail would be open. `UP` or `DOWN` is enough.
