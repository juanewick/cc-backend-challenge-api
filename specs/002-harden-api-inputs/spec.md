# Feature Specification: Endurecer las entradas

**Feature Branch**: `feat/hardening-api`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "Endurecer las operaciones ya entregadas con validaciones de entrada. Un pedido ilegible, con un campo de más, repetido o de otro tipo se rechaza entero y la respuesta no cita el texto enviado ni un detalle interno. La cantidad de acciones va de 1 a 2147483647; un monto que no entra en ese entero no se guarda truncado. El monto y el precio son positivos, con hasta 8 dígitos enteros y 2 decimales half-up. La cuenta es de 1 a 20 dígitos. El ticker es de 1 a 10 letras o dígitos. El identificador de orden es un entero de 1 a 2147483647. La búsqueda se recorta en los extremos, admite hasta 100 caracteres, no admite caracteres de control ni el carácter nulo, y se compara literal sin depender del idioma de la máquina. El mensaje nombra el campo, es estable y no repite el valor enviado. No hay sesiones, límite de intentos ni clave de reintento. El rechazo por fondos o acciones insuficientes se sigue grabando. La cuenta 10001, la búsqueda PAMP y una orden válida siguen respondiendo como antes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rechazar un pedido que no se puede interpretar (Priority: P1)

Una persona, o un cliente automático, envía un pedido roto: no es un documento de datos, está vacío, trae un tipo distinto del esperado, repite un campo o incluye un dato que la operación no pide. La operación lo rechaza, no graba nada y no devuelve el texto recibido ni un diagnóstico interno.

**Why this priority**: Un pedido ilegible o con campos de más es la puerta de entrada de datos basura. Si se procesa, puede grabar una orden distinta de la intención o mostrar un detalle interno.

**Independent Test**: Enviar un cuerpo que no se puede leer, un cuerpo vacío, un número donde va un texto, un texto donde va una cantidad, el mismo campo dos veces y un campo que la operación no define. En todos los casos no aparece una orden nueva y la respuesta no cita el cuerpo enviado.

**Acceptance Scenarios**:

1. **Given** un cuerpo que no es un documento de datos válido, **When** se envía una orden, **Then** no se graba nada y la respuesta informa un pedido ilegible, sin el cuerpo recibido y sin un detalle interno.
2. **Given** un cuerpo vacío, un documento que no es un objeto, o un envío que no es un documento de datos, **When** se envía, **Then** se rechaza igual que un pedido ilegible.
3. **Given** un campo con un tipo distinto del esperado, como una cantidad en texto no numérico o un ticker que no es texto, **When** se envía, **Then** se nombra el campo, no se graba nada y no se reinterpreta el valor.
4. **Given** un campo que la operación no define, **When** se envía junto con un pedido por lo demás válido, **Then** se rechaza el pedido completo y no se ignora el campo de más.
5. **Given** el mismo campo repetido con dos valores, **When** se envía, **Then** se rechaza y no se elige uno de los dos.

---

### User Story 2 - Rechazar números que no entran en el modelo (Priority: P2)

Una persona envía una cantidad, un monto, un precio o un identificador negativo, cero cuando se exige un positivo, o tan grande que no se puede guardar entero. La operación rechaza el pedido completo. No conserva la parte que sí entra ni lo convierte en otro número.

**Why this priority**: Un número que desborda o que cambia de signo puede reservar o ejecutar una cantidad distinta de la pedida. Eso mueve dinero o acciones de más.

**Independent Test**: Enviar una cantidad `-1`, una cantidad `0`, una cantidad mayor que `2147483647`, un monto o un precio negativo, un monto o un precio con 9 dígitos enteros, y un identificador de orden `0` o negativo. Ninguno graba una orden ni cambia el portfolio. La cantidad `2147483647`, si el resto del pedido es válido y la cuenta lo cubre, no se rechaza por tamaño.

**Acceptance Scenarios**:

1. **Given** una cantidad, un monto o un precio negativo, **When** se envía una orden, **Then** no se graba y la respuesta nombra ese campo.
2. **Given** una cantidad cero, o un precio cero o ausente en una orden con precio límite, **When** se envía, **Then** no se graba.
3. **Given** una cantidad mayor que `2147483647`, o un monto o precio con más de 8 dígitos enteros, **When** se envía, **Then** no se graba una cifra más chica ni un número de otro signo.
4. **Given** un monto que, al dividirlo por el precio aplicable, da más acciones que `2147483647`, **When** se envía, **Then** no se graba ninguna orden, ni siquiera una rechazada por negocio, con una cantidad menor o de otro signo.
5. **Given** un identificador de orden que no es un entero de `1` a `2147483647`, **When** se cancela, **Then** no se opera sobre otra orden.
6. **Given** una posición corta ya existente, **When** se consulta el portfolio, **Then** la cantidad negativa informada se conserva. Esta feature no prohíbe ese resultado.

