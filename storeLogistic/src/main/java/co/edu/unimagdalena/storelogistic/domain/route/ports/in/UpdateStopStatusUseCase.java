package co.edu.unimagdalena.storelogistic.domain.route.ports.in;

import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
import co.edu.unimagdalena.storelogistic.domain.route.values.StopStatus;

import java.time.LocalDate;

public interface UpdateStopStatusUseCase {

    record Command(Long routeId, Long stopId, StopStatus resultado, LocalDate fechaEntrega) {}

    Stop execute(Command command);
}
