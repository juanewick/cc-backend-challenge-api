# brokerage-api

API HTTP de una cuenta de inversión en pesos. Se consulta el portfolio, se buscan activos por ticker o nombre, y se envían o cancelan órdenes de compra y venta. No hay autenticación. El conjunto de cuentas, precios y órdenes de referencia se carga con la base.

## Arquitectura

Se usó arquitectura hexagonal. El dominio no conoce HTTP ni JPA: los casos de uso entran por puertos y la persistencia sale por otros. Los adapters traducen.

| Paquete (`com.cc.brokerage`) | Contenido |
|---|---|
| `domain` | Modelo, reglas y errores. `port.in` son los casos de uso. `port.out` son los repositorios. `service` calcula el portfolio y las cotizaciones. Los errores llevan una categoría (entrada inválida, inexistente, conflicto), no un status HTTP. |
| `application` | Implementa los casos de uso. |
| `adapter.in.web.controller` | Rutas HTTP. |
| `adapter.in.web.api` | Pedidos, respuestas y errores. Traduce cada categoría de error a su status HTTP. |
| `adapter.out.persistence` | Un adapter por agregado (`User`, `Instrument`, `MarketPrice`, `Order`) implementa cada puerto de salida. |
| `adapter.out.persistence.entity` | Tablas JPA. |
| `adapter.out.persistence.repository` | `JpaRepository` y las consultas que agregan en la base. |
| `config` | Beans de Spring. El prefijo HTTP de los `RestController` y los límites de montos salen de `application.yml`. |

## Funcionalidades

- Consultar el portfolio: valor total, pesos disponibles y posiciones (cantidad, valor de mercado, rendimiento total y diario). `unpricedTickers` avisa qué posiciones no tienen cierre y quedaron fuera del valor total.
- Buscar activos cuyo ticker o nombre contienen un texto, sin distinguir mayúsculas.
- Enviar órdenes `MARKET` (se ejecutan al cierre vigente) o `LIMIT` (quedan abiertas y reservan pesos o acciones).
- Rechazar una orden `MARKET` que trae `price` (`BRK-027`, sin grabar la orden).
- Guardar como `REJECTED` la orden que no entra por fondos o acciones insuficientes.
- Cancelar una orden abierta `NEW` y devolver la reserva.
- Listar las órdenes de una cuenta por páginas, opcionalmente filtradas por estado.

## Arranque local con Docker

Hace falta Docker Desktop (o Docker Engine con Compose) en la máquina. No hace falta instalar Java ni PostgreSQL en el host.

Desde esta carpeta, `cc-backend-challenge`:

```bash
docker compose up --build
```

Quedan la aplicación en http://localhost:8080 y PostgreSQL con los datos de referencia. El banner al arrancar es `brokerage-api`.

Ejemplos:

```bash
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=banco"
curl -s "http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders?status=NEW&page=0&pageSize=20"
```

Hay una colección de ejemplo en [`postman/brokerage.postman_collection.json`](postman/brokerage.postman_collection.json).

