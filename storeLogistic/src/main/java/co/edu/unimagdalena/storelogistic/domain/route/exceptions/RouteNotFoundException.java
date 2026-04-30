package co.edu.unimagdalena.storelogistic.domain.route.exceptions;

public class RouteNotFoundException extends RouteException {
    public RouteNotFoundException(Long routeId) {
        super("Route not found with id: " + routeId);
    }
}