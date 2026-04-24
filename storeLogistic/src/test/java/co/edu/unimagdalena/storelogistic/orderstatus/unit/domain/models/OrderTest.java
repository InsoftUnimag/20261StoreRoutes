package co.edu.unimagdalena.storelogistic.orderstatus.unit.domain.models;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("updateStatus → sets finalStatus, effectivenessRate, carrierId, and updatedAt")
    void updateStatus_setsAllFields() {
        Order order = Order.create(1L, 10L, null);

        order.updateStatus(FinalStatus.ENTREGADO_COMPLETO, 50L);

        assertThat(order.finalStatus()).isEqualTo(FinalStatus.ENTREGADO_COMPLETO);
        assertThat(order.effectivenessRate().value()).isEqualTo(100);
        assertThat(order.carrierId()).isEqualTo(50L);
        assertThat(order.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus → updatedAt is after createdAt")
    void updateStatus_updatedAtIsAfterCreatedAt() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.NO_ENTREGADO, 50L);

        assertThat(order.updatedAt()).isAfterOrEqualTo(order.createdAt());
    }

    @Test
    @DisplayName("updateStatus → FALTANTE_INVENTARIO gives effectivenessRate=-100")
    void updateStatus_faltanteInventario_rateIsNegative100() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.FALTANTE_INVENTARIO, 50L);

        assertThat(order.effectivenessRate().value()).isEqualTo(-100);
    }

    @Test
    @DisplayName("requiresAlert → true for NO_ENTREGADO")
    void requiresAlert_noEntregado_returnsTrue() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.NO_ENTREGADO, 50L);
        assertThat(order.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("requiresAlert → true for RECHAZO_PARCIAL")
    void requiresAlert_rechazoParcial_returnsTrue() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.RECHAZO_PARCIAL, 50L);
        assertThat(order.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("requiresAlert → true for FALTANTE_INVENTARIO")
    void requiresAlert_faltanteInventario_returnsTrue() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.FALTANTE_INVENTARIO, 50L);
        assertThat(order.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("requiresAlert → true for DEVOLUCION_ERROR_EMPRESA")
    void requiresAlert_devolucion_returnsTrue() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.DEVOLUCION_ERROR_EMPRESA, 50L);
        assertThat(order.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("requiresAlert → false for ENTREGADO_COMPLETO")
    void requiresAlert_entregadoCompleto_returnsFalse() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.ENTREGADO_COMPLETO, 50L);
        assertThat(order.requiresAlert()).isFalse();
    }

    @Test
    @DisplayName("restore → reconstructs all fields without side effects")
    void restore_setsAllFieldsDirectly() {
        var createdAt = java.time.LocalDateTime.now().minusDays(1);
        var updatedAt = java.time.LocalDateTime.now();

        Order order = Order.restore(5L, 20L, 50L,
                FinalStatus.RECHAZO_PARCIAL,
                FinalStatus.RECHAZO_PARCIAL.effectivenessRate(),
                createdAt, updatedAt);

        assertThat(order.orderId()).isEqualTo(5L);
        assertThat(order.clientId()).isEqualTo(20L);
        assertThat(order.carrierId()).isEqualTo(50L);
        assertThat(order.finalStatus()).isEqualTo(FinalStatus.RECHAZO_PARCIAL);
        assertThat(order.effectivenessRate().value()).isEqualTo(80);
        assertThat(order.createdAt()).isEqualTo(createdAt);
        assertThat(order.updatedAt()).isEqualTo(updatedAt);
    }
}
