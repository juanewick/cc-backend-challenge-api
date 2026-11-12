# Research: Portfolio, búsqueda y órdenes

## Spring Boot 4.1 y Java 25

**Decision**: Java 25 y Spring Boot 4.1.2, con Maven y el parent `spring-boot-starter-parent`.

**Rationale**: El pedido fija Java 25 (LTS) y Spring Boot 4.1.x. La línea 4.1 publicada al 2026-11-12 es 4.1.2 (22 de octubre de 2026). Pide Java 17 como mínimo y es compatible con Java 25. El starter web de esta generación es `spring-boot-starter-webmvc`; el de pruebas de MVC es `spring-boot-starter-webmvc-test`.

**Alternatives considered**: Spring Boot 4.1.0 y 4.1.1 quedan atrás en parches. Spring Boot 4.2 todavía está en release candidate. Gradle también sirve; Maven es el default del parent y alcanza para un solo módulo.

## JUnit que trae Spring Boot 4.1

**Decision**: Usar JUnit Jupiter 6.0.3 y Mockito 5.23.0, las versiones que administra `spring-boot-starter-test` 4.1.2. Los tests de servicios usan `@ExtendWith(MockitoExtension.class)` y mocks de los puertos. Los de cálculo puro no necesitan mocks.

**Rationale**: El pedido nombra JUnit 5. Spring Boot 4.1 ya no gestiona Jupiter 5.x: el starter de test trae `junit-jupiter` 6.0.3, que es la línea siguiente de la misma API (`@Test`, `@BeforeEach`, aserciones y `MockitoExtension`). Fijar Jupiter 5 chocaría con el BOM y con el soporte de test de Boot 4.1.

**Alternatives considered**: Bajar el proyecto a Spring Boot 3.5 para conservar el artefacto JUnit 5. Eso incumple Spring Boot 4.1.x.

## Acceso a datos

**Decision**: Spring Data JPA solo como adaptador de salida. El esquema no lo genera Hibernate (`ddl-auto: none`). El SQL entregado se carga tal cual. El cálculo de portfolio, reservas y cantidad de acciones vive en un servicio de dominio puro, no en consultas dispersas.

**Rationale**: El pedido permite Spring Data o SQL directo. El esquema ya existe, es chico y hay que respetarlo. JPA mapea las cuatro tablas y el dominio sigue testeable sin base. Las columnas sin comillas de PostgreSQL quedan en minúsculas (`accountnumber`, `instrumentid`, `previousclose`); el mapeo usa esos nombres reales.

**Alternatives considered**: `JdbcTemplate` o jOOQ para todo. Sirven, pero JPA cubre el CRUD de órdenes y usuarios con menos código, y el cálculo igual queda fuera del ORM. Un esquema generado por Hibernate cambiaría el modelo entregado.

## PostgreSQL en Docker

**Decision**: Imagen oficial fijada en `postgres:18.6`. El parche 18.7 salió el 2026-11-12, pero ese tag todavía no estaba en Docker Hub. No se usa `latest`.

**Rationale**: El 2026-11-12 salió PostgreSQL 18.7, con los parches de seguridad de esa fecha. La imagen `latest` vista el mismo día todavía apuntaba a 18.6. El pedido pide la última estable parcheada, no un tag flotante.

**Alternatives considered**: PostgreSQL 17.x está parcheado, pero no es la última estable. Dejar `postgres:18` también flota entre patches.

## Aplicación standalone

**Decision**: `compose.yaml` levanta dos servicios: `postgres` y `app`. La app se construye con un Dockerfile de dos etapas (`eclipse-temurin:25-jdk` para compilar, `eclipse-temurin:25-jre` para correr). El SQL de referencia entra por `docker-entrypoint-initdb.d`. La app espera el healthcheck de Postgres y publica el puerto 8080.

**Rationale**: El pedido pide una app Dockerizada que arranque sola, con la base y los datos, sin instalar Java ni PostgreSQL en el host.

**Alternatives considered**: Solo la base en Docker y la app en el host. No cumple el arranque único. Una imagen única con Postgres y Java adentro mezcla dos procesos y empeora el ciclo de vida.

## LocalStack y Testcontainers

**Decision**: Las clases `*IT.java` usan Testcontainers. El contenedor de PostgreSQL es la base real de los escenarios felices y de error. El mismo harness levanta LocalStack con el módulo Testcontainers y comprueba que responde; ninguna operación de negocio llama a AWS.

**Rationale**: El pedido nombra las dos herramientas. LocalStack emula AWS, y esta aplicación no tiene S3, colas ni ningún otro servicio de AWS. Usarlo como base sería incorrecto. Dejarlo fuera incumpliría el pedido escrito. Arrancarlo y verificarlo cumple el requisito sin inventar una dependencia de producto.

**Alternatives considered**: Omitir LocalStack. Guardar órdenes en S3 solo para tener algo que testear contra LocalStack.

## Identidad y contrato HTTP

**Decision**: La cuenta se identifica por `accountNumber` en la ruta. El activo, por `ticker`. La orden guardada, por su id. No hay autenticación ni Spring Security.

**Rationale**: Así quedó definido en la especificación. El ticker es lo que devuelve la búsqueda. El id de orden existe en el modelo y hace única la cancelación.

**Alternatives considered**: Identificar al usuario por el id numérico interno. Filtra peor el modelo público, porque el número de cuenta ya es el identificador de negocio.

## Concurrencia del disponible

**Decision**: Crear o cancelar una orden corre en una transacción que bloquea la fila del usuario antes de calcular el disponible.

**Rationale**: Dos compras simultáneas no pueden usar dos veces el mismo efectivo. El volumen de referencia es de cuatro cuentas, así que un bloqueo por cuenta alcanza.

**Alternatives considered**: Bloqueo optimista sin fila de usuario. Es más fácil de perder un caso de carrera en el test de integración.
