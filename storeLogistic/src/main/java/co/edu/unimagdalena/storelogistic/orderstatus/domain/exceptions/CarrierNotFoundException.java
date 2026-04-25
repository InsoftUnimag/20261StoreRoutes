package co.edu.unimagdalena.storelogistic.orderstatus.domain.exceptions;

public class CarrierNotFoundException extends LogisticsException {

    public CarrierNotFoundException(Long carrierId) {
        super("No existe un transportista con el id proporcionado: " + carrierId);
    }
}
