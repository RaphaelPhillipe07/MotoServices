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
public class ServicoRankingResponseVO {

    private String descricao;
    private TipoServico tipo;
    private long quantidade;
    private BigDecimal totalBaseMaoDeObra;
}
