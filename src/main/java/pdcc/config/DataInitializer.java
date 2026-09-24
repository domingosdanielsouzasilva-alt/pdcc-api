package pdcc.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pdcc.model.Usuario;
import pdcc.repository.UsuarioRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner criarUsuarioAdmin(
            UsuarioRepository usuarioRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (usuarioRepository.findByUsername("admin").isEmpty()) {
                Usuario usuario = new Usuario();
                usuario.setNome("Administrador");
                usuario.setUsername("admin");
                usuario.setPassword(passwordEncoder.encode("123456"));
                usuario.setRole("ADMIN");

                usuarioRepository.save(usuario);
            }
        };
    }
}
