package co.edu.unimagdalena.storelogistic.route.domain.exceptions;

public class RouteException extends RuntimeException {
    public RouteException(String message) {
        super(message);
    }
    public RouteException(String message, Throwable cause) {
        super(message, cause);
    }
}