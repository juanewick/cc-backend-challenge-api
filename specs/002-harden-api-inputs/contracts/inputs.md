# Input contract

The contract of the operations is still the OpenAPI the application generates at `/v3/api-docs`. This file does not replace it: it fixes the rejections of this feature. The error body keeps its shape: `{ "message": "..." }`.

The four operations are the ones already published:

- `GET /api/brokerage-api/v1/accounts/{accountNumber}/portfolio`
- `GET /api/brokerage-api/v1/instruments?q=`
- `POST /api/brokerage-api/v1/accounts/{accountNumber}/orders`
- `POST /api/brokerage-api/v1/accounts/{accountNumber}/orders/{orderId}/cancellation`

Invalid input answers 400. An account, instrument or order that does not exist answers 404, and an order of another account answers the same as a missing one. An order that is not open stays at 409. None of this stores a new order, except the business rejection that already existed (insufficient cash or shares, zero resulting size, market order without a close).

## Order request

| Case | Message |
|---|---|
| Body unreadable, empty, not an object, or with trailing tokens | The request cannot be read. |
| Content type other than `application/json` | 415, code `UNSUPPORTED_MEDIA_TYPE` |
| A field the operation does not define | The request contains a field that is not allowed. |
| That same field with another type, or a number that does not fit the size | The ticker field is not valid., and the equivalent for side, type, size, amount or price |
| Account outside 1 to 20 digits | The account is not valid. |
| Ticker outside 1 to 10 letters or digits | The ticker is not valid. |
| Side other than `BUY` or `SELL` | Side must be BUY or SELL. |
| Missing type | Order type is required. |
| Type other than `MARKET` or `LIMIT` | Order type is required. |
| Size and amount together, or neither | Exactly one quantity is required: shares or amount. |
| Size outside 1 to 2147483647, also when it comes from an amount | Size must be an integer from 1 to 2147483647. |
| Amount that is not a number, not positive or above 8 integer digits after rounding | Amount is not a valid number., Amount must be greater than zero., or Amount exceeds the maximum allowed. |
| Limit price missing, not positive or above the same maximum | Price is required and must be greater than zero., Price is not a valid number., or Price exceeds the maximum allowed. |

Extra decimals are not rejected: they are rounded half-up to 2 before applying the 8-integer-digit cap.

## Paths and search

| Case | Message | Status |
|---|---|---|
| Account that is not 1 to 20 digits | The account is not valid. | 400 |
| Well-formed account that does not exist | Account not found. | 404 |
| Order that is not an integer from 1 to 2147483647 | The order is not valid. | 400 |
| Well-formed order that does not exist | Order not found. | 404 |
| Search missing or empty after trimming | The search has no text. | 400 |
| Search longer than 100 characters | The search exceeds the maximum length. | 400 |
| Search with a control or null character | The search is not valid. | 400 |
| Instrument that does not exist, with a valid ticker format | Instrument not found. | 404 |

The accepted search may carry letters, digits, spaces and a name's punctuation. It is compared literally. `PAMP` and `pamp` still find the reference instrument.

## What an input response does not contain

- The body sent.
- The value of the field.
- The name of a field the operation does not define.
- An internal detail of the reader, the database or the platform.
