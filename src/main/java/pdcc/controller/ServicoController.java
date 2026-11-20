package pdcc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pdcc.model.Servico;
import pdcc.model.Usuario;
import pdcc.model.Veiculo;
import pdcc.repository.ServicoRepository;
import pdcc.repository.UsuarioRepository;
import pdcc.repository.VeiculoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/servicos")
@CrossOrigin(origins = "*")
public class ServicoController {

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public List<Servico> listar() {
        return servicoRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Servico> salvar(@RequestBody Servico servico) {
        if (!dadosValidos(servico)) {
            return ResponseEntity.badRequest().build();
        }

        Veiculo veiculo = veiculoRepository.findById(servico.getVeiculo().getId())
                .orElse(null);

        if (veiculo == null) {
            return ResponseEntity.badRequest().build();
        }

        Usuario responsavel = usuarioRepository.findById(servico.getResponsavel().getId())
                .orElse(null);

        if (responsavel == null) {
            return ResponseEntity.badRequest().build();
        }

        servico.setVeiculo(veiculo);
        servico.setResponsavel(responsavel);

        Servico novoServico = servicoRepository.save(servico);
        return ResponseEntity.ok(novoServico);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Servico> buscarPorId(@PathVariable Long id) {
        return servicoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Servico> atualizar(
            @PathVariable Long id,
            @RequestBody Servico servico
    ) {
        Servico servicoExistente = servicoRepository.findById(id)
                .orElse(null);

        if (servicoExistente == null) {
            return ResponseEntity.notFound().build();
        }

        if (!dadosValidos(servico)) {
            return ResponseEntity.badRequest().build();
        }

        Veiculo veiculo = veiculoRepository.findById(servico.getVeiculo().getId())
                .orElse(null);

        if (veiculo == null) {
            return ResponseEntity.badRequest().build();
        }

        Usuario responsavel = usuarioRepository.findById(servico.getResponsavel().getId())
                .orElse(null);

        if (responsavel == null) {
            return ResponseEntity.badRequest().build();
        }

        servicoExistente.setDataServico(servico.getDataServico());
        servicoExistente.setDiagnostico(servico.getDiagnostico());
        servicoExistente.setServicoExecutado(servico.getServicoExecutado());
        servicoExistente.setPecasTrocadas(servico.getPecasTrocadas());
        servicoExistente.setValorServico(servico.getValorServico());
        servicoExistente.setGarantiaFim(servico.getGarantiaFim());
        servicoExistente.setVeiculo(veiculo);
        servicoExistente.setResponsavel(responsavel);

        Servico servicoAtualizado = servicoRepository.save(servicoExistente);
        return ResponseEntity.ok(servicoAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!servicoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        servicoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean dadosValidos(Servico servico) {
        if (servico.getVeiculo() == null || servico.getVeiculo().getId() == null) {
            return false;
        }

        if (servico.getResponsavel() == null || servico.getResponsavel().getId() == null) {
            return false;
        }

        if (servico.getDataServico() == null) {
            return false;
        }

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Fortaleza"));

        if (servico.getDataServico().isAfter(hoje)) {
            return false;
        }

        if (servico.getValorServico() != null
                && servico.getValorServico().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        return servico.getGarantiaFim() == null
                || !servico.getGarantiaFim().isBefore(servico.getDataServico());
    }
}
