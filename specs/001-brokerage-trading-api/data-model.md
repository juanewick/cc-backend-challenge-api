# Data Model: Portfolio, búsqueda y órdenes

El esquema entregado se carga sin rediseño. Los nombres de columna son los que PostgreSQL guarda: los identificadores sin comillas quedan en minúsculas.

## User

Tabla `users`.

| Campo | Tipo | Reglas |
|---|---|---|
| id | entero, identidad | Lo asigna la base |
| email | texto | Informativo |
| accountnumber | texto, hasta 20 | Identificador público. La cuenta de referencia es `10001` |

Relación: una cuenta tiene muchas órdenes.

## Instrument

Tabla `instruments`.

| Campo | Tipo | Reglas |
|---|---|---|
| id | entero, identidad | Lo asigna la base |
| ticker | texto, hasta 10 | Identificador público del activo. Único en los datos de referencia |
| name | texto | Participa de la búsqueda |
| type | texto, hasta 10 | `ACCIONES` o `MONEDA` |

`ARS` es la única moneda (`type = MONEDA`). No se muestra como posición. No se aceptan órdenes nuevas `CASH_IN` ni `CASH_OUT` sobre ella; las ya guardadas sí cuentan para el efectivo.

## MarketPrice

Tabla `marketdata`.

| Campo | Tipo | Reglas |
|---|---|---|
| id | entero, identidad | Desempata dos precios del mismo día: gana el id mayor |
| instrumentid | entero | Referencia al activo |
| high, low, open | decimal (10,2) | Pueden ser nulos. No se informan |
| close | decimal (10,2) | Último precio cuando la fila es la vigente |
| previousclose | decimal (10,2) | Cierre anterior para el rendimiento diario |
| date | fecha | Día de calendario, no hora |

El precio vigente de un activo es la fila de mayor `date` y, a igualdad, mayor `id`.

## Order

Tabla `orders`.

| Campo | Tipo | Reglas |
|---|---|---|
| id | entero, identidad | Identificador de la cancelación |
| instrumentid | entero | Activo. En un ingreso o egreso histórico, la moneda `ARS` |
| userid | entero | Cuenta dueña |
| size | entero | Acciones, o pesos enteros en un ingreso o egreso histórico. Siempre positivo en una orden nueva |
| price | decimal (10,2) | Precio unitario en pesos. En mercado es el cierre vigente. En límite es el precio enviado |
| type | texto | `MARKET` o `LIMIT` |
| side | texto | `BUY`, `SELL`, `CASH_IN`, `CASH_OUT` |
| status | texto | `NEW`, `FILLED`, `REJECTED`, `CANCELLED` |
| datetime | timestamp | Momento de registro de la orden |

### Transiciones

```text
solicitud MARKET válida        -> FILLED
solicitud LIMIT válida         -> NEW
compra sin pesos, venta sin
acciones, monto que no compra
una acción, mercado sin cierre -> REJECTED
NEW + cancelación del titular  -> CANCELLED
```

No hay otras transiciones. `FILLED`, `REJECTED` y `CANCELLED` no cambian. Una orden límite no pasa a `FILLED` por el paso del tiempo ni por el precio vigente.

Una solicitud inválida no inserta fila: cuenta inexistente, ticker inexistente, lado `CASH_IN` o `CASH_OUT`, cantidad y monto juntos o ninguno, cantidad no entera positiva, monto no positivo, límite sin precio o con precio no positivo.

### Efecto sobre la cuenta

Solo participan las filas ya guardadas.

- Efectivo liquidado = suma de `size * price` de `CASH_IN FILLED` y de `SELL FILLED`, menos la misma suma de `CASH_OUT FILLED` y de `BUY FILLED`.
- Reserva de pesos = suma de `size * price` de `BUY NEW`.
- Pesos disponibles = efectivo liquidado − reserva de pesos.
- Cantidad neta = suma de `size` de `BUY FILLED` − suma de `size` de `SELL FILLED`, por activo, sin incluir `MONEDA`.
- Acciones disponibles = cantidad neta − suma de `size` de `SELL NEW` de ese activo.
- Capital neto = suma de `size * price` de `BUY FILLED` − suma de `size * price` de `SELL FILLED`.
- Valor de mercado = cantidad neta × cierre vigente.
- Rendimiento total, en porcentaje = `(valor de mercado − capital neto) / |capital neto| × 100`, con dos decimales, half-up. Si el capital neto es 0, el rendimiento total es nulo.
- Rendimiento diario, en porcentaje = `(cierre − cierre anterior) / cierre anterior × 100`, con la misma escala. Si el cierre anterior es nulo o 0, es nulo.
- Valor total = efectivo liquidado + valor de mercado de las posiciones que tienen cierre. Una posición sin cierre se lista con cantidad y con valores nulos, y no suma al total.
- Se omiten las posiciones de cantidad neta 0 y la moneda. El resto se ordena por ticker. La cantidad negativa se conserva.

Los importes de una orden nueva de compra o venta usan el precio aplicable: cierre vigente en `MARKET`, precio enviado en `LIMIT`. Si el pedido trae un monto, `size = floor(monto / precio aplicable)`. El efectivo que mueve es `size * precio`, no el monto original.

## Portfolio

No es una tabla. Es el resultado de calcular `Order` + `MarketPrice` para una cuenta.

| Campo | Reglas |
|---|---|
| accountNumber | El consultado |
| totalValue | Valor total, dos decimales |
| availableCash | Pesos disponibles, dos decimales |
| positions | Lista ordenada por ticker |

Cada posición: ticker, name, quantity, marketValue, totalReturnPercent, dailyReturnPercent.

## InstrumentQuote

No es una tabla. Es el resultado de búsqueda.

| Campo | Reglas |
|---|---|
| ticker, name, type | Del activo |
| lastPrice | Cierre vigente, o nulo |
| dailyReturnPercent | Según la fórmula, o nulo |

La búsqueda exige un texto no vacío. Coincide si el ticker o el nombre contienen el texto, sin distinguir mayúsculas. No corrige tipeos. Sin duplicados. Orden por ticker.
