package co.edu.unimagdalena.storelogistic.infrastructure.fleet.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStatusRequest {
    @NotNull(message = "New status is required")
    private String newStatus;
}