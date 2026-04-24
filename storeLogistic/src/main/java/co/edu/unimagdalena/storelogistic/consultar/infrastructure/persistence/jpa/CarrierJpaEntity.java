package co.edu.unimagdalena.storelogistic.consultar.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "carrier")
public class CarrierJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrier")
    private Long carrierId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "email", nullable = false, unique = true)
    private String email;
}
