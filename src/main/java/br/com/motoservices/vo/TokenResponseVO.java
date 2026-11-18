package br.com.motoservices.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resposta de {@code POST /auth/login}: token JWT e dados do usuário.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseVO {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UsuarioResponseVO usuario;
}
