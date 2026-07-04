package br.com.motoservices.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoVO {

    private Double totalPecas;

    private Double totalMaoDeObra;

    private Double descontoAplicado;

    private Double totalFinal;
}
