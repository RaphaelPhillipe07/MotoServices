package br.com.motoservices.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuração de autenticação. Em produção o segredo JWT e a senha do administrador
 * devem vir de variáveis de ambiente; os padrões existem apenas para desenvolvimento local.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "motoservices.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {

        /** Segredo HMAC-SHA256; precisa de pelo menos 32 caracteres. */
        private String secret;

        private String issuer = "motoservices";

        /** Validade do token em minutos. */
        private long expirationMinutes = 60;
    }

    @Getter
    @Setter
    public static class Admin {

        /** Cria o usuário inicial quando a tabela de usuários está vazia. */
        private boolean habilitado = true;

        private String username = "admin";

        private String nome = "Administrador";

        /** Senha em texto puro lida do ambiente e convertida em BCrypt na inicialização. */
        private String senha;
    }

    @Getter
    @Setter
    public static class Cors {

        /** Origens da SPA autorizadas a chamar a API diretamente (sem proxy). */
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:4173");
    }
}
