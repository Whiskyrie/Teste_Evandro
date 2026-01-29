-- ============================================================================
-- PARTE 3: BANCO DE DADOS E ANÁLISE
-- Schema DDL - PostgreSQL 17+
-- ============================================================================
-- Decisões técnicas:
-- - Modelo semi-normalizado (3 tabelas + view materializada)
-- - NUMERIC(15,2) para valores monetários (precisão financeira)
-- - Campos separados trimestre/ano (reflete granularidade real)
-- - Índices seletivos para queries analíticas
-- ============================================================================

-- Drop existing objects (se existirem)
DROP MATERIALIZED VIEW IF EXISTS despesas_agregadas CASCADE;
DROP TABLE IF EXISTS despesas_consolidadas CASCADE;
DROP TABLE IF EXISTS operadoras CASCADE;
DROP TABLE IF EXISTS metadata_importacao CASCADE;
DROP FUNCTION IF EXISTS get_trimestre_data_inicio(INTEGER, INTEGER);

-- ============================================================================
-- TABELA: operadoras (dimensão)
-- ============================================================================
-- Armazena dados cadastrais das operadoras de planos de saúde
-- Chave: CNPJ (formato: 99999999999999 - 14 dígitos sem formatação)
-- ============================================================================

CREATE TABLE operadoras (
    cnpj                VARCHAR(14) PRIMARY KEY,
    razao_social        VARCHAR(255) NOT NULL,
    registro_ans        VARCHAR(10),
    modalidade          VARCHAR(100),
    uf                  CHAR(2),
    status_validacao    VARCHAR(50) NOT NULL,
    mensagem_validacao  TEXT,
    
    -- Metadados
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    -- Relaxado: permite CNPJs inválidos (REG_ANS temporário) - validação em status_validacao
    CONSTRAINT chk_cnpj_formato CHECK (cnpj ~ '^\d+$'),
    CONSTRAINT chk_uf_formato CHECK (uf IS NULL OR uf ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_status_validacao CHECK (
        status_validacao IN ('VALIDO', 'CNPJ_INVALIDO', 'RAZAO_SOCIAL_VAZIA', 'VALOR_NEGATIVO')
    )
);

-- Índices para queries analíticas
CREATE INDEX idx_operadoras_uf ON operadoras(uf) WHERE uf IS NOT NULL;
CREATE INDEX idx_operadoras_registro_ans ON operadoras(registro_ans) WHERE registro_ans IS NOT NULL;
CREATE INDEX idx_operadoras_razao_social ON operadoras(razao_social);

-- Comentários
COMMENT ON TABLE operadoras IS 'Dados cadastrais das operadoras de planos de saúde';
COMMENT ON COLUMN operadoras.cnpj IS 'CNPJ da operadora (14 dígitos sem formatação)';
COMMENT ON COLUMN operadoras.status_validacao IS 'Status da validação: VALIDO | CNPJ_INVALIDO | RAZAO_SOCIAL_VAZIA | VALOR_NEGATIVO';

-- ============================================================================
-- TABELA: despesas_consolidadas (fato)
-- ============================================================================
-- Armazena despesas com eventos/sinistros consolidadas por operadora/trimestre
-- Origem: dados da ANS (contas contábeis 411*/412*)
-- ============================================================================

CREATE TABLE despesas_consolidadas (
    id                      SERIAL PRIMARY KEY,
    cnpj_operadora          VARCHAR(14) NOT NULL,
    trimestre               INTEGER NOT NULL,
    ano                     INTEGER NOT NULL,
    valor_despesas          NUMERIC(15, 2) NOT NULL,
    status_consistencia     VARCHAR(50) NOT NULL,
    flag_valor_suspeito     VARCHAR(20) NOT NULL,
    
    -- Metadados
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_despesas_operadora 
        FOREIGN KEY (cnpj_operadora) 
        REFERENCES operadoras(cnpj)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_trimestre_valido CHECK (trimestre BETWEEN 1 AND 4),
    CONSTRAINT chk_ano_valido CHECK (ano BETWEEN 2020 AND 2030),
    -- Removido: permite valores negativos marcados com flag VALOR_NEGATIVO
    CONSTRAINT chk_status_consistencia CHECK (
        status_consistencia IN ('CONSISTENTE', 'CNPJ_DUPLICADO_RAZAO_DIVERGENTE', 'OPERADORA_SEM_MOVIMENTACAO')
    ),
    CONSTRAINT chk_flag_valor_suspeito CHECK (
        flag_valor_suspeito IN ('OK', 'VALOR_ZERO', 'VALOR_NEGATIVO')
    ),
    
    -- Unicidade: uma operadora não pode ter mais de um registro por trimestre
    CONSTRAINT uk_despesas_operadora_periodo 
        UNIQUE (cnpj_operadora, ano, trimestre)
);

-- Índices para queries analíticas
CREATE INDEX idx_despesas_cnpj ON despesas_consolidadas(cnpj_operadora);
CREATE INDEX idx_despesas_temporal ON despesas_consolidadas(ano DESC, trimestre DESC);
CREATE INDEX idx_despesas_composite ON despesas_consolidadas(cnpj_operadora, ano, trimestre);
CREATE INDEX idx_despesas_valor ON despesas_consolidadas(valor_despesas DESC);

-- Comentários
COMMENT ON TABLE despesas_consolidadas IS 'Despesas consolidadas com eventos/sinistros por operadora e trimestre';
COMMENT ON COLUMN despesas_consolidadas.trimestre IS 'Trimestre (1=Q1, 2=Q2, 3=Q3, 4=Q4)';
COMMENT ON COLUMN despesas_consolidadas.valor_despesas IS 'Valor total de despesas em R$ (precisão: 2 casas decimais)';
COMMENT ON COLUMN despesas_consolidadas.status_consistencia IS 'CONSISTENTE | CNPJ_DUPLICADO_RAZAO_DIVERGENTE | OPERADORA_SEM_MOVIMENTACAO';
COMMENT ON COLUMN despesas_consolidadas.flag_valor_suspeito IS 'OK | VALOR_ZERO | VALOR_NEGATIVO';

-- ============================================================================
-- MATERIALIZED VIEW: despesas_agregadas
-- ============================================================================
-- Agregação pré-calculada por operadora/UF com estatísticas
-- Atualização: manual via REFRESH MATERIALIZED VIEW
-- ============================================================================

CREATE MATERIALIZED VIEW despesas_agregadas AS
SELECT 
    o.razao_social,
    o.uf,
    COUNT(DISTINCT CONCAT(d.ano, '-', d.trimestre)) as quantidade_trimestres,
    SUM(d.valor_despesas) as total_despesas,
    ROUND(AVG(d.valor_despesas)::NUMERIC, 2) as media_despesas,
    ROUND(
        STDDEV_POP(d.valor_despesas)::NUMERIC, 
        2
    ) as desvio_padrao_despesas
FROM despesas_consolidadas d
JOIN operadoras o ON d.cnpj_operadora = o.cnpj
WHERE o.uf IS NOT NULL  -- Apenas operadoras com UF conhecida
GROUP BY o.razao_social, o.uf
ORDER BY total_despesas DESC;

-- Índice para busca rápida
CREATE INDEX idx_agregadas_uf ON despesas_agregadas(uf);
CREATE INDEX idx_agregadas_total ON despesas_agregadas(total_despesas DESC);

-- Comentários
COMMENT ON MATERIALIZED VIEW despesas_agregadas IS 'Agregação pré-calculada por operadora/UF com estatísticas (requer REFRESH manual)';

-- ============================================================================
-- TABELA: metadata_importacao
-- ============================================================================
-- Controle de importações e auditoria
-- ============================================================================

CREATE TABLE metadata_importacao (
    id                      SERIAL PRIMARY KEY,
    tipo_arquivo            VARCHAR(50) NOT NULL,
    nome_arquivo            VARCHAR(255) NOT NULL,
    total_registros         INTEGER NOT NULL,
    registros_importados    INTEGER NOT NULL,
    registros_rejeitados    INTEGER NOT NULL,
    data_importacao         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario                 VARCHAR(100),
    mensagens_erro          TEXT,
    
    -- Constraints
    CONSTRAINT chk_tipo_arquivo CHECK (
        tipo_arquivo IN ('OPERADORAS', 'DESPESAS_CONSOLIDADAS', 'DESPESAS_ENRIQUECIDAS')
    )
);

-- Comentários
COMMENT ON TABLE metadata_importacao IS 'Controle e auditoria de importações de dados';

-- ============================================================================
-- FUNÇÃO: get_trimestre_data_inicio
-- ============================================================================
-- Retorna a data de início do trimestre (primeiro dia)
-- Útil para análises temporais e conversões para DATE
-- ============================================================================

CREATE OR REPLACE FUNCTION get_trimestre_data_inicio(t INTEGER, a INTEGER) 
RETURNS DATE AS $$
BEGIN
    -- Validações
    IF t NOT BETWEEN 1 AND 4 THEN
        RAISE EXCEPTION 'Trimestre inválido: % (deve ser 1-4)', t;
    END IF;
    
    IF a NOT BETWEEN 2020 AND 2030 THEN
        RAISE EXCEPTION 'Ano inválido: % (deve ser 2020-2030)', a;
    END IF;
    
    -- Cálculo: primeiro dia do trimestre
    -- Q1 = janeiro (1), Q2 = abril (4), Q3 = julho (7), Q4 = outubro (10)
    RETURN MAKE_DATE(a, (t-1)*3 + 1, 1);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Comentários
COMMENT ON FUNCTION get_trimestre_data_inicio IS 'Retorna data de início do trimestre (formato: YYYY-MM-DD)';

-- ============================================================================
-- GRANTS (ajustar conforme necessário)
-- ============================================================================
-- Para desenvolvimento/testes, pode usar usuário com permissões completas
-- Em produção, separar usuários read-only e read-write
-- ============================================================================

-- Exemplo (ajustar usuário conforme ambiente):
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;
-- GRANT SELECT ON despesas_agregadas TO readonly_user;

-- ============================================================================
-- VERIFICAÇÕES
-- ============================================================================

-- Verificar criação das tabelas
DO $$ 
BEGIN
    RAISE NOTICE '✓ Schema criado com sucesso!';
    RAISE NOTICE '  - Tabelas: operadoras, despesas_consolidadas, metadata_importacao';
    RAISE NOTICE '  - View materializada: despesas_agregadas';
    RAISE NOTICE '  - Função: get_trimestre_data_inicio(trimestre, ano)';
    RAISE NOTICE '';
    RAISE NOTICE 'Próximo passo: executar import.sql para carregar dados';
END $$;
