# Quickstart: Consulta de la cuenta

## Prerrequisitos

JDK 25 y Maven en el host, o el servicio ya levantado con `docker compose up --build` desde `cc-backend-challenge/`. La base es la de referencia. No hay que migrar el esquema.

## Verificación automática

Desde `cc-backend-challenge/`:

```text
mvn verify
```

Eso cubre el portfolio de `10001` con liquidado y reservado, el listado y la consulta de la compra límite de `LOMA`, el alta con `Location`, el motivo y el importe de un rechazo, `ggal` contra `GGAL`, la búsqueda de `PESOS` y `ARS`, la cotización de `PAMP`, el código de error junto al mensaje, y que un segundo alta igual crea otra orden.

## A mano, con el servicio en el puerto 8080

```text
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=PESOS"
curl -s http://localhost:8080/api/brokerage-api/v1/instruments/PAMP
curl -s http://localhost:8080/v3/api-docs
```

El portfolio de referencia muestra `settledCash` `753000.00` y `reservedCash` `125500.00`. La búsqueda de `PESOS` no trae `ARS`. La cotización de `PAMP` trae el mismo último precio que una compra a mercado de ese ticker.

El detalle de campos y códigos está en [contracts/consulta.md](contracts/consulta.md). Las cifras de la cuenta están en [data-model.md](data-model.md).