---

### User Story 3 - Tratar el texto como dato y no como instrucción (Priority: P3)

Una persona envía, en la cuenta, el ticker o la búsqueda, un texto que parece una instrucción para leer, cambiar o borrar datos. Ese texto no se ejecuta. Si no cumple el formato del campo, se rechaza. Si es un texto de búsqueda permitido, se compara literalmente y, como máximo, no devuelve filas. En ningún caso cambia datos que el pedido válido no cambiaría, ni la respuesta muestra un error interno.

**Why this priority**: La cuenta, el ticker y la búsqueda llegan de quien llama. Sin un formato cerrado, un texto hostil puede alterar la consulta o filtrar un error interno.

**Independent Test**: Pedir el portfolio de `10001' OR '1'='1`, crear una orden con ticker `PAMP'; DROP TABLE orders;--` y buscar `pamp' OR 1=1`. La cuenta y el ticker se rechazan. La búsqueda no modifica datos y no devuelve un error interno. El portfolio de `10001` y la búsqueda `PAMP` siguen respondiendo como antes.

**Acceptance Scenarios**:

1. **Given** un número de cuenta con letras, espacios, signos o más de 20 dígitos, **When** se usa en portfolio, alta o cancelación, **Then** se rechaza y no se elige otra cuenta.
2. **Given** un ticker vacío, con espacios, con signos o con más de 10 letras o dígitos, **When** se envía una orden, **Then** no se graba nada.
3. **Given** un texto de búsqueda vacío, con más de 100 caracteres, con caracteres de control o con un carácter nulo, **When** se busca, **Then** no hay listado y no se muestra un error interno.
4. **Given** un texto de búsqueda de hasta 100 caracteres, formado por letras, dígitos, espacios y la puntuación habitual de un nombre, **When** se busca, **Then** se compara como texto literal, sin distinguir mayúsculas y sin depender del idioma de la máquina. Un texto que parece una instrucción no cambia datos y no amplía el resultado más allá de las coincidencias literales.
5. **Given** la cuenta `10001` y el ticker `PAMP`, **When** se consultan con su forma válida, **Then** el resultado es el de la feature anterior.

---

### Edge Cases

- Recortar espacios no convierte un identificador en otro. Una cuenta o un ticker con espacios al inicio o al final se rechaza.
- Un texto de búsqueda sí puede recortarse en los extremos. Después se le aplican el largo máximo de 100 caracteres y la prohibición de caracteres de control y del carácter nulo.
- Más de 2 decimales en un monto o un precio no es basura: se redondea half-up a 2 decimales, igual que el resto de los importes. Esta feature no lo rechaza por la cantidad de decimales.
- `BUY` y `SELL` son los únicos lados de una orden nueva. `MARKET` y `LIMIT` son los únicos tipos. Cualquier otro texto, incluidas variantes de mayúsculas o espacios, se rechaza. Un ingreso o un egreso de pesos nuevo sigue sin grabar una orden.
- Una orden nueva sigue exigiendo exactamente una forma de cantidad: acciones o monto. Las dos, o ninguna, se rechazan.
- Cuando el error corresponde a un campo, la respuesta nombra ese campo, usa el mismo texto para el mismo caso y no incluye el valor enviado. Una cuenta, un activo o una orden que no existen se informan sin repetir el valor enviado.
- Una compra o una venta que no entra por fondos o acciones insuficientes se sigue guardando rechazada. Esta feature no convierte ese caso en un rechazo de entrada.
- Un pedido válido de la cuenta de referencia no empieza a fallar por estas reglas.
- Quien conoce el número de cuenta opera. Esta feature no agrega sesiones, permisos ni un límite de intentos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Todo dato de entrada de una operación MUST validarse antes de leer o grabar el estado de una cuenta. Un dato inválido MUST NOT grabar una orden, MUST NOT cambiar saldos y MUST NOT elegir otro registro parecido.
- **FR-002**: Un cuerpo ilegible, vacío, que no sea un documento de datos, o que no sea un objeto MUST rechazarse. La respuesta MUST NOT incluir el cuerpo recibido ni un detalle interno.
- **FR-003**: Un campo con un tipo distinto del esperado, un campo repetido o un campo que la operación no define MUST rechazar el pedido completo. El sistema MUST NOT ignorar el campo de más, MUST NOT elegir uno de los valores repetidos y MUST NOT convertir un texto en número ni un número en texto.
- **FR-004**: Cuando el error corresponde a un campo, la respuesta MUST nombrar ese campo. El texto MUST ser estable para el mismo caso y MUST NOT incluir el valor enviado ni un diagnóstico interno. Una cuenta, un activo o una orden inexistentes MUST informarse sin repetir el valor enviado.
- **FR-005**: Una cantidad de acciones MUST ser un entero de `1` a `2147483647`. Un monto o un precio MUST ser mayor que cero y MUST tener como máximo 8 dígitos enteros. Los decimales de más MUST redondearse half-up a 2. Un valor negativo, cero cuando el campo es obligatorio, no numérico o con más de 8 dígitos enteros MUST rechazarse entero, sin guardar una parte del número ni cambiarle el signo.
- **FR-006**: Si un monto, dividido por el precio aplicable, no cabe en un entero de `1` a `2147483647`, el pedido MUST rechazarse sin grabar una orden, incluida una orden rechazada por negocio.
- **FR-007**: Un identificador de orden MUST ser un entero de `1` a `2147483647`. Cero, negativo, texto u otro número fuera de ese rango MUST rechazarse sin operar sobre una orden distinta.
- **FR-008**: Un número de cuenta MUST ser de 1 a 20 dígitos, sin otros caracteres. Un ticker de orden MUST ser de 1 a 10 letras o dígitos, sin espacios ni signos. Un valor fuera de ese formato MUST rechazarse. Los espacios al inicio o al final de la cuenta o del ticker MUST rechazarse y MUST NOT recortarse.
- **FR-009**: Un texto de búsqueda MUST recortarse solo en los extremos, MUST tener de 1 a 100 caracteres después de ese recorte y MUST NOT contener caracteres de control ni el carácter nulo. El texto aceptado MUST compararse de forma literal, sin distinguir mayúsculas y sin depender del idioma de la máquina, y MUST NOT ejecutarse como instrucción. Una búsqueda inválida o sin coincidencias MUST NOT modificar datos ni devolver un error interno.
- **FR-010**: Un lado distinto de compra o venta, o un tipo distinto de mercado o límite, MUST rechazarse. La operación MUST seguir exigiendo exactamente una forma entre cantidad de acciones y monto. Un ingreso o un egreso de pesos nuevo MUST seguir sin grabar una orden.
- **FR-011**: Una consulta o una orden que ya era válida MUST seguir siéndolo. La cantidad negativa de una posición corta informada por el portfolio MUST conservarse. Una orden que no entra por fondos o acciones insuficientes MUST seguir guardándose rechazada.

