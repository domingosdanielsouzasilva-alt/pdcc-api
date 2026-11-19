package pdcc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pdcc.model.Veiculo;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByCliente_Id(Long clienteId);

    boolean existsByPlacaIgnoreCase(String placa);

    boolean existsByPlacaIgnoreCaseAndIdNot(String placa, Long id);
}
