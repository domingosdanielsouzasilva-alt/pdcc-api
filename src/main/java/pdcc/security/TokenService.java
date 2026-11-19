package pdcc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    private final SecretKey chave;
    private final long tempoExpiracao;

    public TokenService(
            @Value("${app.jwt-secret}") String jwtSecret,
            @Value("${app.jwt-expiration-ms:28800000}") long tempoExpiracao
    ) {
        this.chave = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.tempoExpiracao = tempoExpiracao;
    }

    public String gerarToken(String username, String nome, String role) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + tempoExpiracao);

        return Jwts.builder()
                .subject(username)
                .claim("nome", nome)
                .claim("role", role)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String obterUsername(String token) {
        return obterClaims(token).getSubject();
    }

    public String obterRole(String token) {
        return obterClaims(token).get("role", String.class);
    }

    public boolean tokenValido(String token) {
        try {
            Claims claims = obterClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims obterClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
