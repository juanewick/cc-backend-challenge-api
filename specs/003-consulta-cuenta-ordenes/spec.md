# Feature Specification: Consulta de la cuenta

**Feature Branch**: `003-consulta-cuenta-ordenes`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "Completar la cuenta sin cambiar el modelo entregado. Se pueden consultar las órdenes de una cuenta y una orden puntual. El portfolio informa también el efectivo liquidado y el reservado. El alta indica cómo volver a esa orden, explica un rechazo y el importe. El ticker se opera igual que se busca. La moneda no se ofrece como acción. Cada rechazo es un problema con type tomado de la definición OpenAPI, título, status HTTP, detalle en español y código estable. invalid_params viene solo en una validación de la API. El contrato publicado explica esos errores, los importes y los porcentajes. Portfolio, búsqueda y orden de mercado usan el mismo cierre vigente."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver las órdenes y la reserva (Priority: P1)

Una persona consulta una cuenta y ve, además del portfolio, las órdenes que esa cuenta ya tiene. Puede abrir una orden puntual y, si sigue abierta, cancelarla. El portfolio muestra cuánto efectivo está liquidado, cuánto está reservado y cuánto queda disponible. Esos importes se calculan con las órdenes ya guardadas. No se vuelven a guardar.

**Why this priority**: Hoy el alta y la cancelación existen, pero quien pierde la respuesta del alta no puede encontrar la orden ni explicar por qué bajó el disponible.

**Independent Test**: Con la cuenta `10001` sin órdenes nuevas, listar sus órdenes, abrir la compra límite abierta de 50 `LOMA` a `710.00`, cancelarla desde ese dato y ver que el disponible sube `35500.00`.

**Acceptance Scenarios**:

1. **Given** la cuenta `10001` y sus órdenes de referencia, **When** se listan sus órdenes, **Then** aparecen todas, de la más antigua a la más nueva y, a igual momento, por identificador creciente. La compra límite abierta de 50 `LOMA` a `710.00` está en el listado, con su ticker, lado, tipo, cantidad, precio, estado y fecha.
2. **Given** ese listado, **When** se consulta esa orden dentro de la cuenta `10001`, **Then** el resultado es esa misma orden.
3. **Given** una orden de la cuenta `10001`, **When** la consulta otra cuenta, **Then** no se muestra y el problema es el mismo que el de una orden inexistente (`404`, `BRK-003`), para no revelar que el identificador existe. La orden no cambia.
4. **Given** un identificador que no existe, **When** se consulta, **Then** el problema informa que la orden no existe, sin repetir el identificador.
5. **Given** la cuenta `10001` antes de cualquier orden nueva, **When** se consulta el portfolio, **Then** el valor total sigue en `889756.00`, el disponible en `627500.00`, el efectivo liquidado es `753000.00` y el reservado es `125500.00`.
6. **Given** un alta aceptada, **When** se mira la respuesta, **Then** indica cómo consultar esa orden, y esa consulta devuelve la orden recién creada.

---

### User Story 2 - Entender el resultado del alta (Priority: P2)

Quien envía una orden ve en esa respuesta si quedó ejecutada, abierta o rechazada, por qué fue rechazada y cuál es el importe que comprometió.

**Why this priority**: Un rechazo hoy se graba y responde creado, sin decir si faltaron pesos, acciones, una acción entera o el cierre.

**Independent Test**: Intentar comprar más acciones de `GGAL` de las que el disponible de `10001` cubre y leer en la respuesta el motivo de fondos insuficientes y el importe. El portfolio de referencia no cambia.

**Acceptance Scenarios**:

