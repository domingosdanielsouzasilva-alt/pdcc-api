package pdcc.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "veiculo_public_token")
public class VeiculoPublicToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @OneToOne
    @JoinColumn(name = "veiculo_id", nullable = false, unique = true)
    private Veiculo veiculo;
}
