package br.com.motoservices.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrada de cadastro de moto no catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MotoRequestVO {

    @NotBlank(message = "marca é obrigatória")
    @Size(max = 100, message = "marca deve ter no máximo 100 caracteres")
    private String marca;

    @NotBlank(message = "modelo é obrigatório")
    @Size(max = 100, message = "modelo deve ter no máximo 100 caracteres")
    private String modelo;

    @NotNull(message = "cilindradas é obrigatório")
    @Positive(message = "cilindradas deve ser positiva")
    private Integer cilindradas;
}
