package br.com.motoservices.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PecaResponseVO {

    private Long id;
    private String codigo;
    private String descricao;
    private BigDecimal valor;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;
}
