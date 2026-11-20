package pdcc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pdcc.model.Servico;
import pdcc.model.Veiculo;
import pdcc.repository.ServicoRepository;
import pdcc.service.VeiculoPublicTokenService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/publico")
@CrossOrigin(origins = "*")
public class PublicoController {

    private final VeiculoPublicTokenService tokenService;
    private final ServicoRepository servicoRepository;

    public PublicoController(
            VeiculoPublicTokenService tokenService,
            ServicoRepository servicoRepository
    ) {
        this.tokenService = tokenService;
        this.servicoRepository = servicoRepository;
    }

    @GetMapping("/veiculos/{token}")
    public ResponseEntity<Map<String, Object>> consultar(@PathVariable String token) {
        Veiculo veiculo = tokenService.buscarVeiculoPorToken(token)
                .orElse(null);

        if (veiculo == null) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> historico = servicoRepository.findByVeiculo(veiculo)
                .stream()
                .map(this::mapearServico)
                .toList();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("modelo", veiculo.getModelo());
        resposta.put("placa", veiculo.getPlaca());
        resposta.put("ano", veiculo.getAno());
        resposta.put("cor", veiculo.getCor());
        resposta.put("historico", historico);

        return ResponseEntity.ok(resposta);
    }

    private Map<String, Object> mapearServico(Servico servico) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("dataServico", servico.getDataServico());
        dados.put("diagnostico", servico.getDiagnostico());
        dados.put("servicoExecutado", servico.getServicoExecutado());
        dados.put("pecasTrocadas", servico.getPecasTrocadas());
        dados.put("garantiaFim", servico.getGarantiaFim());
        dados.put("statusGarantia", obterStatusGarantia(servico));

        return dados;
    }

    private String obterStatusGarantia(Servico servico) {
        if (servico.getGarantiaFim() == null) {
            return "SEM_GARANTIA";
        }

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Fortaleza"));

        if (servico.getGarantiaFim().isBefore(hoje)) {
            return "EXPIRADA";
        }

        return "EM_GARANTIA";
    }
}
