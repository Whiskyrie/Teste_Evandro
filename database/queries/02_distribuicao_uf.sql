-- ============================================================================
-- QUERY 2: Distribuição de despesas por UF (Top 5 estados)
-- ============================================================================
-- Lista os 5 estados com maiores despesas totais
-- Desafio adicional: calcula também a média por operadora em cada UF
-- ============================================================================

-- Decisões técnicas:
-- 1. Agrupa por UF usando SUM e AVG
-- 2. COUNT(DISTINCT) para total de operadoras por estado
-- 3. Média por operadora = total_uf / quantidade_operadoras_uf
-- 4. COALESCE para UFs nulas (operadoras sem cadastro)
-- 5. Formatação monetária com TO_CHAR
-- ============================================================================

WITH despesas_por_uf AS (
    SELECT 
        COALESCE(o.uf, 'SEM UF') as uf,
        COUNT(DISTINCT o.cnpj) as quantidade_operadoras,
        SUM(d.valor_despesas) as total_despesas,
        AVG(d.valor_despesas) as media_despesas_trimestre,
        -- Média por operadora = total do estado / número de operadoras
        SUM(d.valor_despesas) / COUNT(DISTINCT o.cnpj) as media_por_operadora
    FROM despesas_consolidadas d
    JOIN operadoras o ON d.cnpj_operadora = o.cnpj
    GROUP BY o.uf
)
SELECT 
    uf as "UF",
    quantidade_operadoras as "Qtd. Operadoras",
    TO_CHAR(total_despesas, 'FM999,999,999,999,990.00') as "Total Despesas (R$)",
    TO_CHAR(media_por_operadora, 'FM999,999,999,990.00') as "Média por Operadora (R$)",
    TO_CHAR(media_despesas_trimestre, 'FM999,999,999,990.00') as "Média por Trimestre (R$)",
    ROUND((total_despesas / SUM(total_despesas) OVER () * 100)::NUMERIC, 2) as "% do Total"
FROM despesas_por_uf
ORDER BY total_despesas DESC
LIMIT 5;

-- ============================================================================
-- ANÁLISE DETALHADA: Distribuição completa por região
-- ============================================================================
-- Visão expandida com agrupamento por região geográfica
-- ============================================================================

\echo ''
\echo '📊 DISTRIBUIÇÃO POR REGIÃO GEOGRÁFICA:'
\echo ''

WITH despesas_por_uf AS (
    SELECT 
        COALESCE(o.uf, 'SEM UF') as uf,
        SUM(d.valor_despesas) as total_despesas,
        COUNT(DISTINCT o.cnpj) as quantidade_operadoras
    FROM despesas_consolidadas d
    JOIN operadoras o ON d.cnpj_operadora = o.cnpj
    GROUP BY o.uf
),
despesas_por_regiao AS (
    SELECT 
        CASE 
            WHEN uf IN ('AC', 'AP', 'AM', 'PA', 'RO', 'RR', 'TO') THEN 'Norte'
            WHEN uf IN ('AL', 'BA', 'CE', 'MA', 'PB', 'PE', 'PI', 'RN', 'SE') THEN 'Nordeste'
            WHEN uf IN ('DF', 'GO', 'MT', 'MS') THEN 'Centro-Oeste'
            WHEN uf IN ('ES', 'MG', 'RJ', 'SP') THEN 'Sudeste'
            WHEN uf IN ('PR', 'RS', 'SC') THEN 'Sul'
            ELSE 'Não Classificado'
        END as regiao,
        uf,
        total_despesas,
        quantidade_operadoras
    FROM despesas_por_uf
)
SELECT 
    regiao as "Região",
    COUNT(DISTINCT uf) as "Estados",
    SUM(quantidade_operadoras) as "Operadoras",
    TO_CHAR(SUM(total_despesas), 'FM999,999,999,999,990.00') as "Total (R$)",
    ROUND((SUM(total_despesas) / SUM(SUM(total_despesas)) OVER () * 100)::NUMERIC, 2) as "% Nacional"
FROM despesas_por_regiao
GROUP BY regiao
ORDER BY SUM(total_despesas) DESC;

-- ============================================================================
-- EXEMPLO DE RESULTADO ESPERADO (Top 5 UFs):
-- ============================================================================
-- UF | Qtd. Operadoras | Total Despesas (R$)      | Média por Operadora (R$) | Média por Trimestre (R$) | % do Total
-- ---|-----------------|--------------------------|--------------------------|--------------------------|-----------
-- SP | 235             | 450,123,456,789.12       | 1,915,420,242.08         | 638,473,414.03           | 45.23
-- RJ | 87              | 156,789,012,345.67       | 1,802,172,556.85         | 600,724,185.62           | 15.76
-- MG | 65              | 89,012,345,678.90        | 1,369,420,702.75         | 456,473,567.58           | 8.94
-- PR | 42              | 45,678,901,234.56        | 1,087,593,362.73         | 362,531,120.91           | 4.59
-- RS | 38              | 34,567,890,123.45        | 909,681,319.04           | 303,227,106.35           | 3.47
-- ============================================================================

-- Análise crítica:
-- - Média por operadora vs Média por trimestre:
--   * Média por operadora: total_uf / quantidade_operadoras (visão macro)
--   * Média por trimestre: AVG(valor_despesas) (distribuição temporal)
-- - Percentual do total: mostra concentração geográfica
-- - COALESCE('SEM UF'): preserva operadoras sem cadastro completo
-- - Região geográfica: análise estratégica para planejamento regional
