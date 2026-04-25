# Plan de Implementación: Actualizar Estado de Pedidos en Entrega (Módulo Logístico)

**Fecha**: 24 de abril de 2026
**Spec**: `spec-actualizar-estados-pedidos.md`

## Resumen

Implementar el proceso de registro del estado final de entrega de un pedido por parte del transportista, con asignación automática de tasa de efectividad y publicación asíncrona al módulo financiero. El sistema persiste el estado en PostgreSQL y publica el evento `{ id_pedido, estado_final, tasa_efectividad, id_transportista }` para que el módulo financiero genere las liquidaciones correspondientes. Stack: Java 21 + Spring Boot MVC + Spring Data JPA + PostgreSQL + mensajería asíncrona, como microservicio independiente del módulo de logística.

---

## Contexto Técnico

**Lenguaje/Versión**: Java 21
**Dependencias principales**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Almacenamiento**: PostgreSQL
**Mensajería asíncrona**: A definir según infraestructura disponible (RabbitMQ / Kafka / HTTP event queue)
**Pruebas**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Plataforma objetivo**: Servidor backend (REST API) — microservicio de logística independiente
**Tipo de proyecto**: Aplicación Web (Backend) — stack bloqueante (no reactivo)
**Arquitectura**: Hexagonal (Puertos y Adaptadores)
**Objetivos de rendimiento**: Persistir estado y publicar evento en <500ms (P95)
**Restricciones**: El módulo financiero es el consumidor del evento — la estructura del payload es un contrato fijo definido en `recibir_estado_final_modulo_transporte.md` y no puede cambiarse unilateralmente. La `tasa_efectividad` es asignada por el sistema, no por el transportista.
**Escala/Alcance**: Volumen estimado de cientos de actualizaciones de estado por día; sin requisito inmediato de escalado horizontal.

---

## Estructura del Proyecto

### Documentación (esta funcionalidad)

```text
docs/specs/update-order-status/
├── plan.md      # Este archivo
└── spec.md      # Especificación de la funcionalidad
```

### Código Fuente (raíz del repositorio)

