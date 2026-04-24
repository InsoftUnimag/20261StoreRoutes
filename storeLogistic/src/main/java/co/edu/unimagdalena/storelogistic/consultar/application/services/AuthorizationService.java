package co.edu.unimagdalena.storelogistic.consultar.application.services;

import co.edu.unimagdalena.storelogistic.consultar.domain.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.route.domain.models.Route;
import co.edu.unimagdalena.storelogistic.route.domain.ports.out.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final RouteRepository routeRepository;

    public Route verifyCarrierHasAccess(Long routeId, Long carrierId) {
        return routeRepository.findByIdAndCarrierId(routeId, carrierId)
                .orElseThrow(AccessDeniedException::new);
    }
}
