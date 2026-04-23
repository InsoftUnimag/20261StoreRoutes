# Documentación Detallada de Implementación
## Feature: Asignar Ruta + Solicitar Ruta | Módulo: Gestión de Rutas

**Versión:** 0.0.1-SNAPSHOT | **Fecha:** 23 de Abril de 2026  
**Stack:** Java 21 + Spring Boot 3.5.13 + PostgreSQL 17 + Flyway 11 + RabbitMQ 3.x

---

## 📋 Contenidos

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura](#arquitectura)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Capa de Dominio](#capa-de-dominio)
5. [Capa de Aplicación](#capa-de-aplicación)
6. [Capa de Infraestructura](#capa-de-infraestructura)
7. [Flujo de Operación](#flujo-de-operación)
8. [API REST](#api-rest)
9. [Mensajería Asíncrona](#mensajería-asíncrona)
10. [Manejo de Errores](#manejo-de-errores)
11. [Base de Datos](#base-de-datos)
12. [Testing](#testing)
13. [Ejecución](#ejecución)

---

## 🎯 Resumen Ejecutivo

**StoreLogistic** implementa dos features interconectadas del módulo de rutas:

- **Asignar Ruta (REST):** dado un `orderId`, busca la mejor ruta disponible con capacidad suficiente, asigna la orden y crea la parada correspondiente. Si no existe ruta, crea una nueva seleccionando el vehículo apropiado según el peso del pedido.
- **Solicitar Ruta (RabbitMQ):** consume eventos `route.request` publicados por otros servicios, delega al mismo caso de uso de asignación y publica el resultado (`route.assigned`) o error (`route.error`) en el broker.

### Características Clave

✅ **Dominio Puro**: capa de dominio sin dependencias de Spring, JPA ni Lombok  
✅ **Concurrencia Segura**: `@Transactional(isolation = SERIALIZABLE)` en `AssignOrderService`  
✅ **Selección Inteligente de Vehículo**: `VehicleType.forWeight()` — única fuente de verdad  
✅ **Validación de Transiciones de Estado**: `RouteStatus` y `StopStatus` con guardas explícitas  
✅ **Mensajería Asíncrona**: Spring Cloud Stream + RabbitMQ Binder con DLQ configurada  
✅ **Manejo de Errores Dual**: errores de negocio → `route.error`; errores inesperados → DLQ  
✅ **Testing Completo**: 53+ pruebas de dominio, aplicación e infraestructura  

---

## 🏗️ Arquitectura

La feature implementa **Arquitectura Hexagonal (Ports & Adapters)** estricta. La regla de dependencias es unidireccional: infraestructura → aplicación → dominio. El dominio no importa nada externo.

**Principios aplicados:**
- Inversión de Dependencias: las interfaces de puerto viven en el dominio
- Aislamiento total: el dominio usa sólo `java.*` y `java.math.*`
- Concurrencia: aislamiento SERIALIZABLE para evitar doble asignación bajo carga
- Mensajería: Consumer funcional (no anotación `@RabbitListener`) vía Spring Cloud Stream

**Stack Tecnológico**

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| **JDK** | Java | 21 LTS |
| **Framework** | Spring Boot | 3.5.13 |
| **Base de Datos** | PostgreSQL | 17 |
| **ORM** | Hibernate / Spring Data JPA | 6.x |
| **Migrations** | Flyway | 11.20.3 |
| **Mensajería** | Spring Cloud Stream + RabbitMQ Binder | 2024.0.1 |
| **Mapping** | MapStruct (abstract class) | 1.6.3 |
| **Utilidades** | Lombok (solo app/infra) | 1.18.x |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers | 5.x / 1.20.x |

---

## 📁 Estructura del Proyecto

```
src/main/java/co/edu/unimagdalena/storelogistic/
│
└── route/                                        # Módulo de Gestión de Rutas
    │
    ├── domain/                                   # ★ CAPA DE DOMINIO (sin dependencias externas)
    │   │
    │   ├── models/                               # Entidades de dominio
    │   │   ├── Route.java                        # Aggregate root — lógica de rutas
    │   │   ├── Stop.java                         # Entidad parada
    │   │   ├── Order.java                        # Objeto de valor de pedido
    │   │   └── RouteVehicle.java                 # Objeto de vehículo leído del módulo flota
    │   │
    │   ├── values/                               # Value Objects
    │   │   ├── LogisticWeight.java               # record — peso en kg, valida > 0
    │   │   ├── RouteCapacity.java                # record — capacidad en kg, valida > 0
    │   │   ├── VehicleType.java                  # enum — clasifica peso → tipo vehículo
    │   │   ├── RouteStatus.java                  # enum — AVAILABLE, CLOSED, PENDING_VEHICLE
    │   │   └── StopStatus.java                   # enum — PENDING, DELIVERED, REJECTED
    │   │
    │   ├── ports/
    │   │   ├── in/
    │   │   │   ├── AssignOrderUseCase.java        # Puerto de entrada REST
    │   │   │   └── ProcessRouteRequestUseCase.java # Puerto de entrada RabbitMQ
    │   │   │
    │   │   └── out/
    │   │       ├── RouteRepository.java
    │   │       ├── StopRepository.java
    │   │       ├── RouteVehicleRepository.java
    │   │       └── OrderRepository.java
    │   │
    │   └── exceptions/
    │       ├── RouteException.java               # Base
    │       ├── CapacityExceededException.java
    │       ├── InvalidStateTransitionException.java
    │       ├── OrderNotFoundException.java
    │       └── RouteNotFoundException.java
    │
    ├── application/                              # ★ CAPA DE APLICACIÓN
    │   └── services/
    │       ├── AssignOrderService.java           # Orquesta asignación con SERIALIZABLE
    │       ├── SelectVehicleService.java         # Selecciona vehículo por tipo de peso
    │       └── ProcessRouteRequestService.java   # Convierte evento → domain → delega
    │
    └── infrastructure/                           # ★ CAPA DE INFRAESTRUCTURA
        │
        ├── web/
        │   ├── controller/
        │   │   └── AssignRouteController.java    # POST /logistics/routes/assignments
        │   └── dto/
        │       ├── AssignOrderRequest.java
        │       ├── AssignOrderResponse.java
        │       └── StopResponse.java
        │
        ├── persistence/
        │   ├── jpa/
        │   │   ├── RouteJpaEntity.java
        │   │   ├── StopJpaEntity.java
        │   │   ├── OrderJpaEntity.java
        │   │   ├── RouteVehicleJpaEntity.java    # Read-only — apunta a tabla vehiculos
        │   │   └── RouteCategoryJpaEntity.java   # Read-only — apunta a tabla categorias
        │   ├── jparepository/
        │   │   ├── RouteSpringRepository.java    # SQL nativo: findBestAvailableWithCapacity
        │   │   └── RouteVehicleSpringRepository.java
        │   └── repository/
        │       ├── RouteRepositoryAdapter.java
        │       ├── StopRepositoryAdapter.java
        │       ├── RouteVehicleRepositoryAdapter.java
        │       └── OrderRepositoryAdapter.java
        │
        ├── mapper/
        │   └── RouteAssignmentMapper.java        # MapStruct abstract class
        │
        ├── messaging/
        │   ├── RouteRequestEvent.java            # Evento entrante (pesoLogistico)
        │   ├── RouteAssignedEvent.java           # Evento de éxito
        │   ├── RouteErrorEvent.java              # Evento de error de negocio
        │   ├── RouteEventPublisher.java          # StreamBridge
        │   └── RouteRequestListener.java         # Consumer<RouteRequestEvent>
        │
        ├── exception/
        │   └── RouteExceptionHandler.java        # @RestControllerAdvice(basePackages=...)
        │
        └── config/
            └── RouteJpaConfig.java               # @EnableJpaRepositories para este módulo
```

### Convenciones de Nombres

| Componente | Sufijo | Ejemplo |
|-----------|--------|---------|
| **Interfaz de dominio entrada** | `UseCase` | `AssignOrderUseCase` |
| **Implementación de use case** | `Service` | `AssignOrderService` |
| **Interfaz de puerto salida** | `Repository` | `RouteRepository` |
| **Adaptador de repositorio** | `Adapter` | `RouteRepositoryAdapter` |
| **Repositorio Spring Data** | `SpringRepository` | `RouteSpringRepository` |
| **Entidad JPA** | `JpaEntity` | `RouteJpaEntity` |
| **Transfer Object** | `Request`/`Response`/`Event` | `AssignOrderRequest`, `RouteRequestEvent` |

---

## 🎯 Capa de Dominio

Contiene la **lógica de negocio pura** independiente de tecnología. Sin Spring, sin JPA, sin Lombok.

### Value Objects

**LogisticWeight.java** — record inmutable, valida peso positivo:
```java
public record LogisticWeight(BigDecimal valueKg) {
    public LogisticWeight {
        if (valueKg == null || valueKg.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Weight must be positive");
    }
    public static LogisticWeight of(double kg) { return new LogisticWeight(BigDecimal.valueOf(kg)); }
    public LogisticWeight add(LogisticWeight other) { return new LogisticWeight(valueKg.add(other.valueKg)); }
}
```

**VehicleType.java** — única fuente de verdad para clasificar peso → tipo de vehículo:
```java
public enum VehicleType {
    URBAN_VAN     (BigDecimal.valueOf(1_500),  "CAMIONETA"),
    SINGLE_TRUCK  (BigDecimal.valueOf(5_000),  "CAMION_SENCILLO"),
    REGIONAL_SEMI (BigDecimal.valueOf(25_000), "TRACTOCAMION_REGIONAL");

    public static VehicleType forWeight(LogisticWeight weight) {
        return Arrays.stream(values())
                .filter(t -> weight.valueKg().compareTo(t.maxKg) <= 0)
                .findFirst()
                .orElseThrow(() -> new CapacityExceededException(
                    "No vehicle type supports " + weight.valueKg() + " kg"));
    }
}
```

**RouteStatus.java** — enum con guardas de transición:
```java
public enum RouteStatus {
    AVAILABLE, CLOSED, PENDING_VEHICLE;

    public boolean isValidTransition(RouteStatus target) {
        return switch (this) {
            case AVAILABLE       -> target == CLOSED;
            case PENDING_VEHICLE -> target == AVAILABLE;
            case CLOSED          -> false;
        };
    }

    public String invalidTransitionMessage(RouteStatus target) {
        return "Cannot transition from " + this + " to " + target;
    }
}
```

**StopStatus.java** — transiciones válidas PENDING → DELIVERED / REJECTED:
```java
public enum StopStatus {
    PENDING, DELIVERED, REJECTED;

    public boolean isValidTransition(StopStatus target) {
        return switch (this) {
            case PENDING   -> target == DELIVERED || target == REJECTED;
            case DELIVERED -> false;
            case REJECTED  -> false;
        };
    }
}
```

### Modelo de Dominio — Route (Aggregate Root)

```java
public class Route {

    private Long routeId;
    private Long vehicleId;
    private RouteCapacity totalCapacity;
    private BigDecimal accumulatedWeightKg;   // BigDecimal — acumula desde 0
    private RouteStatus status;
    private LocalDate dispatchDate;
    private List<Stop> stops;

    // ── Fábricas ──────────────────────────────────────────────────────────────

    public static Route createNew(Long vehicleId, RouteCapacity capacity, LocalDate date) {
        // vehicleId != null → AVAILABLE, null → PENDING_VEHICLE
    }

    public static Route reconstitute(...) { /* rehidrata desde BD */ }

    // ── Lógica de negocio ─────────────────────────────────────────────────────

    public boolean canAcceptWeight(LogisticWeight weight) {
        BigDecimal remaining = totalCapacity.valueKg().subtract(accumulatedWeightKg);
        return weight.valueKg().compareTo(remaining) <= 0;
    }

    public BigDecimal occupancyPercentage() {
        return accumulatedWeightKg
                .multiply(BigDecimal.valueOf(100))
                .divide(totalCapacity.valueKg(), 2, RoundingMode.HALF_UP);
    }

    public boolean isFull() {
        return occupancyPercentage().compareTo(BigDecimal.valueOf(95)) >= 0;
    }

    public Stop assignOrder(Order order) {
        if (!canAcceptWeight(order.logisticWeight()))
            throw new CapacityExceededException("...");
        this.accumulatedWeightKg = this.accumulatedWeightKg.add(order.logisticWeight().valueKg());
        Stop stop = Stop.create(this.routeId, order.orderId(), stops.size() + 1, order.deliveryAddress());
        this.stops.add(stop);
        return stop;
    }

    public void close() {
        if (!status.isValidTransition(RouteStatus.CLOSED))
            throw new InvalidStateTransitionException(status.invalidTransitionMessage(RouteStatus.CLOSED));
        this.status = RouteStatus.CLOSED;
    }

    public void activate() {
        if (!status.isValidTransition(RouteStatus.AVAILABLE))
            throw new InvalidStateTransitionException(status.invalidTransitionMessage(RouteStatus.AVAILABLE));
        this.status = RouteStatus.AVAILABLE;
    }
}
```

**Regla de negocio clave:** `isFull()` considera la ruta llena cuando la ocupación alcanza o supera el **95%**. Al cerrarse (SC3), se marca como `CLOSED` y ya no acepta más pedidos.

### Modelo de Dominio — Stop

```java
public class Stop {
    private Long stopId;
    private Long routeId;
    private Long orderId;
    private int sequence;
    private String deliveryAddress;
    private StopStatus status;      // PENDING por defecto
    private LocalDate deliveryDate;

    public void markDelivered(LocalDate date) {
        if (!status.isValidTransition(StopStatus.DELIVERED))
            throw new InvalidStateTransitionException(status.invalidTransitionMessage(StopStatus.DELIVERED));
        this.status = StopStatus.DELIVERED;
        this.deliveryDate = date;
    }

    public void markRejected() {
        if (!status.isValidTransition(StopStatus.REJECTED))
            throw new InvalidStateTransitionException(status.invalidTransitionMessage(StopStatus.REJECTED));
        this.status = StopStatus.REJECTED;
    }
}
```

### Puertos de Dominio

**Entrada:**
```java
public interface AssignOrderUseCase {
    record AssignResult(Route route, boolean isNewRoute) {}
    AssignResult assign(Long orderId);
}

public interface ProcessRouteRequestUseCase {
    void process(Long orderId, BigDecimal weightKg, String deliveryAddress);
}
```

**Salida:**
```java
public interface RouteRepository {
    Optional<Route> findBestAvailableWithCapacity(LogisticWeight weight);
    Route save(Route route);
}

public interface StopRepository {
    Stop save(Stop stop);
}

public interface RouteVehicleRepository {
    Optional<RouteVehicle> findAvailableByType(String categoryName);
    Optional<RouteVehicle> findMaxCapacity();
}

public interface OrderRepository {
    Optional<Order> findById(Long orderId);
}
```

---

## 💼 Capa de Aplicación

Orquesta la lógica de negocio sin depender de infraestructura.

### SelectVehicleService

Encapsula la selección del vehículo más adecuado según el peso del pedido:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SelectVehicleService {

    private final RouteVehicleRepository vehicleRepository;

    public RouteVehicle selectFor(LogisticWeight weight) {
        VehicleType type = VehicleType.forWeight(weight);
        log.info("Selecting vehicle of type={} for weight={} kg", type, weight.valueKg());

        return vehicleRepository.findAvailableByType(type.getCategoryName())
                .or(() -> {
                    log.warn("No vehicle of type={} found, falling back to max-capacity vehicle", type);
                    return vehicleRepository.findMaxCapacity()
                            .filter(v -> v.loadCapacity().compareTo(weight.valueKg()) >= 0);
                })
                .orElseThrow(() -> new CapacityExceededException(
                    "No available vehicle can handle " + weight.valueKg() + " kg"));
    }
}
```

**Algoritmo:**
1. Clasificar peso → `VehicleType` (URBAN_VAN / SINGLE_TRUCK / REGIONAL_SEMI)
2. Buscar vehículo disponible del tipo exacto
3. Fallback: vehículo de mayor capacidad si existe uno con capacidad suficiente
4. Sin vehículo → `CapacityExceededException`

### AssignOrderService

Caso de uso principal, protegido con aislamiento SERIALIZABLE:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignOrderService implements AssignOrderUseCase {

    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final SelectVehicleService selectVehicleService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AssignResult assign(Long orderId) {
        log.info("Assigning orderId={} to route", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // SC1: Existe ruta disponible con espacio
        Optional<Route> existing = routeRepository.findBestAvailableWithCapacity(order.logisticWeight());
        if (existing.isPresent()) {
            Route route = existing.get();
            Stop stop = route.assignOrder(order);
            stopRepository.save(stop);
            if (route.isFull()) {
                log.warn("Route routeId={} reached 95% capacity — closing", route.routeId());
                route.close();
            }
            routeRepository.save(route);
            log.info("Order {} assigned to existing routeId={}", orderId, route.routeId());
            return new AssignResult(route, false);
        }

        // SC2: Crear ruta nueva
        RouteVehicle vehicle = selectVehicleService.selectFor(order.logisticWeight());
        RouteCapacity capacity = RouteCapacity.of(vehicle.loadCapacity());
        Route newRoute = Route.createNew(vehicle.vehicleId(), capacity, LocalDate.now());
        Route saved = routeRepository.save(newRoute);
        Stop stop = saved.assignOrder(order);
        stopRepository.save(stop);

        // SC3: La nueva ruta ya queda llena con este primer pedido
        if (saved.isFull()) {
            log.warn("New route routeId={} is already full after first order — closing", saved.routeId());
            saved.close();
            routeRepository.save(saved);
        }

        log.info("Order {} assigned to new routeId={}", orderId, saved.routeId());
        return new AssignResult(saved, true);
    }
}
```

### ProcessRouteRequestService

Convierte el evento de mensajería en parámetros de dominio y delega:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessRouteRequestService implements ProcessRouteRequestUseCase {

    private final OrderRepository orderRepository;
    private final AssignOrderUseCase assignOrderUseCase;

    @Override
    public void process(Long orderId, BigDecimal weightKg, String deliveryAddress) {
        log.info("Processing route request for orderId={}", orderId);
        // Persiste o recupera la Order, luego delega
        assignOrderUseCase.assign(orderId);
    }
}
```

---

## 🌐 Capa de Infraestructura

### Adaptador Web — REST Controller

**AssignRouteController.java**

```java
@Slf4j
@RestController
@RequestMapping("/logistics/routes/assignments")
@RequiredArgsConstructor
public class AssignRouteController {

    private final AssignOrderUseCase assignOrderUseCase;
    private final RouteAssignmentMapper mapper;

    @PostMapping
    public ResponseEntity<AssignOrderResponse> assign(@Valid @RequestBody AssignOrderRequest request) {
        log.info("POST /logistics/routes/assignments orderId={}", request.orderId());
        AssignOrderUseCase.AssignResult result = assignOrderUseCase.assign(request.orderId());

        AssignOrderResponse response = mapper.toResponse(result.route());
        HttpStatus status = result.isNewRoute() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
```

**Semántica HTTP:**
- `200 OK` — pedido asignado a ruta existente
- `201 Created` — pedido asignado a ruta nueva
- `404 Not Found` — pedido no existe
- `422 Unprocessable Entity` — peso excede toda capacidad disponible
- `409 Conflict` — transición de estado inválida

### DTOs

```java
public record AssignOrderRequest(@NotNull Long orderId) {}

@Builder
public class AssignOrderResponse {
    private Long routeId;
    private Long vehicleId;
    private BigDecimal totalCapacityKg;
    private BigDecimal accumulatedWeightKg;
    private BigDecimal occupancyPercentage;
    private String status;
    private LocalDate dispatchDate;
    private List<StopResponse> stops;
}

@Builder
public class StopResponse {
    private Long stopId;
    private Long orderId;
    private int sequence;
    private String deliveryAddress;
    private String status;
    private LocalDate deliveryDate;
}
```

### Entidades JPA

**RouteJpaEntity.java** — mapea tabla `routes`:
```java
@Entity
@Table(name = "routes")
public class RouteJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_route")
    private Long routeId;

    @Column(name = "id_vehicle")
    private Long vehicleId;

    @Column(name = "total_capacity_kg")
    private BigDecimal totalCapacityKg;

    @Column(name = "accumulated_weight_kg")
    private BigDecimal accumulatedWeightKg;

    @Column(name = "status")
    private String status;

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    @OneToMany(mappedBy = "routeId", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<StopJpaEntity> stops = new ArrayList<>();
}
```

**Patrón de entidades read-only del módulo flota:** `RouteVehicleJpaEntity` y `RouteCategoryJpaEntity` son entidades JPA de sólo lectura que apuntan a las mismas tablas `vehiculos` y `categorias` del módulo flota. Esto evita ciclos de dependencia entre módulos manteniendo un único esquema de base de datos.

### Mapper — MapStruct (abstract class)

```java
@Mapper(componentModel = "spring")
public abstract class RouteAssignmentMapper {

    public abstract AssignOrderResponse toResponse(Route route);
    public abstract StopResponse toStopResponse(Stop stop);

    // Conversiones manuales donde los tipos no coinciden directamente
    protected String routeStatusToString(RouteStatus status) { return status.name(); }
    protected String stopStatusToString(StopStatus status)   { return status.name(); }
    protected BigDecimal routeCapacityToDecimal(RouteCapacity c) { return c.valueKg(); }

    // Rehidratación domain → JPA y JPA → domain implementadas manualmente
    public abstract RouteJpaEntity toJpa(Route route);
    public abstract Route toDomain(RouteJpaEntity entity);

    protected String resolveVehicleType(RouteCategoryJpaEntity category) {
        // Mapea nombre de categoría a VehicleType.categoryName
        return category != null ? category.getNombre() : null;
    }
}
```

### Query Nativa — Mejor Ruta Disponible

`RouteSpringRepository.java`:
```java
@Query(value = """
    SELECT * FROM routes
    WHERE status = 'AVAILABLE'
      AND (total_capacity_kg - accumulated_weight_kg) >= :weightKg
    ORDER BY accumulated_weight_kg DESC
    LIMIT 1
    """, nativeQuery = true)
Optional<RouteJpaEntity> findBestAvailableWithCapacity(@Param("weightKg") BigDecimal weightKg);
```

**Estrategia:** ordena por `accumulated_weight_kg DESC` para llenar rutas existentes antes de crear nuevas (bin-packing greedy).

---

## 🔄 Flujo de Operación

### Asignar Ruta (REST)

```
HTTP POST /api/v1/logistics/routes/assignments
{ "orderId": 42 }
        │
        ▼
┌───────────────────────────┐
│  AssignRouteController    │
│  • Valida @NotNull        │
│  • Llama assignOrder(42)  │
└────────┬──────────────────┘
         │
         ▼
┌──────────────────────────────────────────────┐
│  AssignOrderService  (@Transactional SERIAL.) │
│                                              │
│  1. orderRepository.findById(42)             │
│     [→ OrderNotFoundException si no existe]  │
│                                              │
│  2. routeRepository.findBestAvailable(weight)│
│     ┌── Existe ──────────────────────────┐   │
│     │  route.assignOrder(order)          │   │
│     │  stopRepository.save(stop)         │   │
│     │  if (route.isFull()) route.close() │   │
│     │  routeRepository.save(route)       │   │
│     │  return AssignResult(route, false) │   │
│     └────────────────────────────────────┘   │
│     ┌── No existe ───────────────────────┐   │
│     │  selectVehicleService.selectFor()  │   │
│     │  Route.createNew(vehicleId, cap)   │   │
│     │  routeRepository.save(newRoute)    │   │
│     │  newRoute.assignOrder(order)       │   │
│     │  stopRepository.save(stop)         │   │
│     │  if (full) newRoute.close()        │   │
│     │  return AssignResult(route, true)  │   │
│     └────────────────────────────────────┘   │
└────────┬─────────────────────────────────────┘
         │
         ├─ isNewRoute=false → HTTP 200 OK
         └─ isNewRoute=true  → HTTP 201 Created
```

### Solicitar Ruta (RabbitMQ)

```
Exchange: route.request
        │
        ▼
┌─────────────────────────┐
│  RouteRequestListener   │
│  Consumer<RouteRequest  │
│  Event>                 │
└────────┬────────────────┘
         │
         ▼
┌────────────────────────────┐
│  ProcessRouteRequestService│
│  Rehidrata Order           │
│  Llama assignOrder(id)     │
└────────┬───────────────────┘
         │
         ├─ Éxito de negocio → RouteEventPublisher.publishAssigned()
         │                     Exchange: route.assigned
         │
         ├─ Excepción de negocio → RouteEventPublisher.publishError()
         │  (RouteException)       Exchange: route.error
         │
         └─ Excepción inesperada → rethrow → DLQ automática
```

---

## 🌍 API REST

### Configuración Base

- **Host:** `localhost:8080`
- **Base Path:** `/api/v1`
- **Documentación OpenAPI:** `http://localhost:8080/api/v1/docs/openapi.json`
- **Swagger UI:** `http://localhost:8080/api/v1/docs/swagger-ui.html`

### Endpoints

#### **POST /logistics/routes/assignments** — Asignar Ruta

**Descripción:** Asigna el pedido especificado a la ruta disponible más adecuada, o crea una nueva ruta si no hay ninguna con capacidad suficiente.

**Request Body:**
```json
{ "orderId": 42 }
```

**Response 200 OK (ruta existente):**
```json
{
  "routeId": 7,
  "vehicleId": 3,
  "totalCapacityKg": 5000.00,
  "accumulatedWeightKg": 2300.00,
  "occupancyPercentage": 46.00,
  "status": "AVAILABLE",
  "dispatchDate": "2026-04-23",
  "stops": [
    {
      "stopId": 12,
      "orderId": 42,
      "sequence": 3,
      "deliveryAddress": "Calle 15 #20-30",
      "status": "PENDING",
      "deliveryDate": null
    }
  ]
}
```

**Response 201 Created (ruta nueva):** mismo cuerpo, la ruta recién creada.

**Response 404 Not Found:**
```json
{
  "codigo": "ORDER_NOT_FOUND",
  "mensaje": "Order with ID 999 not found",
  "timestamp": "2026-04-23T10:15:00"
}
```

**Response 422 Unprocessable Entity:**
```json
{
  "codigo": "CAPACITY_EXCEEDED",
  "mensaje": "No vehicle type supports 30000 kg",
  "timestamp": "2026-04-23T10:15:00"
}
```

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/logistics/routes/assignments" \
  -H "Content-Type: application/json" \
  -d '{"orderId": 42}'
```

---

## 📨 Mensajería Asíncrona

### Configuración Spring Cloud Stream

`application.yml`:
```yaml
spring:
  cloud:
    stream:
      bindings:
        routeRequest-in-0:
          destination: route.request
          group: route-assignment-service
        routeAssigned-out-0:
          destination: route.assigned
        routeError-out-0:
          destination: route.error
      rabbit:
        bindings:
          routeRequest-in-0:
            consumer:
              auto-bind-dlq: true
              dlq-name: route.request.dlq
```

### Contratos de Eventos

**RouteRequestEvent (entrante):**
```json
{
  "orderId": 42,
  "pesoLogistico": 1200.0,
  "deliveryAddress": "Calle 15 #20-30"
}
```

**RouteAssignedEvent (saliente — éxito):**
```json
{
  "orderId": 42,
  "routeId": 7,
  "stopSequence": 3,
  "status": "AVAILABLE"
}
```

**RouteErrorEvent (saliente — error de negocio):**
```json
{
  "orderId": 42,
  "errorCode": "CAPACITY_EXCEEDED",
  "message": "No vehicle type supports 30000 kg"
}
```

### Estrategia de Manejo de Errores

| Tipo de Excepción | Comportamiento | Destino |
|---|---|---|
| `RouteException` (negocio) | Captura + publica evento error | `route.error` exchange |
| `Exception` (inesperada) | Rethrow | DLQ (`route.request.dlq`) |

Esta estrategia garantiza que los errores de negocio sean observables por consumidores de `route.error` sin saturar la DLQ con casos legítimos de rechazo de negocio.

---

## ⚠️ Manejo de Errores

### Manejadores por Módulo

Cada módulo tiene su propio `@RestControllerAdvice` con `basePackages` para evitar conflictos:

```java
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.route")
public class RouteExceptionHandler { ... }

@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.fleet")
public class GlobalExceptionHandler { ... }
```

### Tabla de Códigos HTTP

| Excepción | HTTP | Código |
|-----------|------|--------|
| `InvalidStateTransitionException` | 409 Conflict | `INVALID_STATE_TRANSITION` |
| `OrderNotFoundException` | 404 Not Found | `ORDER_NOT_FOUND` |
| `RouteNotFoundException` | 404 Not Found | `ROUTE_NOT_FOUND` |
| `CapacityExceededException` | 422 Unprocessable Entity | `CAPACITY_EXCEEDED` |
| `MethodArgumentNotValidException` | 400 Bad Request | `VALIDATION_ERROR` |
| `IllegalArgumentException` | 400 Bad Request | `INVALID_ARGUMENT` |

### Estructura de Error

```json
{
  "codigo": "ORDER_NOT_FOUND",
  "mensaje": "Order with ID 42 not found",
  "detalles": {},
  "timestamp": "2026-04-23T10:15:00"
}
```

---

## 🗄️ Base de Datos

### Migraciones Flyway

#### **V4__create_route_tables.sql**

```sql
CREATE TABLE IF NOT EXISTS orders (
    id_order         BIGSERIAL    PRIMARY KEY,
    logistic_weight  NUMERIC      NOT NULL CHECK (logistic_weight > 0),
    delivery_address VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS routes (
    id_route              BIGSERIAL    PRIMARY KEY,
    id_vehicle            BIGINT       REFERENCES vehiculos(id_vehiculo) ON DELETE SET NULL,
    total_capacity_kg     NUMERIC      NOT NULL CHECK (total_capacity_kg > 0),
    accumulated_weight_kg NUMERIC      NOT NULL DEFAULT 0,
    status                VARCHAR(50)  NOT NULL DEFAULT 'AVAILABLE',
    dispatch_date         DATE         NOT NULL
);

CREATE TABLE IF NOT EXISTS stops (
    id_stop          BIGSERIAL    PRIMARY KEY,
    id_route         BIGINT       NOT NULL REFERENCES routes(id_route) ON DELETE CASCADE,
    id_order         BIGINT       NOT NULL REFERENCES orders(id_order),
    sequence         INTEGER      NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    delivery_date    DATE
);
```

**Nota:** El campo de peso se llama `logistic_weight` (no `total_weight` ni `peso_total`). Esta convención se mantiene consistente en todo el sistema: `logisticWeight` en Java, `logistic_weight` en SQL, `pesoLogistico` en eventos RabbitMQ.

#### **V5__create_route_indexes.sql**

```sql
CREATE INDEX idx_routes_status ON routes(status);
CREATE INDEX idx_routes_accumulated_weight ON routes(accumulated_weight_kg);
CREATE INDEX idx_stops_id_route ON stops(id_route);
```

**Justificación:**
- `idx_routes_status`: filtra rutas `AVAILABLE` en la query de asignación
- `idx_routes_accumulated_weight`: ordena por ocupación en la selección greedy
- `idx_stops_id_route`: JOIN en la consulta de paradas de una ruta

---

## 🧪 Testing

### Estructura de Tests

```
src/test/java/co/edu/unimagdalena/storelogistic/route/
│
├── domain/
│   ├── models/
│   │   ├── RouteTest.java              ✅ 14 casos (canAccept, occupancy, isFull, assignOrder, transitions)
│   │   └── StopTest.java               ✅  6 casos (markDelivered, markRejected, transiciones de estado)
│   └── values/
│       ├── LogisticWeightTest.java     ✅  4 casos (positivo, cero, negativo, add)
│       └── VehicleTypeTest.java        ✅  6 casos (cada tipo, borde, exceso)
│
├── application/
│   └── services/
│       ├── AssignOrderServiceTest.java ✅ 10 casos (SC1, SC2, SC3, cierre, errores)
│       └── SelectVehicleServiceTest.java ✅ 5 casos (tipo exacto, fallback, sin vehículo)
│
└── infrastructure/
    ├── web/
    │   └── AssignRouteControllerTest.java ✅ 8 casos (200, 201, 404, 422, validación)
    └── messaging/
        └── RouteRequestListenerTest.java  ✅ 4 casos (éxito, error negocio, error inesperado)
```

### Ejemplo — RouteTest.java

```java
@Test
@DisplayName("assignOrder → throws CapacityExceededException when no space")
void assignOrder_noCapacity_throwsException() {
    Order order = new Order(1L, LogisticWeight.of(2_000.0), "Addr");
    assertThatThrownBy(() -> emptyRoute.assignOrder(order))
            .isInstanceOf(CapacityExceededException.class);
}

@Test
@DisplayName("close → throws when route is PENDING_VEHICLE")
void close_pendingVehicle_throwsInvalidTransition() {
    Route pending = Route.createNew(null, CAPACITY_1500, TODAY);
    assertThatThrownBy(pending::close)
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("PENDING_VEHICLE");
}
```

### Ejemplo — StopTest.java

```java
@Test
@DisplayName("markDelivered → throws when REJECTED")
void markDelivered_fromRejected_throwsInvalidTransition() {
    pendingStop.markRejected();
    assertThatThrownBy(() -> pendingStop.markDelivered(LocalDate.now()))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("REJECTED");
}
```

### Ejemplo — AssignRouteControllerTest.java

```java
@Test
void post_existingRoute_returns200() throws Exception {
    AssignOrderUseCase.AssignResult result =
            new AssignOrderUseCase.AssignResult(buildRoute(), false);
    when(assignOrderUseCase.assign(42L)).thenReturn(result);

    mockMvc.perform(post("/logistics/routes/assignments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"orderId\":42}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.routeId").value(1));
}

@Test
void post_newRoute_returns201() throws Exception {
    AssignOrderUseCase.AssignResult result =
            new AssignOrderUseCase.AssignResult(buildRoute(), true);
    when(assignOrderUseCase.assign(1L)).thenReturn(result);

    mockMvc.perform(post("/logistics/routes/assignments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"orderId\":1}"))
            .andExpect(status().isCreated());
}
```

### Ejecutar Tests

```bash
# Todos los tests del módulo route
./gradlew test --tests "*route*"

# Solo tests de dominio
./gradlew test --tests "*RouteTest" --tests "*StopTest"

# Con reporte
./gradlew test jacocoTestReport
# Reporte en: build/reports/jacoco/test/html/index.html
```

---

## 🚀 Ejecución

### Prerequisitos

1. **Java 21 LTS**
2. **PostgreSQL 17** con base de datos `storeLogistic`
3. **RabbitMQ 3.x** corriendo en `localhost:5672`
4. **Gradle** (incluido en `gradlew`)

### Variables de Entorno

```bash
DB_URL=jdbc:postgresql://localhost:5432/storeLogistic
DB_USERNAME=postgres
DB_PASSWORD=tu_password
RABBIT_HOST=localhost
RABBIT_PORT=5672
RABBIT_USERNAME=guest
RABBIT_PASSWORD=guest
```

### Ejecución

```bash
./gradlew bootRun
```

### Verificación

```bash
# Probar asignación REST
curl -X POST "http://localhost:8080/api/v1/logistics/routes/assignments" \
  -H "Content-Type: application/json" \
  -d '{"orderId": 1}'

# Publicar evento en RabbitMQ (requiere rabbitmqadmin o herramienta similar)
# Exchange: route.request
# Payload:
# { "orderId": 1, "pesoLogistico": 500.0, "deliveryAddress": "Calle 1 #2-3" }
```

### Troubleshooting

| Problema | Solución |
|----------|----------|
| `ORDER_NOT_FOUND` en REST | Verificar que el pedido exista en la tabla `orders` |
| `CAPACITY_EXCEEDED` | El peso supera los 25,000 kg (límite de `REGIONAL_SEMI`) |
| Eventos no se procesan | Verificar que RabbitMQ esté corriendo y el exchange `route.request` exista |
| Error de migración Flyway | Verificar que las migraciones V1-V3 del módulo flota se ejecutaron primero |

---

## 📊 Capacidades Implementadas

✅ Asignar pedido a ruta disponible (REST)  
✅ Crear ruta nueva con vehículo apropiado cuando no hay disponible  
✅ Cierre automático de ruta al alcanzar 95% de ocupación  
✅ Selección inteligente de tipo de vehículo por peso  
✅ Validación de transiciones de estado en Route y Stop  
✅ Consumir eventos de asignación por RabbitMQ  
✅ Publicar eventos de resultado (assigned / error)  
✅ DLQ automática para errores inesperados  
✅ Testing unitario completo (53+ tests)  
✅ Persistencia ACID con aislamiento SERIALIZABLE  

---

## 📝 Información del Proyecto

| Atributo | Valor |
|----------|-------|
| **Versión** | 0.0.1-SNAPSHOT |
| **Fecha** | 23 de Abril de 2026 |
| **Java** | 21 LTS |
| **Spring Boot** | 3.5.13 |
| **Spring Cloud Stream** | 2024.0.1 |
| **PostgreSQL** | 17 |
| **Rama** | feature/route-assignment |

---

**Fin de la Documentación.**