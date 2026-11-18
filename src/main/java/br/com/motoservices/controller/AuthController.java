package br.com.motoservices.controller;

import br.com.motoservices.dao.UsuarioDAO;
import br.com.motoservices.exception.CredenciaisInvalidasException;
import br.com.motoservices.model.Usuario;
import br.com.motoservices.security.JwtService;
import br.com.motoservices.vo.LoginRequestVO;
import br.com.motoservices.vo.TokenResponseVO;
import br.com.motoservices.vo.UsuarioResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login e dados da sessão. O token é um JWT Bearer emitido pela própria aplicação.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e sessão do painel")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDAO usuarioDAO;
    private final JwtService jwtService;

    @Operation(summary = "Autenticar e obter token JWT",
            description = "Valida username/senha e devolve um token Bearer com validade configurável.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Credenciais válidas")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @PostMapping("/login")
    public ResponseEntity<TokenResponseVO> login(@Valid @RequestBody LoginRequestVO request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getSenha()));
        } catch (AuthenticationException ex) {
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = usuarioDAO.findByUsername(request.getUsername())
                .orElseThrow(CredenciaisInvalidasException::new);
        JwtService.TokenGerado token = jwtService.gerarToken(usuario);
        return ResponseEntity.ok(new TokenResponseVO(
                token.token(), "Bearer", token.expiresIn(), UsuarioResponseVO.from(usuario)));
    }

    @Operation(summary = "Dados do usuário autenticado",
            description = "Retorna o perfil associado ao token enviado no header Authorization.")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseVO> me(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarioDAO.findByUsername(jwt.getSubject())
                .filter(Usuario::isAtivo)
                .orElseThrow(CredenciaisInvalidasException::new);
        return ResponseEntity.ok(UsuarioResponseVO.from(usuario));
    }
}
