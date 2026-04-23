package co.edu.unimagdalena.storelogistic.fleet.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.domain.ports.out.CategoryRepository;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.CategoryType;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleFilter;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.VehicleStatus;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto.ListVehiclesResponse;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto.RegisterVehicleResponse;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto.VehicleDTO;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto.VehicleDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {Optional.class, LoadCapacity.class, VehicleStatus.class,
                   BigDecimal.class, CategoryType.class})
public abstract class VehicleMapper {

    @Autowired
    protected CategoryRepository categoryRepository;

    @Mapping(target = "category", expression = "java(resolveCategory(vehicle.getCategoryId()))")
    @Mapping(target = "loadCapacity", expression = "java(Optional.ofNullable(vehicle.getLoadCapacity()).map(LoadCapacity::getWeightKg).orElse(null))")
    @Mapping(target = "status", expression = "java(Optional.ofNullable(vehicle.getStatus()).map(VehicleStatus::toString).orElse(null))")
    @Mapping(target = "occupancyPercentage", expression = "java(vehicle.occupancyPercentage())")
    public abstract VehicleDTO toVehicleDTO(Vehicle vehicle);

    @Mapping(target = "category", expression = "java(resolveCategory(vehicle.getCategoryId()))")
    @Mapping(target = "loadCapacity", expression = "java(Optional.ofNullable(vehicle.getLoadCapacity()).map(LoadCapacity::getWeightKg).orElse(null))")
    @Mapping(target = "status", expression = "java(Optional.ofNullable(vehicle.getStatus()).map(VehicleStatus::toString).orElse(null))")
    @Mapping(target = "occupancyPercentage", expression = "java(vehicle.occupancyPercentage())")
    public abstract VehicleDetailResponse toVehicleDetailResponse(Vehicle vehicle);

    @Mapping(target = "status", expression = "java(Optional.ofNullable(vehicle.getStatus()).map(VehicleStatus::toString).orElse(null))")
    public abstract RegisterVehicleResponse toRegisterVehicleResponse(Vehicle vehicle);

    public ListVehiclesResponse toListVehiclesResponse(List<Vehicle> vehicles) {
        var dtos = vehicles.stream()
                .map(this::toVehicleDTO)
                .collect(Collectors.toList());
        return ListVehiclesResponse.builder()
                .total(dtos.size())
                .data(dtos)
                .build();
    }

    public VehicleFilter toVehicleFilter(String category, String status,
                                         Double minCapacity, Double maxCapacity) {
        CategoryType categoryType = Optional.ofNullable(category)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return Optional.of(CategoryType.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);

        VehicleStatus vehicleStatus = Optional.ofNullable(status)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return Optional.of(VehicleStatus.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);

        LoadCapacity capMin = Optional.ofNullable(minCapacity)
                .map(d -> new LoadCapacity(BigDecimal.valueOf(d)))
                .orElse(null);
        LoadCapacity capMax = Optional.ofNullable(maxCapacity)
                .map(d -> new LoadCapacity(BigDecimal.valueOf(d)))
                .orElse(null);

        return VehicleFilter.builder()
                .category(categoryType)
                .status(vehicleStatus)
                .minCapacity(capMin)
                .maxCapacity(capMax)
                .build();
    }

    protected String resolveCategory(Long categoryId) {
        return Optional.ofNullable(categoryId)
                .flatMap(id -> categoryRepository.findById(id)
                        .map(c -> c.getType().toString()))
                .orElse(null);
    }
}