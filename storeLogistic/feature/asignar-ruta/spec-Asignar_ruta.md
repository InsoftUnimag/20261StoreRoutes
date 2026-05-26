# Feature Specification: Asignar Ruta a Pedido (Proceso Interno - Módulo 2: Logística)

## User Story 

### Asignación de Pedidos a Rutas por Capacidad de Carga (Priority: P1)

Como sistema interno del módulo de Logística, debo asignar cada pedido a una ruta disponible (o crear una nueva) verificando que la carga no exceda la capacidad del vehículo y respetando la consolidación al 95% de capacidad antes del despacho.

**Why this priority**: Es el proceso fundamental que permite ejecutar entregas. Sin asignación correcta de pedidos a rutas, no hay entregas.

**Independent Test**: Puede probarse de forma independiente cuando se ingresa un pedido con pesoTotal y se verifica que: (1) Se asigna a una ruta existente si tiene capacidad, (2) Se crea una nueva ruta si no hay disponibilidad, (3) Se cierra la ruta al alcanzar 95% de capacidad.

**Acceptance Scenarios**:

1. **Scenario**: Asignar pedido a ruta existente con capacidad disponible
   - **Given** existe una ruta con estado "disponible" y capacidad suficiente para el pesoTotal
   - **When** se procesa la solicitud de asignación del pedido
   - **Then** se asigna el pedido a la ruta existente y se actualiza el peso acumulado
   
2. **Scenario**: Crear nueva ruta cuando no hay capacidad disponible
   - **Given** no existe ruta con capacidad disponible para el pesoTotal
   - **When** se procesa la solicitud de asignación del pedido
   - **Then** se crea una nueva ruta, se asigna el vehículo más eficiente, se asigna el pedido y se registra la fecha de despacho
   
3. **Scenario**: Cerrar ruta al alcanzar 95% de capacidad
   - **Given** se asigna un pedido que causa que la ruta alcance o supere el 95% de su capacidad total
   - **When** se completa la asignación del pedido
   - **Then** se marca la ruta como "cerrada" para no aceptar más pedidos

4. **Scenario**: Ruta queda PENDING_VEHICLE cuando no hay vehículo disponible
   - **Given** no existe vehículo con capacidad disponible para el pesoTotal del pedido
   - **When** se procesa la asignación del pedido (creando nueva ruta)
   - **Then** la ruta se crea con estado "PENDING_VEHICLE", sin vehículo asignado

5. **Scenario**: Vehículo cambia automáticamente a EN_RUTA al cerrar ruta
   - **Given** una ruta con vehículo asignado alcanza ≥95% de capacidad
   - **When** se marca la ruta como "cerrada"
   - **Then** el vehículo asociado cambia automáticamente de estado a "EN_RUTA"

---

### Edge Cases

- **¿Qué sucede si el pesoTotal excede la capacidad máxima de cualquier vehículo?** Se crea nueva ruta con el vehículo de mayor capacidad; si aún así excede, se genera error.

- **¿Qué sucede si el pesoTotal es exactamente igual a la capacidad restante?** Se asigna el pedido a esa ruta, se marca como cerrada (alcanzó 100%).

- **¿Qué sucede si no hay vehículos disponibles en el momento?** Se crea la ruta como pendiente (esperando disponibilidad de vehículo).

- **¿Qué sucede si múltiples pedidos llegan simultáneamente?** Deben procesarse secuencialmente para mantener integridad de cálculos de capacidad.

## Requirements 

### Functional Requirements

- **FR-001**: System MUST [validar que pesoTotal + peso acumulado de la ruta ≤ capacidad del vehículo antes de asignar.]

- **FR-002**: System MUST [asignar el pedido a una ruta existente disponible que tenga capacidad suficiente, priorizando la que mayor porcentaje de uso alcance.]

- **FR-003**: System MUST [crear una nueva ruta al asignar un pedido cuando no exista ruta con capacidad disponible. Si hay vehículo con capacidad suficiente, se asigna automáticamente. Si no, la ruta queda en estado PENDING_VEHICLE y se asigna vehículo posteriormente via `AssignVehicleToPendingRoutesService`.]

- **FR-004**: System MUST [seleccionar el tipo de vehículo basado en pesoTotal, siguiendo la clasificación: Camioneta (hasta 1.5 ton), Camión Sencillo (hasta 5 ton), Tractocamión Regional (>25 ton).]

- **FR-005**: System MUST [marcar la ruta como "cerrada" cuando alcance o supere el 95% de su capacidad total.]

- **FR-006**: System MUST [registrar la fecha de despacho para la ruta (fecha actual o próxima ruta disponible).]

- **FR-007**: System MUST [cambiar el estado del vehículo a EN_RUTA automáticamente cuando la ruta se cierra al alcanzar ≥95% de capacidad, invocando `ChangeVehicleStatusUseCase` desde `AssignOrderService`.]


### Key Entities

- **[Ruta]**: Conjunto de paradas y pedidos asignados a un vehículo. Atributos: idRuta, lista de paradas, idVehículo, capacidad_total, peso_acumulado, estado (disponible/cerrada), fecha_despacho.

- **[Parada]**: Punto de entrega dentro de una ruta. Atributos: idParada, idRuta (FK), idPedido (FK), secuencia, dirección_entrega, estado (pendiente/entregado/rechazado), fecha_entrega.

- **[Vehículo]**: Medio de transporte. Atributos: idVehículo, tipo, capacidad_carga, estado, idTransportista.

- **[Pedido]**: Entrega a realizar. Atributos: idPedido, pesoTotal, dirección_entrega.

## Success Criteria 

### Measurable Outcomes

- **SC-001**: Precisión de asignación. El 100% de los pedidos asignados deben cumplir:
  * Peso no exceder capacidad del vehículo
  * Estar asociados a una ruta con estado válido
  * Tener vehículo disponible (o asignado correctamente)

- **SC-002**: Eficiencia de capacidad. Al menos el 95% de las rutas despachadas deben cumplir con ≥95% de ocupación antes de despacho.

- **SC-003**: Creación automática de rutas. El 100% de pedidos sin ruta disponible deben generar automáticamente una nueva ruta (con vehículo si hay disponible, o PENDING_VEHICLE si no).

- **SC-004**: Vehículo en ruta. El 100% de los vehículos asociados a rutas cerradas deben tener estado EN_RUTA.

