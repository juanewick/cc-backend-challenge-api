# Quickstart: Account queries

## Prerequisites

JDK 25 and Maven on the host, or the service already running with `docker compose up --build` from `cc-backend-challenge/`. The database is the reference one. There is no schema to migrate.

## Automatic verification

From `cc-backend-challenge/`:

```text
mvn verify
```

That covers the portfolio of `10001` with settled and reserved cash, the list and the query of the `LOMA` limit buy, placement with `Location`, the reason and amount of a rejection, `ggal` against `GGAL`, the search of `PESOS` and `ARS`, the quote of `PAMP`, the error code next to the message, and that a second identical placement creates another order.

## By hand, with the service on port 8080

```text
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=PESOS"
curl -s http://localhost:8080/api/brokerage-api/v1/instruments/PAMP
curl -s http://localhost:8080/v3/api-docs
```

The reference portfolio shows `settledCash` `753000.00` and `reservedCash` `125500.00`. The search of `PESOS` does not bring `ARS`. The quote of `PAMP` carries the same latest price as a market buy of that ticker.

The detail of fields and codes is in [contracts/queries.md](contracts/queries.md). The account figures are in [data-model.md](data-model.md).
