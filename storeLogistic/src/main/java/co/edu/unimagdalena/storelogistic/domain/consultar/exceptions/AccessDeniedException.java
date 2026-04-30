package co.edu.unimagdalena.storelogistic.domain.consultar.exceptions;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super("Acceso denegado");
    }
}
