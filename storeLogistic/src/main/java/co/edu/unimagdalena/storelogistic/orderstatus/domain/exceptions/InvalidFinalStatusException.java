package co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions;

public class InvalidFinalStatusException extends LogisticsException {

    public InvalidFinalStatusException(String value) {
        super("El valor '" + value + "' no es un estado final válido. " +
              "Valores permitidos: Entregado Completo, Rechazo Parcial, No Entregado, " +
              "Devolución (Error Empresa), Faltante de Inventario");
    }
}
