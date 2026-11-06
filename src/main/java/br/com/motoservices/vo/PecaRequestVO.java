package br.com.motoservices.vo;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entrada de cadastro de peça no estoque.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PecaRequestVO {

    @NotBlank(message = "codigo é obrigatório")
    @Size(max = 50, message = "codigo deve ter no máximo 50 caracteres")
    private String codigo;

    @NotBlank(message = "descricao é obrigatória")
    @Size(max = 255, message = "descricao deve ter no máximo 255 caracteres")
    private String descricao;

    @NotNull(message = "valor é obrigatório")
    @PositiveOrZero(message = "valor não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "valor deve ter no máximo 2 casas decimais")
    private BigDecimal valor;

    @NotNull(message = "quantidadeEstoque é obrigatória")
    @PositiveOrZero(message = "quantidadeEstoque não pode ser negativa")
    private Integer quantidadeEstoque;

    @NotNull(message = "estoqueMinimo é obrigatório")
    @PositiveOrZero(message = "estoqueMinimo não pode ser negativo")
    private Integer estoqueMinimo;
}
