-- Query 1: Top 5 operadoras com maior crescimento percentual
WITH trimestre_periodo AS (
    -- Identificar períodos disponíveis (conversão ano+trimestre para número sequencial)
    SELECT 
        cnpj_operadora,
        ano,
        trimestre,
        valor_despesas,
        ano * 4 + trimestre as periodo_num  -- 2025*4+1 = 8101 (Q1/2025)
    FROM despesas_consolidadas
),
dados_periodo AS (
    -- Agregar valores iniciais e finais por operadora
    SELECT 
        t.cnpj_operadora,
        MIN(t.periodo_num) as periodo_inicial_num,
        MAX(t.periodo_num) as periodo_final_num
    FROM trimestre_periodo t
    GROUP BY t.cnpj_operadora
),
valores_periodo AS (
    -- Buscar valores específicos de cada período
    SELECT 
        dp.cnpj_operadora,
        dp.periodo_inicial_num,
        dp.periodo_final_num,
        ti.ano as ano_inicial,
        ti.trimestre as trimestre_inicial,
        ti.valor_despesas as valor_inicial,
        tf.ano as ano_final,
        tf.trimestre as trimestre_final,
        tf.valor_despesas as valor_final
    FROM dados_periodo dp
    JOIN trimestre_periodo ti ON dp.cnpj_operadora = ti.cnpj_operadora 
        AND dp.periodo_inicial_num = ti.periodo_num
    JOIN trimestre_periodo tf ON dp.cnpj_operadora = tf.cnpj_operadora 
        AND dp.periodo_final_num = tf.periodo_num
)
SELECT 
    o.razao_social as "Operadora",
    o.uf as "UF",
    CONCAT('Q', vp.trimestre_inicial, '/', vp.ano_inicial) as "Período Inicial",
    TO_CHAR(vp.valor_inicial, 'FM999,999,999,990.00') as "Valor Inicial (R$)",
    CONCAT('Q', vp.trimestre_final, '/', vp.ano_final) as "Período Final",
    TO_CHAR(vp.valor_final, 'FM999,999,999,990.00') as "Valor Final (R$)",
    ROUND(
        ((vp.valor_final - vp.valor_inicial) / NULLIF(vp.valor_inicial, 0) * 100)::NUMERIC, 
        2
    ) as "Crescimento (%)"
FROM valores_periodo vp
JOIN operadoras o ON vp.cnpj_operadora = o.cnpj
WHERE 
    vp.valor_inicial > 0  -- Evita divisão por zero e crescimentos artificiais
    AND vp.periodo_final_num > vp.periodo_inicial_num  -- Apenas operadoras com múltiplos trimestres
ORDER BY "Crescimento (%)" DESC NULLS LAST
LIMIT 5;

-- ============================================================================
-- EXEMPLO DE RESULTADO ESPERADO:
-- ============================================================================
-- Operadora                                    | UF | Período Inicial | Valor Inicial    | Período Final | Valor Final      | Crescimento (%)
-- ---------------------------------------------|----|-----------------|------------------|---------------|------------------|----------------
-- UNIMED BELÉM COOPERATIVA DE TRABALHO MÉDICO | PA | Q1/2025         | 2,234,567,890.12 | Q3/2025       | 6,895,432,100.45 | 208.56
-- ...
-- ============================================================================

-- Análise crítica sobre operadoras com trimestres ausentes:
-- - Estratégia adotada: incluir com COALESCE (ausente = R$ 0)
-- - Justificativa: reflete realidade (suspensão, sem movimentação)
-- - Alternativa rejeitada: excluir operadoras incompletas (perde informação)
-- - Filtro adicional: período_final > período_inicial (exige evolução temporal)
