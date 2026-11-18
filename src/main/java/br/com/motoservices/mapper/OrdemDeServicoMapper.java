package br.com.motoservices.mapper;

import br.com.motoservices.model.Moto;
import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.ServicoDaOficina;
import br.com.motoservices.model.ServicoTrocaPeca;
import br.com.motoservices.model.enums.TipoServico;
import br.com.motoservices.vo.OrdemDeServicoResponseVO;
import br.com.motoservices.vo.ServicoResponseVO;

import java.util.List;

/**
 * Conversão entre entidade {@link OrdemDeServico} e VO de saída.
 */
public final class OrdemDeServicoMapper {

    private OrdemDeServicoMapper() {
    }

    public static OrdemDeServicoResponseVO toResponse(OrdemDeServico os) {
        List<ServicoResponseVO> servicos = os.getServicos().stream()
                .map(servico -> toServico(servico, os.getMoto()))
                .toList();

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
                os.getFormaPagamento(),
                os.getTotalPecas(),
                os.getTotalMaoDeObra(),
                os.getDescontoAplicado(),
                os.getTotalFinal(),
                servicos
        );
    }

    private static ServicoResponseVO toServico(ServicoDaOficina servico, Moto moto) {
        ServicoTrocaPeca troca = servico instanceof ServicoTrocaPeca t ? t : null;
        return new ServicoResponseVO(
                servico.getId(),
                troca == null ? TipoServico.REVISAO : TipoServico.TROCA_PECA,
                servico.getDescricao(),
                servico.getTempoEstimado(),
                servico.getValorBaseMaoDeObra(),
                servico.calcularValorFinal(moto),
                troca == null || troca.getPecaEnvolvida() == null
                        ? null : troca.getPecaEnvolvida().getId(),
                troca == null ? null : troca.getPecaFornecidaPelaLoja()
        );
    }
}
