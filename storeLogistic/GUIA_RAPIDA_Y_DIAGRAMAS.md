# Guía Rápida & Diagramas Visuales - StoreLogistic Flota

## 📊 Diagrama de Arquitectura Hexagonal

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                          CAPA EXTERNA - HTTP                        ┃
┃  ┌───────────────────────────────────────────────────────────┐     ┃
┃  │  FlotaController  POST/GET/PATCH /vehiculos              │     ┃
┃  └──────────────────────┬──────────────────────────────────┘      ┃
┃                         │                                          ┃
┃                    DTO ↕ Domain                                    ┃
┃                         │                                          ┃
┃  ┌──────────────────────▼──────────────────────────────────┐      ┃
┃  │           VehiculoMapper (Conversión)                   │      ┃
┃  └──────────────────────┬──────────────────────────────────┘      ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                           │
┏━━━━━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  CAPA DE APLICACIÓN      │                                        ┃
┃  ┌──────────────────────▼──────────────────────────────────┐     ┃
┃  │                                                           │     ┃
┃  │  RegistrarVehiculoService ─────┐                        │     ┃
┃  │  ListarVehiculosService        │                        │     ┃
┃  │  ObtenerVehiculoService        ├─ Implementan            │     ┃
┃  │  CambiarEstadoVehiculoService ─┤  Use Cases              │     ┃
┃  │                                 │  (Puertos IN)          │     ┃
┃  │                                 │                        │     ┃
┃  └───────────────────┬─────────────┴────────────────────────┘     ┃
┃                      │                                             ┃
┃                      │ Llamadas a Repositorios                     ┃
┃                      │ (Puertos OUT)                               ┃
┗━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                       │
┏━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  CAPA DE DOMINIO     │                                            ┃
┃  ┌──────────────────▼───────────────────────────────────┐        ┃
┃  │  PUERTOS (Interfaces):                                │        ┃
┃  │  • RegistrarVehiculoUseCase (IN)                       │        ┃
┃  │  • ListarVehiculosUseCase (IN)                         │        ┃
┃  │  • ObtenerVehiculoUseCase (IN)                         │        ┃
┃  │  • CambiarEstadoVehiculoUseCase (IN)                   │        ┃
┃  │  • VehiculoRepository (OUT)                            │        ┃
┃  │  • CategoriaRepository (OUT)                           │        ┃
┃  └──────────────────┬───────────────────────────────────┘        ┃
┃  ┌──────────────────▼───────────────────────────────────┐        ┃
┃  │  MODELOS DE DOMINIO:                                  │        ┃
┃  │  • Vehiculo (Aggregate Root)                          │        ┃
┃  │  • Categoria (Entity)                                 │        ┃
┃  │  • EstadoVehiculo (Value Object / Enum)               │        ┃
┃  │  • CapacidadCarga (Value Object)                       │        ┃
┃  │  • TipoCategoria (Value Object / Enum)                 │        ┃
┃  │  • FiltroVehiculo (Value Object)                       │        ┃
┃  └──────────────────────────────────────────────────────┘        ┃
┃  ┌──────────────────────────────────────────────────────┐        ┃
┃  │  EXCEPCIONES DE DOMINIO:                              │        ┃
┃  │  • FlotaException (Base)                              │        ┃
┃  │  • VehiculoNotFoundException                           │        ┃
┃  │  • TransicionEstadoInvalidaException                   │        ┃
┃  └──────────────────────────────────────────────────────┘        ┃
┗━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                       │
┏━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  CAPA DE INFRAESTRUCTURA │                                        ┃
┃  ┌──────────────────────▼──────────────────────────────┐        ┃
┃  │  ADAPTADOR DE PERSISTENCIA:                          │        ┃
┃  │  VehiculoRepositoryAdapter ◄─┐                      │        ┃
┃  │                               │ Implementa          │        ┃
┃  │  CategoriaRepositoryAdapter ◄─┤ Puertos OUT         │        ┃
┃  │                               │                     │        ┃
┃  │                               └─ (VehiculoRepository) │       ┃
┃  └──────────────────┬─────────────────────────────────┘        ┃
┃  ┌──────────────────▼─────────────────────────────────┐        ┃
┃  │  SPRING DATA JPA:                                   │        ┃
┃  │  • VehiculoSpringRepository (extends JpaRepository) │        ┃
┃  │  • CategoriaSpringRepository                         │        ┃
┃  └──────────────────┬─────────────────────────────────┘        ┃
┃  ┌──────────────────▼─────────────────────────────────┐        ┃
┃  │  ENTIDADES JPA:                                     │        ┃
┃  │  • VehiculoJpaEntity                                │        ┃
┃  │  • CategoriaJpaEntity                               │        ┃
┃  └──────────────────┬─────────────────────────────────┘        ┃
┃  ┌──────────────────▼─────────────────────────────────┐        ┃
┃  │  MANEJADOR DE EXCEPCIONES:                          │        ┃
┃  │  GlobalExceptionHandler → ErrorResponse             │        ┃
┃  └────────────────────────────────────────────────────┘        ┃
┗━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                       │
                       ▼
              ┌─────────────────┐
              │   PostgreSQL    │
              │   Base de Datos │
              └─────────────────┘
