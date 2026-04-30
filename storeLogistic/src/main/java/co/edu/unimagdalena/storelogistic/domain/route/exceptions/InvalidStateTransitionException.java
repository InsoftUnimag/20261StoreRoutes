package co.edu.unimagdalena.storelogistic.domain.route.exceptions;

public class InvalidStateTransitionException extends RouteException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}