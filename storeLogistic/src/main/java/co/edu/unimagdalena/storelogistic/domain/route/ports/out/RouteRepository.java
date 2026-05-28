package co.edu.unimagdalena.storelogistic.domain.route.ports.out;

import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.values.LogisticWeight;
import co.edu.unimagdalena.storelogistic.domain.route.values.RouteStatus;

import java.util.List;
import java.util.Optional;

public interface RouteRepository {

    Optional<Route> findAvailableWithCapacity(LogisticWeight weight);

    List<Route> findByStatus(RouteStatus status);

    Route save(Route route);

    Optional<Route> findById(Long routeId);

    List<Route> findAll();
}