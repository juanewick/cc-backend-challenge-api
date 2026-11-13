# Data Model: Portfolio, search and orders

The delivered schema is loaded without redesign. Column names are the ones PostgreSQL stores: unquoted identifiers end up lower case.

## User

Table `users`.

| Field | Type | Rules |
|---|---|---|
| id | integer, identity | Assigned by the database |
| email | text | Informational |
| accountnumber | text, up to 20 | Public identifier. The reference account is `10001` |

Relation: an account has many orders.

## Instrument

Table `instruments`.

| Field | Type | Rules |
|---|---|---|
| id | integer, identity | Assigned by the database |
| ticker | text, up to 10 | Public identifier of the instrument. Unique in the reference data |
| name | text | Used by the search |
| type | text, up to 10 | `ACCIONES` or `MONEDA` |

`ARS` is the only currency (`type = MONEDA`). It is not shown as a position. New `CASH_IN` or `CASH_OUT` orders on it are not accepted; the stored ones do count towards cash.

## MarketPrice

Table `marketdata`.

| Field | Type | Rules |
|---|---|---|
| id | integer, identity | Breaks a tie between two prices on the same day: the higher id wins |
| instrumentid | integer | Reference to the instrument |
| high, low, open | decimal (10,2) | May be null. Not reported |
| close | decimal (10,2) | Latest price when the row is the current one |
| previousclose | decimal (10,2) | Previous close for the daily return |
| date | date | Calendar day, no time |

The current price of an instrument is the row with the highest `date` and, on a tie, the highest `id`.

## Order

Table `orders`.

| Field | Type | Rules |
|---|---|---|
| id | integer, identity | Identifier used to cancel |
| instrumentid | integer | Instrument. On a historical deposit or withdrawal, the `ARS` currency |
| userid | integer | Owning account |
| size | integer | Shares, or whole pesos on a historical deposit or withdrawal. Always positive on a new order |
| price | decimal (10,2) | Unit price in pesos. On market it is the current close. On limit it is the price sent |
| type | text | `MARKET` or `LIMIT` |
| side | text | `BUY`, `SELL`, `CASH_IN`, `CASH_OUT` |
| status | text | `NEW`, `FILLED`, `REJECTED`, `CANCELLED` |
| datetime | timestamp | When the order was recorded |

### Transitions

```text
valid MARKET request             -> FILLED
valid LIMIT request              -> NEW
buy without pesos, sell without
shares, amount that buys no
share, market without a close    -> REJECTED
NEW + cancellation by the owner  -> CANCELLED
```

There are no other transitions. `FILLED`, `REJECTED` and `CANCELLED` do not change. A limit order does not become `FILLED` with time or with the current price.

An invalid request inserts no row: missing account, missing ticker, side `CASH_IN` or `CASH_OUT`, size and amount together or neither, a size that is not a positive integer, a non-positive amount, a limit without a price or with a non-positive price, a market order with a price.

### Effect on the account

Only rows already stored count.

- Settled cash = sum of `size * price` of `CASH_IN FILLED` and `SELL FILLED`, minus the same sum of `CASH_OUT FILLED` and `BUY FILLED`.
- Peso reservation = sum of `size * price` of `BUY NEW`.
- Available pesos = settled cash − peso reservation.
- Net quantity = sum of `size` of `BUY FILLED` − sum of `size` of `SELL FILLED`, per instrument, excluding `MONEDA`.
- Available shares = net quantity − sum of `size` of `SELL NEW` on that instrument.
- Average cost = sum of `size * price` of `BUY FILLED` / sum of `size` of `BUY FILLED`.
- Average sale price = sum of `size * price` of `SELL FILLED` / sum of `size` of `SELL FILLED`.
- Market value = net quantity × current close.
- Total return, as a percentage, with two decimals, half-up: if the net quantity is positive, `(close − average cost) / average cost × 100`; if it is negative, `(average sale price − close) / average sale price × 100`. Without buys (or without sells, on a short) it is null.
- Daily return, as a percentage = `(close − previous close) / previous close × 100`, with the same scale. If the previous close is null or 0, it is null.
- Total value = settled cash + market value of the positions that have a close. A position without a close is listed with its quantity and null values, is left out of the total and its ticker goes into `unpricedTickers`.
- Positions with net quantity 0 and the currency are omitted. The rest are sorted by ticker. A negative quantity is kept.

The amounts of a new buy or sell use the applicable price: the current close on `MARKET`, the price sent on `LIMIT`. If the request carries an amount, `size = floor(amount / applicable price)`. The cash moved is `size * price`, not the original amount.

## Portfolio

Not a table. It is the result of computing `Order` + `MarketPrice` for an account.

| Field | Rules |
|---|---|
| accountNumber | The one queried |
| totalValue | Total value, two decimals |
| availableCash | Available pesos, two decimals |
| positions | List sorted by ticker |

Each position: ticker, name, quantity, marketValue, totalReturnPercent, dailyReturnPercent.

## InstrumentQuote

Not a table. It is the search result.

| Field | Rules |
|---|---|
| ticker, name, type | From the instrument |
| lastPrice | Current close, or null |
| dailyReturnPercent | Per the formula, or null |

The search requires a non-empty text. It matches if the ticker or the name contains the text, case-insensitively. It does not correct typos. No duplicates. Sorted by ticker.