```

---

## 🔄 Máquina de Estados de Vehículos

```
                    ┌─────────────────────────────────────────┐
                    │   EN_MANTENIMIENTO (Estado Inicial)    │
                    │   Vehículo: registrarNuevo()           │
                    └─────┬──────────────┬────────────────────┘
                          │              │
                   [válida]│              │[válida]
                          │              │
                          ▼              ▼
                    ┌──────────────┐  ┌──────────────────┐
                    │ DISPONIBLE   │  │ FUERA_DE_SERVICIO│
                    │ (listo para  │  │ (requiere rep.   │
                    │  entregas)   │  │  eventual)       │
                    └──────┬───────┘  └──────────┬───────┘
                           │                     │
                   ┌───────┼──────┐              │
                   │       │      │              │
                [v]│       │[v]   │[v]           │[v]
                   │       │      │              │
                   ▼       │      ▼              │
            ┌──────────────────┐                 │
            │    EN_RUTA       │                 │
            │ (transportando   │                 │
            │  carga actual)   │                 │
            └──────────────────┘                 │
                   │                             │
                [v]│    [v]                      │
                   ▼     │                       │
                   ├─────┘                       │
                   │                             │
            disponible            disponible     │
                   │              para man.      │
                   ▼                ▼            │
            DISPONIBLE ─────► EN_MANTENIMIENTO   │
                                    │            │
                                    └────────────┴─────► FUERA_DE_SERVICIO
                                         [v]

Leyenda:
  [v] = Transición válida permitida
  [✗] = Transición NO permitida (lanza excepción HTTP 409)
  (   ) = Descripción del estado
