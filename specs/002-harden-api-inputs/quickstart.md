# Quickstart: Harden the inputs

Validation of the behaviour of [spec.md](spec.md). The exact texts are in [contracts/inputs.md](contracts/inputs.md) and the rules in [data-model.md](data-model.md).

## Prerequisites

- Docker, with Compose, for the manual run.
- JDK 25 and Maven 3.6.3 or later, for `mvn verify`.

## Tests

From `cc-backend-challenge`:

```bash
mvn verify
```

The tests of `MoneyMath`, `InputText`, the use cases and `HardenInputsIT` must pass, together with the existing portfolio, search and order tests.

## Start

```bash
docker compose up --build
```

The application is at `http://localhost:8080`.

## Regression of what was already valid

```bash
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=PAMP"
curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"PAMP","side":"BUY","type":"LIMIT","size":1,"price":"1.00"}'
```

The portfolio of `10001` and the `PAMP` search answer as before. The valid limit order is stored. A buy without enough cash is still stored as rejected.

## Input rejections

```bash
curl -s -o /tmp/body -w '%{http_code}' -X POST \
  http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d 'not-a-document'

curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"PAMP","side":"BUY","type":"LIMIT","size":1,"price":"1.00","extra":1}'

curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"PAMP","side":"BUY","type":"LIMIT","size":1,"size":2,"price":"1.00"}'

curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001%27%20OR%20%271%27%3D%271/portfolio

curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders/0/cancellation
```

The first answers 400 and `The request cannot be read.`, without the body sent. The extra field and the repeated size answer 400 without storing an order. The account with signs answers 400 and does not return the portfolio of `10001`. Order `0` answers 400 and does not cancel another order.

```bash
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=pamp%27%20OR%201%3D1"
```

That text fits the length and a name's punctuation. It answers 200 with an empty list, modifies no data and carries no internal error.
