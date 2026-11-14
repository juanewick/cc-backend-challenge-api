# brokerage-api

API HTTP de una cuenta de inversión en pesos: consulta del portfolio, búsqueda de activos y envío o cancelación de órdenes de compra y venta. No tiene autenticación. Usa una base PostgreSQL en Neon que ya tiene las cuentas, precios y órdenes de referencia.

## Funcionalidades

- Portfolio: valor total, pesos disponibles y posiciones (cantidad, valor de mercado, rendimiento total y diario). `unpricedTickers` avisa qué posiciones no tienen cierre y quedaron fuera del valor total.
- Búsqueda paginada de activos cuyo ticker o nombre contiene un texto, sin distinguir mayúsculas.
- Órdenes `MARKET` (se ejecutan al último cierre) y `LIMIT` (quedan abiertas y reservan pesos o acciones).
- Una orden sin fondos o acciones suficientes se graba como `REJECTED`.
- Cancelación de una orden abierta (`NEW`), que libera la reserva.
- Listado paginado de las órdenes de una cuenta, con filtro opcional por estado.

## Arranque

Hace falta Docker con Compose. Desde esta carpeta:

```bash
docker compose up --build
```

La API queda en http://localhost:8080. Al arrancar, la consola muestra el banner `Brokerage api` con la versión de la aplicación y de Spring Boot. Para bajarla: `docker compose down`.

> **Credenciales versionadas a propósito.** `compose.yaml` trae la URL, el usuario y la contraseña de Neon para que quien baje el repositorio pueda levantar la API sin configurar nada. En un ambiente productivo no lo haría: las credenciales vendrían de un gestor de secretos o de variables inyectadas por la plataforma, y las rotaría si quedaran expuestas.

Ejemplos:

```bash
curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=banco"
curl -s "http://localhost:8080/api/brokerage-api/v1/accounts/10001/orders?status=NEW"
```

Hay una colección en [`postman/brokerage.postman_collection.json`](postman/brokerage.postman_collection.json).

Los tests (`mvn verify`) piden JDK 25, Maven y Docker. Corren contra un PostgreSQL de Testcontainers cargado con `src/test/resources/db/database.sql` y nunca tocan Neon.

## Configuración

Todo se pasa por variables de entorno. Fuera de Compose, la aplicación arranca solo si recibe las tres de la base.

| Variable | Por defecto | Uso |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | sin valor | Conexión a PostgreSQL (URL en formato JDBC). |
| `BROKERAGE_MONEY_SCALE`, `BROKERAGE_MONEY_MIN_AMOUNT`, `BROKERAGE_MONEY_MAX_AMOUNT` | `2`, `0`, `99999999.99` | Decimales y rango de montos y precios. |
| `BROKERAGE_MONEY_MIN_SHARES`, `BROKERAGE_MONEY_MAX_SHARES` | `1`, `2147483647` | Rango de cantidad de acciones por orden. |
| `BROKERAGE_INSTRUMENT_SEARCH_MAX_PAGE`, `BROKERAGE_INSTRUMENT_SEARCH_MAX_PAGE_SIZE` | `50`, `100` | Página más profunda y tamaño máximo de la búsqueda. |
| `BROKERAGE_ORDER_LIST_MAX_PAGE`, `BROKERAGE_ORDER_LIST_MAX_PAGE_SIZE` | `1000`, `100` | Lo mismo para el listado de órdenes. |

## Endpoints de estado y documentación

Están fuera de `/api/brokerage-api` y no piden autenticación.

| Endpoint | Respuesta |
|---|---|
| `GET /actuator/health` | `200` con `{"groups":["liveness","readiness"],"status":"UP"}`; `503` con `DOWN` si la base no responde. |
| `GET /actuator/health/liveness`, `GET /actuator/health/readiness` | Sondas para un orquestador: `200` con `{"status":"UP"}`. |
| `GET /actuator/info` | `200` con `{"app":{"name":"brokerage-api"}}`. |
| `GET /v3/api-docs`, `GET /v3/api-docs.yaml` | Contrato OpenAPI 3.1 en JSON o YAML. |
| `GET /swagger-ui.html` | Swagger UI, para probar la API desde el navegador. |

Health no muestra componentes: sin autenticación, ese detalle quedaría abierto. El resto de Actuator no está publicado. El contrato lo genera springdoc a partir de los controllers, así que no hay una copia a mano que se desactualice.

## Arquitectura

Arquitectura hexagonal: el dominio no conoce HTTP ni JPA. Los casos de uso entran por puertos, la persistencia sale por otros y los adapters traducen.

| Paquete (`com.cc.brokerage`) | Contenido |
|---|---|
| `domain` | Modelo, reglas y errores. `port.in` son los casos de uso, `port.out` los repositorios y `service` calcula portfolio y cotizaciones. Cada error tiene una categoría (entrada inválida, inexistente, conflicto), no un status HTTP. |
| `application` | Implementación de los casos de uso. |
| `adapter.in.web` | Controllers, pedidos, respuestas y traducción de cada categoría de error a su status HTTP. |
| `adapter.out.persistence` | Un adapter por agregado, entidades JPA y consultas que agregan en la base. |
| `config` | Beans de Spring y propiedades de `application.yml`. |

