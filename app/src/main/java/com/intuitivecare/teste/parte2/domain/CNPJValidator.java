package com.intuitivecare.teste.parte2.domain;

/**
 * Validador de CNPJ com cálculo de dígitos verificadores.
 * Implementa algoritmo oficial da Receita Federal.
 */
public class CNPJValidator {
    
    private static final int[] PESOS_PRIMEIRO_DIGITO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO_DIGITO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    
    /**
     * Valida formato e dígitos verificadores do CNPJ.
     * 
     * @param cnpj CNPJ com ou sem formatação (99.999.999/9999-99 ou 99999999999999)
     * @return true se CNPJ é válido
     */
    public static boolean validar(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return false;
        }
        
        // Remove formatação
        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");
        
        // Verifica tamanho
        if (cnpjLimpo.length() != 14) {
            return false;
        }
        
        // Rejeita CNPJs com todos os dígitos iguais
        if (cnpjLimpo.matches("(\\d)\\1{13}")) {
            return false;
        }
        
        // Valida dígitos verificadores
        try {
            int primeiroDigito = calcularDigito(cnpjLimpo.substring(0, 12), PESOS_PRIMEIRO_DIGITO);
            int segundoDigito = calcularDigito(cnpjLimpo.substring(0, 12) + primeiroDigito, PESOS_SEGUNDO_DIGITO);
            
            String digitosCalculados = String.valueOf(primeiroDigito) + segundoDigito;
            String digitosOriginais = cnpjLimpo.substring(12, 14);
            
            return digitosCalculados.equals(digitosOriginais);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Calcula um dígito verificador do CNPJ.
     */
    private static int calcularDigito(String base, int[] pesos) {
        int soma = 0;
        
        for (int i = 0; i < base.length(); i++) {
            int digito = Character.getNumericValue(base.charAt(i));
            soma += digito * pesos[i];
        }
        
        int resto = soma % 11;
        return (resto < 2) ? 0 : (11 - resto);
    }
    
    /**
     * Normaliza CNPJ removendo formatação.
     */
    public static String normalizar(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        return cnpj.replaceAll("[^0-9]", "");
    }
    
    /**
     * Formata CNPJ no padrão 99.999.999/9999-99.
     */
    public static String formatar(String cnpj) {
        String cnpjLimpo = normalizar(cnpj);
        
        if (cnpjLimpo == null || cnpjLimpo.length() != 14) {
            return cnpj; // Retorna original se inválido
        }
        
        return String.format("%s.%s.%s/%s-%s",
            cnpjLimpo.substring(0, 2),
            cnpjLimpo.substring(2, 5),
            cnpjLimpo.substring(5, 8),
            cnpjLimpo.substring(8, 12),
            cnpjLimpo.substring(12, 14)
        );
    }
}
