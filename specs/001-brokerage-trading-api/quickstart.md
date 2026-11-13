# Quickstart: Portfolio, search and orders

Manual validation of the behaviour described in [spec.md](spec.md). The contract is the OpenAPI the application generates at `http://localhost:8080/v3/api-docs` (YAML at `/v3/api-docs.yaml`, reading at `/swagger-ui.html`). The formulas are in [data-model.md](data-model.md).

## Prerequisites

- Docker, with Compose.
- JDK 25 and Maven 3.6.3 or later, only to run the tests on the host.

## Start

From `cc-backend-challenge`:

```bash
docker compose up --build
```

The application is at `http://localhost:8080` and PostgreSQL has the reference SQL. There is no need to install Java or the database on the host for this run.

## Get the reference portfolio

```bash
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
```

Expected result: `availableCash` `627500.00`, `totalValue` `889756.00`, and the positions `BMA` (−10), `METR` (500) and `PAMP` (40) with the values of story 1 of the specification.

## Search

```bash
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=banco"
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=pamp"
```

`banco` returns `BBAR`, `BHIP`, `BMA` and `BPAT`, in that order. `pamp` returns `PAMP` once, with latest price `925.85` and daily return `0.44`.

## Place and cancel

```bash
curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H "Content-Type: application/json" \
  -d '{"ticker":"GGAL","side":"BUY","type":"MARKET","size":1}'
```

The response is `FILLED`, with price `885.80` and size `1`.

On a freshly initialised database, the open limit buy of 50 `LOMA` at `710.00` has id `5`:

```bash
curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders/5/cancellation
```

The status becomes `CANCELLED` and the available balance grows by `35500.00`.

## Health and identity

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/info
```

Health answers `{"status":"UP"}` when the application and the database answer, without components. Info includes `app.name` equal to `brokerage-api`. `/actuator/env` and the rest of Actuator do not answer.

## Tests

```bash
mvn verify
```

`*Test.java` covers the calculation and the services with Mockito. `*IT.java` starts PostgreSQL and LocalStack with Testcontainers and walks through the happy and error scenarios. `ActuatorIT` runs first and checks health, info and that the rest of Actuator is not published. They do not depend on a `docker compose` that is already running.
