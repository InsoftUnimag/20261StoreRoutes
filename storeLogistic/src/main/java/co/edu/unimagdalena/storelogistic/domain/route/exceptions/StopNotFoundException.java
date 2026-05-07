package co.edu.unimagdalena.storelogistic.domain.route.exceptions;

public class StopNotFoundException extends RouteException {
    public StopNotFoundException(Long stopId) {
        super("Stop not found with id: " + stopId);
    }
}