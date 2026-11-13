brokerage-api
=============

API HTTP de una cuenta de inversión en pesos. Se consulta el portfolio, se buscan activos por ticker o nombre, y se envían o cancelan órdenes de compra y venta. No hay autenticación. El conjunto de cuentas, precios y órdenes de referencia se carga con la base.

Funcionalidades
---------------
- Consultar el portfolio: valor total, pesos disponibles y posiciones (cantidad, valor de mercado, rendimiento total y diario).
- Buscar activos cuyo ticker o nombre contienen un texto, sin distinguir mayúsculas.
- Enviar órdenes MARKET (se ejecutan al cierre vigente) o LIMIT (quedan abiertas y reservan pesos o acciones).
- Guardar como REJECTED la orden que no entra por fondos o acciones insuficientes.
- Cancelar una orden abierta NEW y devolver la reserva.

Arranque local con Docker
-------------------------
Hace falta Docker Desktop (o Docker Engine con Compose) en la máquina. No hace falta instalar Java ni PostgreSQL en el host.

Desde esta carpeta, cc-backend-challenge:

  docker compose up --build

Quedan la aplicación en http://localhost:8080 y PostgreSQL con los datos de referencia. El banner al arrancar es brokerage-api.

Ejemplos:

  curl -s http://localhost:8080/api/brokerage-api/v1/accounts/10001/portfolio
  curl -s "http://localhost:8080/api/brokerage-api/v1/instruments?q=banco"

Hay una colección de ejemplo en postman/brokerage.postman_collection.json.

Para saber si el proceso y la base responden:

  curl -s http://localhost:8080/actuator/health
  curl -s http://localhost:8080/actuator/info

Health devuelve {"status":"UP"} y nada más. Info identifica la aplicación como brokerage-api. El resto de Actuator no está publicado.

El contrato de la API lo genera la aplicación. No hay una copia en specs:

  http://localhost:8080/v3/api-docs
  http://localhost:8080/v3/api-docs.yaml
  http://localhost:8080/swagger-ui.html

Para bajarla:

  docker compose down

Los tests en el host (opcional) piden JDK 25 y Maven: mvn verify.

Decisiones de planificación
---------------------------
Java 25, Spring Boot 4.1.2 y JUnit 6. Los tests usan JUnit Jupiter 6.0.3, la versión que trae ese Spring Boot, junto con Mockito.
PostgreSQL se fija en la imagen postgres:18.6. El 2026-11-12 salió el parche 18.7, pero ese tag todavía no estaba en Docker Hub.
La integración usa Testcontainers contra PostgreSQL para los escenarios de negocio. LocalStack se arranca en esa misma suite porque el pedido lo pide; la aplicación no llama a AWS.
Los *IT.java comparten un PostgreSQL y un LocalStack arrancados una sola vez. Un contenedor por clase cambiaba el puerto y el contexto cacheado de Spring seguía apuntando al anterior.
El Dockerfile de compilación usa maven:3.9.11-eclipse-temurin-25 para no instalar Maven aparte en la etapa de build.
Antes de cada escenario de integración se recrean las órdenes de referencia con TRUNCATE, para que un test no deje saldo a otro.
El banner y el nombre de la aplicación son brokerage-api.
La ruta base es /api/brokerage-api. Cada controller versiona con /v1, para poder agregar /v2 más adelante sin mover la base. Ejemplos:
  GET  /api/brokerage-api/v1/accounts/{accountNumber}/portfolio
  GET  /api/brokerage-api/v1/instruments
  POST /api/brokerage-api/v1/accounts/{accountNumber}/orders
  POST /api/brokerage-api/v1/accounts/{accountNumber}/orders/{orderId}/cancellation
Las rutas sin esa base, o con la base pero sin /v1, no se atienden.
Actuator publica solo health e info, en /actuator. Health no muestra componentes. Info repite el nombre brokerage-api.
El contrato OpenAPI lo genera springdoc-openapi 3.2.0 (springdoc-openapi-starter-webmvc-ui). Esa línea está construida sobre Spring Boot 4.1.2. La 3.0.x es Boot 4.0 y la 2.x es Boot 3. No se guarda una copia del contrato en specs: se lee en /v3/api-docs, /v3/api-docs.yaml y /swagger-ui.html.

Decisiones de la especificación
================================

Pregunta
--------
¿Cómo se calcula el rendimiento total de cada posición?

