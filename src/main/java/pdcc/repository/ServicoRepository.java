package pdcc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pdcc.model.Servico;
import pdcc.model.Veiculo;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByVeiculo(Veiculo veiculo);
}
