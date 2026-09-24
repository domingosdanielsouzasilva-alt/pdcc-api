package pdcc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pdcc.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