1. **Given** la cuenta `10001`, **When** se envía una compra que pide más pesos de los disponibles, **Then** la orden queda `REJECTED` y la respuesta dice que faltan fondos, junto con el importe de la cantidad por el precio aplicable.
2. **Given** la cuenta `10001` con 40 acciones disponibles de `PAMP`, **When** se venden 100 a mercado, **Then** la respuesta dice que faltan acciones.
3. **Given** el cierre de `GGAL`, **When** se compra por un monto que no completa una acción, **Then** la respuesta dice que la cantidad resultante es cero.
4. **Given** un activo sin cierre, **When** se envía una orden de mercado, **Then** la respuesta dice que no hay precio vigente.
5. **Given** un alta `FILLED` o `NEW`, **When** se lee la respuesta, **Then** trae el importe calculado, cantidad por precio ya redondeado a dos decimales. Una consulta posterior muestra la orden que ya está guardada. El motivo del rechazo se explica solo en la respuesta del alta y no se agrega a la orden.

---

### User Story 3 - Operar el ticker que la búsqueda encuentra (Priority: P3)

Una persona busca un activo sin cuidar las mayúsculas y después lo opera con el mismo texto. La moneda en pesos no aparece como si fuera una acción que se puede comprar.

**Why this priority**: La búsqueda ya acepta `ggal`. El alta con ese mismo texto hoy informa que el activo no existe.

**Independent Test**: Enviar una compra de `ggal` en la cuenta `10001` y comprobar que opera `GGAL`. Buscar `PESOS` y `ARS` y comprobar que no se ofrece una acción operable.

**Acceptance Scenarios**:

1. **Given** el catálogo de referencia, **When** se envía una orden con ticker `ggal`, **Then** la orden es sobre `GGAL` y la respuesta muestra ese ticker.
2. **Given** el catálogo de referencia, **When** se busca `PESOS` o `ARS`, **Then** la moneda no viene como activo operable. Sigue sin poder registrarse un ingreso o un egreso nuevo.
3. **Given** un ticker del catálogo, **When** se pide su cotización, **Then** se obtiene ese único activo, con último precio y rendimiento diario, o no disponibles si no hay cierre. Una búsqueda por texto sigue devolviendo las coincidencias de ticker o nombre.

---

### User Story 4 - Leer un problema y el contrato (Priority: P4)

Quien integra la cuenta distingue un rechazo por un código `BRK-xxx` y por el tipo de problema publicado en el contrato. El detalle y los títulos están en inglés. Si el rechazo es una validación de la API, la respuesta nombra los parámetros inválidos. Si no lo es, esa lista no viene.

**Why this priority**: Hoy el cliente solo puede comparar el texto del mensaje. Un `0.48` de rendimiento se puede leer como 0,48 % o como 48 %.

**Independent Test**: Provocar un ticker inválido, una cuenta inexistente y una cancelación de una orden que no está abierta. El ticker inválido trae `invalid_params`. La cuenta inexistente y la orden que no está abierta traen su código y su detalle, sin `invalid_params` y sin el valor enviado. El contrato describe esos problemas y las unidades.

**Acceptance Scenarios**:

