package com.intuitivecare.teste.parte2.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Agregação de despesas por Razão Social e UF com estatísticas.
 */
public class DespesasAgregadasPorOperadora {
    
    private final String razaoSocial;
    private final String uf;
    private final List<BigDecimal> valoresPorTrimestre;
    
    public DespesasAgregadasPorOperadora(String razaoSocial, String uf) {
        this.razaoSocial = razaoSocial;
        this.uf = uf;
        this.valoresPorTrimestre = new ArrayList<>();
    }
    
    public void adicionarValor(BigDecimal valor) {
        if (valor != null) {
            valoresPorTrimestre.add(valor);
        }
    }
    
    public String getRazaoSocial() {
        return razaoSocial;
    }
    
    public String getUf() {
        return uf;
    }
    
    /**
     * Calcula o total de despesas.
     */
    public BigDecimal getTotal() {
        return valoresPorTrimestre.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula a média de despesas por trimestre.
     */
    public BigDecimal getMedia() {
        if (valoresPorTrimestre.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal total = getTotal();
        return total.divide(
            BigDecimal.valueOf(valoresPorTrimestre.size()), 
            2, 
            RoundingMode.HALF_UP
        );
    }
    
    /**
     * Calcula o desvio padrão das despesas.
     * Fórmula: √(Σ(x - média)² / n)
     */
    public BigDecimal getDesvioPadrao() {
        if (valoresPorTrimestre.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal media = getMedia();
        
        // Calcula soma dos quadrados das diferenças
        BigDecimal somaQuadrados = valoresPorTrimestre.stream()
            .map(valor -> valor.subtract(media))
            .map(diferenca -> diferenca.pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Divide pelo número de elementos
        BigDecimal variancia = somaQuadrados.divide(
            BigDecimal.valueOf(valoresPorTrimestre.size()),
            2,
            RoundingMode.HALF_UP
        );
        
        // Raiz quadrada
        return BigDecimal.valueOf(Math.sqrt(variancia.doubleValue()))
            .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Retorna número de trimestres com dados.
     */
    public int getQuantidadeTrimestres() {
        return valoresPorTrimestre.size();
    }
}
