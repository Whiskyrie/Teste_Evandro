package com.intuitivecare.teste.parte1.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class DespesaOperadoraTest {
    
    @Test
    void deveCriarDespesaComValorPositivo() {
        DespesaOperadora despesa = DespesaOperadora.criar(
            "12345678901234",
            "Operadora Teste LTDA",
            2,
            2025,
            new BigDecimal("10000.00")
        );
        
        assertNotNull(despesa);
        assertEquals("12345678901234", despesa.cnpj());
        assertEquals(StatusConsistencia.CONSISTENTE, despesa.statusConsistencia());
        assertEquals(FlagValorSuspeito.OK, despesa.flagValorSuspeito());
    }
    
    @Test
    void deveMarcarValorZeroComoSuspeito() {
        DespesaOperadora despesa = DespesaOperadora.criar(
            "12345678901234",
            "Operadora Teste LTDA",
            2,
            2025,
            BigDecimal.ZERO
        );
        
        assertEquals(FlagValorSuspeito.VALOR_ZERO, despesa.flagValorSuspeito());
    }
    
    @Test
    void deveMarcarValorNegativoComoSuspeito() {
        DespesaOperadora despesa = DespesaOperadora.criar(
            "12345678901234",
            "Operadora Teste LTDA",
            2,
            2025,
            new BigDecimal("-5000.00")
        );
        
        assertEquals(FlagValorSuspeito.VALOR_NEGATIVO, despesa.flagValorSuspeito());
    }
    
    @Test
    void deveMarcarComoCnpjDuplicado() {
        DespesaOperadora despesa = DespesaOperadora.criar(
            "12345678901234",
            "Operadora Teste LTDA",
            2,
            2025,
            new BigDecimal("10000.00")
        );
        
        DespesaOperadora marcada = despesa.marcarComoCnpjDuplicado();
        
        assertEquals(StatusConsistencia.CNPJ_DUPLICADO_RAZAO_DIVERGENTE, marcada.statusConsistencia());
        assertEquals(despesa.cnpj(), marcada.cnpj());
    }
    
    @Test
    void deveLancarExcecaoParaCnpjNulo() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            DespesaOperadora.criar(
                null,
                "Operadora Teste LTDA",
                2,
                2025,
                new BigDecimal("10000.00")
            );
        });
        assertNotNull(exception);
    }
    
    @Test
    void deveLancarExcecaoParaTrimestreInvalido() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            DespesaOperadora.criar(
                "12345678901234",
                "Operadora Teste LTDA",
                5, // Trimestre inválido
                2025,
                new BigDecimal("10000.00")
            );
        });
        assertNotNull(exception);
    }
}
