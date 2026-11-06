package br.com.motoservices.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "TB_CLIENTE", schema = "MOTOSERVICES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /**
     * CPF (11 dígitos) ou CNPJ (14 dígitos), sempre sem pontuação.
     * A validação dos dígitos verificadores fica em {@code Documento} / {@code ClienteBO},
     * pois uma única constraint de CPF rejeitaria CNPJs.
     */
    @Column(nullable = false, unique = true)
    private String cpfCnpj;

    @Column(nullable = false)
    private String contato;

    @Column(name = "proxima_revisao")
    private LocalDate proximaRevisao;

    /**
     * Motos do cliente. O vínculo é criado automaticamente na primeira OS
     * (regra permissiva) e também pode ser gerenciado por endpoint próprio.
     */
    @ManyToMany
    @JoinTable(
            name = "TB_CLIENTE_MOTO",
            schema = "MOTOSERVICES",
            joinColumns = @JoinColumn(name = "cliente_id"),
            inverseJoinColumns = @JoinColumn(name = "moto_id"))
    private Set<Moto> motos = new HashSet<>();

    public Set<Moto> getMotos() {
        if (motos == null) {
            motos = new HashSet<>();
        }
        return motos;
    }
}
