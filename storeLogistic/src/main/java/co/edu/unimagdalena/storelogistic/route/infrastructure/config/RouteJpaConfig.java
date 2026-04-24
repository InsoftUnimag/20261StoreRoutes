package co.edu.unimagdalena.storelogistic.route.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "co.edu.unimagdalena.storelogistic.route.infrastructure.persistence.jparepository"
})
public class RouteJpaConfig {
}
