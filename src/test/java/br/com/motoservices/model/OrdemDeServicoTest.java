package br.com.motoservices.model;

import br.com.motoservices.exception.TransicaoStatusInvalidaException;
import br.com.motoservices.exception.ValidacaoException;
import br.com.motoservices.model.enums.StatusOS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdemDeServicoTest {

    private OrdemDeServico osComStatus(StatusOS status) {
        OrdemDeServico os = new OrdemDeServico();
        os.setId(1L);
        os.setCliente(new Cliente(1L, "Maria", "52998224725", "11999999999", null));
        os.setMoto(new Moto(1L, "Honda", "CG 160", 160));
        os.setStatus(status);
        os.setDataAbertura(LocalDateTime.now());
        return os;
    }

    private ServicoRevisao revisao() {
        ServicoRevisao servico = new ServicoRevisao(true);
        servico.setDescricao("Revisão de 10.000 km");
        servico.setValorBaseMaoDeObra(new BigDecimal("100.00"));
        return servico;
    }

    @Test
    @DisplayName("adicionar serviço mantém a referência bidirecional")
    void adicionarServicoPreencheReferenciaBidirecional() {
        OrdemDeServico os = osComStatus(StatusOS.ABERTA);
        ServicoRevisao servico = revisao();

        os.adicionarServico(servico);

        assertEquals(1, os.getServicos().size());
        assertSame(os, servico.getOrdemDeServico());
    }

    @Test
    @DisplayName("remover serviço desfaz a referência bidirecional")
    void removerServicoDesfazReferencia() {
        OrdemDeServico os = osComStatus(StatusOS.ABERTA);
        ServicoRevisao servico = revisao();
        os.adicionarServico(servico);

        os.removerServico(servico);

        assertEquals(0, os.getServicos().size());
        assertNull(servico.getOrdemDeServico());
    }

    @Test
    @DisplayName("OS concluída ou fechada não aceita novos serviços")
    void osEncerradaNaoAceitaServico() {
        OrdemDeServico os = osComStatus(StatusOS.CONCLUIDA);

        assertThrows(ValidacaoException.class, () -> os.adicionarServico(revisao()));
    }

    @Test
    @DisplayName("transições do ciclo de vida são aceitas e fechamento preenche a data")
    void transicoesValidas() {
        OrdemDeServico os = osComStatus(StatusOS.ABERTA);

        os.alterarStatus(StatusOS.EM_ANDAMENTO);
        os.alterarStatus(StatusOS.AGUARDANDO_CLIENTE);
        os.alterarStatus(StatusOS.EM_ANDAMENTO);
        os.alterarStatus(StatusOS.CONCLUIDA);
        os.alterarStatus(StatusOS.FECHADA);

        assertEquals(StatusOS.FECHADA, os.getStatus());
        assertNotNull(os.getDataFechamento());
    }

    @Test
    @DisplayName("salto de ABERTA direto para FECHADA é rejeitado")
    void transicaoInvalidaSalto() {
        OrdemDeServico os = osComStatus(StatusOS.ABERTA);

        assertThrows(TransicaoStatusInvalidaException.class,
                () -> os.alterarStatus(StatusOS.FECHADA));
        assertEquals(StatusOS.ABERTA, os.getStatus());
    }

    @Test
    @DisplayName("OS fechada não retorna a nenhum status")
    void osFechadaEhTerminal() {
        OrdemDeServico os = osComStatus(StatusOS.FECHADA);

        assertThrows(TransicaoStatusInvalidaException.class,
                () -> os.alterarStatus(StatusOS.EM_ANDAMENTO));
    }
}
