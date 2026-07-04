package br.com.motoservices.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PecaVO {

    private Long id;
    private String codigo;
    private String descricao;
    private Double valor;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;
}
