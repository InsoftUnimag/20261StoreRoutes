package co.edu.unimagdalena.storelogistic.consultar.domain.ports.in;

import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;

import java.util.List;

public interface QueryStopsUseCase {

    List<Stop> query(Long routeId, Long carrierId);
}
