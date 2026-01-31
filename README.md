# Teste Técnico - Intuitive Care

Sistema completo de análise de despesas de operadoras de planos de saúde, desenvolvido em **Java 25 + Gradle** (backend ETL), **FastAPI + Python** (API REST), e **Vue.js 3** (interface web) como parte do processo seletivo para estagiário de desenvolvimento.

## Índice

- [Visão Geral](#visão-geral)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)
- [Execução das Partes](#execução-das-partes)
  - [Parte 1: Integração com API e Consolidação](#parte-1-integração-com-api-e-consolidação)
  - [Parte 2: Validação CNPJ e Enriquecimento](#parte-2-validação-cnpj-e-enriquecimento)
  - [Parte 3: Banco de Dados e Queries Analíticas](#parte-3-banco-de-dados-e-queries-analíticas)
  - [Parte 4: API REST e Interface Web](#parte-4-api-rest-e-interface-web)
- [Decisões Técnicas](#decisões-técnicas)
- [Resultados e Validações](#resultados-e-validações)

---

## Visão Geral

Este projeto implementa um sistema completo de análise de despesas de operadoras de planos de saúde:

1. **Extração (Parte 1)**: Integração com API da ANS para coletar dados de despesas
2. **Transformação (Parte 2)**: Validação de CNPJ, enriquecimento com dados cadastrais, agregações
3. **Carregamento (Parte 3)**: Persistência em PostgreSQL com queries analíticas
4. **Exposição (Parte 4)**: API REST (FastAPI) + Interface Web (Vue.js)

### Principais Funcionalidades

**ETL (Partes 1-3):**
- Coleta automática de dados via API REST (ANS)
- Validação de CNPJ com dígitos verificadores
- Enriquecimento com dados cadastrais de operadoras
- Agregações estatísticas (média, mediana, mínimo, máximo)
- Banco de dados PostgreSQL com schema otimizado
- Queries analíticas: crescimento, distribuição geográfica, benchmarking

**API + Web (Parte 4):**
- API REST com 4 endpoints (lista, detalhes, despesas, estatísticas)
- Paginação e filtros (busca, UF)
- Interface web responsiva com Vue.js
- Dashboard com gráficos interativos (Chart.js)
- Navegação client-side (Vue Router)
- State management (Pinia)

### Stack Tecnológica

**Backend ETL (Java):**
- **Java 25** (JDK 25 com Virtual Threads e Records)
- **Gradle 9.3.0** (Kotlin DSL)
- **Apache HttpClient 5.4.1** (REST API integration)
- **Apache Commons CSV 1.12.0** (CSV parsing)
- **Apache Commons Compress 1.27.1** (ZIP handling)
- **Jackson 2.18.2** (JSON processing)
- **SLF4J + Logback** (Structured logging)
- **JUnit Jupiter** (Testing)

**Database:**
- **PostgreSQL 18.1** (Database com views materializadas)

**API Backend (Python):**
- **Python 3.14.2**
- **FastAPI 0.115.0** (API framework)
- **Uvicorn 0.32.0** (ASGI server)
- **psycopg2-binary 2.9.10** (PostgreSQL driver)
- **Pydantic 2.12.5** (Validation)

**Frontend (JavaScript):**
- **Vue 3** (Composition API)
- **Vite 7.2.5 + Rolldown** (Build tool experimental)
- **Vue Router 4** (Navigation)
- **Pinia** (State management)
- **Chart.js** (Data visualization)
- **Axios** (HTTP client)

---

## Estrutura do Projeto

```
Teste_Evandro/
├── app/                                      # Backend ETL (Java)
│   ├── src/
│   │   ├── main/java/com/intuitivecare/teste/
│   │   │   ├── parte1/                      # Integração com API ANS
│   │   │   │   ├── Parte1Main.java
│   │   │   │   ├── domain/
│   │   │   │   ├── application/
│   │   │   │   └── infrastructure/
│   │   │   └── parte2/                      # Validação e enriquecimento
│   │   │       ├── Parte2Main.java
│   │   │       └── domain/
│   │   └── test/java/                       # Testes unitários
│   ├── dados/
│   │   ├── parte1/                          # CSV consolidados
│   │   └── parte2/                          # CSV enriquecidos
│   └── build.gradle.kts
├── database/                                 # PostgreSQL (Parte 3)
│   ├── queries/
│   │   ├── 01_crescimento_percentual.sql
│   │   ├── 02_distribuicao_uf.sql
│   │   └── 03_operadoras_acima_media.sql
│   ├── schema.sql
│   ├── import.sql
│   └── setup.sh
├── api-backend/                              # API REST (Parte 4 - FastAPI)
│   ├── app/
│   │   ├── main.py                          # Entry point
│   │   ├── database.py                      # Connection pooling
│   │   ├── models/
│   │   │   └── schemas.py                   # Pydantic models
│   │   └── routes/
│   │       ├── operadoras.py                # Endpoints operadoras
│   │       └── estatisticas.py              # Endpoint estatísticas
│   ├── requirements.txt
│   └── README.md
├── web-interface/                            # Frontend (Parte 4 - Vue.js)
│   ├── src/
│   │   ├── views/                           # Páginas
│   │   │   ├── HomeView.vue                 # Lista operadoras
│   │   │   ├── OperadoraDetailView.vue      # Detalhes
│   │   │   └── DashboardView.vue            # Dashboard
│   │   ├── stores/                          # Pinia stores
│   │   │   ├── operadoras.js
│   │   │   └── estatisticas.js
│   │   ├── services/
│   │   │   └── api.js                       # Axios client
│   │   ├── router/
│   │   │   └── index.js                     # Vue Router
│   │   ├── App.vue                          # Layout principal
│   │   ├── main.js                          # Entry point
│   │   └── style.css                        # Estilos globais
│   ├── package.json
│   ├── vite.config.js
│   └── README.md
├── Postman_Collection.json                   # Collection para testes da API
├── DECISOES_TECNICAS.md                      # Trade-offs documentados
└── README.md
```

---

## Pré-requisitos

### Software Necessário

**Partes 1-3 (ETL + Database):**
- **JDK 25** (ou superior)
- **PostgreSQL 14+** (testado com PostgreSQL 18.1)

**Parte 4 - Backend (API):**
- **Python 3.14+** (ou 3.12+)
- **pip** (gerenciador de pacotes Python)

**Parte 4 - Frontend (Web):**
- **Node.js 18+** (ou superior)
- **npm** (vem com Node.js)

**Geral:**
- **Git** (para clonar o repositório)

> **Nota**: Gradle não precisa estar instalado! O projeto usa Gradle Wrapper (`gradlew`).

### Verificar Instalações

```bash
# Java
java -version
# Deve mostrar: java version "25" ou superior

# PostgreSQL
psql --version

# Python
python3 --version
# Deve mostrar: Python 3.12+ ou 3.14+

# Node.js
node --version
# Deve mostrar: v18+ ou superior

# npm
npm --version

# Git
git --version
```

---

## Instalação

### 1. Clonar o Repositório

```bash
git clone <url-do-repositorio>
cd Teste_Evandro
```

### 2. Verificar JDK 25

```bash
java -version
# Deve mostrar: java version "25" ou superior
```

Se não tiver JDK 25, instale via:

**Linux (SDKMAN)**:
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-open
```

**macOS (Homebrew)**:
```bash
brew install openjdk@25
```

**Windows**:
Baixe do site oficial da Oracle/OpenJDK ou use [Adoptium](https://adoptium.net/).

### 3. Configurar PostgreSQL

```bash
# Verificar se PostgreSQL está rodando
sudo systemctl status postgresql

# Iniciar PostgreSQL (se não estiver rodando)
sudo systemctl start postgresql

# Criar usuário (se necessário)
sudo -u postgres createuser -s $USER
```

### 4. Build do Projeto

```bash
# Linux/macOS
./gradlew build

# Windows
gradlew.bat build
```

Isso irá:
- Baixar todas as dependências do Maven Central
- Compilar o código Java 25
- Executar os testes unitários
- Gerar o JAR executável em `app/build/libs/`

#### Verificar Build

```bash
# Ver tasks disponíveis
./gradlew tasks --group="application"

# Output esperado:
# run - Runs this project as a JVM application
# runParte2 - Executa Parte 2: Transformação e Validação de Dados
```

---

## Execução das Partes

### Parte 1: Integração com API e Consolidação

**Objetivo**: Coletar dados de despesas da API da ANS e consolidar em CSV.

#### Executar

```bash
# Linux/macOS
./gradlew run

# Windows
gradlew.bat run
```

#### O que acontece?

A classe `Parte1Main.java` executa:

1. **Busca operadoras ativas** via API `/operadoras` (filtro: `ativa=Sim`)
2. **Baixa arquivos ZIP** com despesas trimestrais (Q1, Q2, Q3 de 2025)
3. **Descompacta e parseia CSV** de dentro dos ZIPs
4. **Consolida dados** em formato único
5. **Valida valores negativos** (flagging com `VALOR_NEGATIVO`)
6. **Gera arquivo** `app/dados/parte1/consolidado_despesas.csv`

#### Arquitetura da Parte 1

```
Parte1Main
    └─> IntegracaoANSService (Orquestração)
            ├─> ANSHttpClient (REST API calls)
            ├─> ZipFileHandler (Download + descompactação)
            ├─> CSVDespesasParser (Parse CSV)
            └─> OperadoraCadastroParser (Parse cadastro)
```

#### Saída Esperada

```
[INFO] Iniciando Parte 1: Integração com API ANS
[INFO] Buscando operadoras ativas...
[INFO] ✓ Operadoras ativas encontradas: 732
[INFO] Coletando despesas do trimestre Q1/2025...
[INFO] ✓ Trimestre Q1/2025: 712 registros
[INFO] Coletando despesas do trimestre Q2/2025...
[INFO] ✓ Trimestre Q2/2025: 718 registros
[INFO] Coletando despesas do trimestre Q3/2025...
[INFO] ✓ Trimestre Q3/2025: 718 registros
[INFO] Total de registros consolidados: 2148
[INFO] Valores negativos detectados: 1 (flag VALOR_NEGATIVO)
[INFO] ✓ Arquivo salvo: app/dados/parte1/consolidado_despesas.csv (211 KB)
[INFO] Parte 1 concluída com sucesso!
```

#### Validação

```bash
# Ver primeiras linhas
head -5 app/dados/parte1/consolidado_despesas.csv

# Contar registros
wc -l app/dados/parte1/consolidado_despesas.csv
# Esperado: 2149 (2148 + header)

# Verificar valores negativos
grep "VALOR_NEGATIVO" app/dados/parte1/consolidado_despesas.csv

# Verificar formato CSV
awk -F',' '{print NF}' app/dados/parte1/consolidado_despesas.csv | sort -u
# Todas as linhas devem ter o mesmo número de colunas
```

---

### Parte 2: Validação CNPJ e Enriquecimento

**Objetivo**: Validar CNPJs, enriquecer com dados cadastrais e gerar agregações estatísticas.

#### Executar

```bash
# Linux/macOS
./gradlew runParte2

# Windows
gradlew.bat runParte2
```

#### O que acontece?

A classe `Parte2Main.java` executa:

1. **Lê dados consolidados** da Parte 1
2. **Valida CNPJs** com algoritmo de dígitos verificadores (`CNPJValidator`)
3. **Enriquece dados** com razão social, UF, modalidade (via API `/operadoras/{registro}`)
4. **Trata casos especiais**: operadoras sem cadastro (usa `REG_ANS` como CNPJ temporário)
5. **Calcula agregações** por UF: média, mediana, mínimo, máximo
6. **Gera arquivos CSV**:
   - `app/dados/parte2/despesas_enriquecidas.csv` (255 KB)
   - `app/dados/parte2/despesas_agregadas.csv` (66 KB)

#### Arquitetura da Parte 2

```
Parte2Main
    ├─> CNPJValidator (Validação com dígitos verificadores)
    ├─> DespesaEnriquecida (Model com dados completos)
    └─> DespesasAgregadasPorOperadora (Agregações estatísticas)
```

#### Algoritmo de Validação CNPJ

```java
// Implementação em CNPJValidator.java
1. Remove caracteres não numéricos
2. Valida tamanho (14 dígitos)
3. Valida sequências repetidas (00000000000000, 11111111111111, etc.)
4. Calcula 1º dígito verificador:
   - Multiplicadores: 5,4,3,2,9,8,7,6,5,4,3,2
   - Soma, calcula resto por 11
   - DV1 = (11 - resto) % 11
5. Calcula 2º dígito verificador:
   - Multiplicadores: 6,5,4,3,2,9,8,7,6,5,4,3,2
   - Soma, calcula resto por 11
   - DV2 = (11 - resto) % 11
6. Compara DVs calculados com os informados
```

#### Saída Esperada

```
[INFO] Iniciando Parte 2: Transformação e Validação de Dados
[INFO] Lendo dados consolidados: app/dados/parte1/consolidado_despesas.csv
[INFO] Total de registros: 2148
[INFO] Validando CNPJs...
[INFO] ✓ CNPJs válidos: 1520 (70.76%)
[INFO] ✗ CNPJs inválidos: 628 (29.24%)
[INFO] Enriquecendo dados com informações cadastrais...
[INFO] ✓ Registros enriquecidos: 2148
[INFO] ✓ Operadoras sem cadastro (REG_ANS usado): 628
[INFO] Calculando agregações por UF...
[INFO] ✓ Estados processados: 27 UFs
[INFO] Salvando arquivos CSV...
[INFO] ✓ Arquivo salvo: app/dados/parte2/despesas_enriquecidas.csv (255 KB)
[INFO] ✓ Arquivo salvo: app/dados/parte2/despesas_agregadas.csv (66 KB)
[INFO] Parte 2 concluída com sucesso!
```

#### Validação

```bash
# Ver estrutura do arquivo enriquecido
head -3 app/dados/parte2/despesas_enriquecidas.csv

# Contar operadoras sem cadastro
grep "SEM_CADASTRO" app/dados/parte2/despesas_enriquecidas.csv | wc -l
# Esperado: 628

# Ver agregações por UF
cat app/dados/parte2/despesas_agregadas.csv

# Contar estados
tail -n +2 app/dados/parte2/despesas_agregadas.csv | wc -l
# Esperado: 27 UFs
```

#### Testar Validação de CNPJ

```bash
# Executar testes unitários
./gradlew test --tests CNPJValidatorTest

# Casos de teste incluem:
# ✓ CNPJ válido: 00.000.000/0001-91
# ✗ CNPJ inválido (DV errado)
# ✗ CNPJ com sequência repetida (11.111.111/1111-11)
# ✗ CNPJ com tamanho incorreto
```

---

### Parte 3: Banco de Dados e Queries Analíticas

**Objetivo**: Carregar dados em PostgreSQL e executar análises SQL.

#### Opção A: Execução Automática (Recomendado)

```bash
# Tornar o script executável
chmod +x database/setup.sh

# Executar setup completo
./database/setup.sh
```

O script `setup.sh` automatiza:
- Verificação de pré-requisitos (psql, arquivos SQL, CSVs)
- Teste de conexão PostgreSQL
- Criação/recriação do banco de dados `teste_intuitive_care`
- Criação do schema (tabelas, views, índices, funções)
- Importação dos dados CSV (COPY bulk loading)
- Execução das 3 queries analíticas com formatação colorida

#### Opção B: Execução Manual Passo a Passo

```bash
# 1. Criar banco de dados
psql -U postgres -c "DROP DATABASE IF EXISTS teste_intuitive_care;"
psql -U postgres -c "CREATE DATABASE teste_intuitive_care;"

# 2. Criar schema (tabelas, views, índices, funções)
psql -U postgres -d teste_intuitive_care -f database/schema.sql

# 3. Importar dados CSV
psql -U postgres -d teste_intuitive_care -f database/import.sql

# 4. Executar queries analíticas
psql -U postgres -d teste_intuitive_care -f database/queries/01_crescimento_percentual.sql
psql -U postgres -d teste_intuitive_care -f database/queries/02_distribuicao_uf.sql
psql -U postgres -d teste_intuitive_care -f database/queries/03_operadoras_acima_media.sql
```

#### O que acontece?

**Schema (`schema.sql`)**:

Cria a seguinte estrutura:

**Tabelas**:
- `operadoras`: Cadastro de operadoras (732 registros)
  - Colunas: cnpj, razao_social, registro_ans, modalidade, uf, status_validacao
- `despesas_consolidadas`: Despesas por operadora/trimestre (2.148 registros)
  - Colunas: cnpj_operadora, trimestre, ano, valor_despesas
  - FK: cnpj_operadora → operadoras.cnpj
- `metadata_importacao`: Auditoria de importações
  - Colunas: tabela, registros_importados, data_importacao

**View Materializada**:
- `despesas_agregadas`: Pré-cálculo de agregações por UF (721 registros)
  - Colunas: uf, qtd_operadoras, total_despesas, media_por_operadora, media_por_trimestre, pct_total

**Função**:
- `get_trimestre_data_inicio(trimestre INTEGER, ano INTEGER) RETURNS DATE`
  - Converte trimestre/ano para data de início (ex: Q1/2025 → 2025-01-01)

**Índices** (10+ índices seletivos):
- `idx_operadoras_cnpj` (UNIQUE)
- `idx_operadoras_uf` (para queries geográficas)
- `idx_despesas_cnpj` (FK lookup)
- `idx_despesas_temporal` (ano, trimestre)
- `idx_despesas_composite` (cnpj + temporal)
- E mais...

**Import (`import.sql`)**:

Executa em 2 fases dentro de transação:

**Fase 1: Importar Operadoras**
```sql
COPY operadoras (cnpj, razao_social, registro_ans, modalidade, uf, status_validacao)
FROM '/path/to/despesas_enriquecidas.csv'
WITH (FORMAT CSV, HEADER TRUE, DELIMITER ',', ENCODING 'UTF8');
```

**Fase 2: Importar Despesas**
```sql
COPY despesas_consolidadas (cnpj_operadora, trimestre, ano, valor_despesas)
FROM '/path/to/consolidado_despesas.csv'
WITH (FORMAT CSV, HEADER TRUE, DELIMITER ',', ENCODING 'UTF8');
```

**Benefícios do COPY**:
- 10x mais rápido que INSERT row-by-row
- Transaction-based (ROLLBACK on error)
- Metadata tracking (registro de auditoria)

**Queries Analíticas**:

**1. `01_crescimento_percentual.sql`** - Top 5 operadoras com maior crescimento percentual
```sql
-- Abordagem: CTEs para separar lógica
-- 1. trimestre_periodo: Converte ano+trimestre em número sequencial
-- 2. dados_periodo: Encontra períodos inicial/final por operadora
-- 3. valores_periodo: JOINs para pegar valores reais
-- 4. Final: JOIN operadoras, calcula crescimento%, ORDER BY DESC
```

**2. `02_distribuicao_uf.sql`** - Distribuição de despesas por estado
```sql
-- Top 5 UFs por valor total
-- Métricas: total, média por operadora, média por trimestre, % do nacional
-- Análise adicional: agrupamento por região geográfica
```

**3. `03_operadoras_acima_media.sql`** - Operadoras acima da média do mercado
```sql
-- Abordagem: CTE-based
-- 1. media_por_trimestre: Calcula média do mercado por trimestre
-- 2. operadoras_comparacao: Compara cada operadora vs média
-- 3. contagem_trimestres: Conta quantos trimestres acima da média
-- Filtro: HAVING SUM(acima_da_media) >= 2 (pelo menos 2 trimestres)
```

#### Saída Esperada

```
========================================
VERIFICAÇÃO DE PRÉ-REQUISITOS
========================================
✓ psql encontrado: /usr/bin/psql
✓ PostgreSQL versão: 18.1
✓ Arquivo schema.sql encontrado
✓ Arquivo import.sql encontrado
✓ Arquivo CSV consolidado_despesas.csv encontrado
✓ Arquivo CSV despesas_enriquecidas.csv encontrado

========================================
TESTE DE CONEXÃO
========================================
✓ Conexão com PostgreSQL estabelecida

========================================
CRIAÇÃO DO BANCO DE DADOS
========================================
Banco de dados 'teste_intuitive_care' já existe. Deseja recriar? (s/N): s
✓ Banco de dados recriado

========================================
CRIAÇÃO DO SCHEMA
========================================
CREATE TABLE
CREATE INDEX
...
✓ Schema criado com sucesso!
  - Tabelas: operadoras, despesas_consolidadas, metadata_importacao
  - View materializada: despesas_agregadas
  - Função: get_trimestre_data_inicio(trimestre, ano)

========================================
IMPORTAÇÃO DE DADOS
========================================
✓ IMPORTAÇÃO CONCLUÍDA COM SUCESSO!

ESTATÍSTICAS GERAIS
Operadoras cadastradas:  732
Despesas importadas:     2148
Agregações geradas:      721
Valor total de despesas: R$ 2,396,072,167,089.24

TOP 5 ESTADOS POR VALOR TOTAL DE DESPESAS:
 uf | operadoras |   total_despesas   
----+------------+--------------------
 SP |        257 | 861,609,669,959.34
 RJ |         63 | 614,615,139,005.70
 MG |        103 | 146,736,524,720.16
 CE |         17 | 143,428,265,746.20
 DF |         16 | 132,687,946,236.84

TOP 5 OPERADORAS POR VALOR TOTAL:
                razao_social                | uf |   total_despesas   
--------------------------------------------+----+--------------------
 BRADESCO SAÚDE S.A.                        | RJ | 282,936,357,877.56
 SUL AMERICA COMPANHIA DE SEGURO SAÚDE      | RJ | 202,842,663,097.02
 AMIL ASSISTÊNCIA MÉDICA INTERNACIONAL S.A. | SP | 193,502,567,621.58
 HAPVIDA ASSISTENCIA MEDICA S.A.            | CE | 91,585,974,472.02
 NOTRE DAME INTERMÉDICA SAÚDE S.A.          | SP | 90,457,368,522.36

========================================
QUERIES ANALÍTICAS
========================================

QUERY 1: Top 5 Crescimento Percentual

                Operadora                | UF | Período Inicial | Valor Inicial (R$) | Período Final | Valor Final (R$) | Crescimento (%) 
-----------------------------------------+----+-----------------+--------------------+---------------+------------------+-----------------
 SAGRADA SAÚDE ASSISTÊNCIA MÉDICA LTDA   | MG | Q1/2025         | 11,894.22          | Q3/2025       | 440,349.12       | 3602.21
 SUL AMÉRICA PARANÁ CLÍNICAS S.A.        | SP | Q1/2025         | 5,743,471.92       | Q3/2025       | 133,986,332.22   | 2232.85
 ...

QUERY 2: Distribuição por UF

 UF | Qtd. Operadoras | Total Despesas (R$) | Média por Operadora (R$) | % do Total 
----+-----------------+---------------------+--------------------------+------------
 SP |             257 | 861,609,669,959.34  | 3,352,566,809.18         | 35.96
 RJ |              63 | 614,615,139,005.70  | 9,755,795,857.23         | 25.65
 ...

QUERY 3: Operadoras Acima da Média

                    Operadora                     | UF | Trimestres Acima | Média (R$)        | Diferença (%) 
--------------------------------------------------+----+------------------+-------------------+---------------
 BRADESCO SAÚDE S.A.                              | RJ |                3 | 94,312,119,292.52 | 1026.78
 SUL AMERICA COMPANHIA DE SEGURO SAÚDE            | RJ |                3 | 67,614,221,032.34 | 707.56
 ...
```

---

### Parte 4: API REST e Interface Web

**Objetivo:** Expor os dados através de uma API REST e criar interface web para visualização.

#### 4.1 Backend - API REST (FastAPI)

**Executar Backend:**

```bash
# Navegar para diretório da API
cd api-backend

# Criar ambiente virtual (recomendado)
python3 -m venv venv
source venv/bin/activate  # Linux/macOS
# ou
venv\Scripts\activate  # Windows

# Instalar dependências
pip install -r requirements.txt

# Executar servidor
python -m uvicorn app.main:app --reload

# Servidor estará em: http://localhost:8000
# Documentação automática: http://localhost:8000/docs
```

**Endpoints Disponíveis:**

1. `GET /api/operadoras` - Lista paginada de operadoras
   - Query params: `page`, `limit`, `search`, `uf`
   - Retorna: `{ data: [...], total, page, limit, total_pages, has_next, has_previous }`

2. `GET /api/operadoras/{cnpj}` - Detalhes da operadora
   - Retorna: Informações cadastrais + estatísticas

3. `GET /api/operadoras/{cnpj}/despesas` - Histórico de despesas
   - Retorna: Lista de despesas ordenada por ano/trimestre DESC

4. `GET /api/estatisticas` - Estatísticas agregadas
   - Retorna: Total operadoras, despesas, médias, top 5, despesas por UF

**Testar com Postman:**

```bash
# Importar collection
# Arquivo: Postman_Collection.json
# Contém 8 requests com testes automatizados
```

**Variáveis de Ambiente:**

```bash
# Criar arquivo .env (opcional)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=teste_intuitive_care
DB_USER=postgres
DB_PASSWORD=postgres
```

#### 4.2 Frontend - Interface Web (Vue.js)

**Executar Frontend:**

```bash
# Navegar para diretório do frontend
cd web-interface

# Instalar dependências
npm install

# Executar servidor de desenvolvimento
npm run dev

# Aplicação estará em: http://localhost:5173
```

**Funcionalidades:**

1. **Lista de Operadoras (`/`)**:
   - Tabela paginada (20 registros por página)
   - Busca por Razão Social ou CNPJ
   - Filtro por UF
   - Badges de status de validação
   - Botão "Detalhes" para cada operadora

2. **Detalhes da Operadora (`/operadora/:cnpj`)**:
   - Informações cadastrais
   - Cards com estatísticas (Total Despesas, Quantidade Trimestres)
   - Histórico de despesas agrupado por ano
   - Tabelas de despesas por trimestre

3. **Dashboard (`/dashboard`)**:
   - 4 cards de métricas gerais
   - Top 5 operadoras (badges ouro/prata/bronze)
   - Gráfico Chart.js dual-axis (despesas + quantidade por UF)
   - Tabela detalhada por UF

**Build para Produção:**

```bash
# Gerar build otimizado
npm run build

# Preview do build
npm run preview

# Arquivos gerados em: dist/
```

**Arquitetura Frontend:**

```
Vue Router → View Component → Pinia Store → API Service (Axios) → FastAPI Backend
                                    ↓
                              Local State (ref, computed)
```

**Tecnologias:**
- **Vue 3**: Composition API com `<script setup>`
- **Vite 7 + Rolldown**: Build ultrarrápido (experimental 2026)
- **Pinia**: State management oficial Vue 3
- **Chart.js**: Gráficos interativos
- **Axios**: HTTP client com interceptors
 BRADESCO SAÚDE S.A.                              | RJ |                3 | 94,312,119,292.52 | 5435.09
 SUL AMERICA COMPANHIA DE SEGURO SAÚDE            | RJ |                3 | 67,614,221,032.34 | 3868.21
 ...

✓ Próximo passo: executar queries analíticas em database/queries/
```

#### Validação do Banco de Dados

```bash
# Verificar tabelas criadas
psql -U postgres -d teste_intuitive_care -c "\dt"

# Contar registros
psql -U postgres -d teste_intuitive_care -c "SELECT COUNT(*) FROM operadoras;"        # 732
psql -U postgres -d teste_intuitive_care -c "SELECT COUNT(*) FROM despesas_consolidadas;"  # 2148

# Ver distribuição por UF
psql -U postgres -d teste_intuitive_care -c "
SELECT uf, COUNT(*) as qtd
FROM operadoras
GROUP BY uf
ORDER BY COUNT(*) DESC
LIMIT 10;
"

# Verificar view materializada
psql -U postgres -d teste_intuitive_care -c "SELECT * FROM despesas_agregadas LIMIT 5;"

# Ver metadata de importação
psql -U postgres -d teste_intuitive_care -c "SELECT * FROM metadata_importacao;"
```

---

## Decisões Técnicas

Consulte [DECISOES_TECNICAS.md](DECISOES_TECNICAS.md) para análise completa dos trade-offs.

### Principais Trade-offs

#### 1. **Java 25 vs Python**
- **Decisão**: Java 25
- **Trade-off**: Complexidade de setup vs performance e type-safety
- **Justificativa**:
  - Type-safety em tempo de compilação (menos bugs em produção)
  - Performance superior (especialmente com Virtual Threads para I/O)
  - Ecossistema robusto (Apache Commons, Jackson)
  - Records e Pattern Matching (Java moderno)
  - Setup mais complexo (JDK 25, Gradle)

#### 2. **Gradle vs Maven**
- **Decisão**: Gradle com Kotlin DSL
- **Trade-off**: Curva de aprendizado vs flexibilidade
- **Justificativa**:
  - Build mais rápido (incremental compilation)
  - Kotlin DSL (type-safe, auto-complete)
  - Tasks customizadas (runParte2)
  - Gradle Wrapper (sem instalação necessária)

#### 3. **CNPJ Inválido: Flag vs Exclusão**
- **Decisão**: Manter registros com flag `SEM_CADASTRO`
- **Trade-off**: Qualidade de dados vs perda de informação
- **Justificativa**: 29% dos registros não têm CNPJ válido; excluir geraria análise incompleta

#### 4. **Valores Negativos: Flag vs Rejeição**
- **Decisão**: Permitir valores negativos com flag `VALOR_NEGATIVO`
- **Trade-off**: Integridade vs realidade operacional
- **Justificativa**: Valores negativos podem representar estornos/ajustes legítimos

#### 5. **Persistência: PostgreSQL vs SQLite**
- **Decisão**: PostgreSQL
- **Trade-off**: Complexidade de setup vs escalabilidade
- **Justificativa**:
  - Window functions, CTEs, materialized views
  - COPY bulk loading (10x mais rápido)
  - Índices parciais e GIN/GiST
  - Preparação para produção

#### 6. **Import: COPY vs INSERT**
- **Decisão**: `COPY` bulk loading
- **Trade-off**: Simplicidade vs performance
- **Justificativa**: 10x mais rápido (2.148 registros em <1s vs ~10s)

#### 7. **Logging: SLF4J + Logback vs System.out**
- **Decisão**: SLF4J + Logback
- **Trade-off**: Configuração vs rastreabilidade
- **Justificativa**:
  - Níveis de log (DEBUG, INFO, WARN, ERROR)
  - Formatação estruturada
  - Output colorido no console
  - Possibilidade de log em arquivo

---

## Resultados e Validações

### Métricas Finais

| Métrica | Valor |
|---------|-------|
| Operadoras cadastradas | 732 |
| Despesas registradas | 2.148 |
| Trimestres analisados | Q1, Q2, Q3/2025 |
| CNPJs válidos | 1.520 (70.76%) |
| CNPJs inválidos (REG_ANS) | 628 (29.24%) |
| Valores negativos | 1 (0.05%) |
| **Valor total de despesas** | **R$ 2,396 trilhões** |
| Estados cobertos | 27 UFs |
| Agregações geradas | 721 (por UF/trimestre) |

### Top 5 Operadoras por Volume Total

| Operadora | UF | Valor Total (R$) |
|-----------|----|-----------------:|
| BRADESCO SAÚDE S.A. | RJ | 282,936,357,877.56 |
| SUL AMERICA COMPANHIA DE SEGURO SAÚDE | RJ | 202,842,663,097.02 |
| AMIL ASSISTÊNCIA MÉDICA INTERNACIONAL S.A. | SP | 193,502,567,621.58 |
| HAPVIDA ASSISTENCIA MEDICA S.A. | CE | 91,585,974,472.02 |
| NOTRE DAME INTERMÉDICA SAÚDE S.A. | SP | 90,457,368,522.36 |

### Distribuição Geográfica

| Região | Estados | Operadoras | Valor Total (R$) | % Nacional |
|--------|---------|------------|------------------|-----------:|
| **Sudeste** | 4 | 441 | 1,669,784,919,933.78 | **69.69%** |
| Nordeste | 9 | 85 | 239,728,268,794.26 | 10.01% |
| Sul | 3 | 116 | 229,008,434,116.32 | 9.56% |
| Centro-Oeste | 4 | 55 | 222,974,961,602.88 | 9.31% |
| Norte | 6 | 26 | 31,025,461,994.70 | 1.29% |

### Top 5 Crescimento Percentual (Q1→Q3/2025)

| Operadora | UF | Crescimento |
|-----------|----|-----------:|
| SAGRADA SAÚDE ASSISTÊNCIA MÉDICA LTDA | MG | +3602.21% |
| SUL AMÉRICA PARANÁ CLÍNICAS S.A. | SP | +2232.85% |
| UNIMED PARAÍBA | PB | +2057.72% |
| EXCELÊNCIA PLANO DE SAÚDE S/A | ES | +1991.76% |
| PORTOMED - PORTO SEGURO SERVIÇOS DE SAUDE | SP | +1255.52% |

### Operadoras Acima da Média

- **91 operadoras** (12.43%) ficaram acima da média do mercado nos 3 trimestres
- **4 operadoras** (0.55%) ficaram acima em 2 trimestres
- **633 operadoras** (86.48%) ficaram abaixo da média

---

## Troubleshooting

### Problema: `Error: JAVA_HOME is not set`

**Solução**:
```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk-25
export PATH=$JAVA_HOME/bin:$PATH

# Ou usar SDKMAN
sdk use java 25-open
```

### Problema: `./gradlew: Permission denied`

**Solução**:
```bash
chmod +x gradlew
./gradlew build
```

### Problema: `psql: error: connection to server failed`

**Solução**:
```bash
# Verificar se PostgreSQL está rodando
sudo systemctl status postgresql

# Iniciar PostgreSQL
sudo systemctl start postgresql

# Verificar porta
sudo lsof -i :5432
```

### Problema: `permission denied for database postgres`

**Solução**:
```bash
# Criar usuário com privilégios
sudo -u postgres createuser -s $USER

# Ou executar como usuário postgres
sudo -u postgres ./database/setup.sh
```

### Problema: Valores negativos na importação

**Solução**: Isso é esperado! O schema foi relaxado para permitir valores negativos flagados. Verifique a coluna `flags` no CSV.

### Problema: Gradle build muito lento

**Solução**:
```bash
# Habilitar daemon e build paralelo
echo "org.gradle.daemon=true" >> gradle.properties
echo "org.gradle.parallel=true" >> gradle.properties

# Limpar cache se necessário
./gradlew clean --no-daemon
```

### Problema: `OutOfMemoryError` durante execução

**Solução**:
```bash
# Aumentar memória heap do Gradle
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
./gradlew run
```

---

## Logs e Debugging

### Verificar Logs da Aplicação

```bash
# Logs são exibidos no console com cores
# Níveis: DEBUG, INFO, WARN, ERROR

# Para ver logs mais verbosos, edite app/src/main/resources/logback.xml
# Altere level de INFO para DEBUG:
# <logger name="com.intuitivecare.teste" level="DEBUG"/>
```

### Verificar Logs de Importação no Banco

```bash
# Ver metadata de importação
psql -U postgres -d teste_intuitive_care -c "
SELECT * FROM metadata_importacao
ORDER BY data_importacao DESC;
"
```

### Verificar Integridade dos Dados

```bash
# Contar registros por trimestre
psql -U postgres -d teste_intuitive_care -c "
SELECT ano, trimestre, COUNT(*) as total
FROM despesas_consolidadas
GROUP BY ano, trimestre
ORDER BY ano, trimestre;
"

# Verificar operadoras sem despesas
psql -U postgres -d teste_intuitive_care -c "
SELECT o.cnpj, o.razao_social
FROM operadoras o
LEFT JOIN despesas_consolidadas d ON o.cnpj = d.cnpj_operadora
WHERE d.cnpj_operadora IS NULL;
"

# Verificar valores negativos
psql -U postgres -d teste_intuitive_care -c "
SELECT COUNT(*) as negativos
FROM despesas_consolidadas
WHERE valor_despesas < 0;
"
```

---

## Reexecutar Partes Individualmente

### Reexecutar Parte 1

```bash
# Apagar dados anteriores (opcional)
rm -rf app/dados/parte1/*

# Executar novamente
./gradlew run
```

### Reexecutar Parte 2

```bash
# Requer dados da Parte 1
./gradlew runParte2
```

### Reexecutar Parte 3

```bash
# Recriar banco limpo
./database/setup.sh

# Ou manualmente:
psql -U postgres -c "DROP DATABASE IF EXISTS teste_intuitive_care;"
psql -U postgres -c "CREATE DATABASE teste_intuitive_care;"
psql -U postgres -d teste_intuitive_care -f database/schema.sql
psql -U postgres -d teste_intuitive_care -f database/import.sql
```

---

## Executar Testes

```bash
# Executar todos os testes
./gradlew test

# Executar teste específico
./gradlew test --tests CNPJValidatorTest

# Ver relatório de testes (HTML)
./gradlew test
# Abrir: app/build/reports/tests/test/index.html

# Executar testes com stack trace
./gradlew test --stacktrace

# Executar testes em modo debug
./gradlew test --debug-jvm
```

---

## Tasks Gradle Disponíveis

```bash
# Listar todas as tasks
./gradlew tasks

# Principais tasks:
./gradlew build          # Compila e executa testes
./gradlew clean          # Limpa build directory
./gradlew run            # Executa Parte 1
./gradlew runParte2      # Executa Parte 2
./gradlew test           # Executa testes unitários
./gradlew jar            # Gera JAR executável
./gradlew dependencies   # Lista dependências do projeto
./gradlew check          # Executa verificações (testes + checks)
```

---

## Referências

### APIs e Documentação

- **API ANS**: [https://dadosabertos.ans.gov.br/FTP/PDA/](https://dadosabertos.ans.gov.br/FTP/PDA/)
- **Documentação CNPJ**: Algoritmo de validação com dígitos verificadores
- **PostgreSQL**: [https://www.postgresql.org/docs/](https://www.postgresql.org/docs/)

### Bibliotecas Utilizadas

- **Apache HttpClient**: [https://hc.apache.org/httpcomponents-client-5.4.x/](https://hc.apache.org/httpcomponents-client-5.4.x/)
- **Apache Commons CSV**: [https://commons.apache.org/proper/commons-csv/](https://commons.apache.org/proper/commons-csv/)
- **Apache Commons Compress**: [https://commons.apache.org/proper/commons-compress/](https://commons.apache.org/proper/commons-compress/)
- **Jackson**: [https://github.com/FasterXML/jackson](https://github.com/FasterXML/jackson)
- **Gradle**: [https://docs.gradle.org/current/userguide/userguide.html](https://docs.gradle.org/current/userguide/userguide.html)

### Java 25

- **JEP 444**: Virtual Threads
- **JEP 440**: Record Patterns
- **JEP 441**: Pattern Matching for switch

---

## Autor

Desenvolvido como parte do processo seletivo para estagiário de desenvolvimento na Intuitive Care.

**Tecnologias**: Java 25, Gradle, PostgreSQL, Apache Commons, Jackson  
**Data**: Janeiro de 2026
