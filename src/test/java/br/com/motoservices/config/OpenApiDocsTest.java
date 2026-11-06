package br.com.motoservices.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sobe a aplicação completa contra um PostgreSQL real (embarcado) e valida:
 * migrações Flyway, mapeamento JPA ({@code ddl-auto: validate}) e o documento OpenAPI gerado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocsTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void encerrar() throws IOException {
        POSTGRES.close();
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("documento OpenAPI expõe recursos, erros RFC 9457 e metadados")
    void expoeDocumentoOpenApi() {
        ResponseEntity<String> resposta = rest.getForEntity("/v3/api-docs", String.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        String corpo = resposta.getBody();
        assertNotNull(corpo);
        assertAll(
                () -> assertTrue(corpo.contains("\"/clientes\""), "recurso de clientes"),
                () -> assertTrue(corpo.contains("\"/motos\""), "recurso de motos"),
                () -> assertTrue(corpo.contains("\"/pecas\""), "recurso de peças"),
                () -> assertTrue(corpo.contains("\"/ordens-servico\""), "recurso de OS"),
                () -> assertTrue(corpo.contains("application/problem+json"), "contrato de erro"),
                () -> assertTrue(corpo.contains("MotoServices API"), "título da API")
        );
    }

    @Test
    @DisplayName("actuator health responde UP com banco disponível")
    void healthUp() {
        ResponseEntity<String> resposta = rest.getForEntity("/actuator/health", String.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertTrue(resposta.getBody().contains("\"status\":\"UP\""));
    }
}
