package com.intuitivecare.teste.parte1.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Agregador de despesas por Registro ANS e Trimestre.
 * Soma todas as despesas de eventos/sinistros de uma operadora em um trimestre.
 */
public class DespesasAgregadas {
    
    private final String registroANS;
    private final Integer trimestre;
    private final Integer ano;
    private final List<BigDecimal> valores;
    
    public DespesasAgregadas(String registroANS, Integer trimestre, Integer ano) {
        this.registroANS = registroANS;
        this.trimestre = trimestre;
        this.ano = ano;
        this.valores = new ArrayList<>();
    }
    
    public void adicionarValor(BigDecimal valor) {
        valores.add(valor);
    }
    
    public BigDecimal getValorTotal() {
        return valores.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public boolean temValoresNegativos() {
        return valores.stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) < 0);
    }
    
    public boolean temValoresZero() {
        return valores.stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);
    }
    
    public FlagValorSuspeito determinarFlag() {
        BigDecimal total = getValorTotal();
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return FlagValorSuspeito.VALOR_ZERO;
        } else if (total.compareTo(BigDecimal.ZERO) < 0) {
            return FlagValorSuspeito.VALOR_NEGATIVO;
        }
        return FlagValorSuspeito.OK;
    }
    
    public String getRegistroANS() {
        return registroANS;
    }
    
    public Integer getTrimestre() {
        return trimestre;
    }
    
    public Integer getAno() {
        return ano;
    }
}
