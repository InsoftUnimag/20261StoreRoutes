package co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stops")
public class StopJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stop")
    private Long stopId;

    @Column(name = "id_route", nullable = false)
    private Long routeId;

    @Column(name = "id_order", nullable = false)
    private Long orderId;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "customer_contact")
    private String customerContact;
}
