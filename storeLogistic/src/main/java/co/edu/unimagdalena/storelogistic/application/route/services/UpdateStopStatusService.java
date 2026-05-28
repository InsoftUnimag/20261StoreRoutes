package co.edu.unimagdalena.storelogistic.application.route.services;

import co.edu.unimagdalena.storelogistic.application.consultar.services.AuthorizationService;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.in.ChangeVehicleStatusUseCase;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;
import co.edu.unimagdalena.storelogistic.domain.route.exceptions.StopNotFoundException;
import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.UpdateStopStatusUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.StopRepository;
import co.edu.unimagdalena.storelogistic.domain.route.values.StopStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateStopStatusService implements UpdateStopStatusUseCase {

    private final AuthorizationService authorizationService;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final ChangeVehicleStatusUseCase changeVehicleStatusUseCase;

    @Override
    @Transactional
    public Stop execute(Command cmd) {
        log.info("Updating stop status: routeId={}, stopId={}, carrierId={}, resultado={}",
                cmd.routeId(), cmd.stopId(), cmd.carrierId(), cmd.resultado());

        authorizationService.verifyRouteWriteAccess(cmd.routeId());

        Stop stop = stopRepository.findByIdAndRouteId(cmd.stopId(), cmd.routeId())
                .orElseThrow(() -> new StopNotFoundException(cmd.stopId()));

        if (cmd.resultado() == StopStatus.DELIVERED) {
            LocalDate fecha = cmd.fechaEntrega() != null ? cmd.fechaEntrega() : LocalDate.now();
            stop.markDelivered(fecha);
        } else {
            stop.markRejected();
        }

        Stop saved = stopRepository.save(stop);
        log.info("Stop {} updated to status={}", cmd.stopId(), saved.status());

        checkAndCompleteRoute(cmd.routeId());

        return saved;
    }

    private void checkAndCompleteRoute(Long routeId) {
        List<Stop> allStops = stopRepository.findByRouteId(routeId);
        boolean allDelivered = allStops.stream()
                .allMatch(s -> s.status() == StopStatus.DELIVERED || s.status() == StopStatus.REJECTED);

        if (!allDelivered) return;

        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null || route.status() == null) return;

        route.markCompleted();
        routeRepository.save(route);

        if (route.vehicleId() != null) {
            changeVehicleStatusUseCase.change(route.vehicleId(), VehicleStatus.DISPONIBLE);
        }

        log.info("Route {} completed — all stops delivered, vehicle {} set to DISPONIBLE",
                routeId, route.vehicleId());
    }
}