```

**Tabla de Transiciones:**

```
Desde ▼ / Hacia ► │ DISP │ RUTA │ MANT │ FUERA
─────────────────┼──────┼──────┼──────┼─────
DISPONIBLE        │  ✗   │  ✓   │  ✓   │  ✓
EN_RUTA           │  ✓   │  ✗   │  ✗   │  ✓
EN_MANTENIMIENTO  │  ✓   │  ✗   │  ✗   │  ✓
FUERA_DE_SERVICIO │  ✗   │  ✗   │  ✓   │  ✗
```

---

## 📚 Estructura de Clases Principales

### Flujo de Datos: Registro de Vehículo

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. HTTP REQUEST                                                 │
│    POST /api/v1/vehiculos                                       │
│    Body: { "categoria": "CAMIONETA_URBANA", ... }               │
└──────────────────┬────────────────────────────────────────────┘
                   │
┌──────────────────▼────────────────────────────────────────────┐
│ 2. FlotaController.registrarVehiculo()                         │
│    • Recibe RegistrarVehiculoRequest (DTO)                     │
│    • Valida @Valid                                             │
│    • Convierte a tipos de Dominio:                             │
│      - TipoCategoria.valueOf(request.getCategoria())           │
│      - new CapacidadCarga(BigDecimal.valueOf(...))             │
└──────────────────┬────────────────────────────────────────────┘
                   │
┌──────────────────▼────────────────────────────────────────────┐
│ 3. RegistrarVehiculoService.registrar()                        │
│    • Busca Categoria por TipoCategoria                         │
│    • Llama Vehiculo.registrarNuevo() (Factory Method)          │
│    • Retorna Vehiculo con estado EN_MANTENIMIENTO             │
│    • Llama vehiculoRepository.save()                           │
└──────────────────┬────────────────────────────────────────────┘
                   │
┌──────────────────▼────────────────────────────────────────────┐
│ 4. VehiculoRepositoryAdapter.save()                            │
│    • Convierte Vehiculo (dominio) → VehiculoJpaEntity (ORM)    │
│    • Extrae valor primitivo: vehiculo.getCapacidadCarga()      │
│                              .getPesoKg()                       │
│    • Extrae string: vehiculo.getEstado().toString()            │
│    • Llama springRepository.save()                             │
└──────────────────┬────────────────────────────────────────────┘
                   │
┌──────────────────▼────────────────────────────────────────────┐
│ 5. VehiculoSpringRepository.save() (Spring Data JPA)           │
│    • Ejecuta SQL: INSERT INTO vehiculos (...)                  │
│    • Retorna VehiculoJpaEntity persistida con ID generado      │
└──────────────────┬────────────────────────────────────────────┘
                   │
┌──────────────────▼────────────────────────────────────────────┐
│ 6. VehiculoRepositoryAdapter.toDomainModel()                   │
│    • Convierte VehiculoJpaEntity → Vehiculo (dominio)          │
│    • Crea: new CapacidadCarga(entity.getCapacidadCarga())      │
│    • Crea: EstadoVehiculo.valueOf(entity.getEstado())          │
└──────────────────┬────────────────────────────────────────────┘
                   │
┌──────────────────▼────────────────────────────────────────────┐
│ 7. VehiculoMapper.toRegistrarVehiculoResponse()                │
│    • Convierte Vehiculo → RegistrarVehiculoResponse (DTO)      │
│    • Incluye: idVehiculo, estado, createdAt                    │
└──────────────────┬────────────────────────────────────────────┘
                   │
└──────────────────▼────────────────────────────────────────────┐
  8. HTTP RESPONSE 201 CREATED
     Body: { 
       "idVehiculo": 1,
       "estado": "EN_MANTENIMIENTO", 
       "createdAt": "2026-04-16T..."
     }
┗─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Referencia Rápida: Casos de Uso

### Use Case 1: Registrar Nuevo Vehículo

```
REQUISITO: Transportista agrega vehículo a flota

ENTRADA:
  • TipoCategoria (CAMIONETA_URBANA | CAMION_SENCILLO | TRACTOCAMION_REGIONAL)
  • Capacidad (kg)
  • ID Transportista

VALIDACIONES:
  ✓ Categoría existe en tabla categorias
  ✓ Capacidad > 0 (validación Value Object)
  ✓ ID Transportista no vacío

PROCESO:
  1. Buscar Categoria por TipoCategoria
  2. Crear Vehiculo.registrarNuevo() → estado = EN_MANTENIMIENTO
  3. Persistir mediante VehiculoRepository.save()

SALIDA:
  • Vehiculo (con ID auto-generado)
  • HTTP 201 Created

EXCEPCIONES:
  • IllegalArgumentException (categoría no existe) → HTTP 400
  • MethodArgumentNotValidException (validación DTO) → HTTP 400
