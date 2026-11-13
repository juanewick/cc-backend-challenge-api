# Research: Endurecer las entradas

## Lectura estricta del documento

**Decision**: Activar en `application.yml` la lectura que Spring Boot 4.1.2 ya expone sobre Jackson 3.1.7:

- `spring.jackson.deserialization.fail-on-unknown-properties: true`
- `spring.jackson.deserialization.fail-on-trailing-tokens: true`
- `spring.jackson.read.strict-duplicate-detection: true`
- `spring.jackson.datatype.enum.fail-on-numbers-for-enums: true`

El customizer estándar de Boot desactiva `FAIL_ON_UNKNOWN_PROPERTIES`. Hay que volver a activarlo. Las claves repetidas salen de `StreamReadFeature.STRICT_DUPLICATE_DETECTION`, en el mapa `spring.jackson.read`.

Las coerciones no tienen propiedad. Un `JsonMapperBuilderCustomizer` (`org.springframework.boot.jackson.autoconfigure`) pone `CoercionAction.Fail` cuando un escalar llega con otra forma: texto donde va un número, número donde va un texto, número donde va un lado o un tipo, y booleano, arreglo u objeto en cualquiera de esos campos. El cuerpo vacío, un arreglo en la raíz y un tipo de contenido que no es un documento de datos siguen el mismo rechazo que un pedido ilegible.

**Rationale**: Bean Validation no ve una clave repetida, un campo desconocido ni un número que no entra en un entero: el lector ya falló o ya eligió un valor. Estas banderas existen en los jars del proyecto (`spring-boot-jackson` 4.1.2 y `jackson-databind` 3.1.7) y no agregan una dependencia.

**Alternatives considered**: Anotaciones de validación sobre `PlaceOrderRequest`. No rechazan el campo de más ni la clave repetida. Leer el cuerpo como árbol y validarlo a mano duplica el lector que ya tiene el adaptador.

## Texto estable, sin el valor enviado

**Decision**: `ApiExceptionHandler` deja de devolver `exception.getMessage()` para los fallos de lectura, de tipo de contenido y de argumento mal formado. El cuerpo sigue siendo `ApiError`, con HTTP 400, salvo el tipo de contenido no soportado, que responde 415 (revisado el 2026-11-13).

- Pedido ilegible, vacío, que no es un objeto, o con tokens de más: `El pedido no se puede leer`.
- Tipo de contenido distinto de `application/json`: 415, código `UNSUPPORTED_MEDIA_TYPE` y encabezado `Accept` con los tipos que la operación lee.
- Campo que la operación no define: `El pedido tiene un campo que no corresponde`. No se nombra esa clave, porque el nombre lo elige quien llama y puede ser el propio texto hostil.
- Campo nuestro repetido o de otro tipo: `El campo cantidad está repetido` o `El campo cantidad no es válido`, y lo mismo para ticker, lado, tipo, monto y precio. El nombre es el del modelo, no el valor.
- Cuenta, activo u orden inexistentes: `La cuenta no existe`, `El activo no existe`, `La orden no existe`. Hoy esos textos concatenan el valor enviado.

Si el lector no indica un campo nuestro, se usa el texto de pedido ilegible. Nunca se copia el mensaje de Jackson: incluye el fragmento leído y la ruta interna.

**Rationale**: La spec pide un texto estable, el nombre del campo cuando es uno de los nuestros, y nada del valor ni del diagnóstico interno. El sobre `{ "message" }` ya es el de la feature 001.

**Alternatives considered**: Problem Details. Cambia el cuerpo que ya consumen los tests y el contrato generado. Devolver el mensaje de Jackson y recortarlo es frágil: el texto del lector no es un contrato.

## Formato antes de la cuenta

**Decision**: Cuenta, ticker, identificador de orden y búsqueda se interpretan en `InputText`, en el dominio, y lanzan `InvalidRequestException` con un texto fijo. Los controladores reciben la ruta y la búsqueda como texto. El id de orden deja de declararse como `Long` para que un número enorme no lo convierta el framework ni se consulte otra orden.

Reglas:

