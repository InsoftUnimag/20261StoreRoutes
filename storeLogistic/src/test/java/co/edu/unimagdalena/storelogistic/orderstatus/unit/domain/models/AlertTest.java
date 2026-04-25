package co.edu.unimagdalena.storelogistic.orderstatus.unit.domain.models;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Order;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertType;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertTest {

    @Test
    @DisplayName("createFor → NO_ENTREGADO yields type NO_ENTREGADO, PENDIENTE status")
    void createFor_noEntregado() {
        Order order = Order.create(1L, 10L, null);
        order.updateStatus(FinalStatus.NO_ENTREGADO, 50L);

        Alert alert = Alert.createFor(order);

        assertThat(alert.type()).isEqualTo(AlertType.NO_ENTREGADO);
        assertThat(alert.status()).isEqualTo(AlertStatus.PENDIENTE);
        assertThat(alert.assignedToSupervisor()).isTrue();
        assertThat(alert.orderId()).isEqualTo(1L);
        assertThat(alert.carrierId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("createFor → RECHAZO_PARCIAL yields type RECHAZO")
    void createFor_rechazoParcial() {
        Order order = Order.create(2L, 10L, null);
        order.updateStatus(FinalStatus.RECHAZO_PARCIAL, 50L);

        Alert alert = Alert.createFor(order);

        assertThat(alert.type()).isEqualTo(AlertType.RECHAZO);
    }

    @Test
    @DisplayName("createFor → FALTANTE_INVENTARIO yields type FALTANTE")
    void createFor_faltanteInventario() {
        Order order = Order.create(3L, 10L, null);
        order.updateStatus(FinalStatus.FALTANTE_INVENTARIO, 50L);

        Alert alert = Alert.createFor(order);

        assertThat(alert.type()).isEqualTo(AlertType.FALTANTE);
    }

    @Test
    @DisplayName("createFor → DEVOLUCION_ERROR_EMPRESA yields type DEVOLUCION")
    void createFor_devolucion() {
        Order order = Order.create(4L, 10L, null);
        order.updateStatus(FinalStatus.DEVOLUCION_ERROR_EMPRESA, 50L);

        Alert alert = Alert.createFor(order);

        assertThat(alert.type()).isEqualTo(AlertType.DEVOLUCION);
    }

    @Test
    @DisplayName("createFor → ENTREGADO_COMPLETO throws IllegalArgumentException")
    void createFor_entregadoCompleto_throws() {
        Order order = Order.create(5L, 10L, null);
        order.updateStatus(FinalStatus.ENTREGADO_COMPLETO, 50L);

        assertThatThrownBy(() -> Alert.createFor(order))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
