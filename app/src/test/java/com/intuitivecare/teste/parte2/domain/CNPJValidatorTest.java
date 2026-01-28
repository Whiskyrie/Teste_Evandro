package com.intuitivecare.teste.parte2.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Testes para validador de CNPJ.
 */
class CNPJValidatorTest {
    
    @Test
    void deveValidarCNPJValidoSemFormatacao() {
        assertTrue(CNPJValidator.validar("11222333000181"));
    }
    
    @Test
    void deveValidarCNPJValidoComFormatacao() {
        assertTrue(CNPJValidator.validar("11.222.333/0001-81"));
    }
    
    @Test
    void deveRejeitarCNPJComDigitoVerificadorInvalido() {
        assertFalse(CNPJValidator.validar("11222333000182")); // Último dígito errado
    }
    
    @Test
    void deveRejeitarCNPJComTamanhoInvalido() {
        assertFalse(CNPJValidator.validar("123456789"));
        assertFalse(CNPJValidator.validar("123456789012345"));
    }
    
    @Test
    void deveRejeitarCNPJComTodosDigitosIguais() {
        assertFalse(CNPJValidator.validar("11111111111111"));
        assertFalse(CNPJValidator.validar("00000000000000"));
    }
    
    @Test
    void deveRejeitarCNPJNulo() {
        assertFalse(CNPJValidator.validar(null));
    }
    
    @Test
    void deveRejeitarCNPJVazio() {
        assertFalse(CNPJValidator.validar(""));
        assertFalse(CNPJValidator.validar("   "));
    }
    
    @Test
    void deveValidarCNPJsReaisConhecidos() {
        // CNPJs reais de empresas públicas
        assertTrue(CNPJValidator.validar("00000000000191")); // Receita Federal
        assertTrue(CNPJValidator.validar("34028316000103")); // AMBEV
    }
    
    @Test
    void deveNormalizarCNPJ() {
        assertEquals("11222333000181", CNPJValidator.normalizar("11.222.333/0001-81"));
        assertEquals("11222333000181", CNPJValidator.normalizar("11222333000181"));
    }
    
    @Test
    void deveFormatarCNPJ() {
        assertEquals("11.222.333/0001-81", CNPJValidator.formatar("11222333000181"));
    }
    
    @Test
    void deveManterOriginalAoFormatarCNPJInvalido() {
        assertEquals("123", CNPJValidator.formatar("123"));
    }
}
