package co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.repository;

import co.edu.unimagdalena.storelogistic.domain.fleet.models.Vehicle;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.CategoryRepository;
import co.edu.unimagdalena.storelogistic.domain.fleet.ports.out.VehicleRepository;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleFilter;
import co.edu.unimagdalena.storelogistic.domain.fleet.values.VehicleStatus;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.jpa.VehicleJpaEntity;
import co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.jparepository.VehicleSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VehicleSpringRepository springRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Vehicle save(Vehicle vehicle) {
        var entity = VehicleJpaEntity.builder()
                .vehicleId(vehicle.getVehicleId())
                .categoryId(vehicle.getCategoryId())
                .loadCapacity(vehicle.getLoadCapacity().getWeightKg())
                .status(vehicle.getStatus().toString())
                .transporterId(vehicle.getTransporterId())
                .currentWeight(vehicle.getCurrentWeight())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();

        var savedEntity = springRepository.save(entity);
        return toDomainModel(savedEntity);
    }

    @Override
    public Optional<Vehicle> findById(Long vehicleId) {
        return springRepository.findById(vehicleId).map(this::toDomainModel);
    }

    @Override
    public List<Vehicle> findWithFilters(VehicleFilter filter) {
        Long categoryId = Optional.ofNullable(filter.getCategory())
                .flatMap(categoryRepository::findByType)
                .map(c -> c.getCategoryId())
                .orElse(null);

        BigDecimal minCapacity = Optional.ofNullable(filter.getMinCapacity())
                .map(LoadCapacity::getWeightKg)
                .orElse(null);
        BigDecimal maxCapacity = Optional.ofNullable(filter.getMaxCapacity())
                .map(LoadCapacity::getWeightKg)
                .orElse(null);
        String status = Optional.ofNullable(filter.getStatus())
                .map(VehicleStatus::toString)
                .orElse(null);

        return springRepository.findWithFilters(categoryId, status, minCapacity, maxCapacity)
                .stream().map(this::toDomainModel).toList();
    }

    private Vehicle toDomainModel(VehicleJpaEntity entity) {
        return Vehicle.builder()
                .vehicleId(entity.getVehicleId())
                .categoryId(entity.getCategoryId())
                .loadCapacity(new LoadCapacity(entity.getLoadCapacity()))
                .status(VehicleStatus.valueOf(entity.getStatus()))
                .transporterId(entity.getTransporterId())
                .currentWeight(entity.getCurrentWeight())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}