# Data Model: Endurecer las entradas

No hay tablas ni columnas nuevas. Los topes de abajo son los que el esquema entregado ya puede guardar.

## Dato de entrada

Valor que llega en una operación y se acepta o se rechaza entero, antes de leer o grabar la cuenta.

| Dato | Forma aceptada | Si no entra |
|---|---|---|
| Documento de alta | Un objeto con los campos de la orden, sin campos de más, sin claves repetidas y sin otro token después | No se graba. Texto de pedido ilegible, de campo desconocido o de campo inválido, según [contracts/entradas.md](contracts/entradas.md) |
| Cuenta | 1 a 20 dígitos, sin recorte | `La cuenta no es válida`. No se busca otra cuenta |
| Ticker | 1 a 10 letras o dígitos, sin recorte y sin cambiar mayúsculas | `El ticker no es válido` |
| Identificador de orden | Entero de 1 a 2147483647 | `La orden no es válida`. No se consulta otra orden |
| Búsqueda | Extremos recortados, 1 a 100 caracteres Unicode, sin controles ISO ni carácter nulo | Texto vacío, máximo superado o búsqueda no válida. No cambia datos |
| Lado | `BUY` o `SELL`, tal cual | No se graba |
| Tipo | `MARKET` o `LIMIT`, tal cual | No se graba |
| Cantidad | Entero de 1 a 2147483647, y ninguna otra forma de cantidad en el mismo pedido | No se graba una cifra menor ni de otro signo |
| Monto | Número mayor que cero, con hasta 8 dígitos enteros después de redondear half-up a 2, y ninguna cantidad de acciones en el mismo pedido | No se graba |
| Precio límite | Mismo rango que el monto, obligatorio solo en `LIMIT` | No se graba |

Un ingreso o un egreso de pesos nuevo (`CASH_IN`, `CASH_OUT`, o un activo de tipo moneda) sigue sin grabar una orden.

## Cantidad derivada de un monto

`parte entera de monto redondeado / precio aplicable`.

- El precio aplicable de una orden de mercado es el último cierre. El de una límite es el precio enviado, ya redondeado.
- Si el cociente es 0, o no hay cierre para una orden de mercado, se guarda `REJECTED`. Es el rechazo de negocio de la feature 001.
- Si el cociente es mayor que 2147483647, no se guarda nada.
- Si la compra no entra en los pesos disponibles, o la venta no entra en las acciones disponibles, se guarda `REJECTED`.

## Rechazo de entrada

No es una orden. No tiene estado, no reserva y no cambia el portfolio.

- Nombra el campo cuando el campo es cuenta, ticker, orden, búsqueda, lado, tipo, cantidad, monto o precio.
- No incluye el valor enviado ni un detalle interno del lector.
- Una cuenta, un activo o una orden que no existen se informan sin repetir el valor.

## Lo que no cambia

- `Order`, `Portfolio` y `InstrumentQuote` siguen como en la feature 001.
- Una posición de cantidad neta negativa se sigue informando.
- El redondeo de importes y porcentajes mostrados sigue en 2 decimales half-up.
- La búsqueda aceptada se compara literal, sin distinguir mayúsculas y sin depender del idioma de la máquina. No se ejecuta como instrucción.
