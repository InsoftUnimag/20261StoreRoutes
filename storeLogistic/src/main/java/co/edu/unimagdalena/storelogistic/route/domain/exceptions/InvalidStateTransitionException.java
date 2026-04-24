package co.edu.unimagdalena.storelogistic.route.domain.exceptions;

public class InvalidStateTransitionException extends RouteException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}