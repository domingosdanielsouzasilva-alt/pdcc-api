package pdcc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pdcc.model.Servico;
import pdcc.repository.ServicoRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/garantias")
@CrossOrigin(origins = "*")
public class GarantiaController {

    @Autowired
    private ServicoRepository servicoRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listarGarantias() {
        List<Servico> servicos = servicoRepository.findAll();

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("servicos", servicos);
        resposta.put("hoje", LocalDate.now());

        return ResponseEntity.ok(resposta);
    }
}
