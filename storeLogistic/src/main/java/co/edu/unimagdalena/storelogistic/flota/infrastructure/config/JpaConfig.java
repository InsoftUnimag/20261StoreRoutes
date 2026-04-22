package co.edu.unimagdalena.storelogistic.flota.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "co.edu.unimagdalena.storelogistic.flota.infrastructure.persistence.jparepository")
public class JpaConfig {
}

