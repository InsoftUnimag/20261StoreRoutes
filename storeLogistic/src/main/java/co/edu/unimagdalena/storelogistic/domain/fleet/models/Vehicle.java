package co.edu.unimagdalena.storelogistic.domain.fleet.models;

import co.edu.unimagdalena.storelogistic.domain.fleet.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    private Long vehicleId;
    private Long categoryId;
    private LoadCapacity loadCapacity;
    private VehicleStatus status;
    private Long transporterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Vehicle createNew(Category category, LoadCapacity loadCapacity, Long transporterId) {
        return Vehicle.builder()
                .categoryId(category.getCategoryId())
                .loadCapacity(loadCapacity)
                .status(VehicleStatus.EN_MANTENIMIENTO)
                .transporterId(transporterId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void changeStatus(VehicleStatus newStatus) {
        Optional.ofNullable(newStatus)
                .filter(s -> status.isValidTransition(s))
                .orElseThrow(() -> new InvalidStateTransitionException(
                        status.invalidTransitionMessage(newStatus)
                ));
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignTransporter(Long transporterId) {
        this.transporterId = transporterId;
        this.updatedAt = LocalDateTime.now();
    }
}