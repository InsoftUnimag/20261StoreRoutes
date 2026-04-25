# Documentación Detallada de Implementación
## Feature: Actualizar Estado de Pedidos en Entrega | Módulo: Logística de Despacho y Distribución

**Versión:** 0.0.1-SNAPSHOT | **Fecha:** 24 de Abril de 2026  
**Stack:** Java 21 + Spring Boot 3.5.13 + Spring Data JPA + Flyway + Testcontainers

---

## Contenidos

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura](#arquitectura)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Capa de Dominio](#capa-de-dominio)
5. [Capa de Aplicación](#capa-de-aplicación)
6. [Capa de Infraestructura](#capa-de-infraestructura)
7. [Migraciones Flyway](#migraciones-flyway)
8. [Flujo de Operación](#flujo-de-operación)
9. [API REST](#api-rest)
10. [Manejo de Errores](#manejo-de-errores)
11. [Testing](#testing)
12. [Ejecución](#ejecución)

---

## Resumen Ejecutivo

El módulo de Logística registra el estado final de entrega de un pedido mediante una operación PUT. La operación:

1. Valida que el transportista exista
2. Busca el pedido y aplica el nuevo estado (con cálculo automático de tasa)
3. Persiste el pedido actualizado
4. Si había un estado previo, guarda un registro de auditoría
5. Si el estado requiere atención, genera una alerta operativa
6. Publica un evento asíncrono al Módulo Financiero (sin bloquear ni hacer rollback si falla)

### Características Clave

**Dominio Puro**: capa de dominio sin dependencias de Spring, JPA ni Lombok  
**Tasa calculada en dominio**: `FinalStatus.effectivenessRate()` como única fuente de verdad  
**Auditoría automática**: solo al corregir, nunca en primera inscripción  
**Alertas tipificadas**: cada estado problemático genera un tipo de alerta específico  
**Evento sin acoplamiento**: `@Async` con try-catch — fallo del broker no afecta la persistencia  
**Merge-save seguro**: columnas NOT NULL del módulo de rutas se preservan en el update

---

## Arquitectura

La feature implementa **Arquitectura Hexagonal (Ports & Adapters)** estricta. La regla de dependencias es unidireccional: infraestructura → aplicación → dominio.

**Principios aplicados:**
- El dominio solo usa `java.*` — sin Spring, sin JPA, sin Lombok
- `UpdateOrderStatusService` depende de cinco puertos de salida (abstracciones), nunca de adaptadores JPA directamente
- La tasa de efectividad es una consecuencia del estado — calculada en `FinalStatus`, nunca en el adaptador o el controlador
- `Alert.createFor(Order)` encapsula toda la lógica de tipo de alerta — el servicio solo decide si llamarlo

**Stack Tecnológico**

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| **JDK** | Java | 21 LTS |
| **Framework** | Spring Boot | 3.5.13 |
| **Persistencia** | Spring Data JPA + Hibernate | 3.5.x |
| **Migraciones** | Flyway | 11.x |
| **Mapping** | MapStruct | 1.6.3 |
| **Utilidades** | Lombok (solo app/infra) | 1.18.x |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers | 5.x / 1.20.x |

---

## Estructura del Proyecto

```
src/main/java/co/edu/unimagdalena/storelogistic/
│
└── orderstatus/                                        # Módulo Actualizar Estado de Pedido
    │
    ├── domain/                                         # ★ CAPA DE DOMINIO (sin dependencias externas)
    │   │
    │   ├── models/
    │   │   ├── Order.java                              # create(), restore(), updateStatus(), requiresAlert()
    │   │   ├── Alert.java                              # createFor(Order) factory
    │   │   ├── Carrier.java                            # carrierId — solo verifica existencia
    │   │   └── OrderStatusAudit.java                   # auditId, orderId, previousStatus, newStatus, carrierId, timestamp
    │   │
    │   ├── values/
    │   │   ├── FinalStatus.java                        # enum con displayName + effectivenessRate()
    │   │   ├── EffectivenessRate.java                  # rango -100..100, constructor package-private
    │   │   ├── AlertType.java                          # NO_ENTREGADO | RECHAZO | FALTANTE | DEVOLUCION
    │   │   └── AlertStatus.java                        # PENDIENTE | RESUELTA
    │   │
    │   ├── ports/
    │   │   ├── in/
    │   │   │   └── UpdateOrderStatusUseCase.java       # Order update(Long orderId, Long carrierId, FinalStatus)
    │   │   └── out/
    │   │       ├── OrderRepository.java                # findById, save
    │   │       ├── CarrierRepository.java              # findById
    │   │       ├── AlertRepository.java                # save
    │   │       ├── OrderStatusAuditRepository.java     # save
    │   │       └── OrderStatusEventPublisher.java      # publish(orderId, status, carrierId)
    │   │
    │   └── exceptions/
    │       ├── LogisticsException.java                 # Excepción base runtime
    │       ├── OrderNotFoundException.java             # 404
    │       ├── CarrierNotFoundException.java           # 404
    │       └── InvalidFinalStatusException.java        # 422
    │
    ├── application/
    │   └── services/
    │       └── UpdateOrderStatusService.java           # Implementa UpdateOrderStatusUseCase
    │
    └── infrastructure/
        ├── config/
        │   └── OrderStatusJpaConfig.java               # @EnableAsync @EnableJpaRepositories
        ├── persistence/
        │   ├── jpa/
        │   │   ├── OrderStatusJpaEntity.java           # @Entity(name="OrderStatusEntity") @Table("orders")
        │   │   ├── CarrierJpaEntity.java               # @Entity(name="CarrierEntity") @Table("vehiculos")
        │   │   ├── OrderAlertJpaEntity.java            # @Entity(name="OrderAlertEntity") @Table("order_alerts")
        │   │   └── OrderStatusAuditJpaEntity.java      # @Entity(name="OrderStatusAuditEntity") @Table("order_status_audit")
        │   ├── jparepository/
        │   │   ├── OrderStatusSpringRepository.java    # JpaRepository<OrderStatusJpaEntity, Long>
        │   │   ├── CarrierSpringRepository.java        # findByTransporterId(Long)
        │   │   ├── AlertSpringRepository.java          # JpaRepository<OrderAlertJpaEntity, Long>
        │   │   └── OrderStatusAuditSpringRepository.java # findByOrderIdOrderByTimestampAsc
        │   └── repository/
        │       ├── OrderStatusRepositoryAdapter.java   # merge-save para preservar columnas de rutas
        │       ├── CarrierRepositoryAdapter.java
        │       ├── AlertRepositoryAdapter.java
        │       └── OrderStatusAuditRepositoryAdapter.java
        ├── messaging/
        │   ├── OrderStatusEventPublisherAdapter.java   # @Async — stub temporal
        │   └── dto/
        │       └── OrderStatusEventDto.java            # id_pedido, tasa_efectividad, id_transportista
        ├── mapper/
        │   └── OrderStatusMapper.java                  # MapStruct abstract class — manual methods
        └── web/
            ├── controller/
            │   └── UpdateOrderStatusController.java    # PUT /logistics/orders/{idPedido}/status
            ├── dto/
            │   ├── UpdateOrderStatusRequest.java       # idTransportista (@NotNull), estadoFinal (@NotBlank)
            │   └── UpdateOrderStatusResponse.java      # idPedido, estadoFinal, tasaEfectividad, idTransportista, fechaActualizacion
            └── exception/
                ├── OrderStatusExceptionHandler.java    # @RestControllerAdvice(basePackages=...)
                └── ErrorResponse.java                  # Estructura estándar de error
```

### Convenciones de Nombres

| Componente | Sufijo | Ejemplo |
|-----------|--------|---------|
| **Puerto de entrada** | `UseCase` | `UpdateOrderStatusUseCase` |
| **Implementación de use case** | `Service` | `UpdateOrderStatusService` |
| **Puerto de salida** | `Repository` / `Publisher` | `OrderRepository`, `OrderStatusEventPublisher` |
| **Adaptador JPA** | `RepositoryAdapter` | `OrderStatusRepositoryAdapter` |
| **Entidad JPA** | `JpaEntity` | `OrderStatusJpaEntity` |
| **Spring Repository** | `SpringRepository` | `OrderStatusSpringRepository` |

---

## Capa de Dominio

Contiene la **lógica de negocio pura** independiente de tecnología. Sin Spring, sin JPA, sin Lombok.

### Value Object — FinalStatus

```java
public enum FinalStatus {

    ENTREGADO_COMPLETO("ENTREGADO_COMPLETO",  EffectivenessRate.of(100)),
    RECHAZO_PARCIAL   ("RECHAZO_PARCIAL",     EffectivenessRate.of(80)),
    NO_ENTREGADO      ("NO_ENTREGADO",        EffectivenessRate.of(0)),
    DEVOLUCION_ERROR_EMPRESA("DEVOLUCION_ERROR_EMPRESA", EffectivenessRate.of(0)),
    FALTANTE_INVENTARIO("FALTANTE_INVENTARIO", EffectivenessRate.of(-100));

    private final String displayName;
    private final EffectivenessRate effectivenessRate;

    public static FinalStatus fromDisplayName(String name) {
        return Arrays.stream(values())
                .filter(s -> s.displayName.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new InvalidFinalStatusException(name));
    }
}
```

`fromDisplayName()` es la **única fuente de verdad** para parsear el string del request. El controlador delega a este método y lanza `InvalidFinalStatusException` (422) si el valor no existe.

### Value Object — EffectivenessRate

```java
public final class EffectivenessRate {

    private final int value;

    EffectivenessRate(int value) {  // package-private — solo FinalStatus puede instanciar
        if (value < -100 || value > 100)
            throw new IllegalArgumentException("EffectivenessRate must be between -100 and 100");
        this.value = value;
    }

    public static EffectivenessRate of(int value) { return new EffectivenessRate(value); }
    public int value() { return value; }
}
```

El constructor package-private garantiza que solo `FinalStatus` pueda crear tasas válidas, eliminando la posibilidad de crear instancias inconsistentes desde fuera del dominio.

### Modelo de Dominio — Order

```java
public class Order {

    // Factory para primera creación (sin estado)
    public static Order create(Long orderId, Long clientId, Long carrierId) { ... }

    // Factory para reconstrucción desde BD (con estado)
    public static Order restore(Long orderId, Long clientId, Long carrierId,
                                FinalStatus finalStatus, EffectivenessRate effectivenessRate,
                                LocalDateTime createdAt, LocalDateTime updatedAt) { ... }

    // Comando — única fuente de mutación de estado
    public void updateStatus(FinalStatus newStatus, Long newCarrierId) {
        this.finalStatus = newStatus;
        this.effectivenessRate = newStatus.effectivenessRate();
        this.carrierId = newCarrierId;
        this.updatedAt = LocalDateTime.now();
    }

    // Consulta — decide si se debe generar alerta (no genera, solo decide)
    public boolean requiresAlert() {
        return finalStatus == FinalStatus.NO_ENTREGADO
            || finalStatus == FinalStatus.RECHAZO_PARCIAL
            || finalStatus == FinalStatus.FALTANTE_INVENTARIO
            || finalStatus == FinalStatus.DEVOLUCION_ERROR_EMPRESA;
    }
}
```

### Modelo de Dominio — Alert

```java
public class Alert {

    // Factory — encapsula el switch de tipos de alerta
    public static Alert createFor(Order order) {
        AlertType type = switch (order.finalStatus()) {
            case NO_ENTREGADO           -> AlertType.NO_ENTREGADO;
            case RECHAZO_PARCIAL        -> AlertType.RECHAZO;
            case FALTANTE_INVENTARIO    -> AlertType.FALTANTE;
            case DEVOLUCION_ERROR_EMPRESA -> AlertType.DEVOLUCION;
            default -> throw new IllegalArgumentException("Estado no genera alerta: " + order.finalStatus());
        };
        return new Alert(null, order.orderId(), order.carrierId(),
                         order.finalStatus(), type, AlertStatus.PENDIENTE, LocalDateTime.now());
    }
}
```

### Excepciones de Dominio

| Excepción | Causa | HTTP |
|-----------|-------|------|
| `OrderNotFoundException` | Pedido no existe en `orders` | 404 |
| `CarrierNotFoundException` | Transportista no existe en `vehiculos` | 404 |
| `InvalidFinalStatusException` | String de estado no reconocido | 422 |

---

## Capa de Aplicación

### UpdateOrderStatusService

```java
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final CarrierRepository carrierRepository;
    private final AlertRepository alertRepository;
    private final OrderStatusAuditRepository auditRepository;
    private final OrderStatusEventPublisher eventPublisher;

    @Override
    public Order update(Long orderId, Long carrierId, FinalStatus status) {
        // 1. Validar que el transportista existe
        carrierRepository.findById(carrierId)
                .orElseThrow(() -> new CarrierNotFoundException(carrierId));

        // 2. Obtener el pedido
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 3. Guardar estado previo antes de mutar
        FinalStatus previousStatus = order.finalStatus();

        // 4. Aplicar nuevo estado (calcula tasa automáticamente)
        order.updateStatus(status, carrierId);

        // 5. Persistir dentro de la transacción
        Order saved = orderRepository.save(order);

        // 6. Auditoría solo si había un estado previo
        if (previousStatus != null) {
            auditRepository.save(OrderStatusAudit.of(
                    null, orderId, previousStatus, status, carrierId, LocalDateTime.now()));
        }

        // 7. Alerta si el estado lo requiere
        if (saved.requiresAlert()) {
            alertRepository.save(Alert.createFor(saved));
        }

        // 8. Evento asíncrono — fallo no hace rollback
        try {
            eventPublisher.publish(orderId, status, carrierId);
        } catch (Exception e) {
            log.warn("Failed to publish order status event for orderId={}: {}", orderId, e.getMessage());
        }

        return saved;
    }
}
```

**Flujo de decisión:**
- La transacción cubre pasos 1–7. Si algo falla en este rango, todo se revierte.
- El try-catch del paso 8 es **intencional**: el evento es una notificación posterior a la entrega. Si el broker no está disponible, el pedido ya está correctamente persistido.

---

## Capa de Infraestructura

### JPA Entities — Coexistencia sobre tabla `orders`

```java
// Módulo de rutas (existente)
@Entity              // nombre JPA por defecto: "OrderJpaEntity"
@Table(name = "orders")
public class OrderJpaEntity { ... }

// Este módulo
@Entity(name = "OrderStatusEntity")  // nombre JPA distinto — evita conflicto
@Table(name = "orders")
public class OrderStatusJpaEntity { ... }
```

El mismo patrón aplica para `vehiculos`:

```java
@Entity(name = "CarrierEntity")
@Table(name = "vehiculos")
public class CarrierJpaEntity {
    @Id @Column(name = "id_vehiculo") private Long vehicleId;
    @Column(name = "id_transportista") private Long transporterId;
}
```

`CarrierSpringRepository.findByTransporterId(Long carrierId)` verifica la existencia del transportista buscando por `id_transportista`, no por `id_vehiculo`.

### Adaptador de Repositorio — Merge-Save

```java
@Override
public Order save(Order order) {
    // Fetch existing entity to preserve logistic_weight and delivery_address (NOT NULL columns
    // owned by the route module — this feature only updates the status-related fields).
    OrderStatusJpaEntity entity = springRepository.findById(order.orderId())
            .orElse(mapper.toEntity(order));
    entity.setCarrierId(order.carrierId());
    entity.setClientId(order.clientId());
    entity.setEstadoFinal(order.finalStatus() != null ? order.finalStatus().displayName() : null);
    entity.setTasaEfectividad(order.effectivenessRate() != null ? order.effectivenessRate().value() : null);
    entity.setUpdatedAt(order.updatedAt());
    return mapper.toDomain(springRepository.save(entity));
}
```

Solo los cinco campos que este módulo posee son actualizados. `logistic_weight` y `delivery_address` permanecen intactos.

### Publisher — StreamBridge (Spring Cloud Stream)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusEventPublisherAdapter implements OrderStatusEventPublisher {

    private static final String BINDING = "publicarEstadoPedido-out-0";

    private final StreamBridge streamBridge;

    @Override
    public void publish(Long orderId, FinalStatus status, Long carrierId) {
        OrderStatusEventDto event = OrderStatusEventDto.builder()
                .id_pedido(orderId)
                .tasa_efectividad(status.effectivenessRate().value())
                .id_transportista(carrierId)
                .build();
        log.info("Publishing order status event to finance module: {}", event);
        streamBridge.send(BINDING, event);
    }
}
```

**Binding en `application.yml`:**
```yaml
publicarEstadoPedido-out-0:
  destination: estado-pedido.event
```

**Payload del evento (contrato con Módulo Financiero):**
```json
{
  "id_pedido": 1,
  "tasa_efectividad": 100,
  "id_transportista": 50
}
```

`estado_final` fue **excluido del payload** por acuerdo de integración — el Módulo Financiero solo requiere la tasa para sus liquidaciones. La misma tecnología (`StreamBridge`) que usa `RouteEventPublisher`, lo que hace que el adaptador sea reemplazable sin cambiar código cuando se configure la cola real.

### Mapper — MapStruct Abstract Class

```java
@Mapper(componentModel = "spring")
public abstract class OrderStatusMapper {

    public abstract UpdateOrderStatusResponse toResponse(Order order);
    public abstract OrderStatusJpaEntity toEntity(Order order);

    public Order toDomain(OrderStatusJpaEntity entity) {
        if (entity.getEstadoFinal() == null) {
            return Order.create(entity.getOrderId(), entity.getClientId(), entity.getCarrierId());
        }
        return Order.restore(entity.getOrderId(), entity.getClientId(), entity.getCarrierId(),
                FinalStatus.fromDisplayName(entity.getEstadoFinal()),
                EffectivenessRate.of(entity.getTasaEfectividad()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    // toAlertEntity, toAlertDomain, toAuditEntity, toAuditDomain — métodos similares
}
```

Se usa una **clase abstracta** en lugar de interfaz porque `toDomain()` requiere lógica condicional (pedido con/sin estado) que MapStruct no puede generar automáticamente.

### Controlador

```java
@RestController
@RequestMapping("/logistics/orders")
@RequiredArgsConstructor
public class UpdateOrderStatusController {

    @PutMapping("/{idPedido}/status")
    public ResponseEntity<UpdateOrderStatusResponse> updateStatus(
            @PathVariable Long idPedido,
            @RequestBody @Valid UpdateOrderStatusRequest request) {

        FinalStatus status;
        try {
            status = FinalStatus.fromDisplayName(request.estadoFinal());
        } catch (InvalidFinalStatusException e) {
            throw e;  // manejado por OrderStatusExceptionHandler → 422
        }

        Order updated = updateOrderStatusUseCase.update(idPedido, request.idTransportista(), status);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
```

---

## Migraciones Flyway

### V8 — Columnas de estado en orders

```sql
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS estado_final        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tasa_efectividad    INTEGER,
    ADD COLUMN IF NOT EXISTS id_cliente          BIGINT,
    ADD COLUMN IF NOT EXISTS id_transportista    BIGINT,
    ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP;
```

Las columnas son nullable — un pedido recién creado por el módulo de rutas no tiene estado de entrega aún.

### V9 — Tabla order_alerts

```sql
CREATE TABLE IF NOT EXISTS order_alerts (
    id_alerta                BIGSERIAL PRIMARY KEY,
    id_pedido                BIGINT NOT NULL REFERENCES orders(id_pedido),
    id_transportista         BIGINT,
    estado_final_registrado  VARCHAR(50),
    tipo                     VARCHAR(50) NOT NULL,
    descripcion              TEXT,
    estado                   VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    asignado_a_supervisor    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### V10 — Tabla order_status_audit

```sql
CREATE TABLE IF NOT EXISTS order_status_audit (
    id_auditoria    BIGSERIAL PRIMARY KEY,
    id_pedido       BIGINT NOT NULL REFERENCES orders(id_pedido),
    estado_anterior VARCHAR(50),
    estado_nuevo    VARCHAR(50) NOT NULL,
    id_transportista BIGINT,
    timestamp       TIMESTAMP NOT NULL DEFAULT NOW()
);
```

`estado_anterior` es nullable — permite distinguir la primera inscripción (NULL) de una corrección real en consultas de auditoría.

### V11 — Índices de rendimiento

```sql
CREATE INDEX IF NOT EXISTS idx_orders_estado_final
    ON orders(estado_final) WHERE estado_final IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_orders_transportista
    ON orders(id_transportista) WHERE id_transportista IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_order_alerts_pedido   ON order_alerts(id_pedido);
CREATE INDEX IF NOT EXISTS idx_order_audit_pedido    ON order_status_audit(id_pedido);
```

---

## Flujo de Operación

```
PUT /api/v1/logistics/orders/{idPedido}/status
         │  Body: { "idTransportista": 50, "estadoFinal": "ENTREGADO_COMPLETO" }
         │
         ▼
┌────────────────────────────────────────────────────────┐
│  UpdateOrderStatusController                           │
│  • Extrae idPedido del path                            │
│  • Parsea estadoFinal → FinalStatus (422 si inválido)  │
│  • Valida @NotNull, @NotBlank (400 si falla)           │
│  • Delega al use case                                  │
└─────────────────────┬──────────────────────────────────┘
                      │
                      ▼
┌────────────────────────────────────────────────────────────────────┐
│  UpdateOrderStatusService  (@Transactional)                        │
│                                                                    │
│  1. CarrierRepository.findById(50) → 404 si no existe             │
│  2. OrderRepository.findById(idPedido) → 404 si no existe         │
│  3. previousStatus = order.finalStatus()                           │
│  4. order.updateStatus(ENTREGADO_COMPLETO, 50)                    │
│     → effectivenessRate = 100, updatedAt = now()                  │
│  5. OrderRepository.save(order)  ← merge-save                     │
│  6. if (previousStatus != null) → AuditRepository.save(...)       │
│  7. if (order.requiresAlert()) → AlertRepository.save(...)        │
│  8. try { eventPublisher.publish(id, status, carrierId) }          │
│     catch { log.warn(...) }  ← no rollback                        │
└─────────────────────┬──────────────────────────────────────────────┘
                      │
               HTTP 200
               {
                 "idPedido": 1,
                 "estadoFinal": "ENTREGADO_COMPLETO",
                 "tasaEfectividad": 100,
                 "idTransportista": 50,
                 "fechaActualizacion": "2026-04-24T09:30:00"
               }
```

---

## API REST

### Configuración Base

- **Host:** `localhost:8080`
- **Base Path:** `/api/v1`

### PUT /logistics/orders/{idPedido}/status

**Descripción:** Registra o actualiza el estado final de entrega de un pedido.

**Request:**
```json
{
  "idTransportista": 50,
  "estadoFinal": "ENTREGADO_COMPLETO"
}
```

**Response 200:**
```json
{
  "idPedido": 1,
  "estadoFinal": "ENTREGADO_COMPLETO",
  "tasaEfectividad": 100,
  "idTransportista": 50,
  "fechaActualizacion": "2026-04-24T09:30:00"
}
```

**Response 200 — RECHAZO_PARCIAL:**
```json
{
  "idPedido": 1,
  "estadoFinal": "RECHAZO_PARCIAL",
  "tasaEfectividad": 80,
  "idTransportista": 50,
  "fechaActualizacion": "2026-04-24T09:30:00"
}
```

**Response 422 — Estado no reconocido:**
```json
{
  "codigo": "ESTADO_INVALIDO",
  "mensaje": "Estado final no válido: ENTREGADO_PARCIAL",
  "timestamp": "2026-04-24T09:30:00"
}
```

**Response 404 — Pedido no encontrado:**
```json
{
  "codigo": "PEDIDO_NO_ENCONTRADO",
  "mensaje": "Pedido no encontrado: 999",
  "timestamp": "2026-04-24T09:30:00"
}
```

**Response 404 — Transportista no encontrado:**
```json
{
  "codigo": "TRANSPORTISTA_NO_ENCONTRADO",
  "mensaje": "Transportista no encontrado: 999",
  "timestamp": "2026-04-24T09:30:00"
}
```

**Response 400 — idTransportista null:**
```json
{
  "codigo": "VALIDACION_FALLIDA",
  "mensaje": "idTransportista: must not be null",
  "timestamp": "2026-04-24T09:30:00"
}
```

**cURL de prueba:**
```bash
# SC1 — ENTREGADO_COMPLETO (tasa 100)
curl -X PUT http://localhost:8080/api/v1/logistics/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"idTransportista": 1, "estadoFinal": "ENTREGADO_COMPLETO"}'

# SC1 — NO_ENTREGADO (tasa 0, genera alerta)
curl -X PUT http://localhost:8080/api/v1/logistics/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"idTransportista": 1, "estadoFinal": "NO_ENTREGADO"}'

# SC1 — FALTANTE_INVENTARIO (tasa -100)
curl -X PUT http://localhost:8080/api/v1/logistics/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"idTransportista": 1, "estadoFinal": "FALTANTE_INVENTARIO"}'

# EC — Estado inválido
curl -X PUT http://localhost:8080/api/v1/logistics/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"idTransportista": 1, "estadoFinal": "ESTADO_INEXISTENTE"}'
```

---

## Manejo de Errores

```java
@RestControllerAdvice(basePackages = "co.edu.unimagdalena.storelogistic.orderstatus")
public class OrderStatusExceptionHandler { ... }
```

El `basePackages` garantiza que este manejador no interfiere con los handlers de `route`, `fleet` ni `paymentmethod`.

### Tabla de Códigos

| Excepción | HTTP | Código JSON |
|-----------|------|-------------|
| `OrderNotFoundException` | 404 | `PEDIDO_NO_ENCONTRADO` |
| `CarrierNotFoundException` | 404 | `TRANSPORTISTA_NO_ENCONTRADO` |
| `InvalidFinalStatusException` | 422 | `ESTADO_INVALIDO` |
| `MethodArgumentNotValidException` | 400 | `VALIDACION_FALLIDA` |
| `MethodArgumentTypeMismatchException` | 400 | `VALIDACION_FALLIDA` |

---

## Testing

### Estructura de Tests

```
src/test/java/co/edu/unimagdalena/storelogistic/orderstatus/
│
├── unit/
│   ├── domain/
│   │   ├── values/
│   │   │   ├── FinalStatusTest.java               ✅ 5 tasas + displayName + fromDisplayName
│   │   │   └── EffectivenessRateTest.java          ✅ equals/hashCode, value(), rango
│   │   └── models/
│   │       ├── OrderTest.java                     ✅ updateStatus, requiresAlert (5 estados), restore
│   │       └── AlertTest.java                     ✅ createFor (4 tipos + throw ENTREGADO_COMPLETO)
│   ├── application/
│   │   └── UpdateOrderStatusServiceTest.java      ✅ 7 casos (SC1, SC2, EC carrier, EC order, publisher falla)
│   └── infrastructure/
│       ├── OrderStatusMapperTest.java             ✅ toResponse, toDomain (con/sin estado), toEntity, toAlert, toAudit
│       └── UpdateOrderStatusControllerTest.java   ✅ 6 casos MockMvc (200, 422, 400, 404×2, tasa 80)
│
├── integration/
│   └── UpdateOrderStatusApiContractTest.java      ✅ 8 casos Testcontainers (PostgreSQL + RabbitMQ reales)
│
└── testdata/
    ├── OrderFixture.java                          # withoutStatus(), withStatus(FinalStatus, carrierId)
    └── CarrierFixture.java                        # standard()
```

### Ejemplo — UpdateOrderStatusServiceTest

```java
@Test
@DisplayName("EC: publisher fails — order is already persisted (no rollback)")
void update_publisherFails_orderRemainsPersistedNoException() {
    Order order = OrderFixture.withoutStatus();
    when(carrierRepository.findById(50L)).thenReturn(Optional.of(CarrierFixture.standard()));
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    doThrow(new RuntimeException("RabbitMQ down")).when(eventPublisher).publish(any(), any(), any());

    Order result = service.update(1L, 50L, FinalStatus.ENTREGADO_COMPLETO);

    // Estado persistido aunque el publisher falla
    assertThat(result.finalStatus()).isEqualTo(FinalStatus.ENTREGADO_COMPLETO);
    verify(orderRepository).save(any());
}
```

### Ejemplo — UpdateOrderStatusApiContractTest (Testcontainers)

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class UpdateOrderStatusApiContractTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Test
    @DisplayName("SC2: correcting existing status saves audit record in DB")
    void correctingExistingStatus_savesAuditRecord() {
        // First registration
        putStatus(orderId, carrierId, "NO_ENTREGADO").andExpect(status().isOk());

        // Correction
        putStatus(orderId, carrierId, "ENTREGADO_COMPLETO").andExpect(status().isOk());

        // Verify audit in real DB
        List<OrderStatusAuditJpaEntity> audits = auditRepository.findByOrderIdOrderByTimestampAsc(orderId);
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getPreviousStatus()).isEqualTo("NO_ENTREGADO");
        assertThat(audits.get(0).getNewStatus()).isEqualTo("ENTREGADO_COMPLETO");
    }
}
```

### Ejecutar Tests

```bash
# Solo tests de orderstatus
./gradlew test --tests "*.orderstatus.*"

# Solo integración (Testcontainers — requiere Docker)
./gradlew test --tests "*UpdateOrderStatusApiContractTest"

# Todos los tests
./gradlew test

# Con reporte de cobertura
./gradlew test jacocoTestReport
# Reporte en: build/reports/jacoco/test/html/index.html
```

---

## Ejecución

### Prerequisitos

1. **Java 21 LTS**
2. **PostgreSQL 17** con base de datos `storeLogistic`
3. **RabbitMQ 3.x** corriendo en `localhost:5672`
4. **Docker** (solo para tests de integración con Testcontainers)

### Ejecución

```bash
./gradlew bootRun
```

Flyway ejecuta automáticamente V8–V11 al arrancar si no están aplicadas.

### Verificar estado de las migraciones

```bash
./gradlew flywayInfo
```

---

## Capacidades Implementadas

Registrar estado final de entrega (5 estados posibles)  
Calcular y persistir tasa de efectividad automáticamente  
Generar alerta operativa ante entrega problemática (4 tipos)  
Registrar auditoría al corregir un estado existente  
Publicar evento asíncrono al Módulo Financiero sin bloquear ni hacer rollback  
Preservar datos NOT NULL del módulo de rutas en la tabla compartida `orders`  
Testing unitario con Mockito para lógica de servicio  
Testing de integración con Testcontainers para contratos de BD reales  
Manejador de errores aislado por módulo (`basePackages`)

---

## Información del Proyecto

| Atributo | Valor |
|----------|-------|
| **Versión** | 0.0.1-SNAPSHOT |
| **Fecha** | 24 de Abril de 2026 |
| **Java** | 21 LTS |
| **Spring Boot** | 3.5.13 |
| **Rama** | feature/actualizar-estado-pedido |

---

**Fin de la Documentación.**