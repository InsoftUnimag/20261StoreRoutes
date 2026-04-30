package co.edu.unimagdalena.storelogistic.domain.route.exceptions;

public class CapacityExceededException extends RouteException {
    public CapacityExceededException(String message) {
        super(message);
    }
}