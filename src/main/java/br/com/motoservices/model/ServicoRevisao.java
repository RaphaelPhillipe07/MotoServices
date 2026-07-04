package br.com.motoservices.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TB_SERVICO_REVISAO", schema = "MOTOSERVICES")
@PrimaryKeyJoinColumn(name = "servico_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicoRevisao extends ServicoDaOficina {

    @Column(name = "checklist_geral")
    private Boolean checklistGeral;

    @Override
    public double calcularValorFinal(Moto moto) {
        // TODO: aplicar fator de risco por cilindrada
        throw new UnsupportedOperationException("Regra de precificação ainda não implementada.");
    }
}
