package com.intuitivecare.teste.parte1.domain;

/**
 * Enum que representa o status de consistência de uma despesa.
 * Baseado na decisão técnica de preservar dados inconsistentes com flags.
 */
public enum StatusConsistencia {
    CONSISTENTE("Consistente"),
    CNPJ_DUPLICADO_RAZAO_DIVERGENTE("CNPJ duplicado com razão social divergente"),
    OPERADORA_SEM_MOVIMENTACAO("Operadora sem movimentação em todos os trimestres");
    
    private final String descricao;
    
    StatusConsistencia(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
