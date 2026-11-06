package br.com.motoservices.mapper;

import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.vo.OrdemDeServicoResponseVO;

/**
 * Conversão entre entidade {@link OrdemDeServico} e VO de saída.
 */
public final class OrdemDeServicoMapper {

    private OrdemDeServicoMapper() {
    }

    public static OrdemDeServicoResponseVO toResponse(OrdemDeServico os) {
        return new OrdemDeServicoResponseVO(
                os.getId(),
                os.getCliente() == null ? null : os.getCliente().getId(),
                os.getCliente() == null ? null : os.getCliente().getNome(),
                os.getMoto() == null ? null : os.getMoto().getId(),
                os.getMoto() == null ? null : os.getMoto().getMarca(),
                os.getMoto() == null ? null : os.getMoto().getModelo(),
                os.getMoto() == null ? null : os.getMoto().getCilindradas(),
                os.getStatus(),
                os.getDataAbertura(),
                os.getDataFechamento(),
                os.getTotalPecas(),
                os.getTotalMaoDeObra(),
                os.getDescontoAplicado(),
                os.getTotalFinal()
        );
    }
}
