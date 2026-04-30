package co.edu.unimagdalena.storelogistic.domain.route.ports.in;

import co.edu.unimagdalena.storelogistic.domain.route.models.Route;

public interface AssignOrderUseCase {

    record AssignResult(Route route, boolean isNewRoute) {}

    AssignResult assign(Long orderId);
}