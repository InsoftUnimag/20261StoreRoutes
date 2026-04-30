package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "CarrierEntity")
@Table(name = "vehiculos")
public class CarrierJpaEntity {

    @Id
    @Column(name = "id_vehiculo")
    private Long vehicleId;

    @Column(name = "id_transportista")
    private Long transporterId;
}
