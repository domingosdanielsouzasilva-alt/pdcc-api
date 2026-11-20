package pdcc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pdcc.model.Veiculo;
import pdcc.model.VeiculoPublicToken;
import pdcc.repository.VeiculoPublicTokenRepository;
import pdcc.repository.VeiculoRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class VeiculoPublicTokenService {

    private final VeiculoPublicTokenRepository tokenRepository;
    private final VeiculoRepository veiculoRepository;

    public VeiculoPublicTokenService(
            VeiculoPublicTokenRepository tokenRepository,
            VeiculoRepository veiculoRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.veiculoRepository = veiculoRepository;
    }

    @Transactional
    public String obterOuCriarToken(Long veiculoId) {
        Optional<VeiculoPublicToken> tokenExistente =
                tokenRepository.findByVeiculo_Id(veiculoId);

        if (tokenExistente.isPresent()) {
            return tokenExistente.get().getToken();
        }

        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));

        VeiculoPublicToken novoToken = new VeiculoPublicToken();
        novoToken.setToken(UUID.randomUUID().toString());
        novoToken.setVeiculo(veiculo);

        return tokenRepository.save(novoToken).getToken();
    }

    public Optional<Veiculo> buscarVeiculoPorToken(String token) {
        return tokenRepository.findByToken(token)
                .map(VeiculoPublicToken::getVeiculo);
    }

    @Transactional
    public void excluirPorVeiculo(Long veiculoId) {
        tokenRepository.deleteByVeiculo_Id(veiculoId);
    }
}
