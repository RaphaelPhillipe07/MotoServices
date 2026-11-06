package br.com.motoservices.vo;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteVO {

    private Long id;

    @NotBlank(message = "nome é obrigatório")
    @Size(max = 200, message = "nome deve ter no máximo 200 caracteres")
    private String nome;

    /** CPF (11 dígitos) ou CNPJ (14 dígitos); a validação dos dígitos fica no domínio. */
    @NotBlank(message = "cpfCnpj é obrigatório")
    @Size(max = 20, message = "cpfCnpj deve ter no máximo 20 caracteres")
    private String cpfCnpj;

    @NotBlank(message = "contato é obrigatório")
    @Size(max = 100, message = "contato deve ter no máximo 100 caracteres")
    private String contato;

    @FutureOrPresent(message = "proximaRevisao não pode estar no passado")
    private LocalDate proximaRevisao;
}
