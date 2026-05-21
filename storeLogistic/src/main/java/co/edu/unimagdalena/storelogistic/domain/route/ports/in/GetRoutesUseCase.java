package co.edu.unimagdalena.storelogistic.domain.route.ports.in;

import co.edu.unimagdalena.storelogistic.domain.route.models.Route;

import java.util.List;

public interface GetRoutesUseCase {

    List<Route> getAll();
}
