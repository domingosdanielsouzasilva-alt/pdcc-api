package pdcc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pdcc.model.VeiculoPublicToken;

import java.util.Optional;

public interface VeiculoPublicTokenRepository extends JpaRepository<VeiculoPublicToken, Long> {

    Optional<VeiculoPublicToken> findByToken(String token);

    Optional<VeiculoPublicToken> findByVeiculo_Id(Long veiculoId);

    void deleteByVeiculo_Id(Long veiculoId);
}
