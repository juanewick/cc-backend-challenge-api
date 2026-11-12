# Feature Specification: Portfolio, búsqueda y órdenes

**Feature Branch**: `001-brokerage-trading-api`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "Una cuenta de inversión en pesos debe poder consultarse (valor total, pesos disponibles para operar y posiciones con cantidad, valor de mercado y rendimiento total), buscar activos del mercado por ticker y/o por nombre, y enviar órdenes de compra o venta MARKET y LIMIT. Las órdenes MARKET se ejecutan al instante al último precio; las LIMIT quedan abiertas. Si no hay efectivo o acciones suficientes, la orden se guarda rechazada. Solo se cancelan órdenes abiertas. El efectivo y las tenencias se reconstruyen desde las órdenes ejecutadas. Las transferencias de pesos se modelan como órdenes. No hay que simular el mercado ni autenticar usuarios. El conjunto de cuentas, activos, precios y órdenes de referencia se usa tal como fue entregado."

## Clarifications

### Session 2026-11-12

- Q: ¿Cómo se calcula el rendimiento total de cada posición? → A: (Valor de mercado − capital neto de compras y ventas ejecutadas) / valor absoluto de ese capital
- Q: ¿Las órdenes que siguen abiertas reservan el dinero o las acciones disponibles? → A: Las compras abiertas reservan pesos y las ventas abiertas reservan acciones
- Q: ¿Se pueden registrar ingresos y egresos de pesos nuevos, o el portfolio solo usa los que ya vienen cargados? → A: No se registran nuevos; el portfolio solo usa los ingresos y egresos ya cargados
- Q: ¿Qué activos devuelve una búsqueda? → A: Los activos cuyo ticker o nombre contienen el texto, sin distinguir mayúsculas
- Q: ¿El rendimiento diario se muestra además del rendimiento total? → A: Se muestra en cada posición y en cada activo encontrado, además del rendimiento total de la posición

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el portfolio (Priority: P1)

Una persona indica el número de cuenta y obtiene el estado de esa cuenta: cuánto vale en total, cuántos pesos tiene libres para operar y qué activos posee, con la cantidad de acciones, el valor en pesos de cada posición, su rendimiento total y su rendimiento diario.

**Why this priority**: Es la información principal del pedido. Con los datos de referencia ya se puede demostrar sin enviar órdenes nuevas.

**Independent Test**: Consultar la cuenta `10001` (`emiliano@test.com`) sobre el conjunto de referencia y comparar efectivo, valor total y posiciones con los valores de esta historia.

**Acceptance Scenarios**:

1. **Given** el conjunto de referencia, **When** se consulta la cuenta `10001`, **Then** el resultado muestra exactamente:
   - pesos disponibles para operar: `627500.00`
   - valor total de la cuenta: `889756.00`
   - posiciones, ordenadas por ticker:

   | Ticker | Cantidad | Valor de mercado | Rendimiento total | Rendimiento diario |
   |--------|----------|------------------|-------------------|--------------------|
   | BMA    | -10      | -15028.00        | 0.48%             | -1.15%             |
   | METR   | 500      | 114750.00        | -8.20%            | -1.08%             |
   | PAMP   | 40       | 37034.00         | -0.18%            | 0.44%              |

2. **Given** una cuenta existente sin órdenes, **When** se consulta su portfolio, **Then** el valor total es `0.00`, los pesos disponibles son `0.00` y el listado de posiciones está vacío.
3. **Given** un número de cuenta que no existe, **When** se consulta el portfolio, **Then** no hay resultado de portfolio y se informa que la cuenta no existe.
4. **Given** la cuenta `10001`, **When** se consulta el portfolio sin identificarse, **Then** la consulta se atiende igual: no hay control de acceso.
5. **Given** los ingresos y egresos de pesos ya cargados en la cuenta `10001`, **When** se consulta su portfolio, **Then** esos movimientos están incorporados en los `753000.00` de efectivo liquidado que sostienen el disponible y el valor total de esta historia. La moneda no aparece como posición.
6. **Given** la cuenta `10001`, **When** se intenta registrar un ingreso o un egreso de pesos nuevo, **Then** no se graba ninguna orden y el portfolio sigue igual.

---

### User Story 2 - Buscar activos (Priority: P2)

