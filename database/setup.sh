#!/usr/bin/env bash
# ============================================================================
# Script de Setup Completo - Parte 3 (Banco de Dados)
# ============================================================================
# Executa a criação do schema, importação de dados e queries analíticas
# Requisitos: PostgreSQL 17+ instalado e rodando
# ============================================================================

set -euo pipefail  # Strict mode: exit on error, undefined vars, pipe failures

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para print colorido
log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

# ============================================================================
# CONFIGURAÇÕES
# ============================================================================

# Configurações do banco (ajustar conforme ambiente)
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DB_NAME="${POSTGRES_DB:-teste_intuitive_care}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

# Diretórios
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
QUERIES_DIR="$SCRIPT_DIR/queries"

# Arquivos SQL
SCHEMA_FILE="$SCRIPT_DIR/schema.sql"
IMPORT_FILE="$SCRIPT_DIR/import.sql"

# ============================================================================
# BANNER
# ============================================================================

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   PARTE 3: BANCO DE DADOS E ANÁLISE                           ║"
echo "║   Teste Intuitive Care - Setup Completo                       ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# VERIFICAÇÕES PRELIMINARES
# ============================================================================

log_info "Verificando pré-requisitos..."

# Verificar PostgreSQL
if ! command -v psql &> /dev/null; then
    log_error "PostgreSQL (psql) não encontrado. Instale antes de continuar."
    exit 1
fi
log_success "PostgreSQL instalado"

# Verificar arquivos SQL
if [[ ! -f "$SCHEMA_FILE" ]]; then
    log_error "Arquivo schema.sql não encontrado em $SCHEMA_FILE"
    exit 1
fi
log_success "schema.sql encontrado"

if [[ ! -f "$IMPORT_FILE" ]]; then
    log_error "Arquivo import.sql não encontrado em $IMPORT_FILE"
    exit 1
fi
log_success "import.sql encontrado"

# Verificar arquivos CSV
CSV_DIR="$PROJECT_ROOT/dados"
if [[ ! -d "$CSV_DIR/parte1" ]] || [[ ! -d "$CSV_DIR/parte2" ]]; then
    log_warning "Diretórios de dados não encontrados. Certifique-se de executar Partes 1 e 2 primeiro."
    log_warning "Esperado: $CSV_DIR/parte1/ e $CSV_DIR/parte2/"
fi

# ============================================================================
# CONEXÃO COM BANCO
# ============================================================================

log_info "Testando conexão com PostgreSQL..."

# Exportar senha para evitar prompt
export PGPASSWORD="$DB_PASSWORD"

# Testar conexão
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1" > /dev/null 2>&1; then
    log_error "Não foi possível conectar ao PostgreSQL."
    log_error "Host: $DB_HOST:$DB_PORT, User: $DB_USER"
    log_error "Verifique se o PostgreSQL está rodando e as credenciais estão corretas."
    exit 1
fi
log_success "Conexão estabelecida com PostgreSQL"

# ============================================================================
# CRIAÇÃO DO BANCO
# ============================================================================

log_info "Criando banco de dados '$DB_NAME'..."

# Verificar se banco já existe
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
    log_warning "Banco '$DB_NAME' já existe."
    read -p "Deseja recriar o banco? [y/N] " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "Dropando banco existente..."
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" > /dev/null
        log_success "Banco dropado"
    else
        log_info "Usando banco existente"
    fi
fi

# Criar banco se não existir
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" > /dev/null 2>&1 || true
log_success "Banco '$DB_NAME' pronto"

# ============================================================================
# EXECUÇÃO DO SCHEMA
# ============================================================================

echo ""
log_info "Executando schema.sql (criação de tabelas, índices, views)..."

if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCHEMA_FILE"; then
    log_success "Schema criado com sucesso"
else
    log_error "Erro ao executar schema.sql"
    exit 1
fi

# ============================================================================
# IMPORTAÇÃO DE DADOS
# ============================================================================

echo ""
log_info "Executando import.sql (carga de dados dos CSVs)..."
log_warning "Certifique-se de que os arquivos CSV estão em:"
log_warning "  - $CSV_DIR/parte1/consolidado_despesas.csv"
log_warning "  - $CSV_DIR/parte2/despesas_enriquecidas.csv"

# Executar import (pode levar alguns segundos)
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$IMPORT_FILE"; then
    log_success "Dados importados com sucesso"
else
    log_error "Erro ao executar import.sql"
    log_error "Verifique se os arquivos CSV existem e estão no formato correto"
    exit 1
fi

# ============================================================================
# QUERIES ANALÍTICAS
# ============================================================================

echo ""
log_info "Executando queries analíticas..."

# Query 1: Crescimento percentual
if [[ -f "$QUERIES_DIR/01_crescimento_percentual.sql" ]]; then
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "QUERY 1: Top 5 Operadoras com Maior Crescimento Percentual"
    echo "════════════════════════════════════════════════════════════════"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$QUERIES_DIR/01_crescimento_percentual.sql"
fi

# Query 2: Distribuição por UF
if [[ -f "$QUERIES_DIR/02_distribuicao_uf.sql" ]]; then
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "QUERY 2: Distribuição de Despesas por UF"
    echo "════════════════════════════════════════════════════════════════"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$QUERIES_DIR/02_distribuicao_uf.sql"
fi

# Query 3: Operadoras acima da média
if [[ -f "$QUERIES_DIR/03_operadoras_acima_media.sql" ]]; then
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "QUERY 3: Operadoras Acima da Média (2+ trimestres)"
    echo "════════════════════════════════════════════════════════════════"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$QUERIES_DIR/03_operadoras_acima_media.sql"
fi

# ============================================================================
# FINALIZAÇÃO
# ============================================================================

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   ✓ SETUP CONCLUÍDO COM SUCESSO!                              ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
log_success "Banco de dados: $DB_NAME"
log_success "Host: $DB_HOST:$DB_PORT"
log_success "Usuário: $DB_USER"
echo ""
log_info "Para conectar manualmente ao banco:"
echo "  psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
echo ""
log_info "Para executar queries individuais:"
echo "  psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f database/queries/01_crescimento_percentual.sql"
echo ""
