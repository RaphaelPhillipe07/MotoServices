package br.com.motoservices.security;

import br.com.motoservices.dao.UsuarioDAO;
import br.com.motoservices.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega o usuário do banco para o {@code DaoAuthenticationProvider}.
 * O papel vira a authority {@code ROLE_<PAPEL>} usada nas regras de autorização.
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetalhesService implements UserDetailsService {

    private final UsuarioDAO usuarioDAO;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioDAO.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getSenha())
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name()))
                .disabled(!usuario.isAtivo())
                .build();
    }
}