```

### Use Case 2: Listar Vehículos con Filtros

```
REQUISITO: Supervisor visualiza flota con filtros opcionales

ENTRADA:
  • categoria? (string enum)
  • estado? (string enum)
  • capacidadMin? (double)
  • capacidadMax? (double)

VALIDACIONES:
  ✓ Enums válidos (ignorar si inválido)
  ✓ capacidadMin > 0 si se proporciona

PROCESO:
  1. Convertir parámetros strings → Objetos dominio (FiltroVehiculo)
  2. Resolver TipoCategoria → ID categoría (consult tabla)
  3. Buscar SQL con cláusulas AND:
     - categoria_id = ? OR NULL
     - estado = ? OR NULL
     - capacidad_carga >= ? OR NULL
     - capacidad_carga <= ? OR NULL
  4. Mapear resultados → VehiculoDTO

SALIDA:
  • ListaVehiculosResponse { total, List<VehiculoDTO> }
  • Incluye: porcentajeOcupacion calculado
  • HTTP 200 OK

PERFORMANCE:
  • Índices en: estado, id_categoria, id_transportista
  • Target <2s para 1000+ registros
```

### Use Case 3: Obtener Vehículo Específico

```
REQUISITO: Consultar detalle completo de vehículo

ENTRADA:
  • idVehiculo (Long, path parameter)

VALIDACIONES:
  ✓ ID es número válido

PROCESO:
  1. VehiculoRepository.findById()
  2. Si existe: mapear a VehiculoDetailResponse
  3. Si no existe: lanzar VehiculoNotFoundException

SALIDA:
  • VehiculoDetailResponse
  • HTTP 200 OK

ERRORES:
  • VehiculoNotFoundException → HTTP 404 Not Found
```

### Use Case 4: Cambiar Estado

```
REQUISITO: Transportista actualiza estado de vehículo

ENTRADA:
  • idVehiculo (Long)
  • nuevoEstado (string enum)

VALIDACIONES:
  ✓ Vehículo existe (VehiculoNotFoundException si no)
  ✓ Transición es válida (máquina de estados)

PROCESO:
  1. @Transactional(isolation = SERIALIZABLE)
  2. findById(idVehiculo)
  3. vehiculo.cambiarEstado(nuevoEstado)
     ↳ Valida: estadoActual.esTransicionValida(nuevoEstado)
     ↳ Si inválida: lanza TransicionEstadoInvalidaException
  4. save(vehiculo)
  5. Retorna Vehiculo actualizado

SALIDA:
  • VehiculoDTO
  • HTTP 200 OK

ERRORES:
  • VehiculoNotFoundException (ID no existe) → HTTP 404
  • TransicionEstadoInvalidaException (cambio prohibido) → HTTP 409
