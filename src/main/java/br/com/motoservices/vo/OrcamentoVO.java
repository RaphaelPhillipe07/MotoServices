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
public class OrcamentoVO {

    private BigDecimal totalPecas;

    private BigDecimal totalMaoDeObra;

    private BigDecimal descontoAplicado;

    private BigDecimal totalFinal;
}
