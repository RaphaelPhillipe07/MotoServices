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
public class OrdemDeServicoResponseVO {

    private Long id;
    private Long clienteId;
    private String clienteNome;
    private Long motoId;
    private String motoMarca;
    private String motoModelo;
    private Integer motoCilindradas;
    private StatusOS status;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;
}
