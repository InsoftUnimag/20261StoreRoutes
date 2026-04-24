package co.edu.unimagdalena.storelogistic.consultar.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "co.edu.unimagdalena.storelogistic.consultar.infrastructure.persistence.jparepository"
})
public class ConsultarJpaConfig {
}