Opciones
--------
- **A: (Valor de mercado − capital neto de compras y ventas ejecutadas) / valor absoluto de ese capital**  <-- seleccionada
- B: (Último cierre − costo promedio de las acciones que siguen en cartera) / costo promedio
- C: (Cierre − cierre anterior) / cierre anterior

Respuesta
---------
A. El rendimiento total es la ganancia o pérdida acumulada de las órdenes ejecutadas, dividida por el valor absoluto del capital neto invertido. No es el rendimiento diario.

Pregunta
--------
¿Las órdenes que siguen abiertas reservan el dinero o las acciones disponibles?

Opciones
--------
- **A: Las compras abiertas reservan pesos y las ventas abiertas reservan acciones**  <-- seleccionada
- B: Solo las órdenes ejecutadas cambian el disponible; las abiertas no reservan
- C: Las compras abiertas reservan pesos; las ventas abiertas no reservan acciones

Respuesta
---------
A. Las compras en estado NEW reservan pesos (cantidad por precio) y las ventas en estado NEW reservan acciones. El disponible de la cuenta 10001 sigue siendo 627500.00.

Pregunta
--------
¿Se pueden registrar ingresos y egresos de pesos nuevos, o el portfolio solo usa los que ya vienen cargados?

Opciones
--------
- A: Se pueden registrar ingresos y egresos nuevos, y quedan guardados como órdenes
- **B: No se registran nuevos; el portfolio solo usa los ingresos y egresos ya cargados**  <-- seleccionada
- C: Se aceptan por la misma operación de compra o venta, indicando ingreso o egreso de pesos

Respuesta
---------
B. El portfolio incorpora los ingresos y egresos ya cargados. No se aceptan ingresos ni egresos nuevos, y un intento no graba una orden.

Pregunta
--------
¿Qué activos devuelve una búsqueda?

Opciones
--------
- **A: Los activos cuyo ticker o nombre contienen el texto, sin distinguir mayúsculas**  <-- seleccionada
- B: Solo los activos cuyo ticker o nombre empiezan por ese texto
- C: También los parecidos aunque el texto no coincida letra por letra

Respuesta
---------
A. La búsqueda devuelve, sin duplicados, los activos cuyo ticker o nombre contienen el texto, sin distinguir mayúsculas. No corrige errores de tipeo.

Pregunta
--------
¿El rendimiento diario se muestra además del rendimiento total?

Opciones
--------
- **A: Se muestra en cada posición y en cada activo encontrado, además del rendimiento total de la posición**  <-- seleccionada
- B: Se muestra solo en la búsqueda; cada posición informa únicamente el rendimiento total
- C: No se muestra; el único rendimiento informado es el total

Respuesta
---------
A. Cada posición muestra el rendimiento total y el rendimiento diario. Cada activo encontrado muestra el rendimiento diario.

Requisito de ubicación
----------------------
El código Java se genera en esta carpeta, cc-backend-challenge.
Los specs viven en esta misma carpeta, junto al código.

Requisito de arranque
---------------------
La aplicación se levanta standalone con Docker, junto con la base de datos y los datos de referencia.
Un único arranque alcanza para consultar el portfolio, buscar activos y enviar órdenes, sin instalar la plataforma ni la base por separado.

MEJORAS
=======

Estas mejoras tocan el modelo entregado por Cocos. No se hicieron: no queríamos tocarlo para no salirnos de alcance. Podemos tener una charla técnica para ver estos puntos.

- Cantidad de la orden y de la posición. La columna orders.size es INT. La entidad de la orden y la cantidad acumulada de cada posición usan ese mismo entero, con tope 2147483647. Varias órdenes grandes pueden desbordar la suma y la tenencia deja de coincidir con las órdenes guardadas. Corregirlo cambia el tipo en la tabla y en las entidades, por un entero más ancho.

- Fecha y hora de la orden. La columna orders.datetime es TIMESTAMP sin zona horaria. La entidad la representa como fecha local y la guarda con la zona del proceso. El mismo instante puede verse distinto según dónde corre el servicio, y la respuesta no trae offset. Corregirlo cambia el tipo en la tabla y en la entidad, por un instante con zona horaria.

- Idempotencia del alta. No la implementamos: no queremos tocar el modelo base. Si no fuera así, agregaríamos una columna para guardar el idempotency-key y una caché para dar alta disponibilidad.
