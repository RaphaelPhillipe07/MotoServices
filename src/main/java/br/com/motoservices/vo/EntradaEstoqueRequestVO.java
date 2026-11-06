package br.com.motoservices.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrada de estoque (compra/recebimento) de uma peça existente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntradaEstoqueRequestVO {

    @NotNull(message = "quantidade é obrigatória")
    @Positive(message = "quantidade deve ser positiva")
    private Integer quantidade;
}
