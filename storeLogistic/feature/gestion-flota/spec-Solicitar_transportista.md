# Feature Specification: Solicitar Transportista y Asignarlo a Vehículo

## User Story 

### Solicitud de Transportista para Asignación a Vehículo (Priority: P1)

Como módulo de Logística de Despacho y Distribución, solicito un transportista disponible al módulo de Transportista con el fin de asignarlo a un vehículo específico y garantizar que haya un conductor disponible para realizar las entregas.

**Why this priority**: La asignación correcta de transportistas a vehículos es fundamental para que las operaciones de distribución se ejecuten. Sin un transportista asignado, un vehículo no puede ser operativo.

**Independent Test**: Puede ser testeado completamente por un módulo de Logística que: (1) Solicita un transportista disponible al módulo de Transportista, (2) Selecciona un transportista de los disponibles, (3) Asigna el transportista al vehículo, (4) Valida que el vehículo tenga el idTransportista actualizado.

**Acceptance Scenarios**:

1. **Scenario**: Obtiene idTransportista disponible
   - **Given** un vehículo requiere asignación de transportista
   - **When** se solicita un idTransportista disponible al módulo de Transportista
   - **Then** se retorna el idTransportista de un transportista disponible

2. **Scenario**: Asigna idTransportista a vehículo exitosamente
   - **Given** un idTransportista válido obtenido del módulo de Transportista
   - **When** se asigna el idTransportista al vehículo
   - **Then** el vehículo actualiza su idTransportista correctamente

3. **Scenario**: No hay transportistas disponibles
   - **Given** no hay transportistas disponibles
   - **When** se solicita un idTransportista
   - **Then** el sistema retorna un error indicando que no hay transportistas disponibles

---

### Edge Cases

1. **¿Qué sucede si se intenta asignar un idTransportista inválido?**
   - El sistema debe retornar un error indicando que el idTransportista no existe.

2. **¿Qué sucede si el idTransportista ya está asignado a otro vehículo?**
   - El sistema debe validar disponibilidad del transportista antes de asignarlo.

3. **¿Qué sucede si se intenta reasignar un idTransportista a un vehículo diferente?**
   - El sistema debe permitir la reasignación actualizando el idTransportista del vehículo.

---

## Requirements 

### Functional Requirements

- **FR-001**: System MUST [solicitar un transportista disponible al módulo de Transportista vía HTTP `GET /transportistas/disponible` usando `RestClient` + `RetryTemplate` con backoff exponencial (máximo 3 intentos, 500ms inicial, multiplicador 2.0).]

- **FR-002**: System MUST [filtrar la respuesta del módulo de Transportista (`TransporterAvailableClientDTO`) para retornar solo transportistas con estado `"DISPONIBLE"`. Si no hay disponibles, lanzar `TransporterNotAvailableException`.]

- **FR-003**: System MUST [validar existencia de un transportista vía HTTP `GET /transportistas/{id}` antes de asignarlo al vehículo. Si el módulo retorna 404, lanzar `InvalidTransporterException`.]

- **FR-004**: System MUST [asignar el idTransportista al vehículo actualizando el campo idTransportista del vehículo.]

- **FR-005**: System MUST [reintentar la comunicación con el módulo de Transportista hasta 3 veces con backoff exponencial en caso de timeout o error transitorio. Si tras 3 intentos el módulo no responde, lanzar `LogisticsException("El módulo de Transportista no está disponible.")`.]

- **FR-006**: System MUST [configurar `RestClient` con timeout de conexión y lectura (default 3s) via `TransporterRestClientConfig`, y `RetryTemplate` via `TransporterRetryConfig` con política de no reintentar errores `TransporterNotAvailableException` ni `InvalidTransporterException`.]

### Key Entities 

- **Vehículo**: Método de transporte, incluye: IdVehículo, Tipo, Estado, idTransportista (referencia al transportista asignado).

- **idTransportista**: Identificador único del transportista asignado al vehículo, proporcionado por el módulo de Transportista.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El módulo de Logística puede solicitar y recibir un idTransportista disponible en menos de 1 segundo.

- **SC-002**: El 100% de las asignaciones de idTransportista a vehículo se registran correctamente.

- **SC-003**: El sistema valida y rechaza el 100% de intentos de asignación con idTransportista inválidos o no disponibles.

- **SC-004**: El sistema proporciona mensajes de error claros cuando no hay transportistas disponibles o el idTransportista es inválido.
