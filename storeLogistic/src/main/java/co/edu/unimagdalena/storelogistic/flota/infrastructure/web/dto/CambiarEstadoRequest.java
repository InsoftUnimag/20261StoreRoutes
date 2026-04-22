package co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoRequest {
    @NotNull(message = "El nuevo estado es requerido")
    private String nuevoEstado;
}