Una persona escribe un texto y obtiene los activos del mercado cuyo ticker o cuyo nombre se parecen a ese texto, sin duplicados.

**Why this priority**: Hace falta encontrar un activo antes de operar, y se puede probar solo con el catálogo de referencia.

**Independent Test**: Buscar `banco` y `pamp` sobre el catálogo de referencia y verificar coincidencias, ausencia de duplicados y datos de precio.

**Acceptance Scenarios**:

1. **Given** el catálogo de referencia, **When** se busca `banco`, **Then** el resultado contiene solo, en este orden, `BBAR` (Banco Frances), `BHIP` (Banco Hipotecario S.A.), `BMA` (Banco Macro S.A.) y `BPAT` (Banco Patagonia).
2. **Given** el catálogo de referencia, **When** se busca `pamp`, **Then** el resultado contiene una sola vez `PAMP` (Pampa Holding S.A.), con último precio `925.85` y rendimiento diario `0.44%`.
3. **Given** el catálogo de referencia, **When** se busca `YPF`, **Then** el resultado incluye `YPFD` porque el ticker contiene ese texto.
4. **Given** el catálogo de referencia, **When** se busca `zzzz`, **Then** el resultado es un listado vacío.
5. **Given** un texto vacío o compuesto solo por espacios, **When** se busca, **Then** no hay listado y se informa que la búsqueda no tiene texto.
6. **Given** un activo sin precio, **When** aparece en una búsqueda, **Then** se lo identifica igual y su precio y rendimiento diario figuran como no disponibles.

---

### User Story 3 - Enviar una orden de mercado (Priority: P3)

Una persona compra o vende un activo al precio vigente. Indica una cantidad exacta de acciones o un monto en pesos. La orden se ejecuta en el acto, queda registrada como ejecutada y el portfolio pasa a reflejar la nueva tenencia y el nuevo efectivo.

**Why this priority**: Es la operación que cambia la cuenta. No depende de dejar órdenes abiertas.

**Independent Test**: Partiendo del conjunto de referencia, comprar 1 acción de `GGAL` a mercado en la cuenta `10001` y verificar estado, precio, efectivo y posición.

**Acceptance Scenarios**:

1. **Given** la cuenta `10001` y el último cierre de `GGAL` en `885.80`, **When** se envía una compra MARKET de 1 acción sin precio, **Then** la orden queda `FILLED`, con cantidad `1` y precio `885.80`, el efectivo liquidado pasa a `752114.20`, los pesos disponibles pasan a `626614.20` y aparece una posición `GGAL` de 1 acción con valor `885.80` y rendimiento total `0.00%`.
2. **Given** la cuenta `10001`, **When** se envía una compra MARKET de `GGAL` por `2000` pesos, **Then** la cantidad es `2` (parte entera de `2000 / 885.80`), el precio guardado es `885.80`, el estado es `FILLED` y el efectivo baja `1771.60`, no `2000`.
3. **Given** la cuenta `10001` con 40 acciones disponibles de `PAMP`, **When** se vende 1 acción MARKET, **Then** la orden queda `FILLED` al cierre `925.85`, la posición de `PAMP` queda en 39 acciones y el efectivo aumenta `925.85`.
4. **Given** una orden MARKET, **When** la solicitud trae además un precio, **Then** ese precio se ignora y se usa el último cierre.
5. **Given** una solicitud MARKET, **When** indica a la vez cantidad de acciones y monto en pesos, o no indica ninguno, **Then** no se graba ninguna orden y se informa que debe indicarse exactamente una de las dos formas.
6. **Given** una solicitud, **When** la cantidad no es un entero positivo, el monto no es mayor que cero, el activo no existe o la cuenta no existe, **Then** no se graba ninguna orden y se informa el dato inválido.

---

### User Story 4 - Enviar una orden límite (Priority: P4)

Una persona deja una orden de compra o de venta a un precio elegido. La orden queda abierta. No se ejecuta sola. Mientras sigue abierta, el efectivo o las acciones que compromete dejan de estar disponibles para operar.

**Why this priority**: El pedido distingue este tipo de orden del de mercado, y el conjunto de referencia ya trae órdenes abiertas que el portfolio debe respetar.

**Independent Test**: Enviar una compra límite nueva y comprobar que queda abierta, que no crea posición y que reduce solo los pesos disponibles.

