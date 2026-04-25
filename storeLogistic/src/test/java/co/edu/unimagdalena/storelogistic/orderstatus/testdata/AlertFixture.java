package co.edu.unimagdalena.storelogistic.orderstatus.testdata;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.Alert;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertStatus;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.AlertType;
import co.edu.unimagdalena.storelogistic.orderstatus.domain.values.FinalStatus;

import java.time.LocalDateTime;

public final class AlertFixture {

    private AlertFixture() {}

    public static Alert noEntregado(Long orderId, Long carrierId) {
        return new Alert(null, orderId, carrierId,
                FinalStatus.NO_ENTREGADO, AlertType.NO_ENTREGADO,
                "Pedido no entregado por el transportista",
                AlertStatus.PENDIENTE, true, LocalDateTime.now());
    }

    public static Alert saved(Long alertId, Long orderId, Long carrierId) {
        return new Alert(alertId, orderId, carrierId,
                FinalStatus.RECHAZO_PARCIAL, AlertType.RECHAZO,
                "Rechazo parcial por parte del cliente",
                AlertStatus.PENDIENTE, true, LocalDateTime.now());
    }
}
