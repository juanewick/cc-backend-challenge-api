# Research: Consulta de la cuenta

## Decision: No se toca el esquema

**Decision**: No se crea ninguna tabla ni columna y no se modifica ninguna columna existente.

**Rationale**: La spec lo prohíbe. Liquidado y reservado ya se calculan en `PortfolioCalculator.Holdings` y no se guardan. El importe es `size * price` al responder. El motivo del rechazo se decide en el alta y no se persiste. El código de error vive solo en el cuerpo HTTP.

**Alternatives considered**: Guardar el motivo en `orders` o el reservado en `users`. Quedan afuera porque cambian el modelo entregado.

## Decision: El listado ordena en la consulta, no con un índice nuevo

**Decision**: `findByUserId` pasa a ordenar por `datetime` ascendente e `id` ascendente en la consulta JPA. No se agrega un índice.

**Rationale**: El conjunto de órdenes de una cuenta de referencia es chico. La spec pide ese orden y prohíbe índices nuevos.

**Alternatives considered**: Ordenar en memoria después de traerlas. Sirve, pero la consulta ya puede pedirlo sin tocar el esquema.

## Decision: Un solo selector de cierre vigente

**Decision**: Un componente de dominio elige, para cada activo, la fila de `marketdata` con mayor `date` y, a igual fecha, mayor `id`. Lo usan portfolio, búsqueda, cotización y el precio de una orden de mercado. La consulta por un solo activo que ya existe (`findFirstByInstrumentIdOrderByDateDescIdDesc`) sigue siendo esa misma regla.

**Rationale**: Hoy `GetPortfolioService` y `SearchInstrumentsService` duplican el recorrido. La spec exige el mismo cierre en las cuatro lecturas.

**Alternatives considered**: Una vista SQL o una columna `is_latest`. Cambiarían el esquema.

## Decision: El ticker del alta se compara sin distinguir mayúsculas

**Decision**: La búsqueda del activo usa la comparación sin mayúsculas de la base, sobre el texto ya validado por `InputText.ticker`. La respuesta muestra el ticker guardado en el catálogo, no el texto enviado.

**Rationale**: `ggal` tiene que operar `GGAL`. No hace falta guardar otra forma del ticker.

**Alternatives considered**: Pasar el ticker a mayúsculas antes de buscar. El catálogo de referencia ya está en mayúsculas, pero la comparación sin distinguir caso no depende de eso.

## Decision: La moneda se filtra en la búsqueda

**Decision**: `SearchInstrumentsService` descarta `InstrumentType.MONEDA` después de la coincidencia de texto. La cotización por ticker de `ARS` responde que el activo no está operable, con el mismo código que un ticker desconocido no usa: un código propio de moneda no operable. Un ingreso o egreso nuevo sigue sin grabarse.

**Rationale**: Buscar `PESOS` o `ARS` no debe ofrecer una acción. El portfolio ya excluye la moneda de las posiciones.

**Alternatives considered**: Borrar `ARS` del catálogo. El conjunto de referencia se usa como fue entregado.

## Decision: El motivo no viaja en la consulta

**Decision**: `PlaceOrderService` devuelve el estado y, si es `REJECTED`, uno de `INSUFFICIENT_CASH`, `INSUFFICIENT_SHARES`, `ZERO_SHARES`, `NO_MARKET_PRICE`. `OrderResponse` del alta incluye `rejectionReason` e `amount`. `GET` de la orden no incluye `rejectionReason`. `amount` en el alta es el nocional ya redondeado y no se guarda.

**Rationale**: La spec separa la respuesta del alta de la consulta posterior. No hay columna para el motivo.

**Alternatives considered**: Recalcular el motivo al consultar. El disponible puede haber cambiado y el motivo dejaría de ser el de esa orden.

## Decision: El código estable acompaña al mensaje

**Decision**: `ApiError` pasa a `code` y `message`. Cada excepción de dominio ya existente recibe un código fijo. El mensaje en español no cambia. Los rechazos de entrada que hoy arma `ApiExceptionHandler` también llevan código, sin citar el valor enviado.

**Rationale**: El cliente no puede depender de comparar el texto. Agregar `code` no guarda nada.

**Alternatives considered**: Un encabezado HTTP para el código. El cuerpo queda autodescriptivo y el contrato generado lo documenta.

## Decision: Cómo volver a la orden

**Decision**: El alta responde `201` y el encabezado `Location` con la ruta `GET` de esa orden. Esa consulta devuelve la misma orden.

**Rationale**: La spec pide indicar cómo consultar la orden recién creada, sin una columna nueva.

**Alternatives considered**: Repetir solo el `id` en el cuerpo. El cuerpo ya lo trae. `Location` es la indicación de la ruta.

## Decision: Anotar el contrato generado

**Decision**: Se documentan con springdoc las operaciones nuevas, los `400`, `404` y `409`, y que los importes son texto a dos decimales y un rendimiento `0.48` es 0,48 %. No se guarda una copia del OpenAPI en specs.

**Rationale**: La feature 001 ya decidió que el contrato lo genera la aplicación. Esta feature solo lo completa.

**Alternatives considered**: Escribir `openapi.yaml` a mano. La spec de la feature 001 lo prohíbe y esta no lo reabre.
