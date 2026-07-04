package br.com.motoservices.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI motoservicesOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MotoServices API")
                .version("0.0.1-SNAPSHOT")
                .description("Sistema de gestão de ordens de serviço para oficinas de motocicletas."));
    }
}
