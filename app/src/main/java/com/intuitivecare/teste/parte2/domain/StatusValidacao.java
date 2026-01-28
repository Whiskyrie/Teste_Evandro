package com.intuitivecare.teste.parte2.domain;

/**
 * Status de validação de uma despesa enriquecida.
 */
public enum StatusValidacao {
    VALIDO("Válido"),
    CNPJ_INVALIDO("CNPJ inválido"),
    RAZAO_SOCIAL_VAZIA("Razão social vazia"),
    VALOR_NEGATIVO("Valor negativo");
    
    private final String descricao;
    
    StatusValidacao(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
