# Data Model: Consulta de la cuenta

No se crean entidades persistentes ni campos persistentes. Lo de abajo es lo que ya está guardado y lo que se calcula al responder.

## Orden

La fila ya existente de `orders`, leída sin columnas nuevas.

- Identificador, cuenta dueña, activo, cantidad, precio, lado, tipo, estado, fecha de alta.
- El listado de una cuenta sale ordenado por fecha de alta ascendente y, a igual fecha, por identificador ascendente.
- Consultar una orden de otra cuenta no la devuelve y no la modifica.
- El estado `REJECTED` ya se guarda. El motivo no es parte de la orden.

## Reserva de la cuenta

Calculada por `PortfolioCalculator` a partir de las órdenes ya guardadas. No se persiste.

- Efectivo liquidado: ingresos y egresos ejecutados, menos compras ejecutadas, más ventas ejecutadas.
- Efectivo reservado: suma del nocional de las compras en estado `NEW`.
- Disponible: liquidado menos reservado.
- En la cuenta `10001`, antes de órdenes nuevas: liquidado `753000.00`, reservado `125500.00`, disponible `627500.00`, valor total `889756.00`.

## Resultado de alta

Solo la respuesta del `POST`. No se guarda.

- Importe: cantidad por precio aplicable, redondeado half-up a 2 decimales.
- Motivo, solo si el estado es `REJECTED`: `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES` o `NO_MARKET_PRICE`.
- La consulta posterior no trae el motivo.

## Cotización

Lectura de un instrumento y de su cierre vigente. No se guarda una cotización nueva.

- Ticker y nombre del catálogo.
- Último precio y rendimiento diario, o ausentes si no hay cierre o el cierre anterior es cero o no existe.
- `ARS` / `PESOS` (`MONEDA`) no es una cotización operable ni un resultado de búsqueda.

## Cierre vigente

La fila de `marketdata` del activo con mayor `date` y, a igual fecha, mayor `id`. Es la misma elección para portfolio, búsqueda, cotización y orden de mercado.

## Código de error

Solo el cuerpo HTTP del problema: `type`, `title`, `status`, `detail` y `code`. No se guarda. En un error de dominio, `code` es `BRK-xxx` y `detail` es el texto en inglés de ese código. No incluye el valor enviado. Un pedido ilegible o un campo de más, repetido o de otro tipo no usa un código `BRK`.
