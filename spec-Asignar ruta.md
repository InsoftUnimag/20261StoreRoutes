# Feature Specification: Asignar ruta

**Created**: [DATE]  

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.
  
  Assign priorities (   P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - [Brief Title] (Priority: P1)

Yo como sistema de asignacion de rutas organizo y verfica que la ruta cumpla los requisitos para ser despachado 

**Why this priority**: Permite la asignacion de ruta solicitadas

**Independent Test**: [Describe how this can be tested independently - e.g., "Can be fully tested by [specific action] and delivers [specific value]"]

**Acceptance Scenarios**:

1. **Scenario**: Asignar un pedido a la ruta disponible
   - **Given** ruta disponible y con capacidad disponible 
   - **When** cuando se solicta una ruta y no excede el peso de la ruta 
   - **Then** se asigna a la ruta disponible
   
2. **Scenario**: Asignar un pedido a una nueva ruta
   - **Given** ruta disponible y sin capacidad disponible 
   - **When** Cuando se solicite una ruta para el pedido
   - **Then** Se crea una nueva ruta y se le asigna el pedido

3. **Scenario**: Asignar un pedido sin rutas disponible
   - **Given** ruta no disponible y sin capacidad disponible 
   - **When** Cuando se solicite una ruta para el pedido
   - **Then** Se crea una nueva ruta y se le asigna el pedido



---


[Add more user stories as needed, each with an assigned priority]

### Edge Cases

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right edge cases.
-->
* ¿Que pasa si se se pierde un pedido o se daña en el proceso de transporte?
* ¿Que pasa si falla un vehiculo mientras esta en ruta
## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->
### Functional Requirements

- **FR-001**: System MUST [Crear nueva ruta, implica verificar la disponibilidad de vehiculos, verificar que la capacidad del vehiculo elegido sea la adecuada para el peso de pedido]
- **FR-002**: System MUST [El sistema DEBE crear una nueva ruta cuando no exista ninguna ruta activa disponible o cuando todas las rutas activas hayan superado su capacidad máxima.]  
- **FR-003**: System MUST [El sistema DEBE verificar el estado de cada ruta (Abierta, Lista para despacho, Despachada, Pendiente de vehículo) antes de asignar un pedido.]
- **FR-004**: System MUST [El sistema DEBE asignar automáticamente el vehículo disponible a las rutas Pendientes de vehículo en cuanto uno quede libre, respetando criterios de capacidad.]
- **FR-005**: System MUST [El sistema DEBE asignar el pedido a la ruta activa con mayor porcentaje de ocupación (consolidación de carga) que tenga capacidad suficiente.]
- **FR-006**: System MUST [verificar que el peso a asignar mas el peso acumulado d la ruta no exceda la capacidad del vehiculo]
- **FR-007**: System MUST [El sistema DEBE asignar el pedido a la ruta activa con mayor porcentaje de ocupación (consolidación de carga) que tenga capacidad suficiente]
- **FR-008**: System MUST [asignar un vehiculo con capacidad de peso adecuada a las rutas automsticamente]
- **FR-009**: System MUST [asignar el pedido a la ruta con mayor porcentaje que tenga la capacidad de carga suficiente]

### Key Entities *(include if feature involves data)*

- **[Ruta]**: [Una serie de paradas asignada a un vehiculo, Idruta, Lista de paradas, Lista de pedidos, Vehiculo asignado,    Fecha de despacho]
- **[Vehiculo]**: [Metodo de transporte con capacidad de carga especifica, IdVehiculo, Capacidad de carga, estado, conductor]
- **[Pedido]**:[Producto a entregar, IdPedido, Peso especifico]
## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable success criteria.
  These must be technology-agnostic and measurable.
-->

### Measurable Outcomes

- **SC-001**: [Precisión, e.g., "El 100% de los pedidos asignados deben cumplir:
* No superar la capacidad del vehículo.
* Estar asociados a una ruta con estado válido.
* Tener vehículo disponible al momento de la asignación."]

- **SC-002**: [Uso eficiente de capacidad, "Al menos el 95% de las rutas despachadas deben cumplir con el mínimo del 95% de ocupación antes del despacho."]
- **SC-003**: [Creación automática de rutas, "El 100% de los pedidos que no encuentren ruta disponible deben generar automáticamente una nueva ruta si existe vehículo disponible."]
- **SC-004**: [Integridad de datos, "0% de rutas creadas sin vehículo asignado (cuando haya disponibilidad)."]

