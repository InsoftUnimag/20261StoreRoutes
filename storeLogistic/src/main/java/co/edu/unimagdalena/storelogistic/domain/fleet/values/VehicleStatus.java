package co.edu.unimagdalena.storelogistic.domain.fleet.values;

import java.util.Optional;

public enum VehicleStatus {
    DISPONIBLE,
    EN_RUTA,
    EN_MANTENIMIENTO,
    FUERA_DE_SERVICIO;

    public boolean isValidTransition(VehicleStatus target) {
        return Optional.ofNullable(target)
                .filter(d -> d != this)
                .map(d -> switch (this) {
                    case EN_MANTENIMIENTO -> d == DISPONIBLE || d == FUERA_DE_SERVICIO;
                    case DISPONIBLE       -> d == EN_RUTA || d == EN_MANTENIMIENTO || d == FUERA_DE_SERVICIO;
                    case EN_RUTA          -> d == DISPONIBLE || d == FUERA_DE_SERVICIO;
                    case FUERA_DE_SERVICIO -> d == EN_MANTENIMIENTO;
                })
                .orElse(false);
    }

    public String invalidTransitionMessage(VehicleStatus target) {
        return String.format("State transition from %s to %s is not allowed", this, target);
    }
}