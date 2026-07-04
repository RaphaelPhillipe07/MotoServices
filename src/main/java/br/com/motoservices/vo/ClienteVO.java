package br.com.motoservices.vo;

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
    private String nome;
    private String cpfCnpj;
    private String contato;
    private LocalDate proximaRevisao;
}
