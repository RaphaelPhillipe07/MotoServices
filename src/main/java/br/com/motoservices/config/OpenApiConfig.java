package br.com.motoservices.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI motoservicesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MotoServices API")
                        .version("0.4.0")
                        .description("""
                                Sistema de gestão de ordens de serviço para oficinas de motocicletas.

                                Erros seguem a RFC 9457 (`application/problem+json`) com os campos
                                `type`, `title`, `status`, `detail`, `instance` e, em validações,
                                `errors: [{field, message}]`.

                                Listagens paginadas usam o envelope
                                `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

                                As rotas protegidas exigem `Authorization: Bearer <token>` obtido em
                                `POST /auth/login`.
                                """)
                        .contact(new Contact().name("MotoServices"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Desenvolvimento local")));
    }
}