Stack: Java 25, Spring Boot 4.1.2, springdoc-openapi 3.2.0, JUnit 6, Mockito y Testcontainers.

## Decisiones de diseño

- **Validación en un solo lugar.** El controller solo lee el JSON; las reglas de una orden nueva están en `PlaceOrderService`. No usé Bean Validation porque los límites son configurables y quedarían duplicados en anotaciones.
- **Montos como texto.** Los montos se envían y devuelven como texto (`"800.00"`); también se acepta un número JSON.
- **Precio en una orden `MARKET`.** Se rechaza con `400` y `BRK-027`, sin grabar la orden: quien manda un precio probablemente espera un tope que la orden de mercado no respeta.
- **Orden rechazada.** Una orden sin fondos, acciones o precio se graba como `REJECTED` y responde `201`, porque existe y se puede consultar. Trae `rejectionReason` y el `amount` que habría costado, que no se reserva ni se descuenta.
- **Errores de formato.** Un cuerpo que no es `application/json` responde `415`; uno ilegible, `400`. Si se repite una propiedad, gana el último valor.
- **Concurrencia.** Alta y cancelación bloquean la cuenta (`PESSIMISTIC_WRITE`), así que dos órdenes simultáneas no pueden gastar el mismo saldo.
- **Cálculos en la base.** Una consulta agrupa por instrumento las órdenes `FILLED` y `NEW` (cantidades, capital, reservas y movimientos de pesos), sin cargar el historial. Las cantidades se suman como `BIGINT`. El último precio sale de `DISTINCT ON`: la fecha más reciente y, si empatan, el id más alto.
- **Paginación.** Búsqueda y listado responden `{content, page, pageSize, totalElements, totalPages}`, con 20 por página por defecto. La página más profunda tiene tope porque un `OFFSET` grande obliga a la base a recorrer todas las filas anteriores. El `COUNT` solo corre cuando la página viene llena. La cuenta y el texto de búsqueda se validan antes que los parámetros de página.
- **Búsqueda.** `%` y `_` se toman literalmente. El resultado se ordena por ticker.
- **Valores desconocidos.** Un tipo, lado o estado que el servicio no conoce se lee como `UNKNOWN` y se registra un aviso. Esas órdenes no cuentan en el portfolio y nunca se vuelven a grabar.
- **Dependencias con vulnerabilidades.** El `pom.xml` adelanta versiones corregidas que Spring Boot 4.1.2 todavía no trae: `jackson-databind` 3.1.8 y 2.21.8 (CVE-2026-110879), `logback` 1.6.7 (CVE-2026-19880) y, en tests, `json-smart` 2.6.1 (CVE-2026-113602) y `commons-codec` 1.23.0 (CVE-2026-119078). También excluye JSONAssert, que trae `android-json` (CVE-2026-110974, sin corrección) y ningún test usa. Cada propiedad se puede borrar cuando Spring Boot incluya la corrección.

## Decisiones de la especificación

- **Rendimiento total:** (último cierre − costo promedio) / costo promedio. El costo promedio es lo pagado en las compras ejecutadas dividido por las acciones compradas; en una posición corta uso el precio promedio de las ventas. Primero lo calculé sobre el capital neto, pero lo cambié porque cada venta con ganancia lo inflaba: comprar 10 a 100 y vender 5 a 150 daba 200 % con el cierre en 150, cuando lo que queda gana 50 %.
- **Rendimiento diario:** (cierre − cierre anterior) / cierre anterior. Se muestra en cada posición y en cada activo encontrado.
- **Órdenes abiertas:** las compras `NEW` reservan pesos y las ventas `NEW` reservan acciones.
- **Ingresos y egresos de pesos:** el portfolio usa los ya cargados. No se aceptan nuevos y un intento no graba una orden.
- **Búsqueda:** devuelve los activos cuyo ticker o nombre contiene el texto, sin distinguir mayúsculas y sin corregir errores de tipeo.

## Mejoras pendientes

Estas mejoras tocan el modelo entregado por Cocos. No las hice para no salirme del alcance; las puedo charlar en una revisión técnica.

- **Cantidad de la orden.** `orders.size` es `INT`, con tope 2147483647 por orden. Para órdenes más grandes hay que ensanchar el tipo en la tabla y la entidad.
- **Fecha y hora.** `orders.datetime` es `TIMESTAMP` sin zona horaria, así que el mismo instante puede verse distinto según dónde corre el servicio. Lo correcto es guardar un instante con zona.
- **Idempotencia del alta.** Repetir el pedido crea otra orden. Guardaría un `Idempotency-Key` en la base, con restricción única por cuenta y clave, en la misma transacción que graba la orden. Así sobrevive a un reinicio y funciona con varias instancias; una caché sola no alcanza.
