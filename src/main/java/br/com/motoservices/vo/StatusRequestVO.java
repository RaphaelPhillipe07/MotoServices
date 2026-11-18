package br.com.motoservices.vo;

import br.com.motoservices.model.enums.StatusOS;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrada de transição operacional de status da OS. FECHADA e CANCELADA têm endpoints próprios.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusRequestVO {

    @NotNull(message = "status é obrigatório")
    private StatusOS status;
}
