package br.com.motoservices.mapper;

import br.com.motoservices.model.HistoricoStatusOS;
import br.com.motoservices.vo.HistoricoStatusResponseVO;

/**
 * Conversão entre {@link HistoricoStatusOS} e VO de saída.
 */
public final class HistoricoStatusMapper {

    private HistoricoStatusMapper() {
    }

    public static HistoricoStatusResponseVO toResponse(HistoricoStatusOS historico) {
        return new HistoricoStatusResponseVO(
                historico.getId(),
                historico.getOrdemDeServico() == null ? null : historico.getOrdemDeServico().getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getDataHora()
        );
    }
}
