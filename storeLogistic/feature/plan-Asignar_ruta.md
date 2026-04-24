# Plan de Implementación: Asignar Ruta a Pedido (Módulo 2: Logística)

**Fecha**: 22 de abril de 2026
**Spec**: `@/docs/specs/assign-route/spec.md`

## Resumen

Implementar el proceso automático de asignación de pedidos a rutas de entrega, validando que la carga no supere la capacidad del vehículo y consolidando rutas al 95% de capacidad antes del despacho. El sistema debe asignar a una ruta existente si hay capacidad disponible, crear una nueva ruta si no la hay, y cerrar automáticamente la ruta al alcanzar el umbral del 95%. Stack: Java 21 + Spring Boot MVC + Spring Data JPA + PostgreSQL, como microservicio independiente del módulo de logística.

## Contexto Técnico

**Lenguaje/Versión**: Java 21
**Dependencias principales**: Spring Boot 3.x, Spring Web (MVC), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, Gradle
**Almacenamiento**: PostgreSQL
**Pruebas**: JUnit 5, Mockito, AssertJ, TestContainers, ArchUnit, Jacoco
**Plataforma objetivo**: Servidor backend (REST API) — microservicio de logística independiente
**Tipo de proyecto**: Aplicación Web (Backend) — stack bloqueante (no reactivo)
**Arquitectura**: Hexagonal (Puertos y Adaptadores)
**Objetivos de rendimiento**: Asignar pedido a ruta en <100ms (P95)
**Restricciones**: `@Transactional(isolation = SERIALIZABLE)` en `AssignOrderService` para garantizar la integridad de capacidad bajo peticiones concurrentes (caso borde de la spec)
**Escala/Alcance**: Proceso interno; volumen estimado de cientos de pedidos/día por ruta; sin requisito inmediato de escalado horizontal

---

## Estructura del Proyecto

### Documentación (esta funcionalidad)

```text
docs/specs/assign-route/
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

```text
src/main/java/co/edu/unimagdalena/storelogistic/route/
├── domain/
│   ├── models/
│   │   ├── Route.java          # idRoute, idVehicle, totalCapacity, accumulatedWeight, status, dispatchDate, stops
│   │   ├── Stop.java           # idStop, idRoute, idOrder, sequence, deliveryAddress, status, deliveryDate
│   │   ├── Vehicle.java        # idVehicle, type, loadCapacity, status, idCarrier — reutilizar de gestión de flota
│   │   └── Order.java          # idOrder, totalWeight, deliveryAddress
│   ├── values/
│   │   ├── TotalWeight.java    # objeto de valor — valida > 0, inmutable
│   │   ├── LoadCapacity.java   # objeto de valor — valida > 0, inmutable — reutilizar de gestión de flota
│   │   ├── VehicleType.java    # URBAN_VAN (≤1.5t) | SINGLE_TRUCK (≤5t) | REGIONAL_SEMI (>25t)
│   │   ├── RouteStatus.java    # AVAILABLE | CLOSED | PENDING_VEHICLE
│   │   └── StopStatus.java     # PENDING | DELIVERED | REJECTED
│   ├── ports/
│   │   ├── in/
│   │   │   └── AssignOrderUseCase.java     # Route assign(Long orderId)
│   │   └── out/
│   │       ├── RouteRepository.java        # findAvailableWithCapacity, save, findById
│   │       ├── StopRepository.java         # save, findByRouteId
│   │       ├── VehicleRepository.java      # findAvailableByCapacity, findMaxCapacity — reutilizar de gestión de flota
│   │       └── OrderRepository.java        # findById
│   └── exceptions/
│       ├── LogisticsException.java         # excepción base
│       ├── CapacityExceededException.java  # el peso del pedido supera la capacidad máxima del vehículo
│       ├── RouteNotFoundException.java
│       └── OrderNotFoundException.java
│
├── application/
│   └── services/
│       ├── AssignOrderService.java         # implementa AssignOrderUseCase — orquesta el flujo completo de asignación
│       └── SelectVehicleService.java       # servicio de dominio de aplicación — clasifica el peso → selecciona el tipo de vehículo disponible
│
└── infrastructure/
    ├── config/
    │   └── JpaConfig.java                  # reutilizar de gestión de flota
    ├── persistence/
    │   ├── jpa/
    │   │   ├── RouteJpaEntity.java         # @OneToMany a StopJpaEntity
    │   │   ├── StopJpaEntity.java          # @ManyToOne a RouteJpaEntity y OrderJpaEntity
    │   │   ├── VehicleJpaEntity.java       # reutilizar de gestión de flota
    │   │   └── OrderJpaEntity.java
    │   ├── repository/
    │   │   ├── RouteRepositoryAdapter.java
    │   │   ├── StopRepositoryAdapter.java
    │   │   ├── VehicleRepositoryAdapter.java   # reutilizar de gestión de flota
    │   │   └── OrderRepositoryAdapter.java
    │   └── jparepository/
    │       ├── RouteSpringRepository.java
    │       ├── StopSpringRepository.java
    │       ├── VehicleSpringRepository.java    # reutilizar de gestión de flota
    │       └── OrderSpringRepository.java
    ├── web/
    │   ├── controller/
    │   │   └── AssignRouteController.java
    │   └── dto/
    │       ├── AssignOrderRequest.java     # orderId, totalWeight, deliveryAddress
    │       └── AssignOrderResponse.java   # routeId, vehicleId, accumulatedWeight, routeStatus, dispatchDate, stop
    ├── mapper/
    │   └── RouteAssignmentMapper.java     # MapStruct @Component — Route/Stop ↔ DTOs y entidades JPA
    └── exception/
        ├── GlobalExceptionHandler.java    # reutilizar de gestión de flota — agregar nuevas excepciones
        └── ErrorResponse.java             # reutilizar de gestión de flota
