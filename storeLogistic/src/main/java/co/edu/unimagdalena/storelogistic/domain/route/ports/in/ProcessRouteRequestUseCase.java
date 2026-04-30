package co.edu.unimagdalena.storelogistic.domain.route.ports.in;

import co.edu.unimagdalena.storelogistic.domain.route.models.Route;

import java.math.BigDecimal;

public interface ProcessRouteRequestUseCase {

    Route process(Long orderId, BigDecimal logisticWeightKg, String deliveryAddress);
}