package br.com.motoservices.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Fonte única das regras de precificação do domínio.
 *
 * <p>Mão de obra recebe fator de risco conforme as cilindradas da moto:
 * acima de 650 cc = +70%, acima de 400 cc = +50%, demais = sem acréscimo.
 * O desconto progressivo é de 5% sobre o total de peças quando a OS possui
 * 5 ou mais peças fornecidas pela loja.</p>
 */
public final class Precificacao {

    public static final int LIMITE_CILINDRADAS_RISCO_ALTO = 650;
    public static final int LIMITE_CILINDRADAS_RISCO_MEDIO = 400;

    public static final BigDecimal FATOR_RISCO_ALTO = new BigDecimal("1.70");
    public static final BigDecimal FATOR_RISCO_MEDIO = new BigDecimal("1.50");
    public static final BigDecimal FATOR_PADRAO = BigDecimal.ONE;

    public static final int MIN_PECAS_PARA_DESCONTO = 5;
    public static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    public static final int CASAS_MONETARIAS = 2;

    private Precificacao() {
    }

    public static BigDecimal fatorDeRisco(Moto moto) {
        if (moto == null || moto.getCilindradas() == null) {
            return FATOR_PADRAO;
        }
        int cilindradas = moto.getCilindradas();
        if (cilindradas > LIMITE_CILINDRADAS_RISCO_ALTO) {
            return FATOR_RISCO_ALTO;
        }
        if (cilindradas > LIMITE_CILINDRADAS_RISCO_MEDIO) {
            return FATOR_RISCO_MEDIO;
        }
        return FATOR_PADRAO;
    }

    public static BigDecimal maoDeObraComFator(BigDecimal valorBaseMaoDeObra, Moto moto) {
        Objects.requireNonNull(valorBaseMaoDeObra, "valorBaseMaoDeObra é obrigatório");
        return arredondar(valorBaseMaoDeObra.multiply(fatorDeRisco(moto)));
    }

    public static BigDecimal descontoProgressivo(BigDecimal totalPecas, int pecasDaLoja) {
        Objects.requireNonNull(totalPecas, "totalPecas é obrigatório");
        if (pecasDaLoja < MIN_PECAS_PARA_DESCONTO) {
            return arredondar(BigDecimal.ZERO);
        }
        return arredondar(totalPecas.multiply(PERCENTUAL_DESCONTO));
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_MONETARIAS, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(CASAS_MONETARIAS, RoundingMode.HALF_UP);
    }
}
