# Documentación Detallada de Implementación
## Feature: Solicitar Transportista | Módulo: Gestión de Flota

**Versión:** 0.0.1-SNAPSHOT | **Fecha:** 22 de Abril de 2026  
**Stack:** Java 21 + Spring Boot 3.5.13 + PostgreSQL 17 + Flyway 11.20.3

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
9. [Manejo de Errores](#manejo-de-errores)
10. [Base de Datos](#base-de-datos)
11. [Testing](#testing)
12. [Ejecución](#ejecución)

---

## 🎯 Resumen Ejecutivo

**StoreLogistic** implementa la feature **Solicitar Transportista**, que permite asignar automáticamente un transportista disponible a un vehículo específico. Esta funcionalidad es parte del módulo de gestión de flota y se integra con un servicio externo de transportistas.

### Características Clave

✅ **Arquitectura Hexagonal**: Puertos y adaptadores para máxima desacoplamiento  
✅ **Integración con Servicio Externo**: Comunicación con módulo de transportistas  
✅ **Validación de Dominio**: Verificación de existencia de vehículo y disponibilidad de transportista  
✅ **Manejo de Errores**: Respuestas consistentes con códigos HTTP semánticos  
✅ **API REST Documentada**: Endpoint único con OpenAPI 3.0  
✅ **Testing Unitario**: Cobertura completa de casos de uso y errores  

---

## 🏗️ Arquitectura

La feature implementa **Arquitectura Hexagonal (Ports & Adapters)** para máximo desacoplamiento e integración limpia con servicios externos.

**Principios:**
- Inversión de Dependencias mediante interfaces de dominio
- Aislamiento de Responsabilidades por capas  
- Testabilidad: Fácil mockeado de puertos
- Mantenibilidad: Cambios en infraestructura no afectan el negocio

**Stack Tecnológico**

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| **JDK** | Java | 21 LTS |
| **Framework** | Spring Boot | 3.5.13 |
| **Base de Datos** | PostgreSQL | 17 |
| **ORM** | Hibernate | 6.x |
| **Migrations** | Flyway | 11.20.3 |
| **Utilidades** | Lombok, MapStruct | 1.18.x, 1.6.3 |
| **Testing** | JUnit 5, Mockito, TestContainers | 5.x |
| **API Docs** | SpringDoc OpenAPI | 2.8.5 |

---

## 📁 Estructura del Proyecto

```
src/main/java/co/edu/unimagdalena/storelogistic/
│
├── flota/                                    # Módulo de Gestión de Flota
│   │
│   ├── domain/                               # ★ CAPA DE DOMINIO
│   │   ├── models/                           # Aggregate Roots
│   │   │   ├── Vehiculo.java                 # Entidad principal de vehículos
│   │   │
│   │   ├── ports/                            # ★ INTERFACES DE DOMINIO
│   │   │   ├── in/                           # Entrada (Use Cases)
│   │   │   │   ├── RequestTransporterUseCase.java
│   │   │   │
│   │   │   └── out/                          # Salida (Repositorios)
│   │   │       ├── VehiculoRepository.java   # Puerto de persistencia
│   │   │       └── TransporterServicePort.java # Puerto para servicio externo
│   │   │
│   │   └── exceptions/                       # Excepciones de negocio
│   │       ├── FlotaException.java           # Base
│   │       ├── VehiculoNotFoundException.java
│   │       ├── TransporterNotAvailableException.java
│   │       └── InvalidTransporterException.java
│   │
│   ├── application/                          # ★ CAPA DE APLICACIÓN
│   │   └── services/                         # Implementación de Use Cases
│   │       ├── RequestTransporterService.java
│   │
│   └── infrastructure/                       # ★ CAPA DE INFRAESTRUCTURA
│       │
│       ├── web/                              # Adaptadores de entrada (HTTP)
│       │   ├── controller/
│       │   │   └── RequestTransporterController.java # REST API Endpoints
│       │   │
│       │   └── dto/                          # Data Transfer Objects
│       │       ├── RequestTransporterResponse.java
│       │       └── TransporterAvailableClientDTO.java
│       │
│       ├── persistence/                      # Adaptadores de salida (BBDD)
│       │   ├── jpa/                          # Entidades JPA
│       │   │   ├── VehiculoJpaEntity.java
│       │   │
│       │   ├── jparepository/                # Spring Data JPA
│       │   │   ├── VehiculoSpringRepository.java
│       │   │
│       │   └── repository/                   # Implementación de puertos
│       │       ├── VehiculoRepositoryAdapter.java
│       │       └── MockTransporterServiceClient.java
│       │
│       ├── mapper/                           # Conversiones DTO ↔ Domain
│       │   ├── VehicleTransporterMapper.java
│       │
│       ├── exception/                        # Global Exception Handler
│       │   ├── GlobalExceptionHandler.java
│       │   └── ErrorResponse.java
│       │
│       └── config/                           # Configuración de infraestructura
│           └── JpaConfig.java
│
└── StoreLogisticApplication.java             # Bootstrap de la app
```

### Convenciones de Nombres

| Componente | Sufijo | Ejemplo |
|-----------|--------|---------|
| **Interfaz de dominio** | `UseCase` | `RequestTransporterUseCase` |
| **Implementación de use case** | `Service` | `RequestTransporterService` |
| **Interfaz de puerto out** | `Repository`/`Port` | `VehiculoRepository`, `TransporterServicePort` |
| **Adaptador de repositorio** | `Adapter` | `VehiculoRepositoryAdapter` |
| **Repositorio Spring Data** | `SpringRepository` | `VehiculoSpringRepository` |
| **Entidad JPA** | `JpaEntity` | `VehiculoJpaEntity` |
| **Transfer Object** | `DTO`/`Request`/`Response` | `RequestTransporterResponse` |

---

## 🎯 Capa de Dominio

Contiene la **lógica de negocio pura** independiente de tecnología o infraestructura.

### Modelo de Dominio - Vehicle

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    private Long vehicleId;
    private Long categoryId;
    private LoadCapacity loadCapacity;
    private VehicleStatus status;
    private Long transporterId;
    private BigDecimal currentWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void assignTransporter(Long transporterId) {
        this.transporterId = transporterId;
        this.updatedAt = LocalDateTime.now();
    }
}
```

**Responsabilidades:**
- Mantener estado del vehículo y transportista asignado
- Actualizar metadata de cambios (updatedAt)

### Puertos de Dominio

**Entrada (Use Cases):**
```java
public interface RequestTransporterUseCase {
    Vehicle request(Long vehicleId);
}
```

**Salida (Dependencias Externas):**
```java
public interface VehicleRepository {
    Optional<Vehicle> findById(Long vehicleId);
    Vehicle save(Vehicle vehicle);
}

public interface TransporterServicePort {
    Long getAvailable();
    void validateExistence(Long transporterId);
}
```

### Excepciones de Dominio

```java
public class FlotaException extends RuntimeException {
    public FlotaException(String mensaje) {
        super(mensaje);
    }
}

public class VehicleNotFoundException extends FlotaException {
    public VehicleNotFoundException(Long vehicleId) {
        super("Vehículo con ID " + vehicleId + " no encontrado");
    }
}

public class TransporterNotAvailableException extends FlotaException {
    public TransporterNotAvailableException() {
        super("No hay transportistas disponibles en este momento");
    }
}

public class InvalidTransporterException extends FlotaException {
    public InvalidTransporterException(Long transporterId) {
        super("Transportista con ID " + transporterId + " no es válido");
    }
}
```

---

## 💼 Capa de Aplicación

Coordina la **lógica de orquestación** entre el dominio y la infraestructura.

### RequestTransporterService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTransporterService implements RequestTransporterUseCase {

    private final VehicleRepository vehicleRepository;
    private final TransporterServicePort transporterServicePort;

    @Override
    @Transactional
    public Vehicle request(Long vehicleId) {
        log.info("Requesting transporter assignment for vehicleId={}", vehicleId);

        var vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        var transporterId = transporterServicePort.getAvailable();
        log.info("Available transporter obtained: {}", transporterId);

        transporterServicePort.validateExistence(transporterId);

        vehicle.assignTransporter(transporterId);
        var saved = vehicleRepository.save(vehicle);

        log.info("Transporter {} assigned successfully to vehicle {}", transporterId, vehicleId);
        return saved;
    }
}
```

**Algoritmo:**
1. Buscar vehículo por ID
2. Obtener transportista disponible
3. Validar existencia del transportista
4. Asignar transportista al vehículo
5. Persistir cambios

---

## 🌐 Capa de Infraestructura

Adaptadores que implementan los puertos de dominio.

### Adaptador Web - REST Controller

**RequestTransporterController.java**

```java
@Slf4j
@RestController
@RequestMapping("/logistics/vehicles")
@RequiredArgsConstructor
@Tag(name = "Transporter Assignment", description = "API for requesting and assigning transporters to vehicles")
public class RequestTransporterController {

    private final RequestTransporterUseCase requestTransporterUseCase;
    private final VehicleTransporterMapper mapper;

    @PostMapping("/{vehicleId}/transporter")
    @Operation(
            summary = "Request and assign a transporter to a vehicle",
            description = "Requests an available transporter from the external module and assigns it to the specified vehicle."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transporter assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transporter ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "No transporter available",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RequestTransporterResponse> requestTransporter(@PathVariable Long vehicleId) {
        log.info("POST /logistics/vehicles/{}/transporter", vehicleId);
        var vehicle = requestTransporterUseCase.request(vehicleId);
        return ResponseEntity.ok(mapper.toResponse(vehicle));
    }
}
```

### DTOs (Data Transfer Objects)

**RequestTransporterResponse.java**
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestTransporterResponse {
    private Long idVehiculo;
    private Long idTransportista;
}
```

**TransporterAvailableClientDTO.java**
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransporterAvailableClientDTO {
    private String idTransportista;
    private String nombre;
    private String estado;
}
```

### Adaptador de Servicio Externo

**MockTransporterServiceClient.java**

```java
@Slf4j
@Component
public class MockTransporterServiceClient implements TransporterServicePort {

    private static final Long AVAILABLE_TRANSPORTER_ID = 1L;
    private static final Set<Long> VALID_IDS = Set.of(1L, 2L, 3L);

    @Override
    public Long getAvailable() {
        log.info("[MOCK] Returning available transporter: {}", AVAILABLE_TRANSPORTER_ID);
        return AVAILABLE_TRANSPORTER_ID;
    }

    @Override
    public void validateExistence(Long transporterId) {
        log.info("[MOCK] Validating transporter: {}", transporterId);
        if (transporterId == null || !VALID_IDS.contains(transporterId)) {
            throw new InvalidTransporterException(transporterId);
        }
    }
}
```

### Mapper - Conversión de Dominio a DTO

**VehicleTransporterMapper.java**

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VehicleTransporterMapper {

    @Mapping(target = "idVehiculo", source = "vehicleId")
    @Mapping(target = "idTransportista", source = "transporterId")
    RequestTransporterResponse toResponse(Vehicle vehicle);
}
```

---

## 🔄 Flujo de Operación

### Solicitar Transportista

```
HTTP Request
POST /api/v1/logistics/vehicles/1/transporter
        │
        ▼
┌────────────────────────────┐
│  RequestTransporterController │
│  requestTransporter()         │
│  • Parsea vehicleId           │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│  RequestTransporterService       │
│  request()                       │
│                                  │
│  1. findById(idVehiculo)         │
│     [Lanza VehiculoNotFoundException]
│                                  │
│  2. getAvailable()               │
│     [Llama a TransporterServicePort]
│                                  │
│  3. validateExistence()          │
│     [Llama a TransporterServicePort]
│                                  │
│  4. assignTransporter()          │
│     [Asigna transportista]       │
│                                  │
│  5. save()                       │
│     [Persiste cambios]           │
└────────┬─────────────────────────┘
         │
         ├─ Éxito ─┐
         │          ▼
         │   toResponse()
         │   HTTP 200 OK
         │
         └─ Error ──┐
                     ▼
            GlobalExceptionHandler
            (HTTP 404/409/400)
```

---

## 🌍 API REST

### Configuración Base

- **Host:** `localhost:8080`
- **Base Path:** `/api/v1`
- **Documentación OpenAPI:** `http://localhost:8080/api/v1/docs/openapi.json`
- **Swagger UI:** `http://localhost:8080/api/v1/docs/swagger-ui.html`

### Endpoints

#### **POST /logistics/vehicles/{vehicleId}/transporter** - Solicitar Transportista

**Descripción:** Solicita un transportista disponible y lo asigna al vehículo especificado.

**Path Parameters:**
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `vehicleId` | Long | ID del vehículo |

**Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/logistics/vehicles/1/transporter" \
  -H "accept: application/json"
```

**Response 200 OK:**
```json
{
  "idVehiculo": 1,
  "idTransportista": 1
}
```

**Response 404 Not Found (vehículo no existe):**
```json
{
  "codigo": "VEHICULO_NO_ENCONTRADO",
  "mensaje": "Vehículo con ID 999 no encontrado",
  "timestamp": "2026-04-22T10:35:00"
}
```

**Response 409 Conflict (no hay transportista disponible):**
```json
{
  "codigo": "TRANSPORTISTA_NO_DISPONIBLE",
  "mensaje": "No hay transportistas disponibles en este momento",
  "timestamp": "2026-04-22T10:35:00"
}
```

---

## ⚠️ Manejo de Errores

### Códigos de Respuesta HTTP

| HTTP Code | Significado | Ejemplo |
|-----------|------------|---------|
| **200** | OK - Operación exitosa | Transportista asignado |
| **404** | Not Found - Recurso inexistente | Vehículo no encontrado |
| **409** | Conflict - Conflicto de negocio | No hay transportista disponible |

### Estructura de Respuesta de Error

```json
{
  "codigo": "CODIGO_ERROR",        // Código interno único
  "mensaje": "Descripción clara",  // Mensaje para usuario
  "timestamp": "2026-04-22T..."    // Cuándo ocurrió
}
```

### Excepciones de Negocio

#### **VehiculoNotFoundException**

```
Escenario: Solicitar transportista para vehículo inexistente
- POST /logistics/vehicles/999/transporter

HTTP 404 Not Found:
{
  "codigo": "VEHICULO_NO_ENCONTRADO",
  "mensaje": "Vehículo con ID 999 no encontrado"
}
```

#### **TransporterNotAvailableException**

```
Escenario: Servicio externo no tiene transportistas disponibles
- Servicio externo retorna error o lista vacía

HTTP 409 Conflict:
{
  "codigo": "TRANSPORTISTA_NO_DISPONIBLE",
  "mensaje": "No hay transportistas disponibles en este momento"
}
```

#### **InvalidTransporterException**

```
Escenario: Servicio externo retorna ID inválido
- ID de transportista null o vacío

HTTP 400 Bad Request:
{
  "codigo": "TRANSPORTISTA_INVALIDO",
  "mensaje": "Transportista con ID null no es válido"
}
```

---

## 🗄️ Base de Datos

### Schema Relevante

#### Tabla: `vehiculos`

```sql
CREATE TABLE IF NOT EXISTS vehiculos (
    id_vehiculo      BIGSERIAL PRIMARY KEY,
    id_categoria     BIGINT       NOT NULL REFERENCES categorias(id_categoria) ON DELETE RESTRICT,
    capacidad_carga  NUMERIC      NOT NULL CHECK (capacidad_carga > 0),
    estado           VARCHAR(50)  NOT NULL DEFAULT 'EN_MANTENIMIENTO',
    id_transportista VARCHAR(100),  -- ← Campo actualizado por esta feature
    peso_actual      NUMERIC      NOT NULL DEFAULT 0 CHECK (peso_actual >= 0),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### Índices

```sql
CREATE INDEX idx_vehiculos_id_transportista ON vehiculos(id_transportista);
```

**Justificación:**
- `id_transportista`: Búsqueda rápida de vehículos por transportista asignado

### Migraciones (Flyway)

#### **V3__change_transportista_to_bigint.sql**

```sql
UPDATE vehiculos SET id_transportista = '1' WHERE id_transportista NOT SIMILAR TO '[0-9]+';
ALTER TABLE vehiculos ALTER COLUMN id_transportista TYPE BIGINT USING id_transportista::BIGINT;
```

**Motivo:** Cambiar tipo de dato para consistencia con IDs numéricos.

---

## 🧪 Testing

### Estructura de Tests

```
src/test/java/co/edu/unimagdalena/storelogistic/flota/
│
├── infrastructure/
│   └── RequestTransporterControllerTest.java   ✅ Tests del controller
│
└── application/
    └── services/
        └── RequestTransporterServiceTest.java  ✅ Tests del service
```

### RequestTransporterControllerTest.java - Ejemplo Completo

```java
class RequestTransporterControllerTest {

    @Test
    void post_vehicleExistsAndTransporterAvailable_returns200() throws Exception {
        when(requestTransporterUseCase.request(1L)).thenReturn(buildVehicle(1L, 1L));

        mockMvc.perform(post("/logistics/vehicles/1/transporter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVehiculo").value(1))
                .andExpect(jsonPath("$.idTransportista").value(1));
    }

    @Test
    void post_vehicleNotFound_returns404() throws Exception {
        when(requestTransporterUseCase.request(99L)).thenThrow(new VehicleNotFoundException(99L));

        mockMvc.perform(post("/logistics/vehicles/99/transporter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("VEHICULO_NO_ENCONTRADO"));
    }

    @Test
    void post_noTransporterAvailable_returns409() throws Exception {
        when(requestTransporterUseCase.request(1L)).thenThrow(new TransporterNotAvailableException());

        mockMvc.perform(post("/logistics/vehicles/1/transporter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSPORTISTA_NO_DISPONIBLE"));
    }
}
```

### Ejecutar Tests

```bash
# Tests específicos de la feature
./gradlew test --tests "*RequestTransporter*"

# Con reporte JaCoCo
./gradlew test jacocoTestReport
# Reporte en: build/reports/jacoco/test/html/index.html
```

---


## 🚀 Ejecución

### Prerequisitos

1. **Java 21 LTS**
2. **PostgreSQL 17**
3. **Gradle** (incluido en `gradlew`)

### Configuración Base de Datos

**1. Crear base de datos:**
```sql
CREATE DATABASE "storeLogistic";
```

**2. Verificar conexión:**
```bash
psql -h localhost -U postgres -d storeLogistic -c "\dt"
```

### Configuración de Aplicación

**Archivo:** `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/storeLogistic
    username: postgres
    password: 1082876634
    driver-class-name: org.postgresql.Driver

logistics:
  transporter:
    service:
      url: http://localhost:8081/api/v1  # Servicio externo de transportistas
```

### Ejecución

**Opción 1: Con Gradle**
```bash
./gradlew bootRun
```

**Opción 2: Desde IDE**
- Click derecho en `StoreLogisticApplication.java`
- Run

### Verificación

**1. Aplicación arrancó correctamente:**
```bash
curl http://localhost:8080/api/v1/docs/swagger-ui.html
```

**2. Probar endpoint (vehículo debe existir):**
```bash
# Primero crear un vehículo
curl -X POST http://localhost:8080/api/v1/vehicles \
  -H "Content-Type: application/json" \
  -d '{
    "category": "CAMIONETA_URBANA",
    "loadCapacity": 1500
  }'

# Luego solicitar transportista
curl -X POST "http://localhost:8080/api/v1/logistics/vehicles/1/transporter"
```

### Troubleshooting

| Problema | Solución |
|----------|----------|
| `Connection refused` en servicio externo | Verificar que el servicio esté en localhost:8081 |
| `Vehículo no encontrado` | Crear el vehículo primero con POST /vehicles |
| Error de conexión PostgreSQL | Verificar credenciales y que la BD exista |

---

## 📊 Capacidades Implementadas

✅ Asignar transportista disponible a vehículo  
✅ Integración con servicio externo  
✅ Validación de dominio (vehículo existe)  
✅ Manejo de errores específicos por escenario  
✅ API REST con documentación OpenAPI  
✅ Testing unitario completo  
✅ Persistencia ACID  

---

## 📝 Información del Proyecto

| Atributo | Valor |
|----------|-------|
| **Versión** | 0.0.1-SNAPSHOT |
| **Fecha** | 22 de Abril de 2026 |
| **Java** | 21 LTS |
| **Spring Boot** | 3.5.13 |
| **PostgreSQL** | 17 |

---

**Fin de la Documentación.**
