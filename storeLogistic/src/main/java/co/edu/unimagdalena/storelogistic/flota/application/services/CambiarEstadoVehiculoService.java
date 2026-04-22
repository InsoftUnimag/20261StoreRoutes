package co.edu.unimagdalena.storelogistic.flota.application.services;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.exceptions.VehiculoNotFoundException;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.CambiarEstadoVehiculoUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.VehiculoRepository;
import co.edu.unimagdalena.storelogistic.flota.domain.values.EstadoVehiculo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CambiarEstadoVehiculoService implements CambiarEstadoVehiculoUseCase {
    private final VehiculoRepository vehiculoRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vehiculo cambiar(Long idVehiculo, EstadoVehiculo nuevoEstado) {
        log.info("Cambiando estado del vehículo ID: {} a: {}", idVehiculo, nuevoEstado);

        var vehiculo = vehiculoRepository.findById(idVehiculo)
                .orElseThrow(() -> new VehiculoNotFoundException(idVehiculo));

        var estadoAnterior = vehiculo.getEstado();
        vehiculo.cambiarEstado(nuevoEstado);
        var actualizado = vehiculoRepository.save(vehiculo);

        log.info("Estado del vehículo {} cambiado exitosamente de {} a {}",
                idVehiculo, estadoAnterior, nuevoEstado);

        return actualizado;
    }
}
