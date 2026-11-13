# Data Model: Account queries

No persistent entities or persistent fields are created. What follows is what is already stored and what is computed when answering.

## Order

The existing `orders` row, read without new columns.

- Id, owning account, instrument, size, price, side, type, status, placement date.
- An account's list comes sorted by ascending placement date and, on the same date, by ascending id.
- Querying an order of another account does not return it and does not modify it.
- The `REJECTED` status is already stored. The reason is not part of the order.

## Account reservation

Computed by `PortfolioCalculator` from the orders already stored. Not persisted.

- Settled cash: filled deposits and withdrawals, minus filled buys, plus filled sells.
- Reserved cash: sum of the notional of the buys in `NEW` status.
- Available: settled minus reserved.
- On account `10001`, before new orders: settled `753000.00`, reserved `125500.00`, available `627500.00`, total value `889756.00`.
- Positions without a close are listed in `unpricedTickers` and left out of the total value.

## Placement result

Only the `POST` response. Not stored.

- Amount: size times the applicable price, rounded half-up to 2 decimals.
- Reason, only if the status is `REJECTED`: `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES` or `NO_MARKET_PRICE`.
- The later query does not carry the reason.

## Quote

Read of an instrument and its current close. No new quote is stored.

- Ticker and name from the catalogue.
- Latest price and daily return, or absent if there is no close or the previous close is zero or missing.
- `ARS` / `PESOS` (`MONEDA`) is neither a tradable quote nor a search result.

## Current close

The `marketdata` row of the instrument with the highest `date` and, on the same date, the highest `id`. It is the same choice for portfolio, search, quote and market order.

## Error code

Only the HTTP body of the problem: `type`, `title`, `status`, `detail` and `code`. Not stored. On a domain error, `code` is `BRK-xxx` and `detail` is the English text of that code. It does not include the value sent. An unreadable request or an extra, repeated or wrongly typed field does not use a `BRK` code.
