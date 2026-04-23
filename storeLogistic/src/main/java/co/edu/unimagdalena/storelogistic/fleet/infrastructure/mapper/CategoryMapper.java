package co.edu.unimagdalena.storelogistic.fleet.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Category;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.CategoryType;
import co.edu.unimagdalena.storelogistic.fleet.domain.values.LoadCapacity;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.persistence.jpa.CategoryJpaEntity;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Optional;

@Slf4j
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {LoadCapacity.class, Optional.class, CategoryType.class})
public abstract class CategoryMapper {

    @Mapping(target = "type", expression = "java(parseType(entity.getType()))")
    @Mapping(target = "maxCapacity", expression = "java(new LoadCapacity(entity.getMaxCapacityKg()))")
    public abstract Category toDomain(CategoryJpaEntity entity);

    @Mapping(target = "type", expression = "java(Optional.ofNullable(domain.getType()).map(CategoryType::name).orElse(null))")
    @Mapping(target = "maxCapacityKg", source = "maxCapacity.weightKg")
    public abstract CategoryJpaEntity toEntity(Category domain);

    protected CategoryType parseType(String type) {
        return Optional.ofNullable(type)
                .flatMap(t -> {
                    try {
                        return Optional.of(CategoryType.valueOf(t));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid category type in DB: '{}'", t);
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }
}