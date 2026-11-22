package pdcc.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pdcc.model.Cliente;
import pdcc.model.Servico;
import pdcc.model.Veiculo;
import pdcc.repository.ClienteRepository;
import pdcc.repository.ServicoRepository;
import pdcc.repository.VeiculoRepository;
import pdcc.service.VeiculoPublicTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import pdcc.service.ProntuarioPdfService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private VeiculoPublicTokenService tokenService;

    @Autowired
    private ProntuarioPdfService prontuarioPdfService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping
    public List<Veiculo> listar() {
        return veiculoRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Veiculo> salvar(@Valid @RequestBody Veiculo veiculo) {
        if (veiculo.getCliente() == null || veiculo.getCliente().getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Cliente cliente = clienteRepository.findById(veiculo.getCliente().getId())
                .orElse(null);

        if (cliente == null) {
            return ResponseEntity.badRequest().build();
        }

        if (veiculo.getPlaca() != null && !veiculo.getPlaca().isBlank()) {
            String placa = veiculo.getPlaca().trim().toUpperCase();

            if (veiculoRepository.existsByPlacaIgnoreCase(placa)) {
                return ResponseEntity.status(409).build();
            }

            veiculo.setPlaca(placa);
        }

        veiculo.setCliente(cliente);

        Veiculo novoVeiculo = veiculoRepository.save(veiculo);
        return ResponseEntity.ok(novoVeiculo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Veiculo> buscarPorId(@PathVariable Long id) {
        return veiculoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Veiculo> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody Veiculo veiculo
    ) {
        Veiculo veiculoExistente = veiculoRepository.findById(id)
                .orElse(null);

        if (veiculoExistente == null) {
            return ResponseEntity.notFound().build();
        }

        if (veiculo.getCliente() == null || veiculo.getCliente().getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Cliente cliente = clienteRepository.findById(veiculo.getCliente().getId())
                .orElse(null);

        if (cliente == null) {
            return ResponseEntity.badRequest().build();
        }

        if (veiculo.getPlaca() != null && !veiculo.getPlaca().isBlank()) {
            String placa = veiculo.getPlaca().trim().toUpperCase();

            if (veiculoRepository.existsByPlacaIgnoreCaseAndIdNot(placa, id)) {
                return ResponseEntity.status(409).build();
            }

            veiculoExistente.setPlaca(placa);
        } else {
            veiculoExistente.setPlaca(veiculo.getPlaca());
        }

        veiculoExistente.setModelo(veiculo.getModelo());
        veiculoExistente.setAno(veiculo.getAno());
        veiculoExistente.setCor(veiculo.getCor());
        veiculoExistente.setCliente(cliente);

        Veiculo veiculoAtualizado = veiculoRepository.save(veiculoExistente);
        return ResponseEntity.ok(veiculoAtualizado);
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<Servico>> historico(@PathVariable Long id) {
        return veiculoRepository.findById(id)
                .map(veiculo -> ResponseEntity.ok(servicoRepository.findByVeiculo(veiculo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!veiculoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (servicoRepository.existsByVeiculo_Id(id)) {
            return ResponseEntity.status(409).build();
        }

        tokenService.excluirPorVeiculo(id);
        veiculoRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/prontuario")
    public ResponseEntity<byte[]> gerarProntuario(@PathVariable Long id) throws IOException {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElse(null);

        if (veiculo == null) {
            return ResponseEntity.notFound().build();
        }

        List<Servico> servicos = servicoRepository.findByVeiculo(veiculo);

        byte[] pdf = prontuarioPdfService.gerar(veiculo, servicos);

        String nomeArquivo = "prontuario-veiculo-" + id + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomeArquivo + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/{id}/qrcode")
    public void gerarQrCode(@PathVariable Long id, HttpServletResponse response) throws Exception {
        if (!veiculoRepository.existsById(id)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String token = tokenService.obterOuCriarToken(id);
        String url = frontendUrl + "/consulta.html?token=" + token;

        BitMatrix matrix = new MultiFormatWriter().encode(
                url,
                BarcodeFormat.QR_CODE,
                250,
                250
        );

        response.setContentType("image/png");

        OutputStream outputStream = response.getOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        outputStream.close();
    }
}
