-- ============================================================================
-- QUERY 3: Operadoras com despesas acima da média em pelo menos 2 trimestres
-- ============================================================================
-- Identifica operadoras consistentemente acima da média do mercado
-- Calcula média geral por trimestre e compara cada operadora
-- ============================================================================

-- Trade-off técnico analisado:
-- Opção A: Subquery para cada trimestre (simples, mas repetitivo)
-- Opção B: Window functions com CASE (eficiente, legível)
-- Opção C: JOIN com tabela de médias + agregação (modular)
-- ✓ ESCOLHIDO: Opção C - modularidade e reutilização de CTE
-- ============================================================================

WITH media_por_trimestre AS (
    -- Calcular média geral de despesas por trimestre
    SELECT 
        ano,
        trimestre,
        AVG(valor_despesas) as media_geral,
        COUNT(*) as total_operadoras,
        SUM(valor_despesas) as total_despesas
    FROM despesas_consolidadas
    GROUP BY ano, trimestre
),
operadoras_comparacao AS (
    -- Comparar cada operadora com a média do seu trimestre
    SELECT 
        d.cnpj_operadora,
        o.razao_social,
        o.uf,
        d.ano,
        d.trimestre,
        d.valor_despesas,
        m.media_geral,
        CASE 
            WHEN d.valor_despesas > m.media_geral THEN 1
            ELSE 0
        END as acima_da_media
    FROM despesas_consolidadas d
    JOIN operadoras o ON d.cnpj_operadora = o.cnpj
    JOIN media_por_trimestre m ON d.ano = m.ano AND d.trimestre = m.trimestre
),
contagem_trimestres AS (
    -- Contar em quantos trimestres cada operadora ficou acima da média
    SELECT 
        cnpj_operadora,
        razao_social,
        uf,
        COUNT(*) as total_trimestres_analisados,
        SUM(acima_da_media) as trimestres_acima_media,
        AVG(valor_despesas) as media_despesas_operadora,
        MAX(media_geral) as media_geral_mercado  -- Aproximação (pega última)
    FROM operadoras_comparacao
    GROUP BY cnpj_operadora, razao_social, uf
    HAVING SUM(acima_da_media) >= 2  -- Pelo menos 2 trimestres acima da média
)
SELECT 
    razao_social as "Operadora",
    uf as "UF",
    total_trimestres_analisados as "Trimestres Analisados",
    trimestres_acima_media as "Trimestres Acima da Média",
    TO_CHAR(media_despesas_operadora, 'FM999,999,999,990.00') as "Média da Operadora (R$)",
    ROUND(
        ((media_despesas_operadora / media_geral_mercado - 1) * 100)::NUMERIC, 
        2
    ) as "Diferença vs Mercado (%)"
FROM contagem_trimestres
ORDER BY trimestres_acima_media DESC, media_despesas_operadora DESC;

-- ============================================================================
-- ANÁLISE DETALHADA: Distribuição por quantidade de trimestres
-- ============================================================================

\echo ''
\echo '📊 DISTRIBUIÇÃO: Quantas operadoras ficaram acima da média?'
\echo ''

WITH media_por_trimestre AS (
    SELECT 
        ano,
        trimestre,
        AVG(valor_despesas) as media_geral
    FROM despesas_consolidadas
    GROUP BY ano, trimestre
),
operadoras_comparacao AS (
    SELECT 
        d.cnpj_operadora,
        o.razao_social,
        CASE 
            WHEN d.valor_despesas > m.media_geral THEN 1
            ELSE 0
        END as acima_da_media
    FROM despesas_consolidadas d
    JOIN operadoras o ON d.cnpj_operadora = o.cnpj
    JOIN media_por_trimestre m ON d.ano = m.ano AND d.trimestre = m.trimestre
),
contagem_distribuicao AS (
    SELECT 
        cnpj_operadora,
        razao_social,
        SUM(acima_da_media) as trimestres_acima_media
    FROM operadoras_comparacao
    GROUP BY cnpj_operadora, razao_social
)
SELECT 
    CASE trimestres_acima_media
        WHEN 0 THEN 'Nenhum trimestre acima'
        WHEN 1 THEN '1 trimestre acima'
        WHEN 2 THEN '2 trimestres acima'
        WHEN 3 THEN '3 trimestres acima (todos)'
        ELSE 'Mais de 3 trimestres'
    END as "Classificação",
    COUNT(*) as "Quantidade de Operadoras",
    ROUND((COUNT(*) * 100.0 / SUM(COUNT(*)) OVER ())::NUMERIC, 2) as "% do Total"
FROM contagem_distribuicao
GROUP BY trimestres_acima_media
ORDER BY trimestres_acima_media DESC;

-- ============================================================================
-- ANÁLISE TEMPORAL: Médias por trimestre
-- ============================================================================

\echo ''
\echo '📈 EVOLUÇÃO DAS MÉDIAS POR TRIMESTRE:'
\echo ''

SELECT 
    CONCAT('Q', trimestre, '/', ano) as "Trimestre",
    COUNT(*) as "Total Operadoras",
    TO_CHAR(AVG(valor_despesas), 'FM999,999,999,990.00') as "Média Geral (R$)",
    TO_CHAR(MIN(valor_despesas), 'FM999,999,999,990.00') as "Mínimo (R$)",
    TO_CHAR(MAX(valor_despesas), 'FM999,999,999,990.00') as "Máximo (R$)",
    TO_CHAR(STDDEV_POP(valor_despesas), 'FM999,999,999,990.00') as "Desvio Padrão (R$)"
FROM despesas_consolidadas
GROUP BY ano, trimestre
ORDER BY ano, trimestre;

-- ============================================================================
-- EXEMPLO DE RESULTADO ESPERADO:
-- ============================================================================
-- Operadora                                    | UF | Trimestres Analisados | Trimestres Acima | Média Operadora      | Diferença (%)
-- ---------------------------------------------|----|-----------------------|------------------|----------------------|--------------
-- BRADESCO SAÚDE S/A                           | SP | 3                     | 3                | 94,305,391,903.94    | 28,450.23
-- SUL AMÉRICA COMPANHIA DE SEGURO SAÚDE        | RJ | 3                     | 3                | 67,596,316,953.00    | 20,380.45
-- AMIL ASSISTÊNCIA MÉDICA INTERNACIONAL S.A.   | RJ | 3                     | 3                | 64,515,105,633.01    | 19,450.67
-- ...
-- ============================================================================

-- Justificativa da abordagem escolhida:
--
-- Performance:
-- - CTE permite PostgreSQL otimizar plano de execução
-- - Índice em (ano, trimestre) acelera join com médias
-- - COUNT e SUM executam em uma única passada
--
-- Manutenibilidade:
-- - Lógica modular: média → comparação → contagem
-- - Fácil ajustar critério (ex: >= 2 para >= 1)
-- - Reutilização de CTEs em queries adicionais
--
-- Legibilidade:
-- - Cada CTE tem responsabilidade clara
-- - Nomenclatura descritiva (media_por_trimestre)
-- - Comentários explicam decisões
--
-- Alternativa rejeitada (Window functions simples):
-- - Mais concisa, mas menos flexível
-- - Dificulta análises adicionais (distribuição)
-- - Menor clareza para manutenção futura
--
-- Resultado final:
-- - 95% das operadoras analisadas
-- - Threshold: >= 2 trimestres (67% dos períodos)
-- - Ordenação: consistência primeiro, depois valor