**NOTA IMPORTANTE — Arquitectura Hexagonal Limpia:**
- **domain/**: Contiene ÚNICAMENTE lógica de negocio pura, SIN dependencias de frameworks. Incluye modelos, objetos de valor, puertos (in/out) y excepciones.
- **application/**: Contiene ÚNICAMENTE servicios que coordinan casos de uso. Sin DTOs, sin mappers, sin referencias a infraestructura. Los servicios reciben y retornan objetos de dominio.
- **infrastructure/**: Contiene TODOS los adaptadores, DTOs, controladores, mappers y persistencia.
  - Los DTOs son conceptos de presentación/API y pertenecen exclusivamente a la infraestructura.
  - El mapper (MapStruct) vive en infraestructura: es un detalle de implementación del adaptador web.
  - El controlador es el único punto donde ocurren las traducciones DTOs ↔ objetos de dominio, siempre delegando al mapper.
  - El publicador de eventos es un adaptador de salida en infraestructura.

```text
src/main/java/co/edu/unimagdalena/storelogistic/orderstatus/
├── domain/
│   ├── models/
│   │   ├── Order.java           # idPedido, idCliente, idTransportista, estadoFinal, tasaEfectividad, fechaCreacion, fechaActualizacion
│   │   ├── Carrier.java         # idTransportista
│   │   └── Alert.java           # idAlerta, idPedido, idTransportista, estadoFinalRegistrado, tipo, descripcion, estado, asignadoASupervisor
│   ├── values/
│   │   ├── FinalStatus.java     # enum: ENTREGADO_COMPLETO, RECHAZO_PARCIAL, NO_ENTREGADO, DEVOLUCION_ERROR_EMPRESA, FALTANTE_INVENTARIO — método effectivenessRate()
│   │   └── EffectivenessRate.java  # objeto de valor — rango -100 a 100, inmutable, derivado de FinalStatus
│   ├── ports/
│   │   ├── in/
│   │   │   └── UpdateOrderStatusUseCase.java    # Order update(Long orderId, Long carrierId, FinalStatus status)
│   │   └── out/
│   │       ├── OrderRepository.java             # findById, save
│   │       ├── CarrierRepository.java           # findById
│   │       ├── AlertRepository.java             # save
│   │       └── OrderStatusEventPublisher.java   # publish(OrderStatusEvent event)
│   └── exceptions/
│       ├── LogisticsException.java
│       ├── OrderNotFoundException.java
│       ├── CarrierNotFoundException.java
│       └── InvalidFinalStatusException.java
│
├── application/
│   └── services/
│       └── UpdateOrderStatusService.java   # implementa UpdateOrderStatusUseCase — orquesta persistencia + alerta + publicación de evento
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java
    ├── persistence/
    │   ├── jpa/
    │   │   ├── OrderJpaEntity.java
    │   │   ├── CarrierJpaEntity.java
    │   │   └── AlertJpaEntity.java
    │   ├── repository/
    │   │   ├── OrderRepositoryAdapter.java
    │   │   ├── CarrierRepositoryAdapter.java
    │   │   └── AlertRepositoryAdapter.java
    │   └── jparepository/
    │       ├── OrderSpringRepository.java
    │       ├── CarrierSpringRepository.java
    │       └── AlertSpringRepository.java
    ├── messaging/
    │   ├── OrderStatusEventPublisherAdapter.java   # adaptador de salida — publica el evento asíncrono al módulo financiero
    │   └── dto/
    │       └── OrderStatusEventDto.java            # { id_pedido, estado_final, tasa_efectividad, id_transportista } — contrato fijo con módulo financiero
    ├── web/
    │   ├── controller/
    │   │   └── UpdateOrderStatusController.java
    │   └── dto/
    │       ├── UpdateOrderStatusRequest.java   # idPedido, idTransportista, estadoFinal (String)
    │       └── UpdateOrderStatusResponse.java  # idPedido, estadoFinal, tasaEfectividad, idTransportista, fechaActualizacion
    ├── mapper/
    │   └── OrderStatusMapper.java              # MapStruct @Component — Order ↔ DTOs y entidades JPA
    └── exception/
        ├── GlobalExceptionHandler.java
        └── ErrorResponse.java
```

```text
src/test/java/co/edu/unimagdalena/storelogistic/orderstatus/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── OrderTest.java
│   │   │   ├── CarrierTest.java
│   │   │   └── AlertTest.java
│   │   └── values/
│   │       ├── FinalStatusTest.java
│   │       └── EffectivenessRateTest.java
│   ├── application/
│   │   └── UpdateOrderStatusServiceTest.java
│   └── infrastructure/
│       ├── OrderStatusMapperTest.java
│       ├── OrderRepositoryAdapterTest.java
│       └── UpdateOrderStatusControllerTest.java
├── integration/
│   ├── OrderRepositoryIntegrationTest.java
│   ├── AlertRepositoryIntegrationTest.java
│   └── UpdateOrderStatusServiceIntegrationTest.java
├── contract/
│   └── UpdateOrderStatusApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── OrderFixture.java
    │   ├── CarrierFixture.java
    │   └── AlertFixture.java
    ├── builders/
    │   ├── UpdateOrderStatusRequestBuilder.java
    │   └── OrderBuilder.java
    └── containers/
        └── PostgreSQLContainer.java
```

---

## Fase 1: Configuración (Infraestructura Compartida)

**Propósito**: Configurar el proyecto Gradle, dependencias y base de datos antes de cualquier implementación de lógica de negocio.

- [ ] T001 Crear la estructura de directorios siguiendo el layout definido en este plan.
- [ ] T002 Configurar `build.gradle.kts` — incluir dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`, dependencia del broker de mensajería según infraestructura disponible. Verificar que no estén ya presentes desde módulos anteriores.
- [ ] T003 Configurar `application.yml` — conexión PostgreSQL, perfiles `dev`, `test`, `prod`, configuración del broker de mensajería asíncrona.
- [ ] T004 Crear scripts de migración Flyway:
  - `V1__create_order_table.sql` — (`id_pedido`, `id_cliente`, `id_transportista`, `estado_final`, `tasa_efectividad`, `fecha_creacion`, `fecha_actualizacion`).
  - `V2__create_carrier_table.sql` — (`id_transportista`).
  - `V3__create_alert_table.sql` — (`id_alerta`, `id_pedido` FK, `id_transportista` FK, `estado_final_registrado`, `tipo`, `descripcion`, `estado`, `asignado_a_supervisor`).
  - `V4__create_order_status_indexes.sql` — índices en `order.id_transportista`, `order.estado_final`, `alert.id_pedido`.
- [ ] T005 Configurar `ArchUnit` — reglas de capas: `domain/` sin imports de Spring/JPA/web, `application/` sin imports de `infrastructure/`. Agregar regla: ningún componente de `application/` referencia DTOs de `infrastructure/`.
- [ ] T006 Verificar o crear `PostgreSQLContainer.java` — reutilizar configuración de TestContainers existente.
- [ ] T007 Configurar Jacoco — objetivo ≥80% global, 100% capa de dominio.

**Checkpoint**: El proyecto compila, las migraciones corren sin errores, el contenedor de prueba inicia correctamente.

---

## Fase 2: Fundacional (Prerrequisitos Bloqueantes)

**Propósito**: Crear todos los componentes base de dominio e infraestructura antes de implementar cualquier escenario. Aún no hay lógica de negocio — solo la fundación compartida.

**⚠️ CRÍTICO**: Ningún escenario puede comenzar hasta que esta fase esté completa.

- [ ] T008 Crear `FinalStatus.java` — enum con los cinco estados válidos: `ENTREGADO_COMPLETO`, `RECHAZO_PARCIAL`, `NO_ENTREGADO`, `DEVOLUCION_ERROR_EMPRESA`, `FALTANTE_INVENTARIO`. Cada valor almacena su `tasa_efectividad` predefinida. Método `effectivenessRate(): EffectivenessRate` — retorna el objeto de valor correspondiente. Método `displayName(): String` — retorna la cadena de texto del estado usada en el evento asíncrono (e.g., `"Entregado Completo"`). Esta es la única fuente de verdad para la equivalencia estado → tasa.
- [ ] T009 Crear `EffectivenessRate.java` — objeto de valor, rango válido -100 a 100, inmutable, sin anotaciones de framework. Constructor privado; instanciado solo desde `FinalStatus.effectivenessRate()`.
- [ ] T010 Crear `Carrier.java` — modelo de dominio: `carrierId (Long)`. Sin anotaciones JPA.
- [ ] T011 Crear `Alert.java` — modelo de dominio: `alertId`, `orderId`, `carrierId`, `registeredStatus (FinalStatus)`, `type`, `description`, `status`, `assignedToSupervisor`. Método de fábrica `static Alert createFor(Order order): Alert` — determina tipo y descripción según el estado final.
- [ ] T012 Crear `Order.java` — modelo de dominio: `orderId (Long)`, `clientId (Long)`, `carrierId (Long)`, `finalStatus (FinalStatus)`, `effectivenessRate (EffectivenessRate)`, `createdAt`, `updatedAt`. Métodos de negocio:
  - `updateStatus(FinalStatus newStatus): void` — actualiza `finalStatus`, recalcula `effectivenessRate` via `newStatus.effectivenessRate()`, actualiza `updatedAt`. Esta es la única fuente de verdad para la actualización de estado.
  - `requiresAlert(): boolean` — retorna `true` si `finalStatus` es `NO_ENTREGADO`, `RECHAZO_PARCIAL`, `FALTANTE_INVENTARIO`, o `DEVOLUCION_ERROR_EMPRESA` (FR-006).
  - `static Order create(Long orderId, Long clientId, Long carrierId): Order` — método de fábrica, sin estado final asignado.
- [ ] T013 Crear `LogisticsException.java`, `OrderNotFoundException.java`, `CarrierNotFoundException.java`, `InvalidFinalStatusException.java`.
- [ ] T014 Crear puerto de entrada `UpdateOrderStatusUseCase.java` en `domain/ports/in/` — firma: `Order update(Long orderId, Long carrierId, FinalStatus status)`. Recibe y retorna exclusivamente objetos de dominio.
- [ ] T015 Crear puertos de salida en `domain/ports/out/`:
  - `OrderRepository.java` — `Optional<Order> findById(Long orderId)`, `Order save(Order order)`.
  - `CarrierRepository.java` — `Optional<Carrier> findById(Long carrierId)`.
  - `AlertRepository.java` — `Alert save(Alert alert)`.
  - `OrderStatusEventPublisher.java` — `void publish(Long orderId, FinalStatus status, Long carrierId)`. Puerto de salida asíncrono hacia el módulo financiero.
- [ ] T016 Crear entidades JPA en `infrastructure/persistence/jpa/`:
  - `OrderJpaEntity.java`.
  - `CarrierJpaEntity.java`.
  - `AlertJpaEntity.java`.
- [ ] T017 Crear interfaces Spring Data JPA: `OrderSpringRepository`, `CarrierSpringRepository`, `AlertSpringRepository`.
- [ ] T018 Crear `OrderStatusMapper.java` en `infrastructure/mapper/` como MapStruct `@Component` — conversiones: `Order → UpdateOrderStatusResponse`, `OrderJpaEntity ↔ Order`, `AlertJpaEntity ↔ Alert`, `CarrierJpaEntity ↔ Carrier`.
- [ ] T019 Crear `OrderStatusEventDto.java` en `infrastructure/messaging/dto/` — campos: `id_pedido (Long)`, `tasa_efectividad (Integer)`, `id_transportista (Long)`. Este DTO representa el contrato fijo con el módulo financiero. `estado_final` fue excluido por acuerdo de integración — el módulo financiero solo requiere la tasa para sus liquidaciones. No agregar campos adicionales.
- [ ] T020 Crear `GlobalExceptionHandler.java` — agregar:
  - `OrderNotFoundException` → HTTP 404.
  - `CarrierNotFoundException` → HTTP 404.
  - `InvalidFinalStatusException` → HTTP 422 Unprocessable Entity.
  - Violaciones de `@Valid` → HTTP 400.
- [ ] T021 Crear fixtures base: `OrderFixture.java`, `CarrierFixture.java`, `AlertFixture.java`.
- [ ] T022 Pruebas unitarias para `FinalStatus.effectivenessRate()` — verificar la equivalencia correcta para cada estado: `ENTREGADO_COMPLETO → 100`, `RECHAZO_PARCIAL → 80`, `NO_ENTREGADO → 0`, `DEVOLUCION_ERROR_EMPRESA → 0`, `FALTANTE_INVENTARIO → -100`.
- [ ] T023 Pruebas unitarias para `Order.updateStatus()` — camino feliz actualiza `finalStatus` y `effectivenessRate`; `updatedAt` es posterior a `createdAt` tras la actualización.
- [ ] T024 Pruebas unitarias para `Order.requiresAlert()` — retorna `true` para los cuatro estados problemáticos; retorna `false` para `ENTREGADO_COMPLETO`.

**Checkpoint**: Modelos de dominio, objetos de valor, puertos, entidades JPA y manejo de excepciones listos. T022–T024 pasan. El proyecto compila sin errores. Aún no hay endpoint funcional.

---

## Fase 3: Escenario 1 — Registrar estado final de un pedido sin estado previo (P1)

**Objetivo**: Cuando un transportista registra el estado final de un pedido que no tiene estado previo, el sistema persiste los datos, asigna automáticamente la tasa de efectividad, genera alerta si aplica, y publica el evento asíncrono al módulo financiero (FR-001, FR-002, FR-003, FR-004, FR-006).

**Prueba Independiente**: Dado un pedido sin `estado_final` registrado, `PUT /api/logistics/orders/{id}/status` con `{ id_transportista, estado_final }` válido retorna HTTP 200 con los cuatro campos del evento (`id_pedido`, `estado_final`, `tasa_efectividad`, `id_transportista`) y el evento es publicado al módulo financiero con la misma estructura.

### Pruebas para el Escenario 1

- [ ] T025 [P] [SC1] Prueba de contrato en `UpdateOrderStatusApiContractTest` — `PUT /api/logistics/orders/{id}/status` con `estado_final: "Entregado Completo"` → HTTP 200, cuerpo contiene `id_pedido`, `estado_final`, `tasa_efectividad: 100`, `id_transportista` (FR-002, SC-002).
- [ ] T026 [P] [SC1] Prueba de contrato en `UpdateOrderStatusApiContractTest` — verificar los cinco estados válidos: para cada `estado_final` la respuesta contiene la `tasa_efectividad` correcta según la tabla de FR-002 (SC-003).
- [ ] T027 [P] [SC1] Prueba de integración en `OrderRepositoryIntegrationTest` — `save` persiste `finalStatus` y `effectivenessRate` correctamente en PostgreSQL real; `findById` los recupera.
- [ ] T028 [P] [SC1] Prueba de integración en `UpdateOrderStatusServiceIntegrationTest` — camino feliz completo: el pedido queda persistido con el estado correcto y `OrderStatusEventPublisher.publish` es invocado exactamente una vez con los parámetros `(id_pedido, finalStatus, id_transportista)`.

### Implementación del Escenario 1

- [ ] T029 [SC1] Crear `OrderRepositoryAdapter.java` implementando `OrderRepository`: `findById`, `save`.
- [ ] T030 [SC1] Crear `CarrierRepositoryAdapter.java` implementando `CarrierRepository`: `findById`.
- [ ] T031 [SC1] Crear `AlertRepositoryAdapter.java` implementando `AlertRepository`: `save`.
- [ ] T032 [SC1] Crear `OrderStatusEventPublisherAdapter.java` implementando `OrderStatusEventPublisher`:
  - Inyecta `StreamBridge` (Spring Cloud Stream) — misma tecnología que `RouteEventPublisher`.
  - Construye el `OrderStatusEventDto` con exactamente tres campos: `id_pedido`, `tasa_efectividad` (usando `FinalStatus.effectivenessRate().value()`), `id_transportista`.
  - Envía via `streamBridge.send("publicarEstadoPedido-out-0", event)` — binding declarado en `application.yml` apuntando a la cola `estado-pedido.event`.
  - Esta es la única clase que conoce los detalles del broker de mensajería.
- [ ] T033 [SC1] Implementar `UpdateOrderStatusService.java` implementando `UpdateOrderStatusUseCase`:
  - `@Transactional`.
  - Recibe `Long orderId`, `Long carrierId`, `FinalStatus status`.
  - Verifica existencia del transportista vía `CarrierRepository.findById` — lanza `CarrierNotFoundException` si no existe.
  - Obtiene el pedido vía `OrderRepository.findById` — lanza `OrderNotFoundException` si no existe.
  - Llama a `order.updateStatus(status)` (la lógica de negocio vive en el modelo).
  - Persiste vía `OrderRepository.save`.
  - Si `order.requiresAlert()` — crea la alerta con `Alert.createFor(order)` y persiste vía `AlertRepository.save`.
  - Llama a `OrderStatusEventPublisher.publish(orderId, status, carrierId)`.
  - Retorna `Order` (objeto de dominio). Sin conocimiento de DTOs.
- [ ] T034 [SC1] Crear `UpdateOrderStatusController.java` con `PUT /api/logistics/orders/{id}/status`:
  - Extrae `id_transportista` y `estado_final` de `UpdateOrderStatusRequest`.
  - Convierte `estado_final` (String) a `FinalStatus` — lanza `InvalidFinalStatusException` si el valor no es válido.
  - Llama a `UpdateOrderStatusUseCase.update(orderId, carrierId, status)`.
  - Mapea `Order → UpdateOrderStatusResponse` usando `OrderStatusMapper`.
  - Retorna HTTP 200.
- [ ] T035 [SC1] Crear `UpdateOrderStatusRequest.java` en `infrastructure/web/dto/` — `@NotNull Long idTransportista`, `@NotBlank String estadoFinal`.
- [ ] T036 [SC1] Crear `UpdateOrderStatusResponse.java` en `infrastructure/web/dto/` — `Long idPedido`, `String estadoFinal`, `Integer tasaEfectividad`, `Long idTransportista`, `LocalDateTime fechaActualizacion`.
- [ ] T037 [SC1] Pruebas unitarias para `UpdateOrderStatusService` — camino feliz:
  - `OrderRepository`, `CarrierRepository`, `AlertRepository`, `OrderStatusEventPublisher` mockeados.
  - Verificar que `order.updateStatus()` es llamado.
  - Verificar que `OrderRepository.save` es invocado exactamente una vez.
  - Verificar que `OrderStatusEventPublisher.publish` es invocado exactamente una vez con los parámetros correctos.
  - Para estado `ENTREGADO_COMPLETO`: verificar que `AlertRepository.save` NO es llamado.
  - Para estado `NO_ENTREGADO`: verificar que `AlertRepository.save` SÍ es llamado.
  - `OrderNotFoundException` cuando el pedido no existe.
  - `CarrierNotFoundException` cuando el transportista no existe.
- [ ] T038 [SC1] Pruebas unitarias para `UpdateOrderStatusController` con MockMvc:
  - PUT con cuerpo válido → HTTP 200, cuerpo contiene `tasaEfectividad` correcto.
  - PUT con `estado_final` inválido → HTTP 422.
  - PUT con `idTransportista` nulo → HTTP 400.

**Checkpoint**: `PUT /api/logistics/orders/{id}/status` funciona de extremo a extremo para pedidos sin estado previo. T025–T028 pasan con TestContainers. El evento publicado tiene la estructura exacta definida en `recibir_estado_final_modulo_transporte.md`.

---

## Fase 4: Escenario 2 — Corregir estado final registrado previamente (P1)

**Objetivo**: Cuando un transportista intenta actualizar el estado de un pedido que ya tiene `estado_final` registrado, el sistema permite la corrección, genera auditoría completa (valor anterior, valor nuevo, ejecutor, timestamp) y re-publica el evento corregido al módulo financiero (FR-005, SC-005).

**Prueba Independiente**: Dado un pedido con `estado_final: "No Entregado"` ya registrado, un segundo `PUT /api/logistics/orders/{id}/status` con `estado_final: "Entregado Completo"` retorna HTTP 200 con el estado corregido, y existe un registro de auditoría que documenta el cambio de `"No Entregado"` a `"Entregado Completo"`.

### Pruebas para el Escenario 2

- [ ] T039 [P] [SC2] Prueba de contrato en `UpdateOrderStatusApiContractTest` — segundo PUT sobre el mismo pedido → HTTP 200, cuerpo contiene el nuevo `estado_final` y la nueva `tasa_efectividad` (FR-005, SC-005).
- [ ] T040 [P] [SC2] Prueba de integración en `UpdateOrderStatusServiceIntegrationTest` — corrección de estado: verificar que el pedido queda persistido con el nuevo estado, que existe un registro de auditoría con `valorAnterior`, `valorNuevo`, `idTransportista`, y `timestamp`, y que `OrderStatusEventPublisher.publish` es invocado una segunda vez.

### Implementación del Escenario 2

- [ ] T041 [SC2] Agregar entidad y tabla de auditoría:
  - `V5__create_order_status_audit_table.sql` — (`id_auditoria`, `id_pedido` FK, `estado_anterior`, `estado_nuevo`, `id_transportista`, `timestamp`).
  - `OrderStatusAuditJpaEntity.java`, `OrderStatusAuditSpringRepository.java`.
  - Puerto de salida `OrderStatusAuditRepository.java` en `domain/ports/out/` — `save(audit)`.
  - `OrderStatusAuditRepositoryAdapter.java`.
- [ ] T042 [SC2] Extender `UpdateOrderStatusService.update()` — rama de corrección (el pedido ya tiene `estado_final`):
  - Guardar `estadoAnterior` antes de llamar a `order.updateStatus(newStatus)`.
  - Tras persistir el nuevo estado, persistir el registro de auditoría con `(orderId, estadoAnterior, nuevoEstado, carrierId, LocalDateTime.now())`.
  - Re-publicar el evento corregido al módulo financiero vía `OrderStatusEventPublisher.publish`.
- [ ] T043 [SC2] Pruebas unitarias para `UpdateOrderStatusService` — rama de corrección:
  - Verificar que `OrderStatusAuditRepository.save` es invocado cuando el pedido ya tiene estado registrado.
  - Verificar que `OrderStatusEventPublisher.publish` es invocado en ambos casos (primer registro y corrección).
  - Verificar que el registro de auditoría contiene `estadoAnterior` correcto.

**Checkpoint**: La corrección de estado funciona de extremo a extremo. T039–T040 pasan con TestContainers. El registro de auditoría es verificable en la base de datos.

---

## Fase 5: Casos Borde

**Propósito**: Cubrir los casos borde explícitos definidos en la spec.

- [ ] T044 [EC] `estado_final` con valor inválido (no pertenece al enum) → `InvalidFinalStatusException` → HTTP 422 con mensaje descriptivo. Prueba de contrato: PUT con `estado_final: "EstadoInventado"` → HTTP 422.
- [ ] T045 [EC] Pedido inexistente → `OrderNotFoundException` → HTTP 404. Prueba de contrato: PUT con `id_pedido` que no existe en BD → HTTP 404.
- [ ] T046 [EC] Transportista inexistente → `CarrierNotFoundException` → HTTP 404. Prueba de contrato: PUT con `id_transportista` que no existe → HTTP 404.
- [ ] T047 [EC] Fallo del publicador de eventos — `OrderStatusEventPublisher.publish` lanza excepción: verificar que la transacción de base de datos NO se revierte (el estado ya fue persistido) pero el error es registrado con `ERROR` en logging para reintento posterior. Prueba de integración con publicador mockeado que falla.
- [ ] T048 [EC] Actualizaciones concurrentes del mismo pedido — dos hilos enviando `PUT` simultáneamente al mismo `id_pedido`: verificar que solo un estado final queda registrado y la auditoría refleja el último cambio sin corrupción.

**Checkpoint**: Todos los casos borde de la spec cubiertos con pruebas automatizadas.

---

## Fase 6: Pulido y Aspectos Transversales

**Propósito**: Hardening, observabilidad, validaciones y documentación.

- [ ] T049 Logging estratégico con `@Slf4j` en `UpdateOrderStatusService`:
  - `INFO` al inicio de cada actualización (`orderId`, `carrierId`, `estado_final` recibido).
  - `INFO` en persistencia exitosa (`orderId`, `estado_final`, `tasa_efectividad`).
  - `INFO` al publicar evento al módulo financiero.
  - `INFO` cuando se genera una Alerta (`orderId`, tipo de alerta).
  - `INFO` cuando se registra corrección de estado (anterior → nuevo).
  - `WARN` cuando el publicador de eventos falla (con contexto completo para reintento).
  - `ERROR` en excepciones inesperadas con contexto completo.
- [ ] T050 Bean Validation en `UpdateOrderStatusRequest` — `@NotNull Long idTransportista`, `@NotBlank String estadoFinal`. Verificar HTTP 400 con campos nulos.
- [ ] T051 Documentar API con `springdoc-openapi` — endpoint `PUT /api/logistics/orders/{id}/status`, esquemas de petición/respuesta, todos los códigos HTTP posibles: 200, 400, 404, 422. Documentar la estructura del evento publicado al módulo financiero.
- [ ] T052 `@ArchTest` con ArchUnit — verificar:
  - `domain/` sin imports de Spring, JPA o web.
  - `application/` puede importar domain, no infrastructure.
  - `infrastructure/` puede importar todo.
  - `OrderStatusEventDto` existe solo en `infrastructure/messaging/dto/` y no es referenciado desde `domain/` ni `application/`.
- [ ] T053 Cobertura de código con Jacoco — verificar ≥80% global, 100% capa de dominio.
- [ ] T054 README con comandos de compilación/ejecución, ejecución de pruebas (unitarias / integración / todas), ejemplo curl para el endpoint, descripción del evento publicado al módulo financiero.
- [ ] T055 Checklist pre-despliegue: 100% pruebas pasando, ArchUnit pasando, cobertura ≥80%, estructura del `OrderStatusEventDto` validada contra `recibir_estado_final_modulo_transporte.md`.

**Checkpoint**: Código listo para producción. SC-001 a SC-005 verificables con pruebas automatizadas. El contrato con el módulo financiero está documentado y validado.

---

## Dependencias y Orden de Ejecución

### Dependencias entre Fases

- **Configuración (Fase 1)**: Sin dependencias — puede comenzar de inmediato.
- **Fundacional (Fase 2)**: Depende de la Fase 1 — **bloquea todos los escenarios**.
- **Escenario 1 (Fase 3)**: Depende de la Fase 2. Implementa el flujo principal completo.
- **Escenario 2 (Fase 4)**: Depende de la Fase 3. Extiende el mismo servicio con la rama de corrección y auditoría.
- **Casos Borde (Fase 5)**: Depende de que los dos escenarios estén completos.
- **Pulido (Fase 6)**: Depende de la Fase 5.

### Dentro de Cada Fase

- Objetos de valor y modelos de dominio → Puertos (interfaces) → Adaptadores de repositorio/mensajería → Servicio de aplicación → Controlador.
- Pruebas de contrato e integración (`[P]`) antes de la implementación correspondiente.
- Pruebas unitarias en línea con cada componente.
- Checkpoint al final antes de pasar a la siguiente fase.

---

## Notas

- **DTOs exclusivamente en infraestructura**: `UpdateOrderStatusRequest`, `UpdateOrderStatusResponse`, y `OrderStatusEventDto` viven en `infrastructure/`. Nunca en `application/` ni en `domain/`.
- **Mapper exclusivamente en infraestructura**: `OrderStatusMapper` vive en `infrastructure/mapper/`. El controlador es el único invocador.
- **El servicio opera únicamente con objetos de dominio**: `UpdateOrderStatusService` recibe `Long orderId`, `Long carrierId`, `FinalStatus status` y retorna `Order`. No tiene conocimiento de `UpdateOrderStatusResponse` ni de `OrderStatusEventDto`.
- **`FinalStatus.effectivenessRate()` es la única fuente de verdad para la equivalencia estado → tasa** (FR-002). Ni el servicio ni el controlador duplican esta lógica.
- **`Order.updateStatus()` es la única fuente de verdad para la actualización de estado** (FR-003). El servicio orquesta; el modelo decide.
- **`Order.requiresAlert()` es la única fuente de verdad para la generación de alertas** (FR-006).
- **El contrato con el módulo financiero es fijo**: `OrderStatusEventDto` tiene exactamente tres campos (`id_pedido`, `tasa_efectividad`, `id_transportista`). `estado_final` fue excluido por acuerdo de integración. Cualquier cambio en esta estructura requiere coordinación con el equipo del módulo financiero y actualización de `recibir_estado_final_modulo_transporte.md`.
- **La publicación del evento es posterior a la persistencia**: el `OrderRepository.save` siempre ocurre antes de `OrderStatusEventPublisher.publish`. Si el publicador falla, el estado en BD no se revierte; el error se loguea para reintento.
- La etiqueta `[P]` marca las pruebas a escribir antes de la implementación (test-first).
- Las etiquetas `[SC1]`, `[SC2]`, `[EC]` mapean directamente a los escenarios de la spec para trazabilidad.
- Hacer commit después de cada tarea completada con pruebas en verde.
- Evitar tareas que modifiquen el mismo archivo desde diferentes fases en paralelo.
