package pdcc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pdcc.model.Usuario;
import pdcc.repository.UsuarioRepository;
import pdcc.security.TokenService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Usuário ou senha não informados.")
            );
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            if (passwordEncoder.matches(password, usuario.getPassword())) {
                String role = String.valueOf(usuario.getRole());

                String token = tokenService.gerarToken(
                        usuario.getUsername(),
                        usuario.getNome(),
                        role
                );

                return ResponseEntity.ok(Map.of(
                        "mensagem", "Login efetuado com sucesso!",
                        "token", token,
                        "nome", usuario.getNome(),
                        "role", role
                ));
            }
        }

        return ResponseEntity.status(401).body(
                Map.of("error", "Usuário ou senha inválidos.")
        );
    }
}
