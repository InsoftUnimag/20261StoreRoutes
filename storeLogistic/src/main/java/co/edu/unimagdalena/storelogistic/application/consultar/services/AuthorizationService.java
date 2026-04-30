package co.edu.unimagdalena.storelogistic.application.consultar.services;

import co.edu.unimagdalena.storelogistic.domain.consultar.exceptions.AccessDeniedException;
import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
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
