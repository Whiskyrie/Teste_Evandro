package com.intuitivecare.teste.parte1.domain;

/**
 * Value Object que representa um trimestre no formato da ANS (YYYY/QQ)
 */
public record TrimestreANS(int ano, int trimestre) {
    
    public TrimestreANS {
        if (trimestre < 1 || trimestre > 4) {
            throw new IllegalArgumentException("Trimestre deve estar entre 1 e 4");
        }
        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("Ano inválido");
        }
    }
    
    /**
     * Retorna o path no formato usado pela ANS: YYYY/trimestre/
     */
    public String toPathFormat() {
        return String.format("%d/trimestre/", ano);
    }
    
    /**
     * Retorna representação legível: "Q1/2024"
     */
    public String toDisplayFormat() {
        return String.format("Q%d/%d", trimestre, ano);
    }
    
    @Override
    public String toString() {
        return toDisplayFormat();
    }
}
