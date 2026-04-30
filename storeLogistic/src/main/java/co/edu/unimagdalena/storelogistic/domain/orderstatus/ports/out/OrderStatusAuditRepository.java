package co.edu.unimagdalena.storelogistic.domain.orderstatus.ports.out;

import co.edu.unimagdalena.storelogistic.domain.orderstatus.models.OrderStatusAudit;

public interface OrderStatusAuditRepository {

    OrderStatusAudit save(OrderStatusAudit audit);
}
