package br.com.motoservices.vo;

import br.com.motoservices.model.enums.TipoServico;
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
 * Entrada de um serviço a ser adicionado à OS.
 *
 * <p>Para {@code REVISAO}, usa-se {@code checklistGeral}.
 * Para {@code TROCA_PECA}, {@code pecaFornecidaPelaLoja} é obrigatório e,
 * quando verdadeiro, {@code pecaId} deve apontar para uma peça cadastrada.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicoRequestVO {

    @NotNull(message = "tipo é obrigatório")
    private TipoServico tipo;

    @NotBlank(message = "descricao é obrigatória")
    @Size(max = 255, message = "descricao deve ter no máximo 255 caracteres")
    private String descricao;

    @PositiveOrZero(message = "tempoEstimado não pode ser negativo")
    private Integer tempoEstimado;

    @NotNull(message = "valorBaseMaoDeObra é obrigatório")
    @PositiveOrZero(message = "valorBaseMaoDeObra não pode ser negativo")
    @Digits(integer = 10, fraction = 2, message = "valorBaseMaoDeObra deve ter no máximo 2 casas decimais")
    private BigDecimal valorBaseMaoDeObra;

    private Boolean checklistGeral;

    private Long pecaId;

    private Boolean pecaFornecidaPelaLoja;
}
