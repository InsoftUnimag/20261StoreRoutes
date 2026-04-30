package co.edu.unimagdalena.storelogistic.domain.consultar.ports.in;

import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;

import java.util.List;

public interface QueryStopsUseCase {

    List<Stop> query(Long routeId, Long carrierId);
}
