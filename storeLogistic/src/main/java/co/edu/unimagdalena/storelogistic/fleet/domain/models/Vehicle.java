package co.edu.unimagdalena.storelogistic.fleet.domain.models;

import co.edu.unimagdalena.storelogistic.fleet.domain.exceptions.InvalidStateTransitionException;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private BigDecimal currentWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal occupancyPercentage() {
        return Optional.ofNullable(loadCapacity)
                .filter(c -> c.getWeightKg().compareTo(BigDecimal.ZERO) != 0)
                .map(c -> Optional.ofNullable(currentWeight).orElse(BigDecimal.ZERO)
                        .divide(c.getWeightKg(), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)))
                .orElse(BigDecimal.ZERO);
    }

    public static Vehicle createNew(Category category, LoadCapacity loadCapacity, Long transporterId) {
        return Vehicle.builder()
                .categoryId(category.getCategoryId())
                .loadCapacity(loadCapacity)
                .status(VehicleStatus.EN_MANTENIMIENTO)
                .transporterId(transporterId)
                .currentWeight(BigDecimal.ZERO)
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