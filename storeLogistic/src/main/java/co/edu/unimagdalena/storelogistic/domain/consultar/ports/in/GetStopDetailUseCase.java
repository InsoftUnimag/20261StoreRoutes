package co.edu.unimagdalena.storelogistic.domain.consultar.ports.in;

import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;

public interface GetStopDetailUseCase {

    Stop get(Long routeId, Long stopId, Long carrierId);
}
