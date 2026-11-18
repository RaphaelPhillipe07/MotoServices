package br.com.motoservices.vo;

import br.com.motoservices.model.enums.TipoServico;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicoResponseVO {

    private Long id;
    private TipoServico tipo;
    private String descricao;
    private Integer tempoEstimado;
    private BigDecimal valorBaseMaoDeObra;
    private BigDecimal valorFinal;
    private Long pecaId;
    private Boolean pecaFornecidaPelaLoja;
}
