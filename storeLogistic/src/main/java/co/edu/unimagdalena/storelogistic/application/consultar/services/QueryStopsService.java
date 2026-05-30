package co.edu.unimagdalena.storelogistic.application.consultar.services;

import co.edu.unimagdalena.storelogistic.domain.consultar.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.domain.consultar.ports.in.QueryStopsUseCase;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.VehicleRepository;
import co.edu.unimagdalena.storelogistic.domain.route.models.Stop;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryStopsService implements QueryStopsUseCase {

    private final AuthorizationService authorizationService;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public List<Stop> query(Long routeId) {
        log.info("Consulting stops: routeId={}", routeId);
        long start = System.currentTimeMillis();
        try {
            var route = authorizationService.verifyRouteReadAccess(routeId);
            Long carrierId = resolveCarrierId(route);
            List<Stop> stops = stopRepository.findByRouteIdOrderBySequence(routeId);
            log.info("Stops query successful: routeId={}, carrierId={}, count={}, elapsed={}ms",
                    routeId, carrierId, stops.size(), System.currentTimeMillis() - start);
            return stops;
        } catch (AccessDeniedException e) {
            log.warn("Access denied: routeId={}", routeId);
            throw e;
        }
    }

    private Long resolveCarrierId(co.edu.unimagdalena.storelogistic.domain.route.models.Route route) {
        if (route.vehicleId() == null) return null;
        return vehicleRepository.findById(route.vehicleId())
                .map(v -> v.getTransporterId())
                .orElse(null);
    }
}
