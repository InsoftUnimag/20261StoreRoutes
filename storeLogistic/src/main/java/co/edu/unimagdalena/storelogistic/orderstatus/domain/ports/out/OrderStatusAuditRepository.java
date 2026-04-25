package co.edu.unimagdalena.storelogistic.orderstatus.domain.ports.out;

import co.edu.unimagdalena.storelogistic.orderstatus.domain.models.OrderStatusAudit;

public interface OrderStatusAuditRepository {

    OrderStatusAudit save(OrderStatusAudit audit);
}