### Key Entities

- **Dato de entrada**: Cada valor que llega en una operación: cuenta, identificador de orden, texto de búsqueda, ticker, lado, tipo, cantidad, monto y precio. Tiene un formato cerrado o un texto acotado. Fuera de ese formato no entra al estado de la cuenta.
- **Rechazo de entrada**: La respuesta de un dato que no se puede aceptar. Nombra el campo cuando hay uno, no repite el valor enviado y no cambia órdenes ni saldos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los pedidos ilegibles, con tipo distinto, con un campo de más o con un campo repetido no crean una orden, y la respuesta no contiene el cuerpo enviado ni un detalle interno.
- **SC-002**: El 100% de las cantidades, montos, precios e identificadores negativos, en cero cuando no corresponden, o por encima del máximo no dejan una orden nueva ni cambian el portfolio. Ningún monto que no cabe en el entero de acciones queda guardado con una cantidad menor.
- **SC-003**: El 100% de los textos de cuenta, ticker y búsqueda que parecen una instrucción no modifican datos y no producen un error interno. La cuenta y el ticker fuera de formato se rechazan. La búsqueda permitida solo devuelve coincidencias literales.
- **SC-004**: El portfolio de `10001`, la búsqueda `PAMP` y una orden válida de esa cuenta siguen completándose como antes de esta feature.

## Assumptions

- Esta feature se apoya en el comportamiento ya entregado de portfolio, búsqueda y órdenes. No cambia la escala de los importes: siguen en 2 decimales half-up, y el tope nuevo de 8 dígitos enteros es el máximo que ya se puede registrar para un precio.
- El tope de `2147483647` acciones es el máximo entero que ya se puede registrar como cantidad. Esta feature lo aplica en el borde y prohíbe guardar una parte del número.
- Los límites de 20 dígitos para la cuenta y de 10 caracteres para el ticker son los que ya identifican a las cuentas y a los activos de referencia. No se amplían en esta feature.
- El largo de 100 caracteres de la búsqueda es un tope nuevo de esta feature. El texto puede incluir letras, dígitos, espacios y la puntuación de un nombre, porque los nombres de referencia la usan. No se le aplica el formato cerrado del ticker.
- Las sesiones, los permisos, el límite de intentos y la clave de reintento quedan fuera de alcance.
- La forma de atar cada texto como dato y de imponer estos formatos se elige en la planificación. Esa elección no cambia las reglas observables de esta spec.
