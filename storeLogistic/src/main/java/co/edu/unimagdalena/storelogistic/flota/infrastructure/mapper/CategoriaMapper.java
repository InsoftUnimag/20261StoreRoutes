package co.edu.unimagdalena.storelogistic.flota.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Categoria;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jpa.CategoriaJpaEntity;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Optional;

@Slf4j
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {CapacidadCarga.class, Optional.class, TipoCategoria.class})
public abstract class CategoriaMapper {

    @Mapping(target = "tipo", expression = "java(parseTipo(entity.getTipo()))")
    @Mapping(target = "capacidadMaxima", expression = "java(new CapacidadCarga(entity.getCapacidadMaximaKg()))")
    public abstract Categoria toDomain(CategoriaJpaEntity entity);

    @Mapping(target = "tipo", expression = "java(Optional.ofNullable(domain.getTipo()).map(TipoCategoria::name).orElse(null))")
    @Mapping(target = "capacidadMaximaKg", source = "capacidadMaxima.pesoKg")
    public abstract CategoriaJpaEntity toEntity(Categoria domain);

    protected TipoCategoria parseTipo(String tipo) {
        return Optional.ofNullable(tipo)
                .flatMap(t -> {
                    try {
                        return Optional.of(TipoCategoria.valueOf(t));
                    } catch (IllegalArgumentException e) {
                        log.warn("Tipo de categoría inválido en DB: '{}'", t);
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }
}