package co.edu.unimagdalena.storelogistic.application.consultar.services;

import co.edu.unimagdalena.storelogistic.domain.consultar.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.values.RouteStatus;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final RouteRepository routeRepository;

    public Route verifyRouteReadAccess(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(AccessDeniedException::new);
        if (route.status() == RouteStatus.PENDING_VEHICLE)
            throw new AccessDeniedException();
        return route;
    }

    public Route verifyRouteWriteAccess(Long routeId) {
        Route route = verifyRouteReadAccess(routeId);
        if (route.status() != RouteStatus.CLOSED)
            throw new AccessDeniedException();
        return route;
    }
}
