package br.com.motoservices.security;

import br.com.motoservices.dao.UsuarioDAO;
import br.com.motoservices.model.Usuario;
import br.com.motoservices.model.enums.Papel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o administrador inicial quando a tabela de usuários está vazia.
 *
 * <p>A senha vem de {@code ADMIN_PASSWORD} (padrão {@code admin123} apenas no perfil dev)
 * e é convertida em BCrypt antes de ser persistida. Depois do primeiro usuário, o
 * bootstrap não altera senhas existentes.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UsuarioDAO usuarioDAO;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SecurityProperties.Admin admin = properties.getAdmin();
        if (!admin.isHabilitado() || usuarioDAO.count() > 0) {
            return;
        }

        if (admin.getSenha() == null || admin.getSenha().isBlank()) {
            log.warn("Nenhum usuário cadastrado e ADMIN_PASSWORD não definido; "
                    + "o painel ficará sem acesso até um usuário ser criado.");
            return;
        }

        Usuario usuario = new Usuario(
                null,
                admin.getUsername(),
                admin.getNome(),
                passwordEncoder.encode(admin.getSenha()),
                Papel.ADMIN,
                true);
        usuarioDAO.save(usuario);
        log.info("Usuário administrador inicial '{}' criado.", admin.getUsername());
    }
}