**Acceptance Scenarios**:

1. **Given** la cuenta `10001`, **When** se envía una compra LIMIT de 10 acciones de `GGAL` a `800.00`, **Then** la orden queda `NEW` con cantidad `10` y precio `800.00`, no existe posición nueva por esa orden, el efectivo liquidado no cambia y los pesos disponibles bajan `8000.00`.
2. **Given** una orden LIMIT, **When** no se envía precio, o el precio no es mayor que cero, **Then** no se graba ninguna orden y se informa que el precio es obligatorio.
3. **Given** la cuenta `10001`, **When** se envía una compra LIMIT de `GGAL` por `2500` pesos a `800.00`, **Then** la orden queda `NEW` con cantidad `3` y reserva `2400.00`.
4. **Given** una orden LIMIT recién aceptada, **When** se vuelve a consultar el portfolio, **Then** la orden sigue `NEW`: no hay ejecución automática contra el último precio.
5. **Given** la cuenta `10001` con 40 acciones disponibles de `PAMP`, **When** se envía una venta LIMIT de 10 acciones a `1000.00`, **Then** la orden queda `NEW`, el efectivo no cambia, las acciones disponibles de `PAMP` pasan a 30 y una venta posterior de 40 acciones se rechaza.

---

### User Story 5 - Rechazar órdenes que no se pueden cubrir (Priority: P5)

Si la compra pide más pesos de los disponibles, o la venta pide más acciones de las disponibles, la orden no se aplica. Queda registrada como rechazada y la cuenta sigue igual.

**Why this priority**: Evita que la cuenta quede en descubierto. Se puede probar con una sola orden inválida sobre los datos de referencia.

**Independent Test**: Intentar comprar más pesos de los disponibles y vender más acciones de las disponibles; el portfolio de `10001` debe quedar idéntico al de la historia 1.

**Acceptance Scenarios**:

1. **Given** la cuenta `10001` con `627500.00` pesos disponibles, **When** se envía una compra MARKET de `1000000` acciones de `GGAL`, **Then** la orden se guarda `REJECTED` y el portfolio sigue igual que en la historia 1.
2. **Given** la cuenta `10001` con 40 acciones disponibles de `PAMP`, **When** se vende `100` acciones MARKET, **Then** la orden se guarda `REJECTED` y la posición de `PAMP` sigue en 40.
3. **Given** el cierre de `GGAL` en `885.80`, **When** se compra por `100` pesos, **Then** la cantidad entera resultante es `0`, la orden se guarda `REJECTED` y los saldos no cambian.
4. **Given** pesos u acciones ya reservados por órdenes `NEW`, **When** una orden nueva necesita ese mismo saldo, **Then** se rechaza: no se reutiliza lo reservado ni se recorta la orden para que entre.
5. **Given** dos órdenes válidas por separado que juntas superan el disponible, **When** se acepta la primera y se envía la segunda, **Then** la segunda se guarda `REJECTED`.

---

### User Story 6 - Cancelar una orden abierta (Priority: P6)

Una persona cancela una orden que todavía está abierta. La orden pasa a cancelada y lo que tenía reservado vuelve a estar disponible. No se puede cancelar una orden ejecutada, rechazada o ya cancelada, ni una orden de otra cuenta.

**Why this priority**: El pedido lo exige, y el conjunto de referencia ya incluye una orden abierta identificable sin crear una nueva.

**Independent Test**: Cancelar la compra límite abierta de 50 acciones de `LOMA` a `710.00` de la cuenta `10001` y comprobar el estado y los pesos disponibles.

**Acceptance Scenarios**:

1. **Given** la cuenta `10001` y su compra límite `NEW` de 50 `LOMA` a `710.00`, **When** el titular la cancela, **Then** el estado pasa a `CANCELLED`, el efectivo liquidado no cambia y los pesos disponibles aumentan `35500.00`, hasta `663000.00`.
2. **Given** una orden `FILLED`, `REJECTED` o `CANCELLED`, **When** se intenta cancelar, **Then** el estado no cambia y se informa que solo se cancelan órdenes abiertas.
3. **Given** una orden abierta de la cuenta `10001`, **When** la cancela otra cuenta, **Then** el estado no cambia y se informa que la orden no pertenece a esa cuenta.
4. **Given** una orden que no existe, **When** se intenta cancelar, **Then** se informa que la orden no existe.
5. **Given** una venta LIMIT `NEW` de 10 `PAMP`, **When** el titular la cancela, **Then** el estado pasa a `CANCELLED` y las acciones disponibles de `PAMP` vuelven a incluir esas 10. El efectivo no cambia.

