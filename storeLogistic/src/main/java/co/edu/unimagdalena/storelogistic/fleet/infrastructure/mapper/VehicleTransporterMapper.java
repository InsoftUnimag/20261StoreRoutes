package co.edu.unimagdalena.storelogistic.fleet.infrastructure.mapper;

import co.edu.unimagdalena.storelogistic.fleet.domain.models.Vehicle;
import co.edu.unimagdalena.storelogistic.fleet.infrastructure.web.dto.RequestTransporterResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VehicleTransporterMapper {

    @Mapping(target = "idVehiculo", source = "vehicleId")
    @Mapping(target = "idTransportista", source = "transporterId")
    RequestTransporterResponse toResponse(Vehicle vehicle);
}