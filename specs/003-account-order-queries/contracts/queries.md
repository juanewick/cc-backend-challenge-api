# Contract: Account queries

Base: `/api/brokerage-api/v1`. Amounts and percentages are text with two decimals. A return of `0.48` is 0.48 %, not 48 %. The error is an `application/problem+json` problem. `detail` does not repeat the value sent. `type` is the URI of the schema in `/v3/api-docs`. `invalid_params` only appears on an API validation.

## Portfolio

`GET /accounts/{accountNumber}/portfolio`

Besides `accountNumber`, `totalValue`, `availableCash` and `positions`, the response carries:

- `settledCash`: settled cash, computed.
- `reservedCash`: cash reserved by open buys, computed.
- `unpricedTickers`: tickers of the positions without a close, sorted. Those positions are listed with null values and are left out of `totalValue`. Empty list when every position has a price.

Each position's `totalReturnPercent` is computed on average cost: `(close − average cost) / average cost` on a long position, `(average sale price − close) / average sale price` on a short one.

Account `10001` with no new orders: `settledCash` `753000.00`, `reservedCash` `125500.00`, `availableCash` `627500.00`, `totalValue` `889756.00`.

## Orders

`GET /accounts/{accountNumber}/orders`

Lists the orders of that account by ascending `datetime` and ascending `id`. Each item carries `id`, `accountNumber`, `ticker`, `side`, `type`, `size`, `price`, `status`, `datetime`. It does not carry `rejectionReason`.

`GET /accounts/{accountNumber}/orders/{orderId}`

The same shape. If the order does not exist or belongs to another account, `404` and `BRK-003`: both responses are identical, so as not to reveal which ids exist. It does not repeat the id.

`POST /accounts/{accountNumber}/orders`

It still creates the order, also when it ends `REJECTED`, and answers `201`. It adds:

- a `Location` header with the `GET` path of that order.
- `amount`: computed amount.
- `rejectionReason`: only if `status` is `REJECTED`. Values: `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES`, `NO_MARKET_PRICE`.

Repeating the same `POST` creates another order.

The ticker is resolved case-insensitively. The response shows the catalogue ticker. `ggal` trades `GGAL`.

A `MARKET` order must not carry `price`: it answers `400` and `BRK-027`, and no order is stored. A body that is not `application/json` answers `415`.

## Instruments

`GET /instruments?q=&page=&pageSize=`

One page of matches sorted by ticker, as `{content, page, pageSize, totalElements, totalPages}`. `page` goes from 0 to `BROKERAGE_INSTRUMENT_SEARCH_MAX_PAGE` (50 unless set) and `pageSize` from 1 to `BROKERAGE_INSTRUMENT_SEARCH_MAX_PAGE_SIZE` (100 unless set), 20 by default; an invalid value answers `400` with `BRK-025` or `BRK-026`. A blank `q` answers `BRK-011` even when the page parameters are also invalid. Without `MONEDA`. `PESOS` and `ARS` return no tradable share.

`GET /instruments/{ticker}`

One quote. If there is no close, `lastPrice` and `dailyReturnPercent` are absent. If the ticker does not exist, `404` and `BRK-002`. If it is a currency, `400` and `BRK-006`.

## Error

```json
{
  "type": "/v3/api-docs#/components/schemas/InvalidParameters",
  "title": "Invalid parameters",
  "status": 400,
  "detail": "The ticker is not valid.",
  "code": "BRK-009",
  "invalid_params": [
    { "field": "ticker", "message": "The ticker is not valid." }
  ]
}
```

`type` points to the generated schema: `InvalidParameters`, `BadRequest`, `NotFound`, `Conflict` or `UnsupportedMediaType`. The titles are "Invalid parameters", "Bad request", "Not found", "Conflict" and "Unsupported media type".

- `InvalidParameters`: validation of a parameter. It carries `invalid_params` with `field` and `message`. It does not carry the value sent.
- `BadRequest`, `NotFound`, `Conflict` and `UnsupportedMediaType`: they do not carry `invalid_params`.

## Domain codes

The `detail` is the English text. The code does not change if that wording is adjusted.

| Situation | Code | Detail |
|-----------|------|--------|
| Missing instrument | `BRK-002` | Instrument not found. |
| Missing order, or one of another account | `BRK-003` | Order not found. |
| Missing account | `BRK-004` | Account not found. |
| Order that is not open | `BRK-005` | Only open orders can be cancelled. |
| Currency not tradable | `BRK-006` | The currency is not tradable. |
| New deposit or withdrawal | `BRK-007` | New cash deposits and withdrawals are not accepted. |
| Account with an invalid format | `BRK-008` | The account is not valid. |
| Invalid ticker | `BRK-009` | The ticker is not valid. |
| Invalid order id | `BRK-010` | The order is not valid. |
| Empty search | `BRK-011` | The search has no text. |
| Search too long | `BRK-012` | The search exceeds the maximum length. |
| Search with a control or null character | `BRK-013` | The search is not valid. |
| Invalid side | `BRK-014` | Side must be BUY or SELL. |
| Missing order type | `BRK-015` | Order type is required. |
| Size and amount together, or neither | `BRK-016` | Exactly one quantity is required: shares or amount. |
| Size out of range | `BRK-017` | Size must be an integer from {minimum} to {maximum}. |
| Non-positive amount | `BRK-018` | Amount must be greater than zero. |
| Amount above the maximum | `BRK-019` | Amount exceeds the maximum allowed. |
| Amount that is not a number | `BRK-020` | Amount is not a valid number. |
| Price missing or not positive | `BRK-021` | Price is required and must be greater than zero. |
| Price above the maximum | `BRK-022` | Price exceeds the maximum allowed. |
| Price that is not a number | `BRK-023` | Price is not a valid number. |
| Price on a MARKET order | `BRK-027` | A MARKET order does not take a price; it fills at the latest close. |

Outside that catalogue, an unreadable request uses `UNREADABLE` and "The request cannot be read.". A content type other than `application/json` answers `415` with `UNSUPPORTED_MEDIA_TYPE`, "The request body must be sent as application/json.". An extra field uses `UNKNOWN_FIELD`. A repeated field is not checked: the last value wins. A field with another type uses `INVALID_FIELD`. Those details are in English too.

The contract generated at `/v3/api-docs` describes these operations, those errors, the two-decimal amounts and the percentage unit.
