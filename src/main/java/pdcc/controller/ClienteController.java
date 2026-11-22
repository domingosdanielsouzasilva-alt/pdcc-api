package pdcc.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pdcc.model.Cliente;
import pdcc.repository.ClienteRepository;
import pdcc.repository.VeiculoRepository;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteRepository repository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @GetMapping
    public List<Cliente> listar() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Cliente> salvar(@Valid @RequestBody Cliente cliente) {
        Cliente novoCliente = repository.save(cliente);
        return ResponseEntity.ok(novoCliente);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody Cliente cliente
    ) {
        return repository.findById(id)
                .map(clienteExistente -> {
                    clienteExistente.setNome(cliente.getNome());
                    clienteExistente.setTelefone(cliente.getTelefone());
                    clienteExistente.setCpf(cliente.getCpf());
                    clienteExistente.setEndereco(cliente.getEndereco());

                    Cliente clienteAtualizado = repository.save(clienteExistente);
                    return ResponseEntity.ok(clienteAtualizado);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (veiculoRepository.existsByCliente_Id(id)) {
            return ResponseEntity.status(409).build();
        }

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
