package pdcc.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String modelo;

    private String placa;

    private String ano;

    private String cor;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
