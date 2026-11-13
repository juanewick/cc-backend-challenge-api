# Contrato: Consulta de la cuenta

Base: `/api/brokerage-api/v1`. Los importes y los porcentajes son texto a dos decimales. Un rendimiento `0.48` es 0,48 %, no 48 %. El error es un problema `application/problem+json`. `detail` no repite el valor enviado. `type` es la URI del esquema en `/v3/api-docs`. `invalid_params` solo aparece en una validación de la API.

## Portfolio

`GET /accounts/{accountNumber}/portfolio`

Además de `accountNumber`, `totalValue`, `availableCash` y `positions`, la respuesta trae:

- `settledCash`: efectivo liquidado, calculado.
- `reservedCash`: efectivo reservado por compras abiertas, calculado.

Cuenta `10001` sin órdenes nuevas: `settledCash` `753000.00`, `reservedCash` `125500.00`, `availableCash` `627500.00`, `totalValue` `889756.00`.

## Órdenes

`GET /accounts/{accountNumber}/orders`

Lista las órdenes de esa cuenta por `datetime` ascendente e `id` ascendente. Cada ítem trae `id`, `accountNumber`, `ticker`, `side`, `type`, `size`, `price`, `status`, `datetime`. No trae `rejectionReason`.

`GET /accounts/{accountNumber}/orders/{orderId}`

La misma forma. Si la orden es de otra cuenta, `409` y `ORDER_NOT_OWNED`. Si no existe, `404` y `ORDER_NOT_FOUND`. Ninguno repite el identificador.

`POST /accounts/{accountNumber}/orders`

Sigue creando la orden, también si queda `REJECTED`, y responde `201`. Agrega:

- encabezado `Location` con la ruta `GET` de esa orden.
- `amount`: importe calculado.
- `rejectionReason`: solo si `status` es `REJECTED`. Valores: `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES`, `NO_MARKET_PRICE`.

Repetir el mismo `POST` crea otra orden.

El ticker se resuelve sin distinguir mayúsculas. La respuesta muestra el ticker del catálogo. `ggal` opera `GGAL`.

## Activos

`GET /instruments?q=`

Igual que antes, sin `MONEDA`. `PESOS` y `ARS` no devuelven una acción operable.

`GET /instruments/{ticker}`

Una cotización. Si no hay cierre, `lastPrice` y `dailyReturnPercent` van ausentes. Si el ticker no existe, `404` e `INSTRUMENT_NOT_FOUND`. Si es moneda, `400` y `INSTRUMENT_NOT_TRADABLE`.

## Error

```json
{
  "type": "/v3/api-docs#/components/schemas/InvalidParameters",
  "title": "Parámetros inválidos",
  "status": 400,
  "detail": "El ticker no es válido",
  "code": "INVALID_TICKER",
  "invalid_params": [
    { "field": "ticker", "message": "El ticker no es válido" }
  ]
}
```

`type` apunta al esquema generado: `InvalidParameters`, `BadRequest`, `NotFound` o `Conflict`.

- `InvalidParameters`: validación de un parámetro. Trae `invalid_params` con `field` y `message`. No trae el valor enviado.
- `BadRequest`, `NotFound` y `Conflict`: no traen `invalid_params`.

## Códigos que acompañan al detalle ya existente

| Situación | Código |
|-----------|--------|
| Cuenta inexistente | `ACCOUNT_NOT_FOUND` |
| Activo inexistente | `INSTRUMENT_NOT_FOUND` |
| Moneda no operable | `INSTRUMENT_NOT_TRADABLE` |
| Orden inexistente | `ORDER_NOT_FOUND` |
| Orden de otra cuenta | `ORDER_NOT_OWNED` |
| Orden que no está abierta | `ORDER_NOT_OPEN` |
| Cuenta con formato inválido | `INVALID_ACCOUNT` |
| Ticker inválido | `INVALID_TICKER` |
| Identificador de orden inválido | `INVALID_ORDER_ID` |
| Búsqueda vacía | `SEARCH_EMPTY` |
| Búsqueda demasiado larga | `SEARCH_TOO_LONG` |
| Búsqueda con control o nulo | `SEARCH_INVALID` |
| Pedido ilegible | `UNREADABLE` |
| Campo de más | `UNKNOWN_FIELD` |
| Campo repetido | `DUPLICATE_FIELD` |
| Campo con tipo distinto | `INVALID_FIELD` |
| Lado, tipo, cantidad, monto o precio inválidos | `INVALID_SIDE`, `INVALID_TYPE`, `INVALID_SIZE`, `INVALID_AMOUNT`, `INVALID_PRICE` |
| Cantidad y monto a la vez, o ninguno | `EXCLUSIVE_QUANTITY` |
| Ingreso o egreso nuevo | `CASH_MOVEMENT` |

El contrato generado en `/v3/api-docs` describe estas operaciones, esos errores, los importes a dos decimales y la unidad del porcentaje.
