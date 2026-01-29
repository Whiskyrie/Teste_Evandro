-- ============================================================================
-- PARTE 3: BANCO DE DADOS E ANÁLISE
-- Import SQL - PostgreSQL 17+
-- ============================================================================
-- Importação dos CSVs gerados nas Partes 1 e 2
-- Estratégia: COPY nativo (bulk load 10x mais rápido)
-- Validação: dados já foram validados nas partes anteriores
-- ============================================================================

-- Configurações de sessão
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

-- ============================================================================
-- PREPARAÇÃO
-- ============================================================================

-- Iniciar transação (rollback em caso de erro)
BEGIN;

-- ============================================================================
-- IMPORTAÇÃO 1: Operadoras (dados enriquecidos da Parte 2)
-- ============================================================================
-- Arquivo: dados/parte2/despesas_enriquecidas.csv
-- Origem: Parte 2 - validação + enriquecimento com cadastro ANS
-- Registros esperados: 2.148 (716 operadoras × 3 trimestres)
-- ============================================================================

DO $$ 
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'IMPORTAÇÃO 1: Operadoras';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Fonte: despesas_enriquecidas.csv';
    RAISE NOTICE '';
END $$;

-- Importar operadoras únicas do arquivo enriquecido
-- Estratégia: criar tabela temporária → INSERT DISTINCT → DROP temp
CREATE TEMP TABLE temp_operadoras (
    cnpj                VARCHAR(14),
    razao_social        VARCHAR(255),
    trimestre           VARCHAR(10),  -- Não usado aqui, mas está no CSV
    ano                 VARCHAR(10),  -- Não usado aqui, mas está no CSV
    valor_despesas      VARCHAR(50),  -- Não usado aqui, mas está no CSV
    registro_ans        VARCHAR(10),
    modalidade          VARCHAR(100),
    uf                  CHAR(2),
    status_validacao    VARCHAR(50),
    mensagem_validacao  TEXT
);

-- COPY do CSV para tabela temporária
-- ATENÇÃO: Ajustar caminho absoluto conforme seu ambiente
\echo 'Carregando dados do CSV...'
\copy temp_operadoras(cnpj, razao_social, trimestre, ano, valor_despesas, registro_ans, modalidade, uf, status_validacao, mensagem_validacao) FROM 'dados/parte2/despesas_enriquecidas.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8';

-- Inserir operadoras distintas (elimina duplicatas por trimestre)
INSERT INTO operadoras (cnpj, razao_social, registro_ans, modalidade, uf, status_validacao, mensagem_validacao)
SELECT DISTINCT
    cnpj,
    razao_social,
    NULLIF(registro_ans, ''),  -- Converte string vazia para NULL
    NULLIF(modalidade, ''),
    NULLIF(uf, ''),
    status_validacao,
    NULLIF(mensagem_validacao, '')
FROM temp_operadoras
ON CONFLICT (cnpj) DO NOTHING;  -- Ignora duplicatas (caso haja)

-- Registrar importação
INSERT INTO metadata_importacao (tipo_arquivo, nome_arquivo, total_registros, registros_importados, registros_rejeitados)
VALUES (
    'OPERADORAS',
    'despesas_enriquecidas.csv',
    (SELECT COUNT(DISTINCT cnpj) FROM temp_operadoras),
    (SELECT COUNT(*) FROM operadoras),
    0
);

-- Limpar temporária
DROP TABLE temp_operadoras;

-- Verificação
DO $$ 
DECLARE
    total INTEGER;
BEGIN
    SELECT COUNT(*) INTO total FROM operadoras;
    RAISE NOTICE '✓ Operadoras importadas: %', total;
    RAISE NOTICE '';
END $$;

-- ============================================================================
-- IMPORTAÇÃO 2: Despesas Consolidadas (dados da Parte 1)
-- ============================================================================
-- Arquivo: dados/parte1/consolidado_despesas.csv (descompactar ZIP antes)
-- Origem: Parte 1 - consolidação de 3 trimestres da ANS
-- Registros esperados: 2.148
-- ============================================================================

DO $$ 
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'IMPORTAÇÃO 2: Despesas Consolidadas';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Fonte: consolidado_despesas.csv';
    RAISE NOTICE '';
END $$;

-- Criar tabela temporária para validação
CREATE TEMP TABLE temp_despesas (
    cnpj                VARCHAR(14),
    razao_social        VARCHAR(255),  -- Não usado (já está em operadoras)
    trimestre           VARCHAR(10),
    ano                 VARCHAR(10),
    valor_despesas      VARCHAR(50),
    status_consistencia VARCHAR(50),
    flag_valor_suspeito VARCHAR(20)
);

-- COPY do CSV
-- ATENÇÃO: Descompactar consolidado_despesas.zip antes!
\echo 'Carregando dados do CSV...'
\copy temp_despesas(cnpj, razao_social, trimestre, ano, valor_despesas, status_consistencia, flag_valor_suspeito) FROM 'dados/parte1/consolidado_despesas.csv' DELIMITER ',' CSV HEADER ENCODING 'UTF8';

