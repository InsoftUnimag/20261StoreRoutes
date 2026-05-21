package co.edu.unimagdalena.storelogistic.application.route.services;

import co.edu.unimagdalena.storelogistic.domain.route.models.Route;
import co.edu.unimagdalena.storelogistic.domain.route.ports.in.GetRoutesUseCase;
import co.edu.unimagdalena.storelogistic.domain.route.ports.out.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRoutesService implements GetRoutesUseCase {

    private final RouteRepository routeRepository;

    @Override
    public List<Route> getAll() {
        return routeRepository.findAll();
    }
}
