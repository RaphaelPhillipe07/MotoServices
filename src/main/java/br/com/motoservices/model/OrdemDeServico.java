package br.com.motoservices.model;

import br.com.motoservices.exception.TransicaoStatusInvalidaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.enums.StatusOS;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "TB_ORDEM_DE_SERVICO", schema = "MOTOSERVICES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrdemDeServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moto_id", nullable = false)
    private Moto moto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOS status;

    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServicoDaOficina> servicos = new ArrayList<>();

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    /**
     * Snapshot imutável do orçamento no momento do fechamento.
     * Nulo enquanto a OS não foi fechada.
     */
    @Column(name = "total_pecas", precision = 12, scale = 2)
    private BigDecimal totalPecas;

    @Column(name = "total_mao_de_obra", precision = 12, scale = 2)
    private BigDecimal totalMaoDeObra;

    @Column(name = "desconto_aplicado", precision = 12, scale = 2)
    private BigDecimal descontoAplicado;

    @Column(name = "total_final", precision = 12, scale = 2)
    private BigDecimal totalFinal;

    @Version
    @Column(nullable = false)
    private Long versao;

    public void adicionarServico(ServicoDaOficina servico) {
        Objects.requireNonNull(servico, "Serviço é obrigatório");
        if (status != null && !status.permiteAdicionarServico()) {
            throw new ValidacaoException(
                    "Não é possível adicionar serviços a uma OS com status " + status + ".");
        }
        servico.setOrdemDeServico(this);
        servicos.add(servico);
    }

    public void removerServico(ServicoDaOficina servico) {
        if (servico != null && servicos.remove(servico)) {
            servico.setOrdemDeServico(null);
        }
    }

    /**
     * Aplica uma transição de status validada pela máquina de estados da OS.
     */
    public void alterarStatus(StatusOS novoStatus) {
        if (status == null) {
            throw new ValidacaoException("OS sem status definido.");
        }
        if (novoStatus == null) {
            throw new ValidacaoException("Novo status é obrigatório.");
        }
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(
                    "Transição de status inválida: " + status + " → " + novoStatus + ".");
        }
        this.status = novoStatus;
        if (novoStatus.isEncerrada() && dataFechamento == null) {
            this.dataFechamento = LocalDateTime.now();
        }
    }
}
