package com.intuitivecare.teste.parte2.domain;

import java.math.BigDecimal;

/**
 * Representa uma despesa enriquecida com dados cadastrais.
 * Inclui validações e informações adicionais da ANS.
 */
public record DespesaEnriquecida(
    String cnpj,
    String razaoSocial,
    Integer trimestre,
    Integer ano,
    BigDecimal valorDespesas,
    String registroANS,
    String modalidade,
    String uf,
    StatusValidacao statusValidacao,
    String mensagemValidacao
) {
    /**
     * Cria despesa enriquecida com validação.
     */
    public static DespesaEnriquecida criar(
        String cnpj,
        String razaoSocial,
        Integer trimestre,
        Integer ano,
        BigDecimal valorDespesas,
        String registroANS,
        String modalidade,
        String uf
    ) {
        StatusValidacao status = validar(cnpj, razaoSocial, valorDespesas);
        String mensagem = gerarMensagemValidacao(status, cnpj, valorDespesas);
        
        return new DespesaEnriquecida(
            cnpj,
            razaoSocial,
            trimestre,
            ano,
            valorDespesas,
            registroANS,
            modalidade,
            uf,
            status,
            mensagem
        );
    }
    
    private static StatusValidacao validar(String cnpj, String razaoSocial, BigDecimal valor) {
        // CNPJ inválido tem prioridade
        if (!CNPJValidator.validar(cnpj)) {
            return StatusValidacao.CNPJ_INVALIDO;
        }
        
        // Razão social vazia
        if (razaoSocial == null || razaoSocial.isBlank()) {
            return StatusValidacao.RAZAO_SOCIAL_VAZIA;
        }
        
        // Valor negativo
        if (valor != null && valor.compareTo(BigDecimal.ZERO) < 0) {
            return StatusValidacao.VALOR_NEGATIVO;
        }
        
        return StatusValidacao.VALIDO;
    }
    
    private static String gerarMensagemValidacao(StatusValidacao status, String cnpj, BigDecimal valor) {
        return switch (status) {
            case CNPJ_INVALIDO -> "CNPJ " + cnpj + " possui dígitos verificadores inválidos";
            case RAZAO_SOCIAL_VAZIA -> "Razão social está vazia ou nula";
            case VALOR_NEGATIVO -> "Valor " + valor + " é negativo";
            case VALIDO -> null;
        };
    }
    
    /**
     * Verifica se o registro é válido.
     */
    public boolean isValido() {
        return statusValidacao == StatusValidacao.VALIDO;
    }
}
