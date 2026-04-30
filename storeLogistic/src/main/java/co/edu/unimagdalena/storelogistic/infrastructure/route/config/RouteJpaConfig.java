package co.edu.unimagdalena.storelogistic.infrastructure.route.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "co.edu.unimagdalena.storelogistic.infrastructure.route.persistence.jparepository"
})
public class RouteJpaConfig {
}
