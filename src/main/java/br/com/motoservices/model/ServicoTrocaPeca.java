package br.com.motoservices.model;

import br.com.motoservices.exception.ValidacaoException;
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

import java.math.BigDecimal;

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
    public BigDecimal calcularValorFinal(Moto moto) {
        BigDecimal maoDeObra = Precificacao.maoDeObraComFator(getValorBaseMaoDeObra(), moto);
        if (!usaPecaDaLoja()) {
            return maoDeObra;
        }
        if (pecaEnvolvida == null || pecaEnvolvida.getValor() == null) {
            throw new ValidacaoException(
                    "Serviço de troca de peça fornecida pela loja exige uma peça com valor cadastrado.");
        }
        return Precificacao.arredondar(maoDeObra.add(pecaEnvolvida.getValor()));
    }

    public BigDecimal valorDaPecaDaLoja() {
        if (!usaPecaDaLoja() || pecaEnvolvida == null) {
            return Precificacao.zero();
        }
        return Precificacao.arredondar(pecaEnvolvida.getValor());
    }

    public boolean usaPecaDaLoja() {
        return Boolean.TRUE.equals(pecaFornecidaPelaLoja);
    }
}
