# Data Model: Harden the inputs

There are no new tables or columns. The caps below are the ones the delivered schema can already store.

## Input value

A value that arrives in an operation and is accepted or rejected whole, before reading or storing the account.

| Value | Accepted shape | If it does not fit |
|---|---|---|
| Order document | An object with the order fields, no extra fields, no repeated keys and no other token after it | Not stored. Unreadable-request, unknown-field or invalid-field text, as in [contracts/inputs.md](contracts/inputs.md) |
| Account | 1 to 20 digits, untrimmed | `The account is not valid.`. No other account is looked up |
| Ticker | 1 to 10 letters or digits, untrimmed and with its case unchanged | `The ticker is not valid.` |
| Order id | Integer from 1 to 2147483647 | `The order is not valid.`. No other order is queried |
| Search | Ends trimmed, 1 to 100 Unicode characters, without ISO controls or the null character | Empty text, maximum exceeded or invalid search. No data changes |
| Side | `BUY` or `SELL`, as is | Not stored |
| Type | `MARKET` or `LIMIT`, as is | Not stored |
| Size | Integer from 1 to 2147483647, and no other form of quantity in the same request | A smaller figure or one of another sign is not stored |
| Amount | Number greater than zero, with up to 8 integer digits after rounding half-up to 2, and no number of shares in the same request | Not stored |
| Limit price | Same range as the amount, required only on `LIMIT` | Not stored |

A new peso deposit or withdrawal (`CASH_IN`, `CASH_OUT`, or an instrument of currency type) still stores no order.

## Size derived from an amount

`integer part of rounded amount / applicable price`.

- The applicable price of a market order is the latest close. That of a limit order is the price sent, already rounded.
- If the quotient is 0, or there is no close for a market order, it is stored `REJECTED`. It is the business rejection of feature 001.
- If the quotient is greater than 2147483647, nothing is stored.
- If the buy does not fit the available pesos, or the sell does not fit the available shares, it is stored `REJECTED`.

## Input rejection

It is not an order. It has no status, reserves nothing and does not change the portfolio.

- It names the field when the field is account, ticker, order, search, side, type, size, amount or price.
- It does not include the value sent or an internal detail of the reader.
- An account, instrument or order that does not exist is reported without repeating the value.

## What does not change

- `Order`, `Portfolio` and `InstrumentQuote` stay as in feature 001.
- A position with a negative net quantity is still reported.
- Rounding of the amounts and percentages shown stays at 2 decimals half-up.
- The accepted search is compared literally, case-insensitively and without depending on the machine locale. It is not executed as an instruction.