Para saber si el proceso y la base responden:

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/info
```

Health devuelve `{"status":"UP"}` y nada más. Info identifica la aplicación como `brokerage-api`. El resto de Actuator no está publicado.

El contrato de la API lo genera la aplicación. No hay una copia en `specs`:

- http://localhost:8080/v3/api-docs
- http://localhost:8080/v3/api-docs.yaml
- http://localhost:8080/swagger-ui.html

Para bajarla:

```bash
docker compose down
```

Los tests en el host (opcional) piden JDK 25 y Maven:

```bash
mvn verify
```

## Decisiones de planificación

- Java 25, Spring Boot 4.1.2 y JUnit 6. Los tests usan JUnit Jupiter 6.0.3, la versión que trae ese Spring Boot, junto con Mockito.
- PostgreSQL se fija en la imagen `postgres:18.6`. El 2026-11-12 salió el parche 18.7, pero ese tag todavía no estaba en Docker Hub.
- La integración usa Testcontainers contra PostgreSQL para los escenarios de negocio. LocalStack se arranca en esa misma suite porque el pedido lo pide; la aplicación no llama a AWS.
- Los `*IT.java` comparten un PostgreSQL y un LocalStack arrancados una sola vez. Un contenedor por clase cambiaba el puerto y el contexto cacheado de Spring seguía apuntando al anterior.
- El Dockerfile de compilación usa `maven:3.9.11-eclipse-temurin-25` para no instalar Maven aparte en la etapa de build.
- Antes de cada escenario de integración se recrean las órdenes de referencia con `TRUNCATE`, para que un test no deje saldo a otro.
- Actuator publica solo health e info, en `/actuator`. Health no muestra componentes. Info repite el nombre `brokerage-api`.
- El contrato OpenAPI lo genera springdoc-openapi 3.2.0 (`springdoc-openapi-starter-webmvc-ui`).

## Decisiones de diseño

- **Validación en un solo lugar.** El controller solo lee el JSON. Las reglas de una orden nueva (lado, tipo, cantidad o monto, rangos, precio de una LIMIT) están en `PlaceOrderService`. Se quitó `spring-boot-starter-validation`: los límites de montos son configurables y con anotaciones habría quedado una segunda copia de las mismas reglas.
- **Montos como texto.** `amount` y `price` se leen como `BigDecimal`. Se envían como texto (`"800.00"`), igual que todos los montos que devuelve la API, y también se aceptan como número JSON. Un valor que no es número responde `BRK-020` o `BRK-023`.
- **Precio en una orden `MARKET`.** Se rechaza con `400` y `BRK-027`, y la orden no se graba. Antes se ignoraba y la orden se ejecutaba al cierre. Quien manda un precio en una orden de mercado probablemente espera un tope, y la orden no lo respeta, así que es mejor avisarle que ejecutar a un precio que no pidió.
- **Campos repetidos.** Un `RequestBodyAdvice` recorre los tokens del cuerpo y rechaza una propiedad repetida en el mismo objeto antes de leerlo. No depende del texto de los mensajes de Jackson.
- **`MoneyMath` es un bean.** Se construye con los límites de `application.yml` y se inyecta. No hay estado estático.
- **El portfolio se suma en la base.** Una consulta agrupa por instrumento las órdenes `FILLED` y `NEW` del usuario (cantidad neta, capital neto, compras y ventas ejecutadas para el costo promedio, reservas y movimientos de pesos). El servicio no carga el historial de órdenes, ni para el portfolio ni al validar una orden nueva con la cuenta bloqueada. La suma de cantidades es `BIGINT`, así que no desborda aunque cada orden sea `INT`.
- **Último precio en la base.** `DISTINCT ON (instrumentid)` devuelve una fila por instrumento: la fecha más reciente y, si empatan, el id más alto. Es una consulta de PostgreSQL, la base del challenge.
- **Búsqueda en la base.** Busca el texto dentro del ticker o del nombre, sin distinguir mayúsculas, con `%` y `_` tomados literalmente. Devuelve hasta 50 activos ordenados por ticker.
- **Listado de órdenes por páginas.** `GET /orders` acepta `status`, `page` (desde 0) y `pageSize` (1 a 100, por defecto 20). Responde `{content, page, pageSize, totalElements, totalPages}`, de la orden más antigua a la más nueva. Antes devolvía un arreglo con todas las órdenes; es un cambio de contrato.
- **Valores desconocidos en la base.** Un tipo, lado o estado que el servicio no conoce se lee como `UNKNOWN` y se registra un aviso. Las órdenes con lado o estado `UNKNOWN` no cuentan en el portfolio y nunca se vuelven a grabar.

## Decisiones de la especificación

### ¿Cómo se calcula el rendimiento total de cada posición?

- A: (Valor de mercado − capital neto de compras y ventas ejecutadas) / valor absoluto de ese capital
- **B: (Último cierre − costo promedio de las acciones que siguen en cartera) / costo promedio** ← seleccionada
- C: (Cierre − cierre anterior) / cierre anterior

**Respuesta: B.** El costo promedio es lo pagado en las compras ejecutadas dividido por las acciones compradas. En una posición corta se usa el precio promedio de las ventas ejecutadas, y la ganancia es que el cierre baje. Sin compras (o sin ventas, en una corta) el rendimiento total sale vacío. El promedio usa todas las compras ejecutadas del activo, también las de una posición que se cerró y se volvió a abrir. No es el rendimiento diario.

Primero se eligió A. Se cambió porque el capital neto se achica con cada venta con ganancia: comprar 10 a 100 y vender 5 a 150 daba 200 % con el cierre en 150, cuando las acciones que quedan ganan 50 %. Si las ventas superaban las compras, el capital neto quedaba negativo y el resultado perdía sentido. Con B, una venta parcial no cambia el rendimiento de lo que queda en cartera. Es la cifra que muestra un broker.

### ¿Las órdenes que siguen abiertas reservan el dinero o las acciones disponibles?

- **A: Las compras abiertas reservan pesos y las ventas abiertas reservan acciones** ← seleccionada
- B: Solo las órdenes ejecutadas cambian el disponible; las abiertas no reservan
- C: Las compras abiertas reservan pesos; las ventas abiertas no reservan acciones

**Respuesta: A.** Las compras en estado `NEW` reservan pesos (cantidad por precio) y las ventas en estado `NEW` reservan acciones. El disponible de la cuenta 10001 sigue siendo 627500.00.

### ¿Se pueden registrar ingresos y egresos de pesos nuevos, o el portfolio solo usa los que ya vienen cargados?

- A: Se pueden registrar ingresos y egresos nuevos, y quedan guardados como órdenes
- **B: No se registran nuevos; el portfolio solo usa los ingresos y egresos ya cargados** ← seleccionada
- C: Se aceptan por la misma operación de compra o venta, indicando ingreso o egreso de pesos

**Respuesta: B.** El portfolio incorpora los ingresos y egresos ya cargados. No se aceptan ingresos ni egresos nuevos, y un intento no graba una orden.

### ¿Qué activos devuelve una búsqueda?

- **A: Los activos cuyo ticker o nombre contienen el texto, sin distinguir mayúsculas** ← seleccionada
- B: Solo los activos cuyo ticker o nombre empiezan por ese texto
- C: También los parecidos aunque el texto no coincida letra por letra

**Respuesta: A.** La búsqueda devuelve, sin duplicados, los activos cuyo ticker o nombre contienen el texto, sin distinguir mayúsculas. No corrige errores de tipeo.

### ¿El rendimiento diario se muestra además del rendimiento total?

- **A: Se muestra en cada posición y en cada activo encontrado, además del rendimiento total de la posición** ← seleccionada
- B: Se muestra solo en la búsqueda; cada posición informa únicamente el rendimiento total
- C: No se muestra; el único rendimiento informado es el total

**Respuesta: A.** Cada posición muestra el rendimiento total y el rendimiento diario. Cada activo encontrado muestra el rendimiento diario.

### Requisito de ubicación

El código Java se genera en esta carpeta, `cc-backend-challenge`. Los specs viven en esta misma carpeta, junto al código.

### Requisito de arranque

La aplicación se levanta standalone con Docker, junto con la base de datos y los datos de referencia. Un único arranque alcanza para consultar el portfolio, buscar activos y enviar órdenes, sin instalar la plataforma ni la base por separado.

## Mejoras

Estas mejoras tocan el modelo entregado por Cocos. No se hicieron: no queríamos tocarlo para no salirnos de alcance. Podemos tener una charla técnica para ver estos puntos.

- **Cantidad de la orden.** La columna `orders.size` es `INT`, con tope 2147483647 por orden. La tenencia ya no desborda: se suma en la base como `BIGINT`. Admitir órdenes más grandes cambia el tipo en la tabla y en la entidad, por un entero más ancho.
- **Fecha y hora de la orden.** La columna `orders.datetime` es `TIMESTAMP` sin zona horaria. La entidad la representa como fecha local y la guarda con la zona del proceso. El mismo instante puede verse distinto según dónde corre el servicio, y la respuesta no trae offset. Corregirlo cambia el tipo en la tabla y en la entidad, por un instante con zona horaria.
- **Idempotencia del alta.** No la implementamos: no queremos tocar el modelo base. Repetir el pedido crea otra orden. Si no fuera así, guardaríamos el `Idempotency-Key` en la base, con una restricción única por cuenta y clave, en la misma transacción que graba la orden. Así sobrevive a un reinicio, sirve con varias instancias, y el bloqueo de la cuenta que ya toma el alta evita que dos pedidos con la misma clave graben dos órdenes. Una caché sola no alcanza: se pierde al reiniciar, no se comparte entre instancias y no da alta disponibilidad. A lo sumo serviría para responder más rápido una repetición.
