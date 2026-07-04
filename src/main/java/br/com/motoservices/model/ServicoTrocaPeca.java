package br.com.motoservices.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TB_SERVICO_TROCA_PECA", schema = "MOTOSERVICES")
@PrimaryKeyJoinColumn(name = "servico_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicoTrocaPeca extends ServicoDaOficina {

    @ManyToOne
    @JoinColumn(name = "peca_id")
    private Peca pecaEnvolvida;

    @Column(name = "peca_fornecida_pela_loja", nullable = false)
    private Boolean pecaFornecidaPelaLoja;

    @Override
    public double calcularValorFinal(Moto moto) {
        // TODO: aplicar fator de risco por cilindrada na mão de obra e, quando
        //       pecaFornecidaPelaLoja = true, somar o valor da peça
        throw new UnsupportedOperationException("Regra de precificação ainda não implementada.");
    }
}
