package co.edu.unimagdalena.storelogistic.consultar.domain.ports.in;

import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;

public interface GetStopDetailUseCase {

    Stop get(Long routeId, Long stopId, Long carrierId);
}
