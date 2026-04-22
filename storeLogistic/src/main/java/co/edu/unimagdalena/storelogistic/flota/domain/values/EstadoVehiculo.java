package co.edu.unimagdalena.storelogistic.flota.domain.values;

import java.util.Optional;

public enum EstadoVehiculo {
    DISPONIBLE,
    EN_RUTA,
    EN_MANTENIMIENTO,
    FUERA_DE_SERVICIO;

    public boolean esTransicionValida(EstadoVehiculo destino) {
        return Optional.ofNullable(destino)
                .filter(d -> d != this)
                .map(d -> switch (this) {
                    case EN_MANTENIMIENTO -> d == DISPONIBLE || d == FUERA_DE_SERVICIO;
                    case DISPONIBLE -> d == EN_RUTA || d == EN_MANTENIMIENTO || d == FUERA_DE_SERVICIO;
                    case EN_RUTA -> d == DISPONIBLE || d == FUERA_DE_SERVICIO;
                    case FUERA_DE_SERVICIO -> d == EN_MANTENIMIENTO;
                })
                .orElse(false);
    }

    public String obtenerMensajeTransicionInvalida(EstadoVehiculo destino) {
        return String.format("No se permite la transición de estado de %s a %s", this, destino);
    }
}

