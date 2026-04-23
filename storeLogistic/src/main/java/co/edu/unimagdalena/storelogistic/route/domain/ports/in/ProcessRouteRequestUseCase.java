package co.edu.unimagdalena.storelogistic.route.domain.ports.in;

import co.edu.unimagdalena.storelogistic.route.domain.models.Route;

import java.math.BigDecimal;

public interface ProcessRouteRequestUseCase {

    Route process(Long orderId, BigDecimal logisticWeightKg, String deliveryAddress);
}