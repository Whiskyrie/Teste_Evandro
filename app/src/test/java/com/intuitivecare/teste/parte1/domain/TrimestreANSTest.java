package com.intuitivecare.teste.parte1.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class TrimestreANSTest {
    
    @Test
    void deveCriarTrimestreValido() {
        TrimestreANS trimestre = new TrimestreANS(2025, 2);
        
        assertEquals(2025, trimestre.ano());
        assertEquals(2, trimestre.trimestre());
    }
    
    @Test
    void deveFormatarDisplayCorretamente() {
        TrimestreANS trimestre = new TrimestreANS(2025, 2);
        
        assertEquals("Q2/2025", trimestre.toDisplayFormat());
    }
    
    @Test
    void deveLancarExcecaoParaTrimestreInvalido() {
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            var ignored = new TrimestreANS(2025, 5);
        });
        assertNotNull(exception1);
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            var ignored = new TrimestreANS(2025, 0);
        });
        assertNotNull(exception2);
    }
    
    @Test
    void deveLancarExcecaoParaAnoInvalido() {
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            var ignored = new TrimestreANS(1999, 2);
        });
        assertNotNull(exception1);
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("unused")
            var ignored = new TrimestreANS(2101, 2);
        });
        assertNotNull(exception2);
    }
}
