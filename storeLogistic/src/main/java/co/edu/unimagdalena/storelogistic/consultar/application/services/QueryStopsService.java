package co.edu.unimagdalena.storelogistic.consultar.application.services;

import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.consultar.domain.ports.in.QueryStopsUseCase;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.StopRepository;
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

    @Override
    public List<Stop> query(Long routeId, Long carrierId) {
        log.info("Consulting stops: routeId={}, carrierId={}", routeId, carrierId);
        long start = System.currentTimeMillis();
        try {
            authorizationService.verifyCarrierHasAccess(routeId, carrierId);
            List<Stop> stops = stopRepository.findByRouteIdOrderBySequence(routeId);
            log.info("Stops query successful: routeId={}, carrierId={}, count={}, elapsed={}ms",
                    routeId, carrierId, stops.size(), System.currentTimeMillis() - start);
            return stops;
        } catch (AccessDeniedException e) {
            log.warn("Access denied: carrierId={} attempted to access routeId={}", carrierId, routeId);
            throw e;
        }
    }
}
