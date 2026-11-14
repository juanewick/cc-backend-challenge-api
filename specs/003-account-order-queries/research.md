# Research: Account queries

## Decision: The schema is not touched

**Decision**: No table or column is created and no existing column is modified.

**Rationale**: The spec forbids it. Settled and reserved cash are already computed in `PortfolioCalculator.Holdings` and not stored. The amount is `size * price` when answering. The rejection reason is decided at placement and not persisted. The error code lives only in the HTTP body.

**Alternatives considered**: Storing the reason in `orders` or the reserved cash in `users`. Left out because they change the delivered model.

## Decision: The list is sorted in the query, not with a new index

**Decision**: `findByUserId` now sorts by ascending `datetime` and ascending `id` in the JPA query. No index is added.

**Rationale**: The set of orders of a reference account is small. The spec asks for that order and forbids new indexes.

**Alternatives considered**: Sorting in memory after fetching them. It works, but the query can already ask for it without touching the schema.

## Decision: A single current-close selector

**Decision**: A domain component picks, for each instrument, the `marketdata` row with the highest `date` and, on the same date, the highest `id`. Portfolio, search, quote and the price of a market order use it. The query for a single existing instrument (`findFirstByInstrumentIdOrderByDateDescIdDesc`) follows that same rule.

**Rationale**: Today `GetPortfolioService` and `SearchInstrumentsService` duplicate the walk. The spec requires the same close in all four reads.

**Alternatives considered**: An SQL view or an `is_latest` column. They would change the schema.

## Decision: The placement ticker is compared case-insensitively

**Decision**: The instrument lookup uses the database's case-insensitive comparison, on the text already validated by `InputText.ticker`. The response shows the ticker stored in the catalogue, not the text sent.

**Rationale**: `ggal` has to trade `GGAL`. There is no need to store another form of the ticker.

**Alternatives considered**: Upper-casing the ticker before looking it up. The reference catalogue is already upper case, but the case-insensitive comparison does not depend on that.

## Decision: The currency is filtered out of the search

**Decision**: `SearchInstrumentsService` drops `InstrumentType.MONEDA` after the text match. The quote by ticker of `ARS` answers that the instrument is not tradable, with its own code for a non-tradable currency, not the one an unknown ticker uses. A new deposit or withdrawal is still not stored.

**Rationale**: Searching `PESOS` or `ARS` must not offer a share. The portfolio already excludes the currency from the positions.

**Alternatives considered**: Deleting `ARS` from the catalogue. The reference set is used as delivered.

## Decision: The reason does not travel in the query

**Decision**: `PlaceOrderService` returns the status and, if it is `REJECTED`, one of `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES`, `NO_MARKET_PRICE`. The placement `OrderResponse` includes `rejectionReason` and `amount`. The order `GET` does not include `rejectionReason`. `amount` on placement is the notional already rounded and is not stored.

**Rationale**: The spec separates the placement response from the later query. There is no column for the reason.

**Alternatives considered**: Recomputing the reason on query. The available balance may have changed and the reason would no longer be that order's.

## Decision: The stable code goes with the message

**Decision**: `ApiError` becomes `code` and `message`. Each existing domain exception gets a fixed code. The message text does not change. The input rejections that `ApiExceptionHandler` builds also carry a code, without quoting the value sent.

**Rationale**: The client cannot depend on comparing the text. Adding `code` stores nothing.

**Alternatives considered**: An HTTP header for the code. The body stays self-describing and the generated contract documents it.

## Decision: How to get back to the order

**Decision**: Placement answers `201` and a `Location` header with the `GET` path of that order. That query returns the same order.

**Rationale**: The spec asks to say how to query the newly created order, without a new column.

**Alternatives considered**: Repeating only the `id` in the body. The body already carries it. `Location` is the path hint.

## Decision: Annotate the generated contract

**Decision**: springdoc documents the new operations, the `400`, `404` and `409`, and that amounts are text with two decimals and a return of `0.48` is 0.48 %. No copy of the OpenAPI is kept in specs.

**Rationale**: Feature 001 already decided the application generates the contract. This feature only completes it.

**Alternatives considered**: Writing `openapi.yaml` by hand. The spec of feature 001 forbids it and this one does not reopen it.