---

### User Story 7 - Identificar el servicio y su ruta pública (Priority: P7)

Quien arranca o invoca el servicio lo reconoce como `brokerage-api` y usa una sola ruta base, `/api/brokerage-api`, con versión `/v1` en cada operación, para consultar el portfolio, buscar activos y enviar o cancelar órdenes.

**Why this priority**: Distingue este servicio de otros, deja una dirección estable y permite publicar otra versión más adelante sin mover la base.

**Independent Test**: Arrancar el servicio y comprobar que se identifica como `brokerage-api`; consultar el portfolio de `10001`, buscar `pamp` y enviar una orden usando `/api/brokerage-api/v1`. La misma consulta sin esa base, o con la base pero sin `/v1`, no se atiende.

**Acceptance Scenarios**:

1. **Given** la aplicación en marcha, **When** se observa la identificación de arranque, **Then** el nombre visible es `brokerage-api`.
2. **Given** la cuenta `10001`, **When** se consulta el portfolio en `/api/brokerage-api/v1/accounts/10001/portfolio`, **Then** la consulta se atiende igual que en la historia 1.
3. **Given** el catálogo de referencia, **When** se busca `pamp` en `/api/brokerage-api/v1/instruments`, **Then** el resultado es el de la historia 2.
4. **Given** una orden de compra o de venta, **When** se envía o se cancela bajo `/api/brokerage-api/v1`, **Then** las reglas de las historias 3 a 6 siguen valiendo.
5. **Given** las mismas operaciones, **When** se invocan sin `/api/brokerage-api` o sin `/v1`, **Then** no hay resultado de negocio: no se consulta el portfolio ni se graba una orden.

---

### User Story 8 - Saber si la API está activa (Priority: P8)

Quien va a operar, o quien acaba de cambiar la configuración, consulta dos rutas fijas. Health dice si el proceso y la base están sanos. Info dice que el proceso es `brokerage-api`. El contrato de las operaciones de negocio se lee del OpenAPI que la propia aplicación genera al arrancar. No hay un contrato escrito a mano.

**Why this priority**: No cambia el cálculo del portfolio. Permite detectar, antes de confiar en el resto de las operaciones, que Actuator quedó expuesto de más o de menos y que la aplicación responde.

**Independent Test**: `GET /actuator/health` devuelve `UP` sin componentes. `GET /actuator/info` devuelve el nombre `brokerage-api`. `GET /actuator/env` no se atiende. `GET /v3/api-docs` devuelve un OpenAPI que incluye el portfolio.

**Acceptance Scenarios**:

1. **Given** la aplicación en marcha y la base disponible, **When** se consulta `/actuator/health`, **Then** la respuesta es correcta y el estado es `UP`, sin el detalle de componentes.
2. **Given** la aplicación en marcha, **When** se consulta `/actuator/info`, **Then** la respuesta es correcta e identifica la aplicación como `brokerage-api`.
3. **Given** la aplicación en marcha, **When** se consulta otro endpoint de Actuator, como `/actuator/env`, **Then** no se atiende.
4. **Given** la aplicación en marcha, **When** se pide `/v3/api-docs`, **Then** el documento es OpenAPI generado por la aplicación e incluye las operaciones de portfolio, búsqueda y órdenes bajo `/api/brokerage-api/v1`. La misma descripción está en `/v3/api-docs.yaml` y se puede leer en `/swagger-ui.html`.

---

### Edge Cases