1. **Given** un parámetro de la API que no cumple la validación, **When** se rechaza la operación, **Then** la respuesta es un problema con `type` igual a la URI del esquema `InvalidParameters` de la definición OpenAPI, `title` "Invalid parameters", `status` igual al HTTP `400`, `detail` en inglés, `code` `BRK-xxx` y `invalid_params`. Cada ítem de `invalid_params` trae `field` y `message`. Ni `detail` ni `message` repiten el valor enviado. El código no cambia si se ajusta la redacción.
2. **Given** un ticker inválido, **When** se rechaza el alta, **Then** el problema es el de parámetros inválidos, `code` es `BRK-009` e `invalid_params` señala el campo `ticker` con el detalle "The ticker is not valid.".
3. **Given** una cuenta, un activo o una orden que no existe, o una orden de otra cuenta, **When** se rechaza la consulta, **Then** `type` es la URI del esquema `NotFound`, `title` es "Not found", `status` es `404`, `code` es `BRK-004`, `BRK-002` o `BRK-003` según el recurso, y `invalid_params` no viene.
4. **Given** una orden que no está abierta, **When** se rechaza la cancelación, **Then** `type` es la URI del esquema `Conflict`, `title` es "Conflict", `status` es `409`, `code` es `BRK-005`, y `invalid_params` no viene.
5. **Given** una regla de negocio que no es una validación de parámetros, como operar la moneda o registrar un ingreso o un egreso nuevo, **When** se rechaza, **Then** `type` es la URI del esquema `BadRequest`, `title` es "Bad request", `status` es `400`, `code` es `BRK-006` o `BRK-007`, y `invalid_params` no viene.
6. **Given** un cuerpo con un campo que no corresponde, **When** se rechaza, **Then** es una validación de la API y trae `invalid_params`, pero no nombra ese campo ni cita el cuerpo.
7. **Given** el contrato publicado por la aplicación, **When** se lee, **Then** publica los esquemas `InvalidParameters`, `BadRequest`, `NotFound` y `Conflict`, describe los rechazos de estas operaciones, presenta los importes como texto a dos decimales y deja explícito que un rendimiento de `0.48` significa 0,48 %. El `type` de cada problema es la URI de su esquema en esa definición.
8. **Given** la cuenta `10001`, una búsqueda `PAMP` y un alta válida, **When** se invocan como antes, **Then** siguen completándose. Un cuerpo ilegible, vacío, con un campo de más, repetido o de otro tipo sigue sin grabar nada y sin citar el cuerpo.

---

### User Story 5 - Usar un solo cierre vigente (Priority: P5)

Portfolio, búsqueda, cotización y orden de mercado toman el mismo último cierre de cada activo: el de la observación más reciente. Alta y cancelación muestran el ticker de ese activo.

**Why this priority**: Si cada lectura elige otro cierre, el precio de la búsqueda y el de la orden de mercado dejan de ser el mismo número.

**Independent Test**: Buscar `PAMP`, pedir su cotización y comprar 1 acción a mercado en la cuenta `10001`. El precio de las tres respuestas es el mismo cierre vigente.

**Acceptance Scenarios**:

1. **Given** un activo con más de un cierre, **When** se consulta el portfolio, se busca, se pide la cotización o se envía una orden de mercado, **Then** las cuatro usan el cierre de la observación más reciente. A igual fecha, manda la observación más nueva.
2. **Given** un alta o una cancelación de un activo que existe, **When** se lee la respuesta, **Then** el ticker es el del activo. No viene vacío.
3. **Given** la cuenta `10001` antes de órdenes nuevas, **When** se consulta el portfolio, **Then** cantidades, valores y rendimientos de `BMA`, `METR` y `PAMP` siguen siendo los de la consulta ya entregada, con el rendimiento total sobre costo promedio.

---

### Edge Cases

