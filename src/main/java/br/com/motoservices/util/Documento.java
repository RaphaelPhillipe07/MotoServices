package br.com.motoservices.util;

/**
 * Normalização e validação de CPF (11 dígitos) e CNPJ (14 dígitos).
 */
public final class Documento {

    private Documento() {
    }

    public static String normalizar(String documento) {
        return documento == null ? null : documento.replaceAll("\\D", "");
    }

    public static boolean isValido(String documento) {
        String digitos = normalizar(documento);
        if (digitos == null) {
            return false;
        }
        if (digitos.length() == 11) {
            return isCpfValido(digitos);
        }
        if (digitos.length() == 14) {
            return isCnpjValido(digitos);
        }
        return false;
    }

    private static boolean isCpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        int primeiro = digitoVerificador(cpf, 9, 10);
        int segundo = digitoVerificador(cpf, 10, 11);
        return cpf.charAt(9) - '0' == primeiro && cpf.charAt(10) - '0' == segundo;
    }

    private static boolean isCnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int primeiro = digitoVerificador(cnpj, 12, 5);
        int segundo = digitoVerificador(cnpj, 13, 6);
        return cnpj.charAt(12) - '0' == primeiro && cnpj.charAt(13) - '0' == segundo;
    }

    /**
     * Calcula um dígito verificador módulo 11 sobre os {@code quantidade} primeiros dígitos.
     */
    private static int digitoVerificador(String digitos, int quantidade, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * peso;
            peso--;
            if (peso < 2) {
                peso = 9;
            }
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
