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

La misma forma. Si la orden es de otra cuenta, `409` y `BRK-001`. Si no existe, `404` y `BRK-003`. Ninguno repite el identificador.

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

Una cotización. Si no hay cierre, `lastPrice` y `dailyReturnPercent` van ausentes. Si el ticker no existe, `404` y `BRK-002`. Si es moneda, `400` y `BRK-006`.

## Error

```json
{
  "type": "/v3/api-docs#/components/schemas/InvalidParameters",
  "title": "Invalid parameters",
  "status": 400,
  "detail": "The ticker is not valid.",
  "code": "BRK-009",
  "invalid_params": [
    { "field": "ticker", "message": "The ticker is not valid." }
  ]
}
```

`type` apunta al esquema generado: `InvalidParameters`, `BadRequest`, `NotFound` o `Conflict`. Los títulos son "Invalid parameters", "Bad request", "Not found" y "Conflict".

- `InvalidParameters`: validación de un parámetro. Trae `invalid_params` con `field` y `message`. No trae el valor enviado.
- `BadRequest`, `NotFound` y `Conflict`: no traen `invalid_params`.

## Códigos de dominio

El `detail` es el texto en inglés. El código no cambia si se ajusta esa redacción.

| Situación | Código | Detail |
|-----------|--------|--------|
| Orden de otra cuenta | `BRK-001` | The order does not belong to this account. |
| Activo inexistente | `BRK-002` | Instrument not found. |
| Orden inexistente | `BRK-003` | Order not found. |
| Cuenta inexistente | `BRK-004` | Account not found. |
| Orden que no está abierta | `BRK-005` | Only open orders can be cancelled. |
| Moneda no operable | `BRK-006` | The currency is not tradable. |
| Ingreso o egreso nuevo | `BRK-007` | New cash deposits and withdrawals are not accepted. |
| Cuenta con formato inválido | `BRK-008` | The account is not valid. |
| Ticker inválido | `BRK-009` | The ticker is not valid. |
| Identificador de orden inválido | `BRK-010` | The order is not valid. |
| Búsqueda vacía | `BRK-011` | The search has no text. |
| Búsqueda demasiado larga | `BRK-012` | The search exceeds the maximum length. |
| Búsqueda con control o nulo | `BRK-013` | The search is not valid. |
| Lado inválido | `BRK-014` | Side must be BUY or SELL. |
| Tipo de orden ausente | `BRK-015` | Order type is required. |
| Cantidad y monto a la vez, o ninguno | `BRK-016` | Exactly one quantity is required: shares or amount. |
| Cantidad fuera de rango | `BRK-017` | Size must be an integer from {mínimo} to {máximo}. |
| Monto no positivo | `BRK-018` | Amount must be greater than zero. |
| Monto por encima del máximo | `BRK-019` | Amount exceeds the maximum allowed. |
| Monto que no es un número | `BRK-020` | Amount is not a valid number. |
| Precio ausente o no positivo | `BRK-021` | Price is required and must be greater than zero. |
| Precio por encima del máximo | `BRK-022` | Price exceeds the maximum allowed. |
| Precio que no es un número | `BRK-023` | Price is not a valid number. |

Fuera de ese catálogo, el pedido ilegible usa `UNREADABLE` y "The request cannot be read.". Un campo de más usa `UNKNOWN_FIELD`. Un campo repetido usa `DUPLICATE_FIELD`. Un campo con otro tipo usa `INVALID_FIELD`. Esos detalles también están en inglés.

El contrato generado en `/v3/api-docs` describe estas operaciones, esos errores, los importes a dos decimales y la unidad del porcentaje.
