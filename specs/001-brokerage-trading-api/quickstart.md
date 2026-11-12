# Quickstart: Portfolio, búsqueda y órdenes

Validación manual del comportamiento descrito en [spec.md](spec.md). El contrato está en [contracts/http-api.yaml](contracts/http-api.yaml) y las fórmulas en [data-model.md](data-model.md).

## Prerrequisitos

- Docker, con Compose.
- JDK 25 y Maven 3.6.3 o posterior, solo para correr los tests en el host.

## Arranque

Desde `cc-backend-challenge`:

```bash
docker compose up --build
```

Quedan la aplicación en `http://localhost:8080` y PostgreSQL con el SQL de referencia. No hace falta instalar Java ni la base en el host para esta corrida.

## Consultar el portfolio de referencia

```bash
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
```

Resultado esperado: `availableCash` `627500.00`, `totalValue` `889756.00`, y las posiciones `BMA` (−10), `METR` (500) y `PAMP` (40) con los valores de la historia 1 de la especificación.

## Buscar

```bash
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=banco"
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=pamp"
```

`banco` devuelve `BBAR`, `BHIP`, `BMA` y `BPAT`, en ese orden. `pamp` devuelve una sola vez `PAMP`, con último precio `925.85` y rendimiento diario `0.44`.

## Enviar y cancelar

```bash
curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H "Content-Type: application/json" \
  -d '{"ticker":"GGAL","side":"BUY","type":"MARKET","size":1}'
```

La respuesta queda `FILLED`, con precio `885.80` y cantidad `1`.

En una base recién inicializada, la compra límite abierta de 50 `LOMA` a `710.00` tiene id `5`:

```bash
curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders/5/cancellation
```

El estado pasa a `CANCELLED` y el disponible sube `35500.00`.

## Tests

```bash
mvn verify
```

`*Test.java` cubre el cálculo y los servicios con Mockito. `*IT.java` levanta PostgreSQL y LocalStack con Testcontainers y recorre los escenarios felices y de error del contrato. No dependen del `docker compose` que ya esté corriendo.
