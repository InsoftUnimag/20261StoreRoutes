package co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "co.edu.unimagdalena.storelogistic.infrastructure.orderstatus.persistence.jparepository"
})
public class OrderStatusJpaConfig {
}
