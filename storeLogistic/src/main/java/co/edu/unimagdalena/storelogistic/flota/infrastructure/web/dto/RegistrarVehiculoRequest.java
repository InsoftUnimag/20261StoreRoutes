package co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarVehiculoRequest {
    @NotNull(message = "La categoría es requerida")
    private String categoria;

    @NotNull(message = "La capacidad de carga es requerida")
    @Positive(message = "La capacidad de carga debe ser mayor a 0")
    private Double capacidadCarga;

    @NotBlank(message = "El ID del transportista es requerido")
    private String idTransportista;
}

