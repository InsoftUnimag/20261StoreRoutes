package co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableJpaRepositories(basePackages = {
        "co.edu.unimagdalena.storelogistic.orderstatus.infrastructure.persistence.jparepository"
})
public class OrderStatusJpaConfig {
}
