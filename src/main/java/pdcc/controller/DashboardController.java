package pdcc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pdcc.model.Servico;
import pdcc.repository.ClienteRepository;
import pdcc.repository.ServicoRepository;
import pdcc.repository.VeiculoRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @GetMapping("/indicadores")
    public ResponseEntity<Map<String, Object>> indicadores() {
        Map<String, Object> dados = new HashMap<>();
        dados.put("totalClientes", clienteRepository.count());
        dados.put("totalVeiculos", veiculoRepository.count());
        dados.put("totalServicos", servicoRepository.count());
        dados.put("ultimosServicos", servicoRepository.findAll());
        return ResponseEntity.ok(dados);
    }
}
