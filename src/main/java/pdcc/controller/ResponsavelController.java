package pdcc.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pdcc.model.Usuario;
import pdcc.repository.UsuarioRepository;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/responsaveis")
@CrossOrigin(origins = "*")
public class ResponsavelController {

    private final UsuarioRepository usuarioRepository;

    public ResponsavelController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<ResponsavelResponse> listar() {
        return usuarioRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        Usuario::getNome,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(usuario -> new ResponsavelResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getRole()
                ))
                .toList();
    }

    public record ResponsavelResponse(
            Long id,
            String nome,
            String role
    ) {
    }
}
