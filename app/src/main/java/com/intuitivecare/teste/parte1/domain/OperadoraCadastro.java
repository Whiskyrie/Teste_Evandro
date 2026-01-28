package com.intuitivecare.teste.parte1.domain;

/**
 * Value Object que representa dados cadastrais de uma operadora.
 */
public record OperadoraCadastro(
    String registroANS,
    String cnpj,
    String razaoSocial,
    String modalidade,
    String uf
) {
    public OperadoraCadastro {
        if (registroANS == null || registroANS.isBlank()) {
            throw new IllegalArgumentException("Registro ANS é obrigatório");
        }
    }
}