- Una posición cuya cantidad neta es cero no se muestra. La posición corta de `BMA` sigue mostrándose, con cantidad y valor de mercado negativos.
- Un activo sin último cierre sigue en la posición con valor y rendimientos no disponibles, y no suma al valor total. Su ticker figura en `unpricedTickers`, para que el valor total no parezca completo.
- Si el cierre anterior es cero o no existe, el rendimiento diario queda no disponible.
- Si no hay costo promedio (sin compras `FILLED` en una larga, sin ventas `FILLED` en una corta), el rendimiento total queda no disponible.
- La moneda sigue sin ser una posición.
- Quien conoce el número de cuenta opera. Esta feature no agrega sesiones, permisos ni un límite de intentos.
- Los tipos ya definidos para la cantidad de la orden y para su fecha no se cambian. No se agrega una fecha de cancelación.
- No se crea ninguna tabla ni ninguna columna, y no se modifica ninguna columna existente. El motivo del rechazo, el efectivo liquidado, el reservado, el importe del alta y el problema de un error se calculan en la respuesta. No se guardan.
- Una validación que nombra un campo conocido, como la cuenta, el ticker, el identificador de la orden, la búsqueda, el lado, el tipo, la cantidad, el monto o el precio, trae ese campo en `invalid_params`. Si faltan las dos formas de cantidad, o vienen las dos, la lista señala cantidad y monto.
- Un pedido ilegible o con un tipo de contenido que no se puede leer es un problema `BadRequest` y no trae `invalid_params`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST listar las órdenes de una cuenta existente y MUST permitir consultar una por su identificador. Una orden de otra cuenta MUST NOT mostrarse y MUST responder igual que una orden inexistente. Una orden inexistente MUST informarse sin repetir el identificador.
- **FR-002**: El listado MUST incluir ticker, lado, tipo, cantidad, precio, estado y fecha, ordenado de la más antigua a la más nueva y, a igual momento, por identificador creciente.
- **FR-003**: El portfolio MUST informar el valor total, el efectivo liquidado, el efectivo reservado y el disponible. El liquidado y el reservado MUST calcularse con las órdenes ya guardadas y MUST NOT guardarse de nuevo. El reservado MUST ser la suma de las compras abiertas. Las ventas abiertas MUST seguir reservando acciones.
- **FR-004**: El alta aceptada o rechazada y guardada MUST indicar cómo consultar esa orden. Esa consulta MUST devolver la misma orden.
- **FR-005**: La respuesta del alta MUST incluir el importe calculado en ese momento, cantidad por precio aplicable ya redondeado. Ese importe MUST NOT guardarse. Si el estado es `REJECTED`, MUST incluir un motivo estable entre fondos insuficientes, acciones insuficientes, cantidad resultante cero y falta de precio vigente. El motivo MUST NOT guardarse y MUST NOT agregarse a la orden.
- **FR-006**: Una consulta de la orden MUST mostrar el estado y los datos que la orden ya guarda. MUST NOT exigir el motivo del rechazo.
- **FR-007**: El alta MUST resolver el ticker sin distinguir mayúsculas. La respuesta MUST mostrar el ticker del catálogo.
- **FR-008**: Una búsqueda MUST NOT ofrecer la moneda como activo operable. Un ingreso o un egreso de pesos nuevo MUST seguir sin grabarse.
- **FR-009**: Se MUST poder pedir la cotización de un ticker. Si no hay cierre, el precio y el rendimiento diario MUST figurar como no disponibles.
- **FR-010**: Todo rechazo de dominio MUST responder un problema con `type`, `title`, `status`, `detail` y `code`. `type` MUST ser la URI del esquema correspondiente en la definición OpenAPI publicada por la aplicación. `status` MUST ser el estado HTTP declarado para ese error. `detail` MUST ser el texto en inglés de ese error y MUST NOT repetir el valor enviado ni un detalle interno. `code` MUST ser el `BRK-xxx` de ese error, MUST existir solo en la respuesta y MUST NOT guardarse. El código no cambia si se ajusta la redacción. Un pedido ilegible, un campo de más, uno repetido o de otro tipo MUST seguir fuera de ese catálogo, con `code` `UNREADABLE`, `UNKNOWN_FIELD`, `DUPLICATE_FIELD` o `INVALID_FIELD` y el detalle en inglés.
- **FR-011**: `invalid_params` MUST venir solo cuando el rechazo es una validación de la API. Cada ítem MUST traer `field` y `message`, y `message` MUST NOT repetir el valor enviado. Un campo que no corresponde MUST tratarse como validación de la API sin nombrar ese campo. En un recurso inexistente, un conflicto o una regla de negocio, `invalid_params` MUST NOT venir.
- **FR-012**: El contrato publicado MUST incluir los esquemas `InvalidParameters`, `BadRequest`, `NotFound` y `Conflict`, MUST describir los rechazos, MUST presentar los importes como texto a dos decimales y MUST dejar explícito que un rendimiento informado como `0.48` es 0,48 %.
- **FR-013**: Portfolio, búsqueda, cotización y orden de mercado MUST usar el cierre de la observación más reciente del activo. A igual fecha, MUST usarse la observación más nueva.
- **FR-014**: Alta y cancelación de un activo existente MUST devolver su ticker.
- **FR-015**: La cuenta `10001`, la búsqueda `PAMP` y un alta válida MUST seguir completándose como antes de esta feature. Las posiciones, el disponible y el valor total de referencia MUST conservarse. Repetir ese alta MUST crear otra orden.
- **FR-016**: Esta feature MUST NOT crear tablas ni columnas, y MUST NOT modificar columnas existentes. Las órdenes, las cuentas, los activos y los precios MUST seguir guardándose solo donde ya se guardan.