```

---

## 🧬 Diagrama de Objetos

### Domain Model: Vehiculo

```java
Vehiculo {
    ├─ idVehiculo: Long              // PK
    ├─ idCategoria: Long             // FK
    ├─ capacidadCarga: CapacidadCarga {
    │   └─ pesoKg: BigDecimal (>0)
    ├─ estado: EstadoVehiculo        // DISPONIBLE, EN_RUTA, EN_MANT, FUERA
    ├─ idTransportista: String       // No vacío
    ├─ pesoActual: BigDecimal        // ≥0
    ├─ createdAt: LocalDateTime      // Auditoría
    ├─ updatedAt: LocalDateTime      // Auditoría
    └─ Métodos:
       ├─ porcentajeOcupacion(): BigDecimal
       ├─ registrarNuevo(...): Vehiculo (factory)
       └─ cambiarEstado(estado): void (valida)
}

EstadoVehiculo {
    ├─ DISPONIBLE
    ├─ EN_RUTA
    ├─ EN_MANTENIMIENTO
    └─ FUERA_DE_SERVICIO
    
    Métodos:
    ├─ esTransicionValida(dest): boolean
    └─ obtenerMensajeTransicionInvalida(dest): String
}

CapacidadCarga (Value Object) {
    ├─ pesoKg: BigDecimal (final, >0)
    └─ Métodos:
       ├─ getPesoKg(): BigDecimal
       ├─ equals(o): boolean
       ├─ hashCode(): int
       └─ toString(): String
}
```

---

## 📈 Diagrama ER (Entidad-Relación)

```
┌──────────────────────────┐
│       categorias         │
├──────────────────────────┤
│ ★ id_categoria (BIGSERIAL)│ PK
│   tipo (VARCHAR 50)      │ UNIQUE
│   capacidad_maxima_kg    │ NUMERIC > 0
│   created_at             │ TIMESTAMP
└──────────┬───────────────┘
           │ FK
           │ 1
           │
           ▼
        1:N
           │
           │ (1 categoría : N vehículos)
           │
┌──────────────────────────────┐
│       vehiculos              │
├──────────────────────────────┤
│ ★ id_vehiculo (BIGSERIAL)    │ PK
│ ★ id_categoria (BIGINT)      │ FK → categorias
│   capacidad_carga (NUMERIC)  │ >0
│   estado (VARCHAR 50)        │ DEFAULT EN_MANT
│   id_transportista (VARCHAR) │
│   peso_actual (NUMERIC)      │ ≥0, DEFAULT 0
│   created_at (TIMESTAMP)     │
│   updated_at (TIMESTAMP)     │
└──────────────────────────────┘

ÍNDICES:
├─ idx_vehiculos_estado
├─ idx_vehiculos_id_categoria
└─ idx_vehiculos_id_transportista

CONSTRAINTS:
├─ PK: id_vehiculo
├─ FK: id_categoria → categorias.id_categoria (RESTRICT)
├─ CHECK: capacidad_carga > 0
└─ CHECK: peso_actual >= 0
```

---

## 🔗 Mapeo de Conversiones

### Transformaciones de Datos

```
HTTP Request
     │
     ▼
RegistrarVehiculoRequest (DTO)
│ categoria: String = "CAMIONETA_URBANA"
│ capacidadCarga: Double = 1500
│ idTransportista: String = "TRANS-001"
     │
     ├─ VehiculoMapper.toFiltroVehiculo()
     │
     ▼
FiltroVehiculo (Value Object)
│ categoria: TipoCategoria = CAMIONETA_URBANA
│ capacidadCarga: CapacidadCarga = 1500 kg
│ idTransportista: String = "TRANS-001"
     │
     ├─ Servicio de Aplicación
     │
     ▼
Vehiculo (Domain Model)
│ idVehiculo: Long = null (pre-persist)
│ idCategoria: Long = 1
│ capacidadCarga: CapacidadCarga = 1500 kg
│ estado: EstadoVehiculo = EN_MANTENIMIENTO
│ idTransportista: String = "TRANS-001"
│ pesoActual: BigDecimal = 0
│ createdAt: LocalDateTime = now()
     │
     ├─ VehiculoRepositoryAdapter.save()
     │
     ▼
VehiculoJpaEntity (JPA Entity)
│ idVehiculo: Long = null
│ idCategoria: Long = 1
│ capacidadCarga: BigDecimal = 1500
│ estado: String = "EN_MANTENIMIENTO"
│ idTransportista: String = "TRANS-001"
│ pesoActual: BigDecimal = 0
│ createdAt: LocalDateTime = now()
     │
     ├─ Hibernate/JPA
     │
     ▼
DATABASE (INSERT vehiculos)
│ id_vehiculo: 1 (auto-generated)
│ id_categoria: 1
│ capacidad_carga: 1500
│ estado: 'EN_MANTENIMIENTO'
│ id_transportista: 'TRANS-001'
│ peso_actual: 0
│ created_at: 2026-04-16 ...
     │
     ├─ Resultado de query
     │
     ▼
VehiculoJpaEntity (con idVehiculo = 1)
     │
     ├─ VehiculoRepositoryAdapter.toDomainModel()
     │
     ▼
Vehiculo (Domain Model, persistido)
     │
     ├─ VehiculoMapper.toRegistrarVehiculoResponse()
     │
     ▼
RegistrarVehiculoResponse (DTO)
│ idVehiculo: Long = 1
│ estado: String = "EN_MANTENIMIENTO"
│ createdAt: LocalDateTime = 2026-04-16 ...
     │
     ▼
HTTP Response 201 Created
{ "idVehiculo": 1, "estado": "EN_MANTENIMIENTO", ... }
```

---

## ⚙️ Configuración de Conexión

### application.yml

```yaml
spring:
  application:
    name: storeLogistic              # Nombre de app
  
  datasource:
    url: jdbc:postgresql://localhost:5432/storeLogistic
    username: postgres
    password: 1082876634             # ⚠️ NO versionear en prod
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate              # No modifica schema automático
    properties:
      hibernate:
        format_sql: true              # Pretty-print SQL
        jdbc:
          batch_size: 20              # Batch inserts
        order_inserts: true
        order_updates: true
    open-in-view: false               # Buena práctica
  
  flyway:
    locations: classpath:db/migration
    baselineOnMigrate: true

server:
  port: 8080
  servlet:
    context-path: /api/v1             # Base URL

springdoc:
  api-docs:
    path: /docs/openapi.json          # OpenAPI JSON
  swagger-ui:
    path: /docs/swagger-ui.html       # UI interactivo
    enabled: true
```

---

## 🚀 Checklist de Implementación

### ✅ Completado

- [x] Modelo de Dominio (Vehiculo, Categoria)
- [x] Value Objects (CapacidadCarga, EstadoVehiculo, TipoCategoria)
- [x] Máquina de Estados (validación transiciones)
- [x] Puertos de Entrada (4 Use Cases)
- [x] Puertos de Salida (Repositorios)
- [x] Servicios de Aplicación (4 implementaciones)
- [x] REST Controller (4 endpoints)
- [x] DTOs especializados (Request/Response)
- [x] Mapper conversión (DTO ↔ Domain)
- [x] JPA Entities (Vehiculo, Categoria)
- [x] Spring Data Repositories (custom queries)
- [x] Global Exception Handler
- [x] Migraciones Flyway (V1 + V2)
- [x] Índices de base de datos
- [x] Validaciones @Valid
- [x] Logging SLF4J
- [x] OpenAPI/Swagger
- [x] Tests unitarios (dominio)
- [x] Transaccionalidad SERIALIZABLE

### ✨ Mejoras Futuras

- [ ] Auditoría completa (quién cambió qué, cuándo)
- [ ] Paginación en listados
- [ ] Búsqueda full-text
- [ ] Eventos de dominio (cambios de estado)
- [ ] Event Sourcing (historial de cambios)
- [ ] Caché (Redis) para categorías
- [ ] Integración con servicio de transportistas
- [ ] WebSockets para notificaciones reales
- [ ] Tests de integración (testcontainers)
- [ ] Tests E2E (Selenium)

---

## 📞 Comandos Útiles

### Compilar y Ejecutar

```bash
# Compilar proyecto
./gradlew build

# Ejecutar aplicación
./gradlew bootRun

# Tests
./gradlew test

# Cobertura JaCoCo
./gradlew jacocoTestReport

# Clean
./gradlew clean
```

### Desarrollo

```bash
# Hot reload (devtools activado)
./gradlew bootRun

# Ejecutar test específico
./gradlew test --tests VehiculoTest

# Ver dependencias
./gradlew dependencies
```

### Base de Datos

```bash
# Conectar a BBDD
psql -h localhost -U postgres -d storeLogistic

# Listar tablas
\dt

# Ver estructura de vehiculos
\d vehiculos

# Query de prueba
SELECT * FROM vehiculos;
```

### API Requests

```bash
# Listar todos
curl http://localhost:8080/api/v1/vehiculos

# Listar con filtros
curl "http://localhost:8080/api/v1/vehiculos?estado=DISPONIBLE"

# Obtener uno
curl http://localhost:8080/api/v1/vehiculos/1

# Registrar
curl -X POST http://localhost:8080/api/v1/vehiculos \
  -H "Content-Type: application/json" \
  -d '{"categoria":"CAMIONETA_URBANA","capacidadCarga":1500,"idTransportista":"T1"}'

# Cambiar estado
curl -X PATCH http://localhost:8080/api/v1/vehiculos/1/estado \
  -H "Content-Type: application/json" \
  -d '{"nuevoEstado":"DISPONIBLE"}'
```

---

## 📊 Tabla Comparativa: DTOs

| DTO | Propósito | Campos | Uso |
|-----|-----------|--------|-----|
| **VehiculoDTO** | Lectura completa | 8 (id, transportista, categoría, capacidad, estado, peso, %, fecha) | GET /vehiculos |
| **VehiculoDetailResponse** | Lectura detalle | Mismo que DTO | GET /vehiculos/{id} |
| **RegistrarVehiculoRequest** | Crear | 3 (categoría, capacidad, transportista) | POST /vehiculos body |
| **RegistrarVehiculoResponse** | Respuesta creación | 3 (id, estado, fecha) | POST /vehiculos response |
| **FiltroVehiculoRequest** | Filtros opcionales | 4 (categoría, estado, capMin, capMax) | GET /vehiculos query |
| **ListaVehiculosResponse** | Lista con total | total + List<VehiculoDTO> | GET /vehiculos response |
| **CambiarEstadoRequest** | Cambiar estado | 1 (nuevoEstado) | PATCH /vehiculos/{id}/estado |

---

## 🔐 Manejo de Seguridad

### Validaciones Implementadas

```
INPUT ──┬─ @NotBlank (campos requeridos)
        ├─ @NotNull (objetos requeridos)
        ├─ @Min/@Max (rangos numéricos)
        ├─ Enum.valueOf() con manejo de error
        └─ Value Object validation (CapacidadCarga > 0)

PROCESSING ──┬─ Máquina de estados validada
             ├─ @Transactional SERIALIZABLE para cambios de estado
             └─ Logging de auditoría

OUTPUT ──┬─ GlobalExceptionHandler
         ├─ Mensajes seguros (sin stack trace en prod)
         └─ HTTP codes semánticos
```

### Variables Sensibles

⚠️ **NO VERSIONEAR:**
```yaml
spring:
  datasource:
    password: 1082876634  # ← Reemplazar con variable de entorno
```

**Solución (producción):**
```bash
export SPRING_DATASOURCE_PASSWORD=xxxxx
java -jar storeLogistic.jar
```

---

## 📖 Referencias de Código

### Patrones Implementados

| Patrón | Implementación | Ubicación |
|--------||---|
| **Hexagonal Architecture** | Puertos + Adaptadores | domain/, application/, infrastructure/ |
| **Domain-Driven Design** | Ubiquitous Language | Nombres de clases y métodos |
| **Value Objects** | Immutable + equals | CapacidadCarga, TipoCategoria |
| **State Machine** | Enum con reglas | EstadoVehiculo.esTransicionValida() |
| **Repository Pattern** | Abstracción persistencia | VehiculoRepository interface |
| **Service Locator** | Inyección de dependencias | @Autowired, @RequiredArgsConstructor |
| **DTO Pattern** | Mapeo Request/Response | VehiculoDTO, RegistrarVehiculoRequest |
| **Global Exception Handler** | @RestControllerAdvice | GlobalExceptionHandler |
| **Mapper Pattern** | Conversión de objetos | VehiculoMapper |
| **Builder Pattern** | Construcción fluida | Vehiculo.builder() |

---

**Documento compilado:** 16 de Abril de 2026  
**Versión:** 1.0

