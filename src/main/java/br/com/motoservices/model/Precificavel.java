package br.com.motoservices.model;

import java.math.BigDecimal;

public interface Precificavel {

    BigDecimal calcularValorFinal(Moto moto);
}
