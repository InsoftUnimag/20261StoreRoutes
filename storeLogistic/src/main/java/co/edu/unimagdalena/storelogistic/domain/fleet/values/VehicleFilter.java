package co.edu.unimagdalena.storelogistic.domain.fleet.values;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleFilter {
    private CategoryType category;
    private LoadCapacity minCapacity;
    private LoadCapacity maxCapacity;
    private VehicleStatus status;

    public boolean isValid() {
        return Optional.ofNullable(minCapacity)
                .flatMap(min -> Optional.ofNullable(maxCapacity)
                        .map(max -> min.getWeightKg().compareTo(max.getWeightKg()) <= 0))
                .orElse(true);
    }
}