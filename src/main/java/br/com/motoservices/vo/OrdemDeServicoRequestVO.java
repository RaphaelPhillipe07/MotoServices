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
 * Entrada de abertura de OS: cliente e moto do catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrdemDeServicoRequestVO {

    @NotNull(message = "clienteId é obrigatório")
    @Positive(message = "clienteId deve ser positivo")
    private Long clienteId;

    @NotBlank(message = "marcaMoto é obrigatória")
    @Size(max = 100, message = "marcaMoto deve ter no máximo 100 caracteres")
    private String marcaMoto;

    @NotBlank(message = "modeloMoto é obrigatório")
    @Size(max = 100, message = "modeloMoto deve ter no máximo 100 caracteres")
    private String modeloMoto;
}
