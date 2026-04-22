package co.edu.unimagdalena.storelogistic.flota.application.services;

import co.edu.unimagdalena.storelogistic.flota.domain.models.Vehiculo;
import co.edu.unimagdalena.storelogistic.flota.domain.exceptions.VehiculoNotFoundException;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.in.ObtenerVehiculoUseCase;
import co.edu.unimagdalena.storelogistic.flota.domain.ports.out.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObtenerVehiculoService implements ObtenerVehiculoUseCase {
    private final VehiculoRepository vehiculoRepository;

    @Override
    public Vehiculo obtener(Long idVehiculo) {
        log.info("Obteniendo vehículo con ID: {}", idVehiculo);
        var vehiculo = vehiculoRepository.findById(idVehiculo)
                .orElseThrow(() -> new VehiculoNotFoundException(idVehiculo));
        log.info("Vehículo encontrado: ID={}, Estado={}", vehiculo.getIdVehiculo(), vehiculo.getEstado());
        return vehiculo;
    }
}

