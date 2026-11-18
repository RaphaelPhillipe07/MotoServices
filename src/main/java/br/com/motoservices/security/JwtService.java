package br.com.motoservices.security;

import br.com.motoservices.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Emite tokens JWT com subject, papel e nome do usuário.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    public TokenGerado gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        long expiracaoSegundos = properties.getJwt().getExpirationMinutes() * 60;
        Instant expiraEm = agora.plus(expiracaoSegundos, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(usuario.getUsername())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim("papel", usuario.getPapel().name())
                .claim("nome", usuario.getNome())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenGerado(token, expiracaoSegundos);
    }

    /**
     * Token emitido e sua validade em segundos.
     */
    public record TokenGerado(String token, long expiresIn) {
    }
}