- Una posición cuya cantidad neta es cero no se muestra. La posición corta de `BMA` en la cuenta de referencia sí se muestra, con cantidad y valor de mercado negativos.
- La moneda en pesos no es una posición, aunque sus movimientos sean órdenes.
- Un activo sin último cierre se lista con su cantidad y con valor de mercado y rendimiento no disponibles, y no suma al valor total de la cuenta.
- Si el cierre anterior es cero o no existe, el rendimiento diario queda no disponible.
- Si el capital neto invertido es cero y la cantidad neta no es cero, el rendimiento total queda no disponible.
- Una orden de mercado sobre un activo sin último cierre se guarda `REJECTED` y no cambia la cuenta.
- El sobrante de un monto que no completa otra acción permanece como pesos; no se compran fracciones.
- Una orden rechazada o cancelada no cambia tenencias, efectivo liquidado ni reservas.
- Cancelar una venta límite abierta devuelve las acciones reservadas, no pesos.
- Los importes se informan con dos decimales y los porcentajes con dos decimales, redondeo half-up.
- Quien conozca el número de cuenta puede consultarla y operarla. No hay sesiones ni permisos.
- Un intento de registrar un ingreso o un egreso de pesos nuevo no graba una orden y no cambia el efectivo.
- Health e info viven en `/actuator`, fuera de `/api/brokerage-api`. El OpenAPI generado vive en `/v3/api-docs`. Esas rutas no son operaciones de portfolio, búsqueda ni órdenes.
- Un endpoint de Actuator distinto de health e info, incluido uno agregado por una versión nueva de la plataforma, no queda publicado. No se usa una lista que exponga todos y después excluya algunos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir consultar el portfolio de una cuenta existente a partir de su número de cuenta.
- **FR-002**: El portfolio MUST informar el valor total de la cuenta, los pesos disponibles para operar y el listado de posiciones.
- **FR-003**: Cada posición MUST informar el activo, la cantidad neta de acciones, el valor de mercado en pesos, el rendimiento total y el rendimiento diario, ambos en porcentaje.
- **FR-004**: Todos los precios y valores monetarios MUST expresarse en pesos.
- **FR-005**: La cantidad neta de un activo MUST ser la suma de las cantidades de sus compras `FILLED` menos la suma de las cantidades de sus ventas `FILLED`.
- **FR-006**: El valor de mercado de una posición MUST ser la cantidad neta multiplicada por el cierre más reciente de ese activo.
- **FR-007**: El capital neto invertido de un activo MUST ser la suma de cantidad por precio de sus compras `FILLED` menos la suma de cantidad por precio de sus ventas `FILLED`.
- **FR-008**: El rendimiento total MUST ser `(valor de mercado − capital neto invertido) / valor absoluto del capital neto invertido`, expresado en porcentaje, usando solo órdenes `FILLED` de ese activo.
- **FR-009**: El rendimiento diario MUST ser `(cierre − cierre anterior) / cierre anterior`, expresado en porcentaje, a partir del precio más reciente.
- **FR-010**: El precio vigente de un activo MUST ser el cierre de su fecha más reciente. Si hay más de un precio en esa fecha, MUST usarse el registrado en último lugar.
- **FR-011**: El efectivo liquidado MUST sumar los ingresos `FILLED`, restar los egresos `FILLED`, restar el importe de las compras `FILLED` y sumar el importe de las ventas `FILLED`. El importe de una compra o venta es cantidad por precio.
- **FR-012**: Los pesos disponibles MUST ser el efectivo liquidado menos el importe reservado por las compras `NEW`.
- **FR-013**: Las acciones disponibles de un activo MUST ser su cantidad neta menos la cantidad de sus ventas `NEW`.
- **FR-014**: El valor total de la cuenta MUST ser el efectivo liquidado más el valor de mercado de las posiciones que tienen cierre. El efectivo reservado sigue formando parte del valor total.
- **FR-015**: Las posiciones de cantidad neta cero y la moneda en pesos MUST excluirse del listado. Las posiciones mostradas MUST ordenarse por ticker.
- **FR-016**: Una búsqueda MUST aceptar un texto y devolver, sin duplicados y ordenados por ticker, los activos cuyo ticker o cuyo nombre contienen ese texto, sin distinguir mayúsculas de minúsculas.
- **FR-017**: Cada activo encontrado MUST informar ticker, nombre, tipo, último precio y rendimiento diario cuando existan.
- **FR-018**: Una orden de compra o de venta MUST indicar la cuenta, el activo, el lado y exactamente una forma de cantidad: acciones enteras o monto en pesos.
- **FR-019**: Cuando la orden indica un monto, la cantidad MUST ser la parte entera de `monto / precio aplicable`, nunca una fracción. El precio aplicable es el último cierre en una orden de mercado y el precio enviado en una orden límite.
- **FR-020**: Una orden MARKET válida MUST guardarse `FILLED` de inmediato, con el último cierre como precio, aunque la solicitud no traiga precio.
- **FR-021**: Una orden LIMIT válida MUST guardarse `NEW` con el precio enviado y MUST permanecer así: el sistema no simula el mercado ni la ejecuta después.
- **FR-022**: Una compra cuyo importe supera los pesos disponibles, o una venta cuya cantidad supera las acciones disponibles, MUST guardarse `REJECTED`. Lo mismo MUST ocurrir cuando la cantidad resultante de un monto es cero o cuando una orden de mercado no tiene cierre.
- **FR-023**: Una orden `REJECTED` o `CANCELLED` MUST dejar sin cambios la tenencia, el efectivo liquidado y las reservas.
- **FR-024**: Una orden `FILLED` MUST quedar reflejada en la siguiente consulta de portfolio.
- **FR-025**: Solo el titular MUST poder cancelar una orden, y solo si su estado es `NEW`. El estado pasa a `CANCELLED` y se libera la reserva de pesos o de acciones.
- **FR-026**: Los ingresos y egresos de pesos ya guardados MUST participar del efectivo. El sistema MUST NOT aceptar un ingreso o un egreso nuevo: no se graba una orden y el efectivo no cambia.
- **FR-027**: Toda orden guardada MUST conservar lado, cantidad, precio, tipo, estado y fecha-hora. Los lados son `BUY`, `SELL`, `CASH_IN` y `CASH_OUT`. Los tipos son `MARKET` y `LIMIT`. Los estados son `NEW`, `FILLED`, `REJECTED` y `CANCELLED`.
- **FR-028**: El sistema MUST operar sobre el conjunto de referencia entregado, sin cambiar su significado: las órdenes históricas `FILLED` cuentan aunque sean límite, y las `NEW`, `CANCELLED` y `REJECTED` históricas se tratan con estas mismas reglas.
- **FR-029**: El sistema MUST NOT exigir autenticación ni restringir el acceso a una cuenta.
- **FR-030**: Una solicitud con datos inválidos, o un ingreso o egreso de pesos nuevo, MUST NOT grabar una orden. El rechazo de negocio de FR-022 sí MUST grabarse.
- **FR-031**: La aplicación MUST poder ponerse en marcha con Docker, junto con su base y los datos de referencia, y MUST atender portfolio, búsqueda y órdenes sin instalar la plataforma ni la base por separado.
- **FR-032**: Al arrancar, la aplicación MUST identificarse como `brokerage-api`.
- **FR-033**: Todas las operaciones de portfolio, búsqueda y órdenes MUST atenderse bajo la ruta base `/api/brokerage-api` y la versión `/v1` de cada operación. Las mismas operaciones sin esa base o sin esa versión MUST NOT atenderse.
- **FR-034**: El sistema MUST exponer por HTTP solo dos endpoints de Actuator: `GET /actuator/health` y `GET /actuator/info`. Health MUST informar `UP` cuando la aplicación y su base responden, y MUST NOT publicar el detalle de componentes. Info MUST identificar la aplicación como `brokerage-api`. Cualquier otro endpoint de Actuator MUST NOT quedar expuesto.
- **FR-035**: El contrato de la API de negocio MUST ser el OpenAPI que springdoc-openapi 3.2.0 genera al arrancar. MUST servirse en `/v3/api-docs` y `/v3/api-docs.yaml`, y MUST poder leerse en `/swagger-ui.html`. El repositorio MUST NOT guardar una copia escrita a mano de ese contrato.

