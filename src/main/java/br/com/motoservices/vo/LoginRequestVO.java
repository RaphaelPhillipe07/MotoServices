package br.com.motoservices.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credenciais enviadas em {@code POST /auth/login}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestVO {

    @NotBlank(message = "username é obrigatório")
    @Size(max = 60, message = "username deve ter no máximo 60 caracteres")
    private String username;

    @NotBlank(message = "senha é obrigatória")
    @Size(max = 100, message = "senha deve ter no máximo 100 caracteres")
    private String senha;
}
