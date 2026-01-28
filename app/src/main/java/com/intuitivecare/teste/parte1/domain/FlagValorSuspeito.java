package com.intuitivecare.teste.parte1.domain;

/**
 * Enum que representa flags para valores suspeitos de despesas.
 * Baseado na decisão técnica de manter valores suspeitos marcados.
 */
public enum FlagValorSuspeito {
    OK("OK"),
    VALOR_ZERO("Valor zero"),
    VALOR_NEGATIVO("Valor negativo");
    
    private final String descricao;
    
    FlagValorSuspeito(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
