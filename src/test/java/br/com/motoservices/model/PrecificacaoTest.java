package br.com.motoservices.model;

import br.com.motoservices.exception.ValidacaoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrecificacaoTest {

    private static final BigDecimal MAO_DE_OBRA = new BigDecimal("100.00");

    private static Moto motoCom(int cilindradas) {
        return new Moto(1L, "Honda", "CG", cilindradas);
    }

    private static Peca pecaCom(String valor) {
        return new Peca(1L, "P-001", "Pastilha de freio", new BigDecimal(valor), 10, 2);
    }

    @ParameterizedTest(name = "{0} cc → fator {1}")
    @CsvSource({
            "150, 1.00",
            "400, 1.00",
            "401, 1.50",
            "650, 1.50",
            "651, 1.70",
            "1000, 1.70"
    })
    @DisplayName("fator de risco segue as faixas de cilindrada")
    void fatorDeRiscoPorCilindrada(int cilindradas, String fatorEsperado) {
        BigDecimal fator = Precificacao.fatorDeRisco(motoCom(cilindradas));
        assertEquals(0, fator.compareTo(new BigDecimal(fatorEsperado)),
                () -> "esperado " + fatorEsperado + " mas foi " + fator);
    }

    @Test
    @DisplayName("mão de obra acima de 650cc recebe +70%")
    void maoDeObraComFatorAlto() {
        assertEquals(new BigDecimal("170.00"),
                Precificacao.maoDeObraComFator(MAO_DE_OBRA, motoCom(700)));
    }

    @Test
    @DisplayName("arredondamento monetário é HALF_UP em duas casas")
    void arredondamentoHalfUp() {
        assertEquals(new BigDecimal("50.00"),
                Precificacao.maoDeObraComFator(new BigDecimal("33.33"), motoCom(500)));
    }

    @Test
    @DisplayName("sem desconto abaixo de 5 peças da loja")
    void semDescontoComQuatroPecas() {
        assertEquals(new BigDecimal("0.00"),
                Precificacao.descontoProgressivo(new BigDecimal("1000.00"), 4));
    }

    @Test
    @DisplayName("desconto de 5% a partir de 5 peças da loja")
    void descontoComCincoPecas() {
        assertEquals(new BigDecimal("50.00"),
                Precificacao.descontoProgressivo(new BigDecimal("1000.00"), 5));
    }

    @Test
    @DisplayName("desconto de 5% com 7 peças da loja")
    void descontoComSetePecas() {
        assertEquals(new BigDecimal("35.00"),
                Precificacao.descontoProgressivo(new BigDecimal("700.00"), 7));
    }

    @Test
    @DisplayName("revisão cobra apenas mão de obra com fator de risco")
    void revisaoCalculaSomenteMaoDeObra() {
        ServicoRevisao revisao = new ServicoRevisao(true);
        revisao.setValorBaseMaoDeObra(MAO_DE_OBRA);

        assertEquals(new BigDecimal("150.00"), revisao.calcularValorFinal(motoCom(500)));
    }

    @Test
    @DisplayName("troca de peça da loja soma mão de obra e valor da peça")
    void trocaComPecaLojaSomaPeca() {
        ServicoTrocaPeca troca = new ServicoTrocaPeca(pecaCom("80.00"), true);
        troca.setValorBaseMaoDeObra(MAO_DE_OBRA);

        assertEquals(new BigDecimal("230.00"), troca.calcularValorFinal(motoCom(500)));
        assertEquals(new BigDecimal("80.00"), troca.valorDaPecaDaLoja());
    }

    @Test
    @DisplayName("peça trazida pelo cliente não entra no total de peças")
    void trocaComPecaDoClienteCobraSoMaoDeObra() {
        ServicoTrocaPeca troca = new ServicoTrocaPeca(pecaCom("80.00"), false);
        troca.setValorBaseMaoDeObra(MAO_DE_OBRA);

        assertEquals(new BigDecimal("100.00"), troca.calcularValorFinal(motoCom(300)));
        assertEquals(new BigDecimal("0.00"), troca.valorDaPecaDaLoja());
    }

    @Test
    @DisplayName("peça da loja sem peça cadastrada é erro de validação")
    void trocaComPecaLojaSemPecaFalha() {
        ServicoTrocaPeca troca = new ServicoTrocaPeca(null, true);
        troca.setValorBaseMaoDeObra(MAO_DE_OBRA);

        assertThrows(ValidacaoException.class, () -> troca.calcularValorFinal(motoCom(300)));
    }
}
