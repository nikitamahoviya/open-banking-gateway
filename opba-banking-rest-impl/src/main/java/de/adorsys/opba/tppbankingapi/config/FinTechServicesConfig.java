package de.adorsys.opba.tppbankingapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Data
@Configuration
@EnableConfigurationProperties
@ComponentScan("de.adorsys.opba.api.security.service")
@PropertySource("classpath:fintech-db.yml")
public class FinTechServicesConfig {

    private final Environment env;

}
