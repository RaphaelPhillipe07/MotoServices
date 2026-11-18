package br.com.motoservices.vo;

import br.com.motoservices.model.Usuario;
import br.com.motoservices.model.enums.Papel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados públicos do usuário autenticado (nunca inclui a senha).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseVO {

    private Long id;
    private String username;
    private String nome;
    private Papel papel;

    public static UsuarioResponseVO from(Usuario usuario) {
        return new UsuarioResponseVO(
                usuario.getId(), usuario.getUsername(), usuario.getNome(), usuario.getPapel());
    }
}