```

**Decisión de Estructura**: Arquitectura hexagonal limpia con tres capas (domain, application, infrastructure). El dominio no tiene dependencias de frameworks. `SelectVehicleService` es un servicio de dominio de la capa de aplicación: contiene lógica de selección que no pertenece a un solo modelo pero es lógica de negocio pura sin conocimiento de infraestructura. El único adaptador de entrada HTTP es `AssignRouteController`. No existen DTOs en `application/` ni en `domain/`.

```text
src/test/java/co/edu/unimagdalena/storelogistic/route/
├── unit/
│   ├── domain/
│   │   ├── models/
│   │   │   ├── RouteTest.java
│   │   │   ├── StopTest.java
│   │   │   └── OrderTest.java
│   │   └── values/
│   │       ├── VehicleTypeTest.java
│   │       ├── RouteStatusTest.java
│   │       └── StopStatusTest.java
│   ├── application/
│   │   ├── AssignOrderServiceTest.java
│   │   └── SelectVehicleServiceTest.java
│   └── infrastructure/
│       ├── RouteAssignmentMapperTest.java
│       ├── RouteRepositoryAdapterTest.java
│       └── AssignRouteControllerTest.java
├── integration/
│   ├── RouteRepositoryIntegrationTest.java
│   ├── StopRepositoryIntegrationTest.java
│   └── AssignOrderServiceIntegrationTest.java
├── contract/
│   └── AssignRouteApiContractTest.java
└── testdata/
    ├── fixtures/
    │   ├── RouteFixture.java
    │   ├── StopFixture.java
    │   ├── VehicleFixture.java          # reutilizar de gestión de flota
    │   └── OrderFixture.java
    ├── builders/
    │   ├── RouteBuilder.java
    │   └── AssignOrderRequestBuilder.java
    └── containers/
        └── PostgreSQLContainer.java     # reutilizar de gestión de flota
