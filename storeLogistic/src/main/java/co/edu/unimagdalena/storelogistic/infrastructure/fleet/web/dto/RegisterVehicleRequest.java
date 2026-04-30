package co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVehicleRequest {
    @NotNull(message = "Category is required")
    private String category;

    @NotNull(message = "Load capacity is required")
    @Positive(message = "Load capacity must be greater than 0")
    private Double loadCapacity;

    @NotNull(message = "Transporter ID is required")
    @Positive(message = "Transporter ID must be greater than 0")
    private Long transporterId;
}