-- Inserir despesas com conversão de tipos
INSERT INTO despesas_consolidadas (cnpj_operadora, trimestre, ano, valor_despesas, status_consistencia, flag_valor_suspeito)
SELECT 
    cnpj,
    CASE 
        WHEN trimestre LIKE 'Q%' THEN SUBSTRING(trimestre FROM 2)::INTEGER
        ELSE trimestre::INTEGER
    END,
    ano::INTEGER,
    valor_despesas::NUMERIC(15, 2),
    status_consistencia,
    flag_valor_suspeito
FROM temp_despesas
WHERE cnpj IN (SELECT cnpj FROM operadoras);  -- FK constraint: apenas CNPJs existentes

-- Contar registros rejeitados (CNPJs não encontrados)
DO $$ 
DECLARE
    rejeitados INTEGER;
BEGIN
    SELECT COUNT(*) INTO rejeitados 
    FROM temp_despesas 
    WHERE cnpj NOT IN (SELECT cnpj FROM operadoras);
    
    IF rejeitados > 0 THEN
        RAISE WARNING 'Atenção: % registros rejeitados (CNPJ não encontrado em operadoras)', rejeitados;
    END IF;
END $$;

-- Registrar importação
INSERT INTO metadata_importacao (tipo_arquivo, nome_arquivo, total_registros, registros_importados, registros_rejeitados)
VALUES (
    'DESPESAS_CONSOLIDADAS',
    'consolidado_despesas.csv',
    (SELECT COUNT(*) FROM temp_despesas),
    (SELECT COUNT(*) FROM despesas_consolidadas),
    (SELECT COUNT(*) FROM temp_despesas WHERE cnpj NOT IN (SELECT cnpj FROM operadoras))
);

-- Limpar temporária
DROP TABLE temp_despesas;

-- Verificação
DO $$ 
DECLARE
    total INTEGER;
    total_valor NUMERIC;
BEGIN
    SELECT COUNT(*), SUM(valor_despesas) 
    INTO total, total_valor 
    FROM despesas_consolidadas;
    
    RAISE NOTICE '✓ Despesas importadas: %', total;
    RAISE NOTICE '  Valor total: R$ %', TO_CHAR(total_valor, 'FM999,999,999,999,990.00');
    RAISE NOTICE '';
END $$;

-- ============================================================================
-- ATUALIZAÇÃO: View Materializada
-- ============================================================================
-- Pré-calcular agregações por operadora/UF
-- ============================================================================

DO $$ 
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'ATUALIZAÇÃO: View Materializada';
    RAISE NOTICE '========================================';
END $$;

REFRESH MATERIALIZED VIEW despesas_agregadas;

-- Verificação
DO $$ 
DECLARE
    total INTEGER;
BEGIN
    SELECT COUNT(*) INTO total FROM despesas_agregadas;
    RAISE NOTICE '✓ Agregações calculadas: %', total;
    RAISE NOTICE '';
END $$;

-- ============================================================================
-- COMMIT
-- ============================================================================
-- Se chegou até aqui, tudo está correto
-- ============================================================================

COMMIT;

-- ============================================================================
-- VERIFICAÇÕES FINAIS
-- ============================================================================

DO $$ 
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'IMPORTAÇÃO CONCLUÍDA COM SUCESSO!';
    RAISE NOTICE '========================================';
    RAISE NOTICE '';
END $$;

-- Estatísticas gerais
SELECT 
    '📊 ESTATÍSTICAS GERAIS' as titulo,
    '' as valor
UNION ALL
SELECT 
    'Operadoras cadastradas:',
    COUNT(*)::TEXT
FROM operadoras
UNION ALL
SELECT 
    'Despesas importadas:',
    COUNT(*)::TEXT
FROM despesas_consolidadas
UNION ALL
SELECT 
    'Agregações geradas:',
    COUNT(*)::TEXT
FROM despesas_agregadas
UNION ALL
SELECT 
    'Valor total de despesas:',
    'R$ ' || TO_CHAR(SUM(valor_despesas), 'FM999,999,999,999,990.00')
FROM despesas_consolidadas;

-- Distribuição por UF
\echo ''
\echo '📍 TOP 5 ESTADOS POR VALOR TOTAL DE DESPESAS:'
SELECT 
    COALESCE(uf, 'SEM UF') as uf,
    COUNT(DISTINCT d.cnpj_operadora) as operadoras,
    TO_CHAR(SUM(d.valor_despesas), 'FM999,999,999,999,990.00') as total_despesas
FROM despesas_consolidadas d
JOIN operadoras o ON d.cnpj_operadora = o.cnpj
GROUP BY uf
ORDER BY SUM(d.valor_despesas) DESC
LIMIT 5;

-- Top 5 operadoras
\echo ''
\echo '🏆 TOP 5 OPERADORAS POR VALOR TOTAL:'
SELECT 
    o.razao_social,
    o.uf,
    TO_CHAR(SUM(d.valor_despesas), 'FM999,999,999,999,990.00') as total_despesas
FROM despesas_consolidadas d
JOIN operadoras o ON d.cnpj_operadora = o.cnpj
GROUP BY o.razao_social, o.uf
ORDER BY SUM(d.valor_despesas) DESC
LIMIT 5;

\echo ''
\echo '✓ Próximo passo: executar queries analíticas em database/queries/'
\echo ''
