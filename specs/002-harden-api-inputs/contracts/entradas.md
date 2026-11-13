# Contrato de entradas

El contrato de las operaciones sigue siendo el OpenAPI que genera la aplicación en `/v3/api-docs`. Este archivo no lo reemplaza: fija los rechazos de esta feature. El cuerpo de error no cambia de forma: `{ "message": "..." }`.

Las cuatro operaciones son las ya publicadas:

- `GET /api/brokerage-api/v1/accounts/{accountNumber}/portfolio`
- `GET /api/brokerage-api/v1/instruments?q=`
- `POST /api/brokerage-api/v1/accounts/{accountNumber}/orders`
- `POST /api/brokerage-api/v1/accounts/{accountNumber}/orders/{orderId}/cancellation`

Un dato inválido responde 400. Una cuenta, un activo o una orden que no existen responden 404, y una orden de otra cuenta responde igual que una inexistente. Una orden que no está abierta sigue en 409. Nada de esto graba una orden nueva, salvo el rechazo de negocio que ya existía (fondos o acciones insuficientes, cantidad resultante cero, orden de mercado sin cierre).

## Pedido de alta

| Caso | Mensaje |
|---|---|
| Cuerpo ilegible, vacío, que no es un objeto, con tokens de más, o con un tipo de contenido distinto | El pedido no se puede leer |
| Un campo que la operación no define | El pedido tiene un campo que no corresponde |
| `ticker`, `side`, `type`, `size`, `amount` o `price` repetido | El campo ticker está repetido, y el equivalente para lado, tipo, cantidad, monto o precio |
| Ese mismo campo con un tipo distinto, o un número que no entra en la cantidad | El campo ticker no es válido, y el equivalente para lado, tipo, cantidad, monto o precio |
| Cuenta fuera de 1 a 20 dígitos | La cuenta no es válida |
| Ticker fuera de 1 a 10 letras o dígitos | El ticker no es válido |
| Lado distinto de `BUY` o `SELL` | El lado debe ser BUY o SELL |
| Tipo ausente | El tipo de orden es obligatorio |
| Tipo distinto de `MARKET` o `LIMIT` | El tipo debe ser MARKET o LIMIT |
| Cantidad y monto juntos, o ninguno | Debe indicarse exactamente una forma de cantidad: acciones o monto |
| Cantidad fuera de 1 a 2147483647, también si sale de un monto | La cantidad debe ser un entero de 1 a 2147483647 |
| Monto que no es número, no es positivo o supera 8 enteros después del redondeo | El campo monto no es válido, El monto debe ser mayor que cero, o El monto supera el máximo permitido |
| Precio límite ausente, no positivo o por encima del mismo máximo | El precio es obligatorio y debe ser mayor que cero, El campo precio no es válido, o El precio supera el máximo permitido |

Los decimales de más no se rechazan: se redondean half-up a 2 antes de aplicar el tope de 8 enteros.

## Rutas y búsqueda

| Caso | Mensaje | Estado |
|---|---|---|
| Cuenta que no es de 1 a 20 dígitos | La cuenta no es válida | 400 |
| Cuenta bien formada que no existe | La cuenta no existe | 404 |
| Orden que no es un entero de 1 a 2147483647 | La orden no es válida | 400 |
| Orden bien formada que no existe | La orden no existe | 404 |
| Búsqueda ausente o vacía después de recortar | La búsqueda no tiene texto | 400 |
| Búsqueda de más de 100 caracteres | La búsqueda supera el máximo permitido | 400 |
| Búsqueda con un carácter de control o nulo | La búsqueda no es válida | 400 |
| Activo que no existe, con ticker de formato válido | El activo no existe | 404 |

La búsqueda aceptada puede llevar letras, dígitos, espacios y la puntuación de un nombre. Se compara literal. `PAMP` y `pamp` siguen encontrando el activo de referencia.

## Lo que una respuesta de entrada no contiene

- El cuerpo enviado.
- El valor del campo.
- El nombre de un campo que la operación no define.
- Un detalle interno del lector, de la base o de la plataforma.
