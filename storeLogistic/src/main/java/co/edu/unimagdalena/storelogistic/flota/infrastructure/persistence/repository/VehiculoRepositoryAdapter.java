package co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.repository;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.CategoriaRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.VehiculoRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.values.CapacidadCarga;
import co.edu.unimagdalena.storelogistic.flota.domain.values.EstadoVehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.values.FiltroVehiculo;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jpa.VehiculoJpaEntity;
import co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jparepository.VehiculoSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VehiculoRepositoryAdapter implements VehiculoRepository {

    private final VehiculoSpringRepository springRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    public Vehiculo save(Vehiculo vehiculo) {
        var entity = VehiculoJpaEntity.builder()
                .idVehiculo(vehiculo.getIdVehiculo())
                .idCategoria(vehiculo.getIdCategoria())
                .capacidadCarga(vehiculo.getCapacidadCarga().getPesoKg())
                .estado(vehiculo.getEstado().toString())
                .idTransportista(vehiculo.getIdTransportista())
                .pesoActual(vehiculo.getPesoActual())
                .createdAt(vehiculo.getCreatedAt())
                .updatedAt(vehiculo.getUpdatedAt())
                .build();

        var savedEntity = springRepository.save(entity);
        return toDomainModel(savedEntity);
    }

    @Override
    public Optional<Vehiculo> findById(Long idVehiculo) {
        return springRepository.findById(idVehiculo).map(this::toDomainModel);
    }

    @Override
    public List<Vehiculo> findWithFilters(FiltroVehiculo filtro) {
        Long categoriaId = Optional.ofNullable(filtro.getCategoria())
                .flatMap(categoriaRepository::findByTipo)
                .map(c -> c.getIdCategoria())
                .orElse(null);

        BigDecimal capacidadMin = Optional.ofNullable(filtro.getCapacidadMin())
                .map(CapacidadCarga::getPesoKg)
                .orElse(null);
        BigDecimal capacidadMax = Optional.ofNullable(filtro.getCapacidadMax())
                .map(CapacidadCarga::getPesoKg)
                .orElse(null);
        String estado = Optional.ofNullable(filtro.getEstado())
                .map(EstadoVehiculo::toString)
                .orElse(null);

        return springRepository.findWithFilters(
                categoriaId,
                estado,
                capacidadMin,
                capacidadMax
        ).stream().map(this::toDomainModel).toList();
    }

    private Vehiculo toDomainModel(VehiculoJpaEntity entity) {
        return Vehiculo.builder()
                .idVehiculo(entity.getIdVehiculo())
                .idCategoria(entity.getIdCategoria())
                .capacidadCarga(new CapacidadCarga(entity.getCapacidadCarga()))
                .estado(EstadoVehiculo.valueOf(entity.getEstado()))
                .idTransportista(entity.getIdTransportista())
                .pesoActual(entity.getPesoActual())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}