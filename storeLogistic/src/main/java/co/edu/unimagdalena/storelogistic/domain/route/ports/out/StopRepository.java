package co.edu.unimagdalena.storelogistic.domain.route.ports.out;

import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;

import java.util.List;

public interface StopRepository {

    Stop save(Stop stop);

    List<Stop> findByRouteId(Long routeId);

    List<Stop> findByRouteIdOrderBySequence(Long routeId);

    java.util.Optional<Stop> findByIdAndRouteId(Long stopId, Long routeId);
}