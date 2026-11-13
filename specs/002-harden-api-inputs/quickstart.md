# Quickstart: Endurecer las entradas

Validación del comportamiento de [spec.md](spec.md). Los textos exactos están en [contracts/entradas.md](contracts/entradas.md) y las reglas, en [data-model.md](data-model.md).

## Prerrequisitos

- Docker, con Compose, para la corrida manual.
- JDK 25 y Maven 3.6.3 o posterior, para `mvn verify`.

## Tests

Desde `cc-backend-challenge`:

```bash
mvn verify
```

Tienen que pasar los tests de `MoneyMath`, de `InputText`, de los casos de uso y `HardenInputsIT`, además de los de portfolio, búsqueda y órdenes que ya existían.

## Arranque

```bash
docker compose up --build
```

La aplicación queda en `http://localhost:8080`.

## Regresión de lo ya válido

```bash
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=PAMP"
curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"PAMP","side":"BUY","type":"LIMIT","size":1,"price":"1.00"}'
```

El portfolio de `10001` y la búsqueda `PAMP` responden como antes. La orden límite válida se graba. Una compra sin fondos suficientes se sigue guardando rechazada.

## Rechazos de entrada

```bash
curl -s -o /tmp/body -w '%{http_code}' -X POST \
  http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d 'no-es-un-documento'

curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"PAMP","side":"BUY","type":"LIMIT","size":1,"price":"1.00","extra":1}'

curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"PAMP","side":"BUY","type":"LIMIT","size":1,"size":2,"price":"1.00"}'

curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001%27%20OR%20%271%27%3D%271/portfolio

curl -s -X POST http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders/0/cancellation
```

El primero responde 400 y `El pedido no se puede leer`, sin el cuerpo enviado. El campo de más y la cantidad repetida responden 400 sin grabar una orden. La cuenta con signos responde 400 y no devuelve el portfolio de `10001`. La orden `0` responde 400 y no cancela otra orden.

```bash
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=pamp%27%20OR%201%3D1"
```

Ese texto entra en el largo y en la puntuación de un nombre. Responde 200 con una lista vacía, no modifica datos y no trae un error interno.
