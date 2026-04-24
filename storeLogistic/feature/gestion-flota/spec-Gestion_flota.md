# Feature Specification: Gestión de Flota

## User Story 

### Administración de Flota y Gestión de Vehículos (Priority: P1)

Como supervizor de flota, necesito visualizar, filtrar, registrar y cambiar el estado de vehículos para administrar eficientemente la flota disponible.

**Why this priority**: Es fundamental tener control completo sobre la flota para asegurar disponibilidad de vehículos para entregas y mantenimiento.

**Independent Test**: Puede probarse de forma independiente cuando un supervizor: (1) Visualiza todos los vehículos, (2) Filtra por categoría y estado, (3) Registra un nuevo vehículo, (4) Cambia el estado de un vehículo, (5) Consulta el historial de cambios.

**Acceptance Scenarios**:

1. **Scenario**: Visualizar listado completo de vehículos
   - **Given** un supervizor logueado en el sistema
   - **When** accede a la sección de gestión de flota
   - **Then** visualiza todos los vehículos con: IdVehículo,IdTransportista, Categoría, CapacidadCarga, Estado, PesoActual, % ocupación.
   
2. **Scenario**: Registrar nuevo vehículo
   - **Given** un supervizor con permisos de administración
   - **When** completa el formulario con: Categoría, CapacidadCarga, IdTransportista
   - **Then** se asigna IdVehículo automático, estado inicial "en mantenimiento", y se registra en auditoría
   
3. **Scenario**: Cambiar estado de vehículo
   - **Given** existe un vehículo registrado
   - **When** el supervizor cambia a un estado válido (ej: en mantenimiento → disponible)
   - **Then** el cambio se persiste inmediatamente

### Edge Cases

- **¿Qué sucede si se intenta transicionar a un estado inválido?** Se rechaza con HTTP 409 y descripción clara de la restricción.

- **¿Qué sucede si múltiples supervisores cambian estado simultáneamente?** Se procesan secuencialmente garantizando integridad de auditoría.

## Requirements 

### Functional Requirements

- **FR-001**: System MUST visualizar el estado de todos los vehículos con: IdVehículo, Categoría, CapacidadCarga, Estado, PesoActual, % ocupación en menos de 2 segundos.

- **FR-002**: System MUST categorizar vehículos en tres tipos: Camioneta Urbana (≤1.5t), Camión Sencillo (≤5t), Tractocamión Regional (>25t).

- **FR-003**: System MUST permitir filtrar vehículos por Categoría, Capacidad (rango), y Estado, combinables con AND lógico.

- **FR-004**: System MUST permitir registrar nuevo vehículo con: Categoría, CapacidadCarga, IdTransportista. Retornar IdVehículo auto-generado con estado inicial "en mantenimiento".

- **FR-005**: System MUST permitir cambiar estado entre transiciones válidas (en mantenimiento <-> disponible, disponible <-> en ruta, cualquier -> fuera de servicio, fuera de servicio <-> en mantenimiento).

### Key Entities

- **Categoría**: Clasificación de vehículos. Atributos: IdCategoría (PK), Nombre (Camioneta Urbana | Camión Sencillo | Tractocamión Regional), CapacidadMaxima (kg).

- **Vehículo**: Medio de transporte de carga. Atributos: IdVehículo (PK, auto-generado), IdCategoría (FK), CapacidadCarga (kg), Estado (disponible | en ruta | en mantenimiento | fuera de servicio), IdTransportista (FK), PesoActual (kg), CreatedAt (DATETIME).

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El 100% de vehículos visualizados en < 2 segundos incluso con 1000+ registros.

- **SC-002**: El 100% de filtros por Categoría, Capacidad, Estado funcionan correctamente, combinables con AND.

- **SC-003**: El 100% de registros de vehículos generan IdVehículo único auto-generado con estado inicial "en mantenimiento" y se persisten en <500ms.

- **SC-004**: El 100% de cambios de estado válidos se persisten en <100ms. El 100% de transiciones inválidas retornan HTTP 409 con descripción clara.

- **SC-005**: El 100% de cambios de estado se persisten en <100ms.
