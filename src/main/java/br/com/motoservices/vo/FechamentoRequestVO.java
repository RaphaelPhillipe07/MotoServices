package br.com.motoservices.vo;

import br.com.motoservices.model.enums.FormaPagamento;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrada opcional do fechamento. Sem corpo, a OS é fechada sem forma de pagamento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FechamentoRequestVO {

    private FormaPagamento formaPagamento;
}
