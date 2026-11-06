package br.com.motoservices.model;

import br.com.motoservices.model.enums.StatusOS;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro imutável de cada transição de status de uma OS.
 */
@Entity
@Table(name = "TB_OS_STATUS_HISTORICO", schema = "MOTOSERVICES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoStatusOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordem_de_servico_id", nullable = false)
    private OrdemDeServico ordemDeServico;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusOS statusNovo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}
