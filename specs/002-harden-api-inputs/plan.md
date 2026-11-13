# Implementation Plan: Endurecer las entradas

**Branch**: `feat/hardening-api` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-harden-api-inputs/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Las operaciones ya entregadas rechazan, antes de leer o grabar una cuenta, un pedido ilegible, un campo de más, uno repetido o de otro tipo, y los números o textos que no entran en el modelo. El cuerpo de error sigue siendo `{ "message": "..." }`. El texto nombra el campo cuando es uno de los nuestros, es estable y no repite el valor enviado ni el diagnóstico del lector de documentos. Jackson 3 queda estricto para la forma del documento. El formato de cuenta, ticker, orden, búsqueda, cantidad, monto y precio se resuelve en el dominio, antes de los repositorios. Un monto que no cabe en el entero de acciones no se guarda, ni siquiera como orden rechazada por negocio.

## Technical Context

**Language/Version**: Java 25, el mismo runtime de la feature 001.

**Primary Dependencies**: Spring Boot 4.1.2 y Jackson 3.1.7 (`tools.jackson`). El mapper se ajusta con propiedades `spring.jackson.*` y un `JsonMapperBuilderCustomizer` de `spring-boot-jackson` para las coerciones, que no tienen propiedad. El cuerpo de error sigue en `ApiError`. No se agrega una librería nueva. `spring-boot-starter-validation` ya está en el módulo y no hace falta para estas reglas: no cubren claves repetidas, campos de más ni el desborde de un entero.

**Storage**: Sin cambios de esquema. La cuenta sigue en 20 caracteres, el ticker en 10, la cantidad y el id de orden en entero, y el precio en `NUMERIC(10, 2)`. Esos topes son los de [data-model.md](data-model.md).

**Testing**: JUnit Jupiter y Mockito, como en la feature 001. Primero `MoneyMath` y los casos de uso (`*Test.java`). Después `*IT.java` con el PostgreSQL de Testcontainers para el pedido HTTP: documento roto, campo de más, campo repetido, tipo distinto, formatos y la regresión de `10001`, `PAMP` y una orden válida. `mvn verify`.

**Target Platform**: El mismo servicio HTTP en contenedor, puerto 8080. Sin endpoints nuevos.

**Project Type**: Servicio HTTP, un solo módulo Maven, arquitectura hexagonal ya existente.

**Performance Goals**: La validación ocurre en la misma petición, antes de consultar la cuenta. No agrega una ida extra a la base. La búsqueda sigue leyendo el catálogo de referencia una vez, después de aceptar el texto.

**Constraints**: Sin sesiones, sin límite de intentos y sin clave de reintento. Sin copia escrita del OpenAPI. El rechazo por fondos o acciones insuficientes, y la cantidad cero que sale de un monto chico, se siguen grabando. Una posición corta puede seguir mostrando cantidad negativa. Los decimales de más se redondean half-up a 2 y no se rechazan por la cantidad de decimales.

**Scale/Scope**: Las cuatro operaciones públicas ya existentes: portfolio, búsqueda, alta de orden y cancelación. El catálogo de referencia no cambia.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` sigue siendo la plantilla sin principios ratificados. No hay gates de proyecto que evaluar. El gate pasa.

Las restricciones que sí obligan son las de la feature 001 y las de esta spec: Java 25, Spring Boot 4.1.2, hexagonal, el esquema entregado, el contrato generado por springdoc, el cuerpo `{ "message": "..." }`, y validar antes de leer o grabar.

Revisión posterior al diseño: [research.md](research.md), [data-model.md](data-model.md), [contracts/entradas.md](contracts/entradas.md) y [quickstart.md](quickstart.md) no agregan tablas, endpoints ni un contrato escrito a mano. El gate sigue en pasa.

## Project Structure

### Documentation (this feature)

```text
specs/002-harden-api-inputs/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── entradas.md
└── tasks.md              # lo crea /speckit-tasks, no este comando
```

### Source Code (repository root)

```text
cc-backend-challenge/
├── src/main/java/com/cc/brokerage/
│   ├── domain/model/MoneyMath.java          # escala, dígitos enteros y cantidad sin truncar
│   ├── domain/model/InputText.java          # cuenta, ticker, orden y búsqueda
│   ├── application/PlaceOrderService.java   # rechaza el desborde antes de grabar
│   ├── application/SearchInstrumentsService.java
│   ├── adapter/in/web/ApiExceptionHandler.java
│   ├── adapter/in/web/MoneyFormats.java
│   ├── adapter/in/web/*Controller.java      # la ruta llega como texto y se interpreta acá
│   └── config/JsonMapperConfig.java         # coerciones que las propiedades no cubren
├── src/main/resources/application.yml       # propiedades de lectura estricta
└── src/test/java/com/cc/brokerage/
    ├── domain/model/MoneyMathTest.java
    ├── domain/model/InputTextTest.java
    ├── application/*ServiceTest.java
    └── adapter/in/web/HardenInputsIT.java
```

**Structure Decision**: Se queda el módulo único y los paquetes hexagonales de la feature 001. El formato es dominio, para testearlo sin HTTP. El adaptador web solo traduce el documento y las rutas, y no deja salir el mensaje del lector. No hay adaptador de persistencia nuevo.

## Complexity Tracking

Sin violaciones. Esta feature no agrega un servicio, una base ni un contrato paralelo.
