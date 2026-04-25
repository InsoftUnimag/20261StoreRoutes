package co.edu.unimagdalena.storelogistic.orderstatus.unit.infrastructure;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.OrderStatusAudit;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.mapper.OrderStatusMapperImpl;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderAlertJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderStatusAuditJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jpa.OrderStatusJpaEntity;
import co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.web.dto.UpdateOrderStatusResponse;
import co.edu.unimagdalena.storelogistic.orderstatus.testdata.OrderFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusMapperTest {

    private final OrderStatusMapperImpl mapper = new OrderStatusMapperImpl();

    @Test
    @DisplayName("toResponse → maps all fields correctly")
    void toResponse_mapsFields() {
        Order order = OrderFixture.withStatus(FinalStatus.ENTREGADO_COMPLETO, 50L);

        UpdateOrderStatusResponse response = mapper.toResponse(order);

        assertThat(response.getIdPedido()).isEqualTo(1L);
        assertThat(response.getEstadoFinal()).isEqualTo("Entregado Completo");
        assertThat(response.getTasaEfectividad()).isEqualTo(100);
        assertThat(response.getIdTransportista()).isEqualTo(50L);
        assertThat(response.getFechaActualizacion()).isNotNull();
    }

    @Test
    @DisplayName("toResponse → null order returns null")
    void toResponse_nullOrder_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("toDomain → entity with estado_final maps to correct FinalStatus")
    void toDomain_entityWithStatus_mapsCorrectly() {
        OrderStatusJpaEntity entity = OrderStatusJpaEntity.builder()
                .orderId(1L)
                .clientId(10L)
                .carrierId(50L)
                .estadoFinal("Rechazo Parcial")
                .tasaEfectividad(80)
                .logisticWeight(new BigDecimal("100"))
                .deliveryAddress("Calle 1")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        Order order = mapper.toDomain(entity);

        assertThat(order.finalStatus()).isEqualTo(FinalStatus.RECHAZO_PARCIAL);
        assertThat(order.effectivenessRate().value()).isEqualTo(80);
        assertThat(order.carrierId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("toDomain → entity without estado_final has null finalStatus")
    void toDomain_entityWithoutStatus_nullFinalStatus() {
        OrderStatusJpaEntity entity = OrderStatusJpaEntity.builder()
                .orderId(1L)
                .logisticWeight(new BigDecimal("100"))
                .deliveryAddress("Calle 1")
                .createdAt(LocalDateTime.now())
                .build();

        Order order = mapper.toDomain(entity);

        assertThat(order.finalStatus()).isNull();
        assertThat(order.effectivenessRate()).isNull();
    }

    @Test
    @DisplayName("toEntity → maps Order to JpaEntity preserving all fields")
    void toEntity_mapsAllFields() {
        Order order = OrderFixture.withStatus(FinalStatus.FALTANTE_INVENTARIO, 50L);

        OrderStatusJpaEntity entity = mapper.toEntity(order);

        assertThat(entity.getEstadoFinal()).isEqualTo("Faltante de Inventario");
        assertThat(entity.getTasaEfectividad()).isEqualTo(-100);
        assertThat(entity.getCarrierId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("toAlertEntity → maps Alert domain to JPA entity")
    void toAlertEntity_mapsCorrectly() {
        Order order = OrderFixture.withStatus(FinalStatus.NO_ENTREGADO, 50L);
        Alert alert = Alert.createFor(order);

        OrderAlertJpaEntity entity = mapper.toAlertEntity(alert);

        assertThat(entity.getEstadoFinalRegistrado()).isEqualTo("No Entregado");
        assertThat(entity.getTipo()).isEqualTo("NO_ENTREGADO");
        assertThat(entity.getEstado()).isEqualTo("PENDIENTE");
        assertThat(entity.isAsignadoASupervisor()).isTrue();
    }

    @Test
    @DisplayName("toAuditEntity → maps audit with null previousStatus")
    void toAuditEntity_nullPreviousStatus() {
        OrderStatusAudit audit = new OrderStatusAudit(
                null, 1L, null, FinalStatus.ENTREGADO_COMPLETO, 50L, LocalDateTime.now());

        OrderStatusAuditJpaEntity entity = mapper.toAuditEntity(audit);

        assertThat(entity.getEstadoAnterior()).isNull();
        assertThat(entity.getEstadoNuevo()).isEqualTo("Entregado Completo");
    }
}