- Cuenta: de 1 a 20 dígitos, el texto completo. No se recorta. `10001` sigue siendo la cuenta de referencia. Los ceros a la izquierda se conservan, porque forman otra cadena.
- Ticker: de 1 a 10 letras o dígitos, el texto completo, sin recorte y sin cambiar mayúsculas. `PAMP` se busca igual que hoy. `pamp` tiene formato válido y, si no hay ese activo, responde que no existe, sin repetir el texto.
- Orden: un entero de 1 a 2147483647. Cero, negativo, texto, espacios y un número más grande se rechazan con `La orden no es válida`. Se aceptan ceros a la izquierda si el valor numérico entra en el rango (`0001` es la orden 1).
- Búsqueda: se recortan solo los extremos. Después, de 1 a 100 caracteres Unicode, sin caracteres de control ISO y sin el carácter nulo. Vacía: `La búsqueda no tiene texto`. Demasiado larga: `La búsqueda supera el máximo permitido`. Con un control: `La búsqueda no es válida`.

La comparación de la búsqueda sigue siendo en memoria, sobre el catálogo ya cargado, con `Locale.ROOT`. El texto no se concatena en una consulta. Así un texto que parece una instrucción no se ejecuta y no depende del idioma de la máquina.

**Rationale**: La spec exige validar antes de leer o grabar, y no elegir un registro parecido. Interpretar el id como `Long` deja pasar valores que el entero de la orden no puede guardar y, en el fallo de conversión, el mensaje por defecto incluye el texto de la ruta.

**Alternatives considered**: Expresiones regulares solo en el controlador. El caso de uso quedaría desprotegido si otro adaptador lo llama. Rechazar la búsqueda con signos: los nombres de referencia usan puntuación, y la spec la permite.

## Monto, precio y cantidad

**Decision**: Monto y precio se leen como texto, no como número suelto, igual que hoy. Si el texto no es un número decimal, el campo no es válido. Si lo es, primero se redondea half-up a 2 decimales y después se exige que sea mayor que cero y que tenga como máximo 8 dígitos enteros. `99999999.994` queda en `99999999.99` y entra. `99999999.995` queda en `100000000.00` y se rechaza por el máximo. Un resultado cero después del redondeo se rechaza por no ser mayor que cero.

`MoneyMath.sharesFromAmount` deja de usar `intValue()`. Calcula el cociente entero y, si es mayor que 2147483647, lanza rechazo de entrada. El caso de uso no graba esa orden. El cociente 0 sigue siendo el rechazo de negocio ya existente: se guarda `REJECTED`. El precio de una orden límite ausente, cero o negativo se sigue rechazando antes de grabar. Un lado distinto de `BUY` o `SELL`, un tipo distinto de `MARKET` o `LIMIT`, y las dos formas de cantidad o ninguna, se siguen rechazando.

Los textos de dominio, sin el valor enviado:

- `La cantidad debe ser un entero de 1 a 2147483647`
- `El monto debe ser mayor que cero`
- `El monto supera el máximo permitido`
- `El precio es obligatorio y debe ser mayor que cero`
- `El precio supera el máximo permitido`
- `Debe indicarse exactamente una forma de cantidad: acciones o monto`
- `El lado debe ser BUY o SELL`
- `El tipo de orden es obligatorio`
- `El tipo debe ser MARKET o LIMIT`

**Rationale**: `NUMERIC(10, 2)` guarda 8 enteros y 2 decimales. Redondear antes de contar los enteros valida el número que se guardaría. `intValue()` se queda con los 32 bits bajos y puede persistir una cantidad chica o negativa; la spec lo prohíbe.

**Alternatives considered**: Rechazar todo monto con más de 2 decimales. La spec dice que esos decimales se redondean. Subir el tope a 12 enteros. La columna actual no los guarda, y esta feature no cambia el esquema.

## Contrato y pruebas

**Decision**: No se escribe un OpenAPI en `specs`. El documento sigue siendo el que genera springdoc al arrancar. [contracts/entradas.md](contracts/entradas.md) fija los textos y los rechazos observables. Los tests de dominio y de servicio salen primero y tienen que fallar antes de la implementación. `HardenInputsIT` cubre el documento HTTP. Los escenarios de `10001`, `PAMP` y una orden válida que ya existen no se aflojan.

**Rationale**: La feature 001 prohíbe una copia escrita a mano del contrato. Los textos de error son parte de lo observable y hoy no están en ese documento generado de forma estable, porque salen de excepciones.

**Alternatives considered**: Anotar el esquema de error para que springdoc publique cada mensaje. Publica nombres de tipos, no el catálogo de frases. No reemplaza los tests.