```

---

## Fase 1: Configuración (Infraestructura Compartida)

**Propósito**: Configurar el proyecto Gradle, dependencias y base de datos antes de cualquier implementación de lógica de negocio.

- [ ] T001 Crear la estructura de directorios siguiendo el layout definido en este plan.
- [ ] T002 Configurar `build.gradle.kts` — incluir dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `testcontainers`, `archunit`. Verificar que no estén ya presentes desde gestión de flota.
- [ ] T003 Configurar `application.yml` — conexión PostgreSQL, perfiles `dev`, `test`, `prod`.
- [ ] T004 Crear scripts de migración Flyway:
  - Verificar que `V1__create_vehicle_table.sql` existe desde gestión de flota (columnas: `id_vehicle`, `type`, `load_capacity`, `status`, `id_carrier`).
  - `V2__create_route_table.sql` — (`id_route`, `id_vehicle` FK, `total_capacity`, `accumulated_weight`, `status`, `dispatch_date`).
  - `V3__create_order_table.sql` — (`id_order`, `total_weight`, `delivery_address`).
  - `V4__create_stop_table.sql` — (`id_stop`, `id_route` FK, `id_order` FK, `sequence`, `delivery_address`, `status`, `delivery_date`).
  - `V5__create_route_indexes.sql` — índices en `route.status`, `route.accumulated_weight`, `stop.id_route`.
- [ ] T005 Configurar `ArchUnit` — verificar reglas de capas existentes desde gestión de flota: domain sin imports de Spring/JPA, application sin imports de infrastructure. Agregar regla: los servicios de `application/` no deben importar desde `infrastructure/`.
- [ ] T006 Verificar `PostgreSQLContainer.java` — reutilizar configuración de TestContainers existente.
- [ ] T007 Configurar Jacoco — objetivo ≥80% global, 100% capa de dominio.

**Checkpoint**: El proyecto compila, las migraciones corren sin errores, el contenedor de prueba inicia correctamente.

---

## Fase 2: Fundacional (Prerrequisitos Bloqueantes)

**Propósito**: Crear todos los componentes base de dominio e infraestructura antes de implementar cualquier escenario. Aún no hay lógica de negocio — solo la fundación compartida.

**⚠️ CRÍTICO**: Ningún escenario puede comenzar hasta que esta fase esté completa.

- [ ] T008 Crear `TotalWeight.java` — objeto de valor, valida > 0, inmutable. Sin anotaciones de framework.
- [ ] T009 Crear `LoadCapacity.java` — objeto de valor, valida > 0, inmutable. Reutilizar de gestión de flota si existe la misma clase en el módulo compartido; crear aquí si el paquete está aislado.
- [ ] T010 Crear `VehicleType.java` — enum con umbrales de peso de FR-004: `URBAN_VAN` (≤1.5t), `SINGLE_TRUCK` (≤5t), `REGIONAL_SEMI` (>25t). Cada valor almacena su capacidad máxima en kg. Método `static forWeight(TotalWeight weight): VehicleType` — devuelve el tipo apropiado; lanza `CapacityExceededException` si ningún tipo aplica.
- [ ] T011 Crear `RouteStatus.java` — enum: `AVAILABLE | CLOSED | PENDING_VEHICLE`.
- [ ] T012 Crear `StopStatus.java` — enum: `PENDING | DELIVERED | REJECTED`.
- [ ] T013 Crear `Order.java` — modelo de dominio: `orderId (Long)`, `totalWeight (TotalWeight)`, `deliveryAddress`. Sin anotaciones JPA.
- [ ] T014 Crear `Stop.java` — modelo de dominio: `stopId`, `routeId`, `orderId (Long)`, `sequence`, `deliveryAddress`, `status (StopStatus)`, `deliveryDate`. Sin anotaciones JPA. Métodos `markDelivered()`, `markRejected()`.
- [ ] T015 Crear `Vehicle.java` — modelo de dominio: `vehicleId`, `type (VehicleType)`, `loadCapacity (LoadCapacity)`, `status`, `carrierId`. Reutilizar de gestión de flota si ya está definido en un módulo de dominio compartido.
- [ ] T016 Crear `Route.java` — modelo de dominio: `routeId`, `vehicleId`, `totalCapacity (LoadCapacity)`, `accumulatedWeight (TotalWeight)`, `status (RouteStatus)`, `dispatchDate`, `stops (List<Stop>)`. Métodos de negocio:
  - `canAcceptWeight(TotalWeight weight): boolean` — valida `accumulatedWeight + weight ≤ totalCapacity` (FR-001).
  - `occupancyPercentage(): BigDecimal` — `(accumulatedWeight / totalCapacity) * 100`, 2 decimales.
  - `isFull(): boolean` — `occupancyPercentage() >= 95` (FR-005). Esta es la única fuente de verdad para el umbral del 95%.
  - `assignOrder(Order order): Stop` — crea y retorna una nueva `Stop`, incrementa `accumulatedWeight`. Lanza `CapacityExceededException` si `canAcceptWeight` es falso.
  - `close()` — cambia el estado a `CLOSED`.
  - `static createNew(String vehicleId, LoadCapacity totalCapacity, LocalDate dispatchDate): Route` — método de fábrica, estado `AVAILABLE`, `accumulatedWeight = 0`.
- [ ] T017 Crear `LogisticsException.java`, `CapacityExceededException.java`, `RouteNotFoundException.java`, `OrderNotFoundException.java`.
- [ ] T018 Crear puerto de entrada `AssignOrderUseCase.java` en `domain/ports/in/` — firma: `Route assign(Long orderId)`. Recibe y retorna exclusivamente objetos de dominio.
- [ ] T019 Crear puertos de salida en `domain/ports/out/`:
  - `RouteRepository.java` — `Optional<Route> findAvailableWithCapacity(TotalWeight weight)`, `Route save(Route route)`, `Optional<Route> findById(String routeId)`.
  - `StopRepository.java` — `Stop save(Stop stop)`, `List<Stop> findByRouteId(String routeId)`.
  - `VehicleRepository.java` — `Optional<Vehicle> findAvailableByType(VehicleType type)`, `Optional<Vehicle> findMaxCapacity()`. Reutilizar de gestión de flota si ya está definido.
  - `OrderRepository.java` — `Optional<Order> findById(Long orderId)`.
- [ ] T020 Crear `SelectVehicleService.java` en `application/services/` — servicio de dominio de capa de aplicación:
  - Recibe `TotalWeight weight`.
  - Delega la clasificación de tipo a `VehicleType.forWeight(weight)` (FR-004) — el enum contiene esta lógica, no el servicio.
  - Llama a `VehicleRepository.findAvailableByType(type)`.
  - Si no se encuentra vehículo del tipo correcto, llama a `VehicleRepository.findMaxCapacity()` como fallback.
  - Si `weight` aún supera la capacidad del vehículo de fallback, lanza `CapacityExceededException` (caso borde de spec).
  - Retorna `Optional<Vehicle>` — vacío si no hay ningún vehículo disponible (activa estado `PENDING_VEHICLE`).
  - Sin conocimiento de DTOs. Sin conocimiento de controladores.
- [ ] T021 Crear entidades JPA:
  - `RouteJpaEntity.java` — `@OneToMany(mappedBy = "route", cascade = ALL)` a `StopJpaEntity`.
  - `StopJpaEntity.java` — `@ManyToOne` a `RouteJpaEntity`; FK a `id_order`.
  - `VehicleJpaEntity.java` — reutilizar de gestión de flota.
  - `OrderJpaEntity.java`.
- [ ] T022 Crear interfaces Spring Data JPA: `RouteSpringRepository`, `StopSpringRepository`, `VehicleSpringRepository` (reutilizar), `OrderSpringRepository`.
- [ ] T023 Crear `RouteAssignmentMapper.java` en `infrastructure/mapper/` como MapStruct `@Component` — conversiones: `Route → AssignOrderResponse`, `Stop → StopDTO` (si está anidado en la respuesta), `RouteJpaEntity ↔ Route`, `StopJpaEntity ↔ Stop`, `OrderJpaEntity → Order`.
- [ ] T024 Crear `GlobalExceptionHandler.java` — reutilizar de gestión de flota, agregar:
  - `CapacityExceededException` → HTTP 422 Unprocessable Entity.
  - `OrderNotFoundException` → HTTP 404.
  - `RouteNotFoundException` → HTTP 404.
  - Violaciones de `@Valid` → HTTP 400.
- [ ] T025 Crear fixtures base: `RouteFixture.java`, `StopFixture.java`, `OrderFixture.java`, `VehicleFixture.java` (reutilizar).
- [ ] T026 Pruebas unitarias para `VehicleType.forWeight()` — clasificación correcta para cada rango de peso (FR-004): ≤1.5t, ≤5t, >25t, y `CapacityExceededException` cuando ningún tipo aplica.
- [ ] T027 Pruebas unitarias para `Route.canAcceptWeight()` — el peso cabe, el peso está exactamente en el límite, el peso lo supera.
- [ ] T028 Pruebas unitarias para `Route.isFull()` — falso al 94.9%, verdadero exactamente al 95.0%, verdadero al 100%.
- [ ] T029 Pruebas unitarias para `Route.assignOrder()` — camino feliz crea parada, incrementa `accumulatedWeight`; lanza `CapacityExceededException` cuando no hay capacidad.
- [ ] T030 Pruebas unitarias para `Route.occupancyPercentage()` — 0%, 50%, 95%, 100%.

**Checkpoint**: Modelos de dominio, objetos de valor, puertos, entidades JPA y manejo de excepciones listos. T026–T030 pasan. El proyecto compila sin errores. Aún no hay endpoint funcional.

---

## Fase 3: Escenario 1 — Asignar pedido a ruta existente (P1)

**Objetivo**: Cuando llega un pedido y una ruta existente tiene capacidad suficiente, el sistema lo asigna a esa ruta, crea la parada correspondiente, actualiza el peso acumulado y prioriza la ruta con mayor porcentaje de ocupación (FR-002).

**Prueba Independiente**: Dado una ruta `AVAILABLE` con capacidad suficiente, `POST /api/logistics/routes/assignments` con un `orderId` válido retorna HTTP 200 con el `routeId` existente, `accumulatedWeight` actualizado, y una parada recién creada con FKs correctas a ruta y pedido.

### Pruebas para el Escenario 1

- [ ] T031 [P] [SC1] Prueba de contrato en `AssignRouteApiContractTest` — `POST /api/logistics/routes/assignments` con ruta existente con capacidad → HTTP 200, cuerpo contiene `routeId`, `accumulatedWeight` actualizado, `stop` con `orderId` (FR-001, FR-002, SC-001).
- [ ] T032 [P] [SC1] Prueba de contrato en `AssignRouteApiContractTest` — dos rutas disponibles: verificar que el `routeId` de la respuesta corresponde a la que tiene mayor porcentaje de ocupación (FR-002).
- [ ] T033 [P] [SC1] Prueba de integración en `RouteRepositoryIntegrationTest` — `findAvailableWithCapacity` retorna la ruta con mayor porcentaje de ocupación cuando hay varias disponibles; retorna vacío cuando todas están al ≥95% o tienen capacidad insuficiente.
- [ ] T034 [P] [SC1] Prueba de integración en `StopRepositoryIntegrationTest` — `save` persiste la parada con las FKs correctas `id_route` e `id_order` en PostgreSQL real.

### Implementación del Escenario 1

- [ ] T035 [SC1] Crear `RouteRepositoryAdapter.java` implementando `RouteRepository`:
  - `findAvailableWithCapacity(TotalWeight weight)` — consulta JPA filtrando `status = AVAILABLE` y `(total_capacity - accumulated_weight) >= weight`; ordenada por `(accumulated_weight / total_capacity) DESC` para priorizar la ruta más ocupada (FR-002); retorna el primer resultado.
  - `save`, `findById`.
- [ ] T036 [SC1] Crear `StopRepositoryAdapter.java` implementando `StopRepository`: `save`, `findByRouteId`.
- [ ] T037 [SC1] Crear `OrderRepositoryAdapter.java` implementando `OrderRepository`: `findById`.
- [ ] T038 [SC1] Implementar `AssignOrderService.java` implementando `AssignOrderUseCase`:
  - `@Transactional(isolation = Isolation.SERIALIZABLE)`.
  - Recibe `Long orderId`.
  - Obtiene `Order` vía `OrderRepository.findById` — lanza `OrderNotFoundException` si no existe.
  - Llama a `RouteRepository.findAvailableWithCapacity(order.getTotalWeight())`.
  - **Rama Escenario 1 — ruta existente**: llama a `route.assignOrder(order)` (la lógica de negocio vive en el modelo), guarda la ruta actualizada vía `RouteRepository.save`, guarda la nueva parada vía `StopRepository.save`.
  - Tras guardar: llama a `route.isFull()` — si es verdadero, llama a `route.close()` y guarda de nuevo (FR-005). `isFull()` solo se comprueba tras persistir para garantizar que la verificación del 95% use el peso final.
  - Retorna `Route` (objeto de dominio). Sin conocimiento de DTOs.
- [ ] T039 [SC1] Crear `AssignRouteController.java` con `POST /api/logistics/routes/assignments`:
  - Extrae `orderId` de `AssignOrderRequest`.
  - Llama a `AssignOrderUseCase.assign(orderId)`.
  - Mapea `Route → AssignOrderResponse` usando `RouteAssignmentMapper`.
  - Retorna HTTP 200.
- [ ] T040 [SC1] Crear `AssignOrderRequest.java` en `infrastructure/web/dto/` — `@NotNull Long orderId`.
- [ ] T041 [SC1] Crear `AssignOrderResponse.java` en `infrastructure/web/dto/` — `routeId`, `vehicleId`, `accumulatedWeight`, `routeStatus`, `dispatchDate`, `stop`.
- [ ] T042 [SC1] Pruebas unitarias para `AssignOrderService` — rama Escenario 1:
  - Camino feliz: `OrderRepository`, `RouteRepository`, `StopRepository` mockeados; verificar que `route.assignOrder()` es llamado; verificar que `RouteRepository.save` y `StopRepository.save` son invocados exactamente una vez.
  - Ruta que no alcanza el 95% tras la asignación: verificar que `route.close()` NO es llamado.
  - `OrderNotFoundException` cuando el pedido no existe.
- [ ] T043 [SC1] Pruebas unitarias para `AssignRouteController` con MockMvc:
  - POST con `orderId` válido → HTTP 200, cuerpo correcto.
  - POST con `orderId` nulo → HTTP 400.

**Checkpoint**: `POST /api/logistics/routes/assignments` con ruta existente funciona de extremo a extremo. T031–T034 pasan con TestContainers.

---

## Fase 4: Escenario 2 — Crear nueva ruta cuando no hay capacidad disponible (P1)

**Objetivo**: Cuando ninguna ruta disponible tiene capacidad suficiente, el sistema crea automáticamente una nueva ruta, selecciona el vehículo más eficiente para el peso del pedido, asigna el pedido y registra la fecha de despacho (FR-003, FR-004, FR-006, SC-003, SC-004).

**Prueba Independiente**: Sin ninguna ruta `AVAILABLE` con capacidad suficiente, `POST /api/logistics/routes/assignments` retorna HTTP 201 con un nuevo `routeId`, el `vehicleId` correcto para el tipo de peso del pedido, y `dispatchDate` no nulo.

### Pruebas para el Escenario 2

- [ ] T044 [P] [SC2] Prueba de contrato en `AssignRouteApiContractTest` — POST sin ruta disponible → HTTP 201, cuerpo contiene nuevo `routeId`, `vehicleId` del tipo correcto según el peso, `dispatchDate` no nulo (FR-003, FR-006, SC-003).
- [ ] T045 [P] [SC2] Prueba de contrato en `AssignRouteApiContractTest` — POST sin vehículo disponible → HTTP 201, `routeStatus: PENDING_VEHICLE`, `vehicleId` es null (caso borde de spec).
- [ ] T046 [P] [SC2] Prueba de integración en `AssignOrderServiceIntegrationTest` — sin rutas disponibles: verificar que `RouteRepository.save` es llamado con un nuevo `routeId`, `VehicleRepository.findAvailableByType` es llamado con el `VehicleType` correcto, y `dispatchDate` está fijado en hoy.
- [ ] T047 [P] [SC2] Pruebas unitarias en `SelectVehicleServiceTest` — clasificación correcta para cada rango de peso: ≤1.5t → `URBAN_VAN`, ≤5t → `SINGLE_TRUCK`, >25t → `REGIONAL_SEMI`; `CapacityExceededException` cuando el peso supera la capacidad máxima del vehículo (caso borde de spec).

### Implementación del Escenario 2

- [ ] T048 [SC2] Crear `VehicleRepositoryAdapter.java` implementando `VehicleRepository`: `findAvailableByType`, `findMaxCapacity`.
- [ ] T049 [SC2] Extender `AssignOrderService.assign()` — **rama Escenario 2** cuando `findAvailableWithCapacity` retorna vacío:
  - Llama a `SelectVehicleService.select(order.getTotalWeight())` — retorna `Optional<Vehicle>`.
  - Si hay vehículo: llama a `Route.createNew(vehicleId, totalCapacity, LocalDate.now())` (FR-006), llama a `route.assignOrder(order)`, guarda ruta y parada. Verifica `isFull()` y cierra si es necesario.
  - Si no hay vehículo: llama a `Route.createNew(null, defaultCapacity, LocalDate.now())` con estado `PENDING_VEHICLE` — no se crea parada aún (caso borde de spec: la ruta se crea pendiente de disponibilidad de vehículo).
  - Retorna objeto de dominio `Route`.
- [ ] T050 [SC2] Actualizar `AssignRouteController` para retornar HTTP 201 cuando se crea una nueva ruta (distinguir de HTTP 200 en asignación a ruta existente usando la presencia del `routeId` de la ruta en la BD antes de la llamada, o una bandera retornada por el servicio envuelta en un objeto de valor `RouteAssignmentResult`).
- [ ] T051 [SC2] Pruebas unitarias para `AssignOrderService` — rama Escenario 2:
  - Vehículo disponible: verificar que `Route.createNew` es llamado, `RouteRepository.save` llamado una vez, `StopRepository.save` llamado una vez, `dispatchDate` es hoy.
  - Sin vehículo disponible: verificar que la ruta se guarda con `PENDING_VEHICLE`, `StopRepository.save` NO es llamado (SC-004 — sin asignación de parada si no hay vehículo disponible).
  - `CapacityExceededException` se propaga correctamente cuando el peso supera todos los tipos de vehículo.

**Checkpoint**: Las nuevas rutas se crean correctamente. SC1 y SC2 funcionan de extremo a extremo de forma independiente. T044–T047 pasan.

---

## Fase 5: Escenario 3 — Cerrar ruta al 95% de capacidad (P1)

**Objetivo**: Cuando la asignación de un pedido lleva la ruta al ≥95% de su capacidad total, el sistema la marca automáticamente como `CLOSED` para que no acepte más pedidos (FR-005, SC-002).

**Prueba Independiente**: Dada una ruta `AVAILABLE` con peso acumulado al 94% de su capacidad, asignar un pedido que lleve el total al ≥95% resulta en la ruta persistida con estado `CLOSED` en la base de datos. Un POST posterior no asigna a esa ruta.

### Pruebas para el Escenario 3

- [ ] T052 [P] [SC3] Prueba de contrato en `AssignRouteApiContractTest` — POST que lleva la ruta al ≥95% → la respuesta contiene `routeStatus: CLOSED`; un POST posterior no asigna a la misma ruta (FR-005, SC-002).
- [ ] T053 [P] [SC3] Prueba de integración en `AssignOrderServiceIntegrationTest` — verificar que `route.close()` es llamado y `RouteRepository.save` persiste el estado `CLOSED` exactamente cuando `accumulatedWeight / totalCapacity >= 0.95`; verificar que NO es llamado cuando está por debajo del 95%.
- [ ] T054 [P] [SC3] Prueba de integración — caso borde: el peso del pedido es exactamente igual a la capacidad restante → la ruta alcanza el 100%, el estado debe ser `CLOSED` (caso borde de spec).

### Implementación del Escenario 3

- [ ] T055 [SC3] Verificar que `Route.isFull()` y `Route.close()` ya están implementados en T016. No se necesita nueva implementación — SC3 ya está gestionado dentro de `AssignOrderService.assign()` en T038 (rama Escenario 1) y T049 (rama Escenario 2). Esta fase se centra en verificar la corrección con pruebas específicas.
- [ ] T056 [SC3] Pruebas unitarias para `Route.isFull()` — casos borde explícitos: 94.9% → falso, 95.0% → verdadero, 100% → verdadero, peso exactamente igual a la capacidad restante → verdadero.
- [ ] T057 [SC3] Pruebas unitarias para `AssignOrderService` — rama de cierre: ruta mockeada con 94% de ocupación + nuevo peso de pedido que lo lleva exactamente al 95%: verificar que `route.close()` es llamado y `RouteRepository.save` es invocado una segunda vez con estado `CLOSED`.

**Checkpoint**: Los 3 escenarios de la spec funcionan de extremo a extremo. T052–T054 pasan con TestContainers.

---

## Fase 6: Casos Borde

**Propósito**: Cubrir los casos borde explícitos definidos en la spec.

- [ ] T058 [EC] El peso del pedido supera la capacidad máxima del vehículo — `VehicleType.forWeight` lanza `CapacityExceededException` → se propaga a través de `SelectVehicleService` → `GlobalExceptionHandler` retorna HTTP 422. Prueba de contrato: POST con peso > 25t cuando no existe ningún vehículo `REGIONAL_SEMI` → HTTP 422 con mensaje descriptivo (caso borde de spec 1).
- [ ] T059 [EC] El peso del pedido es exactamente igual a la capacidad restante de la ruta — la ruta alcanza el 100%, estado `CLOSED`. La prueba unitaria en `RouteTest` ya cubre esto en T056. Agregar prueba de integración: verificar que el POST siguiente no asigna a esa ruta (caso borde de spec 2).
- [ ] T060 [EC] No hay vehículos disponibles en absoluto — `SelectVehicleService.select()` retorna `Optional.empty()`, ruta creada con `PENDING_VEHICLE`. La prueba unitaria verifica que no se crea parada. La prueba de integración verifica que la ruta se persiste con `vehicleId` nulo y estado `PENDING_VEHICLE` (caso borde de spec 3).
- [ ] T061 [EC] Pedidos concurrentes que llegan simultáneamente — `@Transactional(isolation = SERIALIZABLE)` garantiza el procesamiento secuencial de las verificaciones de capacidad. Prueba de integración: 10 hilos publicando pedidos simultáneamente a la misma ruta; verificar que el `accumulatedWeight` final es correcto y no se viola ninguna restricción de capacidad (caso borde de spec 4).

**Checkpoint**: Todos los casos borde de la spec cubiertos con pruebas automatizadas.

---

## Fase 7: Pulido y Aspectos Transversales

**Propósito**: Hardening, observabilidad, validaciones y documentación.

- [ ] T062 Logging estratégico con `@Slf4j` en `AssignOrderService`:
  - `INFO` al inicio de cada asignación (orderId recibido).
  - `INFO` en asignación exitosa a ruta existente (orderId, routeId, nuevo accumulatedWeight, % de ocupación).
  - `INFO` cuando se crea una nueva ruta (nuevo routeId, vehicleId, vehicleType, dispatchDate).
  - `INFO` cuando se cierra una ruta (routeId, % de ocupación final).
  - `WARN` cuando la ruta alcanza el 90% de ocupación (advertencia pre-cierre).
  - `WARN` cuando no hay vehículo disponible (routeId creado como PENDING_VEHICLE).
  - `ERROR` en excepciones inesperadas con contexto completo.
- [ ] T063 Bean Validation en `AssignOrderRequest` — `@NotNull Long orderId`. Verificar HTTP 400 con orderId nulo.
- [ ] T064 Documentar API con `springdoc-openapi` — endpoint `POST /api/logistics/routes/assignments`, esquemas de petición/respuesta, todos los códigos HTTP posibles: 200, 201, 400, 404, 422.
- [ ] T065 `@ArchTest` con ArchUnit — verificar:
  - `domain/` sin imports de Spring, JPA o web.
  - `application/` puede importar domain, no infrastructure.
  - `infrastructure/` puede importar todo.
  - Ninguna clase en `domain/` o `application/` importa desde `infrastructure/`.
- [ ] T066 Optimización de consultas — revisar N+1 en `findAvailableWithCapacity` (`@EntityGraph` si aplica); validar índices con `EXPLAIN ANALYZE` en `route.status` y `route.accumulated_weight`.
- [ ] T067 Configurar `/actuator/health` con verificación de conectividad a BD.
- [ ] T068 Cobertura de código con Jacoco — verificar ≥80% global, 100% capa de dominio.
- [ ] T069 README con comandos de compilación/ejecución, ejecución de pruebas (unitarias / integración / todas), ejemplos curl para el endpoint.
- [ ] T070 Checklist pre-despliegue: 100% pruebas pasando, ArchUnit pasando, cobertura ≥80%, sin vulnerabilidades en dependencias.

**Checkpoint**: Código listo para producción. SC-001 a SC-004 verificables con pruebas automatizadas.

---

## Dependencias y Orden de Ejecución

### Dependencias entre Fases

- **Configuración (Fase 1)**: Sin dependencias — puede comenzar de inmediato.
- **Fundacional (Fase 2)**: Depende de la Fase 1 — **bloquea todos los escenarios**.
- **Escenario 1 (Fase 3)**: Depende de la Fase 2. Sin dependencia de otros escenarios.
- **Escenario 2 (Fase 4)**: Depende de la Fase 2. Puede ejecutarse en paralelo con la Fase 3 si el tamaño del equipo lo permite; en solitario: implementar después de la Fase 3 ya que extiende el mismo servicio.
- **Escenario 3 (Fase 5)**: Depende de la Fase 3 (`route.assignOrder()` debe existir). Extiende el flujo, no lo reemplaza.
- **Casos Borde (Fase 6)**: Depende de que los 3 escenarios estén completos.
- **Pulido (Fase 7)**: Depende de la Fase 6.

### Dentro de Cada Fase

- Modelos de dominio y métodos → Puertos (interfaces) → Adaptadores de Repositorio/Servicio → Servicio de Aplicación → Controlador.
- Pruebas de contrato e integración (`[P]`) antes de la implementación correspondiente.
- Pruebas unitarias en línea con cada componente.
- Checkpoint al final antes de pasar a la siguiente fase.

---

## Notas

- **DTOs exclusivamente en infraestructura**: `AssignOrderRequest` y `AssignOrderResponse` viven en `infrastructure/web/dto/`. Nunca en `application/` ni en `domain/`.
- **Mapper exclusivamente en infraestructura**: `RouteAssignmentMapper` vive en `infrastructure/mapper/`. El controlador es el único invocador.
- **El servicio opera únicamente con objetos de dominio**: `AssignOrderService` recibe `Long orderId` y retorna `Route`. No tiene conocimiento de `AssignOrderResponse` ni de ningún otro DTO.
- **La lógica de negocio vive en los modelos de dominio**: `Route.assignOrder()`, `Route.isFull()`, `Route.close()`, `Route.canAcceptWeight()` son las únicas fuentes de verdad para sus respectivas reglas. El servicio orquesta; el modelo decide.
- **`VehicleType.forWeight()` es la única fuente de verdad para la clasificación por peso** (FR-004). Ni `SelectVehicleService` ni ninguna otra clase duplica esta lógica.
- **`Route.isFull()` es la única fuente de verdad para el umbral del 95%** (FR-005). Ni el servicio ni el controlador duplican esta lógica.
- **`@Transactional(isolation = SERIALIZABLE)` en `AssignOrderService` es innegociable** según la restricción de concurrencia de la spec.
- La verificación de cierre al 95% debe ocurrir **después** de `RouteRepository.save` — el servicio guarda el peso actualizado primero, luego verifica `isFull()`, luego guarda de nuevo con estado `CLOSED` si es necesario.
- La etiqueta `[P]` marca las pruebas a escribir antes de la implementación (test-first).
- Las etiquetas `[SC1]`, `[SC2]`, `[SC3]`, `[EC]` mapean directamente a los escenarios de la spec para trazabilidad.
- Hacer commit después de cada tarea completada con pruebas en verde.
- Evitar tareas que modifiquen el mismo archivo desde diferentes escenarios en paralelo.
