# Feature Specification: Gestión de Flota

## User Story 

### Administración y Monitoreo de Flota en Tiempo Real (Priority: P1)

Como supervizor de flota, necesito administrar la información de vehículos disponibles y monitorear el estado en tiempo real de la flota despachada para asegurar que las entregas se ejecuten de forma eficiente y dentro de los parámetros operativos definidos.

**Why this priority**: El supervizor es el responsable de administrar la flota y garantizar que las operaciones de distribución se ejecuten correctamente. Sin visibilidad y control sobre el estado de vehículos y entregas en tiempo real.

**Independent Test**: Puede ser testeado completamente por un supervizor que: (1) Accede al dashboard de administración de flota, (2) Visualiza todos los vehículos disponibles y sus estados, (3) Consulta detalles de paradas completadas y pendientes, (4) Gestiona incidencias cuando un vehículo queda fuera de servicio.

**Acceptance Scenarios**:

1. **Scenario**: Visualización del estado general de la flota
   - **Given** un supervizor de flota logueado en el sistema
   - **When** accede al dashboard principal
   - **Then** ve el inventario completo de vehículos categorizados por tipo (Camioneta Urbana, Camión Sencillo, Tractocamión Regional) con su capacidad máxima, peso actual, porcentaje de ocupación y estado (disponible/en ruta/mantenimiento/fuera de servicio)




### Edge Cases

1. **¿Qué sucede si un vehículo se daña durante la ruta?**
   - El supervizor cambia el estado del vehículo a "Fuera de Servicio" y el sistema genera una alerta identificando los pedidos pendientes para redistribución manual.



## Requirements 

### Functional Requirements

- **FR-001**: System MUST permitir al supervizor visualizar el estado de todos los vehículos categorizados por tipo con su capacidad máxima y estado operativo (disponible/en ruta/mantenimiento/fuera de servicio)
- **FR-002**: System MUST categorizar la flota en tres tipos con sus capacidades: Camioneta Urbana (hasta 1.5 ton), Camión Sencillo (hasta 5 ton), Tractocamión Regional (superior a 25 ton)
- **FR-003**: System MUST permitir al supervizor cambiar el estado de un vehículo a "Fuera de Servicio" y generar alerta de redistribución de pedidos pendientes
- **FR-004**: System MUST mantener un historial completo de eventos de la flota (cambios de estado, alertas) para auditoría y análisis (si)

### Key Entities 

- **Vehículo**: Método de transporte con capacidad de carga específica, incluye: IdVehículo, Tipo (Camioneta Urbana/Camión Sencillo/Tractocamión), Capacidad de carga (kg), Estado (disponible/en ruta/mantenimiento/fuera de servicio), idTransportista, Timestamp de última actualización.
- **Alerta**: Notificación de evento operativo, incluye: IdAlerta, Tipo (desviación/retraso/fuera de servicio), IdVehículo, Descripción, Timestamp de generación, Estado (pendiente/resuelta)

## Success Criteria 

### Measurable Outcomes

- **SC-001**: El supervizor puede visualizar el estado completo de la flota (vehículos activos, ubicaciones, paradas) en menos de 2 segundos desde que abre el dashboard
- **SC-002**: El tiempo promedio de respuesta del supervisor ante una alerta es menor a 5 minutos - raro
- **SC-003**: El 100% de los cambios de estado de vehículos (incluyendo "Fuera de Servicio") se registran con timestamp y son persistidos en base de datos
- **SC-004**: El sistema proporciona historial completo y auditable de todos los eventos de flota (estados, alertas) para análisis posterior
