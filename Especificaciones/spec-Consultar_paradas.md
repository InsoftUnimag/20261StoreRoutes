# Feature Specification: Consultar paradas de las rutas

## User Scenarios & Testing 

### User Story 1 - Visualizar lista de paradas en una ruta asignada (Priority: P1)

Como transportista (conductor), quiero poder consultar la lista completa de paradas asignadas a mi ruta específica para monitorear el progreso de las entregas y asegurar una distribución eficiente.

**Why this priority**: Esta es la funcionalidad core para que los transportistas tengan visibilidad inmediata sobre sus rutas asignadas, crítico para la ejecución diaria de entregas.

**Independent Test**: Puede ser probado independientemente creando una ruta asignada a un transportista y verificando que la lista se muestre correctamente, entregando valor al permitir monitoreo básico sin otras funcionalidades.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa de paradas en ruta asignada
   - **Given** una ruta planificada asignada al transportista con múltiples paradas
   - **When** el transportista solicita consultar paradas de su ruta
   - **Then** se muestra una lista ordenada de paradas con detalles como direccion y contacto del cliente.

### Edge Cases

1. **Transportista intenta consultar ruta no asignada**  
   Respuesta: Retornar error 403 "Acceso denegado" y registrar en logs sin revelar si la ruta existe.


## Requirements 

### Functional Requirements

- **FR-001**: El sistema DEBE permitir a transportistas consultar únicamente las paradas de rutas asignadas a ellos, mostrando al menos: ID de parada, dirrecion,, contacto del cliente.
- **FR-002**: El sistema DEBE registrar logs de consultas para auditoría, sin afectar el rendimiento de la consulta.
- **FR-003**: El sistema DEBE restringir el acceso solo a rutas asignadas al transportista, denegando consultas a rutas no autorizadas.

### Key Entities 

- **Ruta**: Representa un viaje planificado asignado a un transportista, con atributos como ID de ruta, vehículo asignado, capacidad utilizada, fecha de despacho.
- **Parada**: Punto de entrega en la ruta, con atributos como IdParada, Idruta, IdPedido, IdCliente, direccion.
- **Vehículo**: Asociado a la ruta, con capacidad de carga y tipo (camioneta, camión, etc, estado del vehiculo.).
- **Transportista**: Usuario con rol de conductor, con permisos limitados a sus rutas asignadas(IdTransportista).

## Success Criteria 

### Measurable Outcomes

- **SC-001**: Los transportistas pueden consultar y visualizar la lista de paradas de su ruta en menos de 3 segundos para rutas con hasta 20 paradas.
- **SC-002**: El 95% de las consultas de paradas se completan sin errores en condiciones normales.
