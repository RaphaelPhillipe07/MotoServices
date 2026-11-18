package br.com.motoservices.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Totais de faturamento de OS encerradas em um período, calculados pelo snapshot do fechamento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FaturamentoResponseVO {

    private LocalDate inicio;
    private LocalDate fim;
    private BigDecimal totalPecas;
    private BigDecimal totalMaoDeObra;
    private BigDecimal descontoAplicado;
    private BigDecimal totalFinal;
    private BigDecimal ticketMedio;
    private long quantidadeOs;
}
