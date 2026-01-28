package com.intuitivecare.teste.parte1.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidade que representa uma despesa de operadora de saúde.
 * Segue princípio Single Responsibility - apenas dados de despesa.
 */
public record DespesaOperadora(
    String cnpj,
    String razaoSocial,
    Integer trimestre,
    Integer ano,
    BigDecimal valorDespesas,
    StatusConsistencia statusConsistencia,
    FlagValorSuspeito flagValorSuspeito
) {
    
    public DespesaOperadora {
        Objects.requireNonNull(cnpj, "CNPJ não pode ser nulo");
        Objects.requireNonNull(razaoSocial, "Razão Social não pode ser nula");
        Objects.requireNonNull(trimestre, "Trimestre não pode ser nulo");
        Objects.requireNonNull(ano, "Ano não pode ser nulo");
        Objects.requireNonNull(valorDespesas, "Valor de despesas não pode ser nulo");
        Objects.requireNonNull(statusConsistencia, "Status de consistência não pode ser nulo");
        Objects.requireNonNull(flagValorSuspeito, "Flag de valor suspeito não pode ser nula");
        
        if (trimestre < 1 || trimestre > 4) {
            throw new IllegalArgumentException("Trimestre deve estar entre 1 e 4");
        }
        
        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("Ano inválido");
        }
    }
    
    /**
     * Factory method para criar despesa consistente
     */
    public static DespesaOperadora criar(
        String cnpj, 
        String razaoSocial, 
        Integer trimestre, 
        Integer ano, 
        BigDecimal valorDespesas
    ) {
        FlagValorSuspeito flag = determinarFlagValor(valorDespesas);
        return new DespesaOperadora(
            cnpj, 
            razaoSocial, 
            trimestre, 
            ano, 
            valorDespesas, 
            StatusConsistencia.CONSISTENTE,
            flag
        );
    }
    
    /**
     * Marca esta despesa como tendo CNPJ duplicado com razão social divergente
     */
    public DespesaOperadora marcarComoCnpjDuplicado() {
        return new DespesaOperadora(
            cnpj, 
            razaoSocial, 
            trimestre, 
            ano, 
            valorDespesas,
            StatusConsistencia.CNPJ_DUPLICADO_RAZAO_DIVERGENTE,
            flagValorSuspeito
        );
    }
    
    private static FlagValorSuspeito determinarFlagValor(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) == 0) {
            return FlagValorSuspeito.VALOR_ZERO;
        } else if (valor.compareTo(BigDecimal.ZERO) < 0) {
            return FlagValorSuspeito.VALOR_NEGATIVO;
        }
        return FlagValorSuspeito.OK;
    }
}
