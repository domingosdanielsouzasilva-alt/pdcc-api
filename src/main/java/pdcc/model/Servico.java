package pdcc.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dataServico;

    @Column(length = 1000)
    private String diagnostico;

    @Column(length = 1000)
    private String servicoExecutado;

    private String pecasTrocadas;

    private BigDecimal valorServico;

    private LocalDate garantiaFim;

    @ManyToOne
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;
}
