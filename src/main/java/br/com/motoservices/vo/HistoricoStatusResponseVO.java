package br.com.motoservices.vo;

import br.com.motoservices.model.enums.StatusOS;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoStatusResponseVO {

    private Long id;
    private Long osId;
    private StatusOS statusAnterior;
    private StatusOS statusNovo;
    private LocalDateTime dataHora;
}