### Key Entities

- **Orden**: La orden ya guardada de una cuenta, con los datos que ya tiene: ticker, lado, tipo, cantidad, precio, estado y fecha de alta. Listarla o consultarla no le agrega datos.
- **Reserva de la cuenta**: Efectivo liquidado, efectivo reservado por compras abiertas y efectivo disponible. Se calcula al responder, con las órdenes ya existentes. No se guarda.
- **Resultado de alta**: Lo que vuelve al crear. El importe y, si fue rechazada, el motivo se calculan en esa respuesta y no se guardan.
- **Cotización**: La lectura de un activo que ya existe, con su último precio y su rendimiento diario, o sin ellos si no hay cierre. No se guarda una cotización nueva.
- **Problema**: El rechazo de una operación. Lleva el tipo publicado en el contrato, un título en inglés, el estado HTTP, el detalle en inglés y un código `BRK-xxx`, salvo el pedido ilegible y el campo de más, repetido o de otro tipo. La lista de parámetros inválidos forma parte del problema solo en una validación de la API. No se guarda.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las cuentas de referencia puede listar sus órdenes y abrir una orden propia. El 100% de los intentos de leer una orden ajena o inexistente no muestra datos de esa orden ni la modifica.
- **SC-002**: En la cuenta `10001`, antes de órdenes nuevas, el portfolio informa `753000.00` liquidado, `125500.00` reservado, `627500.00` disponible y `889756.00` de valor total.
- **SC-003**: El 100% de los rechazos guardados del alta traen un motivo de los cuatro definidos y el importe. Una compra con `ggal` opera `GGAL`. Buscar `PESOS` o `ARS` no devuelve una acción operable.
- **SC-004**: El 100% de los rechazos traen `type`, `title`, `status`, `detail` y `code`, sin el valor enviado. `invalid_params` está en el 100% de las validaciones de la API y en el 0% de los demás rechazos. El contrato publicado describe esos problemas y la unidad de los porcentajes. El precio de `PAMP` en la búsqueda, en la cotización y en una compra a mercado es el mismo.

## Assumptions

- Esta feature se apoya en el portfolio, la búsqueda, el alta y la cancelación ya entregados, y en el endurecimiento de las entradas. No reabre esas reglas salvo donde un escenario de esta feature lo dice.
- Repetir el mismo pedido crea otra orden, como hasta ahora. No se agregan sesiones, permisos ni un tope de intentos.
- No se crea una tabla ni una columna, y no se modifica ninguna columna. La cantidad y la fecha de la orden quedan con los tipos del modelo de referencia. La fecha sigue siendo la del alta. No se agrega una fecha de cancelación.
- Quien consulta la orden ve el estado `REJECTED` y los datos que esa orden ya tenía. El motivo se entiende en la respuesta del alta.
- Quien conoce el número de cuenta opera, igual que antes.
- El detalle de cada rechazo de dominio es el mensaje en inglés de su código `BRK-xxx`. El problema le agrega tipo, título, estado y ese código. Ajustar la redacción no cambia el código.
- `type` no es una dirección de otra empresa. Es la URI del esquema dentro de la definición OpenAPI que publica esta aplicación.
- Los títulos son "Invalid parameters", "Bad request", "Not found" y "Conflict", uno por esquema.
