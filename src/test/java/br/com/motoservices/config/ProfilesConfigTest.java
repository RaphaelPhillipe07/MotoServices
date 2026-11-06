package br.com.motoservices.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfilesConfigTest {

    private PropertySource<?> carregar(String arquivo) throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load(arquivo, new ClassPathResource(arquivo));
        assertEquals(1, sources.size(), arquivo + " deve ser um YAML válido");
        return sources.get(0);
    }

    @Test
    @DisplayName("perfil padrão é dev")
    void perfilPadraoEhDev() throws Exception {
        assertEquals("${SPRING_PROFILES_ACTIVE:dev}",
                carregar("application.yml").getProperty("spring.profiles.active"));
    }

    @Test
    @DisplayName("dev mantém valores padrão localhost/postgres para uso local")
    void devTemValoresLocais() throws Exception {
        PropertySource<?> dev = carregar("application-dev.yml");

        assertEquals("${DB_USER:postgres}", dev.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASS:postgres}", dev.getProperty("spring.datasource.password"));
        assertEquals(true, dev.getProperty("spring.jpa.properties.hibernate.format_sql"));
    }

    @Test
    @DisplayName("prod não define credenciais padrão e exige variáveis de ambiente")
    void prodSemCredenciaisPadrao() throws Exception {
        PropertySource<?> prod = carregar("application-prod.yml");

        assertEquals("${DB_URL}", prod.getProperty("spring.datasource.url"));
        assertEquals("${DB_USER}", prod.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASS}", prod.getProperty("spring.datasource.password"));
        assertNull(prod.getProperty("spring.datasource.password.default"));
    }

    @Test
    @DisplayName("documentação interativa fica desabilitada em produção")
    void prodDesabilitaSwagger() throws Exception {
        PropertySource<?> prod = carregar("application-prod.yml");

        assertEquals(false, prod.getProperty("springdoc.api-docs.enabled"));
        assertEquals(false, prod.getProperty("springdoc.swagger-ui.enabled"));
    }
}
