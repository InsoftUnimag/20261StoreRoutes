# Feature Specification: Consultar Método de Pago de un Pedido

## User Story

### Consulta de Forma de Pago al Módulo Financiero (Priority: P1)

Yo como módulo de Logística de Despacho y Distribución consulto el método de pago de un pedido al módulo Financiero, enviando solo el idPedido, para determinar las condiciones de pago y mostrarlas al conductor en el detalle de la parada.

**Why this priority**: Es crítico validar la forma de pago antes de completar la entrega. El método determina si se cobra en entrega (CONTRA_ENTREGA) o si el cliente tiene cartera comercial (CARTERA_COMERCIAL).

**Independent Test**: Puede probarse de forma independiente cuando se consulta el método de pago de un pedido válido y el sistema retorna: idPedido, formaPago y valorContraEntrega. La consulta es síncrona al endpoint del Módulo Financiero.

**Communication Pattern**: Síncrono. El `FinanceModuleClient` (Logística) llama directamente al endpoint del Módulo Financiero. El resultado se consume internamente y se devuelve al conductor vía `StopDetailDTO`.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa retorna CONTRA_ENTREGA con valor a cobrar
   - **Given** existe un pedido con forma_pago = CONTRA_ENTREGA
   - **When** `FinanceModuleClient.findByOrderId(id)` llama a `GET /api/v1/pedidos/{id}/pago-transporte`
   - **Then** se retorna `{idPedido, formaPago: "CONTRA_ENTREGA", valorContraEntrega: 50000.00}`

2. **Scenario**: Consulta exitosa retorna CARTERA_COMERCIAL con valor 0
   - **Given** existe un pedido con forma_pago = CARTERA_COMERCIAL
   - **When** `FinanceModuleClient.findByOrderId(id)` llama a `GET /api/v1/pedidos/{id}/pago-transporte`
   - **Then** se retorna `{idPedido, formaPago: "CARTERA_COMERCIAL", valorContraEntrega: 0}`

3. **Scenario**: Error cuando el pedido no existe
   - **Given** se consulta con un idPedido que no existe
   - **When** `FinanceModuleClient.findByOrderId(id)` llama al módulo financiero
   - **Then** el Módulo Financiero retorna 404 y el sistema lanza `OrderNotFoundException`

---

### Edge Cases

- **¿Qué sucede cuando el pedido no existe?** El Módulo Financiero retorna 404; el sistema lanza `OrderNotFoundException`.

- **¿Qué sucede cuando el cliente no tiene forma de pago asignada?** El Módulo Financiero retorna 422; el sistema lanza `PaymentMethodNotRegisteredException`.

- **¿Qué sucede si el endpoint del Módulo Financiero no está disponible?** El sistema reintenta hasta 3 veces con backoff exponencial (500ms inicial, 2x). Si falla, lanza `FinanceServiceUnavailableException`.

## Requirements

### Functional Requirements

- **FR-001**: System MUST [enviar GET request al Módulo Financiero con endpoint `/api/v1/pedidos/{id}/pago-transporte` usando solo el idPedido.]

- **FR-002**: System MUST [procesar la respuesta del Módulo Financiero que retorna `{idPedido, formaPago, valorContraEntrega}` donde `formaPago` = CONTRA_ENTREGA o CARTERA_COMERCIAL, y `valorContraEntrega` es el monto a cobrar (0 si no es CONTRA_ENTREGA).]

- **FR-003**: System MUST [manejar errores del Módulo Financiero: `OrderNotFoundException` (404), `PaymentMethodNotRegisteredException` (422), `FinanceServiceUnavailableException` (503).]

- **FR-004**: System MUST [retentar la consulta en caso de timeout o indisponibilidad del Módulo Financiero (máximo 3 reintentos con backoff exponencial).]

- **FR-005**: System MUST [incluir `paymentMethod` y `totalACobrar` en el `StopDetailDTO` cuando el conductor consulte el detalle de una parada.]

### Key Entities

- **[Pedido]**: Documento de entrega. Atributos: idPedido, idCliente, forma_pago.

- **[Forma de Pago]**: Método de pago del cliente. Valores válidos:
  * `CONTRA_ENTREGA`: Pago al momento de la entrega — incluye `valorContraEntrega` con el total a cobrar.
  * `CARTERA_COMERCIAL`: Crédito comercial (sin pago en entrega) — `valorContraEntrega` es `0`.

- **[OrderPaymentMethod]**: Modelo de dominio que representa la respuesta del módulo financiero. Atributos: `orderId`, `paymentMethod`, `totalPedido`.

- **[FinancePaymentMethodResponse]**: DTO de infraestructura que mapea la respuesta JSON del módulo financiero. Campos: `idPedido`, `formaPago`, `valorContraEntrega`.

### Flujo interno (Logística → Módulo Financiero)

```
QueryStopsController.getStopDetail()
  → ConsultPaymentMethodUseCase.consult(orderId)
    → FinanceGatewayPort.findByOrderId(orderId)
      → FinanceModuleClient.callFinanceModule(orderId)
        → GET /api/v1/pedidos/{id}/pago-transporte (HTTP externo)
        → FinancePaymentMethodResponse {idPedido, formaPago, valorContraEntrega}
        → PaymentMethodMapper.toDomain() → OrderPaymentMethod
      → OrderPaymentMethod retornado al controller
    → QueryStopsMapper.toDetailDTO(stop, payment)
  → StopDetailDTO { ..., paymentMethod, totalACobrar }
```

El conductor **no llama directamente** al módulo financiero. La información de pago se sirve como parte del detalle de la parada.

### Request/Response Contract (Logística → Módulo Financiero)

**Request**:
```
GET /api/v1/pedidos/{id_pedido}/pago-transporte
```

**Success Response (HTTP 200) — CONTRA_ENTREGA**:
```json
{
  "idPedido": 123,
  "formaPago": "CONTRA_ENTREGA",
  "valorContraEntrega": 50000.00
}
```

**Success Response (HTTP 200) — CARTERA_COMERCIAL**:
```json
{
  "idPedido": 123,
  "formaPago": "CARTERA_COMERCIAL",
  "valorContraEntrega": 0
}
```

## Success Criteria

### Measurable Outcomes

- **SC-001**: Exactitud en la consulta. El 100% de las consultas deben retornar formaPago correcta (CONTRA_ENTREGA o CARTERA_COMERCIAL) o excepción válida.

- **SC-002**: Disponibilidad. El 99% de las consultas deben completarse en menos de 3 segundos (incluidos reintentos).

- **SC-003**: Integridad de datos. El 100% de las respuestas exitosas deben incluir idPedido y formaPago exactamente como lo retorna el Módulo Financiero.
