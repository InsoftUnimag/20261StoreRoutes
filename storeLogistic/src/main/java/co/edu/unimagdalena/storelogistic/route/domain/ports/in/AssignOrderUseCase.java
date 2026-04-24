package co.edu.unimagdalena.storelogistic.route.domain.ports.in;

import co.edu.unimagdalena.storelogistic.route.domain.models.Route;

public interface AssignOrderUseCase {

    record AssignResult(Route route, boolean isNewRoute) {}

    AssignResult assign(Long orderId);
}