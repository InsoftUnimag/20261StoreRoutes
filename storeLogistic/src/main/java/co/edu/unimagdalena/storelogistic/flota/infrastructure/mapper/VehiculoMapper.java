package co.edu.unimagdalena.storelogistic.flota.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.CategoriaRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import co.edu.unimagdalena.storelogistic.flota.domain.values.EstadoVehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.FiltroVehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.TipoCategoria;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto.ListaVehiculosResponse;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto.RegistrarVehiculoResponse;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto.VehiculoDTO;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.web.dto.VehiculoDetailResponse;
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
        imports = {Optional.class, CapacidadCarga.class, EstadoVehiculo.class,
                   BigDecimal.class, TipoCategoria.class})
public abstract class VehiculoMapper {

    @Autowired
    protected CategoriaRepository categoriaRepository;

    @Mapping(target = "categoria", expression = "java(resolverCategoria(vehiculo.getIdCategoria()))")
    @Mapping(target = "capacidadCarga", expression = "java(Optional.ofNullable(vehiculo.getCapacidadCarga()).map(CapacidadCarga::getPesoKg).orElse(null))")
    @Mapping(target = "estado", expression = "java(Optional.ofNullable(vehiculo.getEstado()).map(EstadoVehiculo::toString).orElse(null))")
    @Mapping(target = "porcentajeOcupacion", expression = "java(vehiculo.porcentajeOcupacion())")
    public abstract VehiculoDTO toVehiculoDTO(Vehiculo vehiculo);

    @Mapping(target = "categoria", expression = "java(resolverCategoria(vehiculo.getIdCategoria()))")
    @Mapping(target = "capacidadCarga", expression = "java(Optional.ofNullable(vehiculo.getCapacidadCarga()).map(CapacidadCarga::getPesoKg).orElse(null))")
    @Mapping(target = "estado", expression = "java(Optional.ofNullable(vehiculo.getEstado()).map(EstadoVehiculo::toString).orElse(null))")
    @Mapping(target = "porcentajeOcupacion", expression = "java(vehiculo.porcentajeOcupacion())")
    public abstract VehiculoDetailResponse toVehiculoDetailResponse(Vehiculo vehiculo);

    @Mapping(target = "estado", expression = "java(Optional.ofNullable(vehiculo.getEstado()).map(EstadoVehiculo::toString).orElse(null))")
    public abstract RegistrarVehiculoResponse toRegistrarVehiculoResponse(Vehiculo vehiculo);

    public ListaVehiculosResponse toListaVehiculosResponse(List<Vehiculo> vehiculos) {
        var dtos = vehiculos.stream()
                .map(this::toVehiculoDTO)
                .collect(Collectors.toList());
        return ListaVehiculosResponse.builder()
                .total(dtos.size())
                .data(dtos)
                .build();
    }

    public FiltroVehiculo toFiltroVehiculo(String categoria, String estado,
                                           Double capacidadMin, Double capacidadMax) {
        TipoCategoria tipoCategoria = Optional.ofNullable(categoria)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return Optional.of(TipoCategoria.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);

        EstadoVehiculo estadoEnum = Optional.ofNullable(estado)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return Optional.of(EstadoVehiculo.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);

        CapacidadCarga capMin = Optional.ofNullable(capacidadMin)
                .map(d -> new CapacidadCarga(BigDecimal.valueOf(d)))
                .orElse(null);
        CapacidadCarga capMax = Optional.ofNullable(capacidadMax)
                .map(d -> new CapacidadCarga(BigDecimal.valueOf(d)))
                .orElse(null);

        return FiltroVehiculo.builder()
                .categoria(tipoCategoria)
                .estado(estadoEnum)
                .capacidadMin(capMin)
                .capacidadMax(capMax)
                .build();
    }

    protected String resolverCategoria(Long idCategoria) {
        return Optional.ofNullable(idCategoria)
                .flatMap(id -> categoriaRepository.findById(id)
                        .map(c -> c.getTipo().toString()))
                .orElse(null);
    }
}