package co.edu.unimagdalena.storelogistic.domain.route.ports.out;

import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.values.LogisticWeight;

import java.util.Optional;

public interface RouteRepository {

    Optional<Route> findAvailableWithCapacity(LogisticWeight weight);

    Route save(Route route);

    Optional<Route> findById(Long routeId);

    Optional<Route> findByIdAndCarrierId(Long routeId, Long carrierId);
}