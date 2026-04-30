package co.edu.unimagdalena.storelogistic.infrastructure.fleet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "co.edu.unimagdalena.storelogistic.infrastructure.fleet.persistence.jparepository"
})
public class JpaConfig {
}