### Key Entities *(include if feature involves data)*

- **Cuenta**: Persona que opera. Se identifica por su número de cuenta y tiene un correo. La cuenta de referencia `10001` corresponde a `emiliano@test.com`.
- **Activo**: Instrumento del mercado, con ticker, nombre y tipo. Los tipos de referencia son acciones y moneda. La moneda es `ARS` (pesos) y no se muestra como posición.
- **Orden**: Movimiento de una cuenta sobre un activo. Tiene cantidad entera, precio en pesos, lado, tipo, estado y fecha-hora. Es la única fuente para reconstruir tenencias, efectivo y reservas.
- **Precio**: Cotización diaria de un activo, con apertura, máximo, mínimo, cierre, cierre anterior y fecha. El cierre de la fecha más reciente es el último precio. En el conjunto entregado, la cotización está fechada por día calendario.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La consulta de la cuenta de referencia `10001` coincide al 100% con el efectivo disponible, el valor total y las tres posiciones definidos en la historia 1, incluidos los redondeos a dos decimales.
- **SC-002**: Sobre el catálogo de referencia, una búsqueda devuelve el 100% de los activos cuyo ticker o nombre contiene el texto, y el 0% de los que no, en menos de 1 segundo.
- **SC-003**: El resultado de una orden de mercado válida ya trae el estado ejecutado y el precio aplicado, y la consulta de portfolio inmediata muestra la tenencia y el efectivo actualizados.
- **SC-004**: El 100% de las compras que superan los pesos disponibles y de las ventas que superan las acciones disponibles quedan rechazadas, y una consulta posterior muestra la cuenta sin cambios.
- **SC-005**: El 100% de las órdenes límite válidas quedan abiertas, y el 100% de las cancelaciones válidas las pasan a canceladas y devuelven el disponible reservado antes de la siguiente consulta.
- **SC-006**: Con el conjunto de referencia, una persona completa en un primer intento las tres tareas centrales: leer el portfolio de `10001`, encontrar `PAMP` por nombre o ticker y dejar registrada una orden de compra o de venta con el estado que corresponde.
- **SC-007**: Con Docker disponible, un único arranque deja la aplicación y sus datos de referencia listos, sin instalar la plataforma ni la base por separado, y la consulta de la cuenta `10001` coincide con la historia 1.
- **SC-008**: El 100% de las operaciones de portfolio, búsqueda y órdenes se invocan bajo la ruta base `/api/brokerage-api` y la versión `/v1`, y al arrancar el servicio se reconoce el nombre `brokerage-api`.
- **SC-009**: Con la base disponible, `/actuator/health` informa `UP` sin componentes y `/actuator/info` informa `brokerage-api`. Ningún otro endpoint de Actuator responde. `/v3/api-docs` describe las operaciones de las historias 1 a 6.

