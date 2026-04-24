package co.edu.unimagdalena.storelogistic.consultar.application.services;

import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.consultar.domain.ports.in.GetStopDetailUseCase;
import co.edu.unimagdalena.storelogistic.route.domain.models.Stop;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStopDetailService implements GetStopDetailUseCase {

    private final AuthorizationService authorizationService;
    private final StopRepository stopRepository;

    @Override
    public Stop get(Long routeId, Long stopId, Long carrierId) {
        log.info("Stop detail requested: routeId={}, stopId={}, carrierId={}", routeId, stopId, carrierId);
        try {
            authorizationService.verifyCarrierHasAccess(routeId, carrierId);
            Stop stop = stopRepository.findByIdAndRouteId(stopId, routeId)
                    .orElseThrow(AccessDeniedException::new);
            log.info("Stop detail found: stopId={}, orderId={}", stop.stopId(), stop.orderId());
            return stop;
        } catch (AccessDeniedException e) {
            log.warn("Access denied: carrierId={} attempted stop {} on route {}", carrierId, stopId, routeId);
            throw e;
        }
    }
}
