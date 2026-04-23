package co.edu.unimagdalena.storelogistic.route.domain.exceptions;

public class CapacityExceededException extends RouteException {
    public CapacityExceededException(String message) {
        super(message);
    }
}