## Assumptions

- El número de cuenta es el identificador público. No hace falta una sesión.
- La cantidad neta negativa se conserva: el conjunto de referencia deja a `BMA` en corto para la cuenta `10001` (20 acciones compradas y 30 vendidas, todas ejecutadas).
- Si el monto alcanza para más acciones de las que el disponible puede cubrir, la orden se rechaza. No se reduce en silencio hasta el máximo cubierto. "Cantidad máxima" se interpreta solo como el máximo de acciones enteras que ese monto puede comprar o vender al precio aplicable.
- En una orden de mercado se ignora un precio enviado por quien opera.
- La definición completa entregada para los datos prevalece cuando el relato resumido difiere: el precio tiene fecha de calendario, y la orden tiene fecha y hora.
- Quedan fuera de alcance la autenticación, la simulación del mercado, las ejecuciones parciales, las fracciones de acciones, otras monedas, el alta o la modificación de cuentas y activos, y el registro de ingresos o egresos de pesos nuevos.
- Un catálogo de ejemplos para invocar cada operación es deseable y no es necesario para dar por cumplido el comportamiento.
- Las restricciones de construcción que acompañaron el pedido original no cambian este comportamiento. La planificación debe aplicarlas tal como fueron pedidas, y las decisiones de diseño que esta especificación no fija deben quedar escritas para quien revise la solución.
- La aplicación se arranca sola con Docker, junto con la base y los datos de referencia. El código Java vive en esta misma carpeta del proyecto.
- springdoc-openapi 3.2.0 es la versión de esa biblioteca para Spring Boot 4.1.x. Está construida sobre Spring Boot 4.1.2. La línea 3.0.x corresponde a Spring Boot 4.0 y la 2.x a Spring Boot 3, así que no aplican a este servicio. El artefacto es `springdoc-openapi-starter-webmvc-ui`, el de Spring MVC con la interfaz de lectura.
- Health no publica componentes ni la base a la que se conecta. Sin autenticación, ese detalle quedaría abierto. Alcanza con `UP` o `DOWN`.
