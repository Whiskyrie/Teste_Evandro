# Decisões Técnicas - Teste Intuitive Care

> Documentação das escolhas técnicas e justificativas para cada parte do teste.

---

## Tecnologias Base

### Java 25
**Decisão:** Utilizar Java 25 (versão mais recente)

**Justificativa:**
- Acesso aos recursos mais modernos da linguagem
- Pattern matching aprimorado
- Melhorias de performance no GC
- Virtual threads (Project Loom) para melhor concorrência
- Demonstra conhecimento atualizado do ecossistema Java

### Gradle
**Decisão:** Gradle como ferramenta de build

**Justificativa:**
- Build incremental mais rápido que Maven
- Configuração mais concisa (Kotlin DSL)
- Flexibilidade para customizações futuras
- Amplamente utilizado em projetos modernos
- Melhor suporte a projetos multi-módulo

### PostgreSQL (versão mais atual)
**Decisão:** PostgreSQL 17+

**Justificativa:**
- Performance superior em queries analíticas
- Suporte robusto a JSON para dados semi-estruturados
- Window functions e CTEs para análises complexas
- ACID completo e confiabilidade em produção
- Open source com comunidade ativa

---

## Parte 1: Integração com API Pública da ANS

### 1.1 Estratégia de Processamento de Arquivos

**Decisão:** Processamento incremental/streaming

**Justificativa:**
- **Escalabilidade:** Arquivos da ANS podem ter centenas de MB
- **Uso de memória:** Streaming mantém footprint de memória constante
- **Resiliência:** Permite processar arquivos maiores que a RAM disponível
- **Performance:** Processamento pipeline (download → extração → parse → validação)
- **Trade-offs:**
  - Menor uso de memória
  - Pode processar arquivos de qualquer tamanho
  - Código um pouco mais complexo
  - Não permite múltiplos passes sobre os dados

**Implementação:**
```java
// Uso de BufferedReader + Apache Commons CSV para streaming
try (BufferedReader reader = Files.newBufferedReader(path);
     CSVParser parser = new CSVParser(reader, csvFormat)) {
    for (CSVRecord record : parser) {
        // Processa linha por linha sem carregar tudo em memória
    }
}
```

### 1.2 Tratamento de CNPJs Duplicados com Razões Sociais Diferentes

**Decisão:** Criar registro para cada variação e marcar como "inconsistente"

**Justificativa:**
- **Preservação de dados:** Mantém todas as informações originais
- **Transparência:** Auditoria completa das inconsistências encontradas
- **Análise posterior:** Permite identificar padrões de erros na fonte
- **Rastreabilidade:** Facilita investigação de problemas específicos
- **Trade-offs:**
  - Nenhum dado é perdido
  - Fácil identificar e corrigir problemas depois
  - Transparência total para análise
  - Aumenta volume de dados
  - Requer coluna adicional de flag

**Implementação:**
```java
// Adicionar coluna "status_consistencia" ao CSV
// Valores: "CONSISTENTE" | "CNPJ_DUPLICADO_RAZAO_DIVERGENTE"
```

### 1.3 Tratamento de Valores Zerados ou Negativos

**Decisão:** Marcar com flag de "suspeito" mas manter no dataset

**Justificativa:**
- **Dados reais:** Valores zero podem ser legítimos (sem despesas no período)
- **Valores negativos:** Podem representar ajustes, estornos ou erros
- **Análise estatística:** Importante manter para identificar outliers
- **Decisão posterior:** Analista pode filtrar depois se necessário
- **Trade-offs:**
  - Preserva informação original
  - Permite análises sobre qualidade dos dados
  - Não introduz viés ao remover dados
  - Requer validação adicional em análises
  - Pode impactar médias se não filtrado

**Implementação:**
```java
// Adicionar coluna "flag_valor_suspeito"
// Valores: "OK" | "VALOR_ZERO" | "VALOR_NEGATIVO"
if (valor == 0) {
    record.setFlagValorSuspeito("VALOR_ZERO");
} else if (valor < 0) {
    record.setFlagValorSuspeito("VALOR_NEGATIVO");
} else {
    record.setFlagValorSuspeito("OK");
}
```

### 1.4 Agregação de Despesas por Operadora

**Decisão:** Agregar todas as contas contábeis 411*/412* (Eventos/Sinistros) por REG_ANS + Trimestre

**Justificativa:**
- **Dados corretos:** Arquivos da ANS contêm linhas de contas contábeis, não operadoras
- **Consolidação:** Cada operadora tem múltiplas contas (41110101, 41110201, 41210101, etc.)
- **Análise real:** Soma total de despesas com eventos/sinistros por operadora
- **Join com cadastro:** REG_ANS é a chave para obter CNPJ e Razão Social
- **Trade-offs:**
  - Reduz de ~350K registros para ~2K (716 operadoras × 3 trimestres)
  - Perde detalhamento por conta contábil (pode ser adicionado depois se necessário)
  - Dados refletem realidade: despesas totais por operadora

**Implementação:**
```java
// 1. Parse CSV extrai REG_ANS, filtra contas 411*/412*
// 2. Agrupa por chave: REG_ANS + Trimestre + Ano
// 3. Soma valores de todas as contas
// 4. Join com cadastro ANS para obter CNPJ e Razão Social
Map<String, DespesasAgregadas> despesasPorOperadora = new HashMap<>();
```

### 1.5 Identificação Dinâmica de Trimestres Disponíveis

**Decisão:** Probing no servidor ANS para identificar últimos trimestres disponíveis

**Justificativa:**
- **Robustez:** Não depende de datas hardcoded que podem ficar desatualizadas
- **Realidade dos dados:** ANS pode atrasar publicação de trimestres
- **Automação:** Sistema funciona sem intervenção manual
- **Validação:** Verifica existência real do arquivo antes de processar
- **Trade-offs:**
  - Requer requisições HTTP HEAD extras (overhead mínimo)
  - Mais complexo que calcular data
  - Mais confiável e resiliente

**Implementação:**
```java
// Testa anos 2025-2024, trimestres 4-1
// Faz HEAD request para verificar se arquivo ZIP existe
// Ordena por mais recente e retorna top 3
public List<TrimestreANS> identificarUltimosTrimestreDisponiveis(int quantidade)
```

### 1.6 Detecção de Operadoras sem Movimentação

**Decisão:** Marcar operadoras com valor ZERO em TODOS os trimestres como OPERADORA_SEM_MOVIMENTACAO

**Justificativa:**
- **Validação de qualidade:** Operadora ativa não deveria ter zero em 3 trimestres seguidos
- **Possíveis causas:** Dados ausentes, operadora suspensa, erro de processamento
- **Diferencial de zero pontual:** Zero em 1 trimestre = OK (pode não ter sinistros), zero em todos = inconsistente
- **Análise:** Permite filtrar operadoras problemáticas em análises estatísticas
- **Trade-offs:**
  - Adiciona novo enum StatusConsistencia
  - 25 operadoras detectadas (~3.5% do total)
  - Melhora qualidade da análise

**Implementação:**
```java
// Após agrupar por CNPJ, verifica se TODOS os trimestres têm valor zero
boolean todosTrimestresZero = despesasCnpj.stream()
    .allMatch(d -> d.valorDespesas().compareTo(BigDecimal.ZERO) == 0);
```

### 1.7 Tratamento de Operadoras sem Cadastro

**Decisão:** Manter no dataset com placeholder "OPERADORA REG_ANS XXXXXX"

**Justificativa:**
- **Preservação total:** 100% dos dados da ANS mantidos
- **Rastreabilidade:** Possível identificar posteriormente
- **Análise de cobertura:** Mostra qualidade do join cadastral (98% de match)
- **Exemplo real:** REG_ANS 000477 tem R$ 655 milhões - dado relevante
- **Trade-offs:**
  - 17 operadoras sem cadastro (~2.4%)
  - CNPJ temporário = REG_ANS para manter estrutura
  - Fácil filtrar depois se necessário
  - Transparência total

---

## Parte 2: Transformação e Validação de Dados

### 2.1 Estratégia de Validação de CNPJs

**Decisão:** [A definir]

**Justificativa:** [A definir]

---

## Parte 3: Banco de Dados e Análise

### 3.1 Normalização do Banco

**Decisão:** [A definir]

**Justificativa:** [A definir]

---

## Parte 4: API e Interface Web

### 4.1 Framework Backend

**Decisão:** [A definir]

**Justificativa:** [A definir]

---

## Changelog de Decisões

| Data | Parte | Decisão | Razão da Mudança |
|------|-------|---------|------------------|
| 2026-01-28 | Geral | Java 25 + Gradle + PostgreSQL | Setup inicial |
| 2026-01-28 | Geral | Não usar Lombok | Incompatibilidade com Java 25 - usar records nativos |
| 2026-01-28 | Parte 1 | Streaming para processamento | Escalabilidade e uso de memória |
| 2026-01-28 | Parte 1 | Manter duplicatas marcadas | Preservação e transparência |
| 2026-01-28 | Parte 1 | Manter valores suspeitos | Análise completa dos dados || 2026-01-28 | Parte 1 | Agregação por REG_ANS | Correção: dados são contas contábeis, não operadoras |
| 2026-01-28 | Parte 1 | Join com cadastro ANS | Obter CNPJ e Razão Social reais |
| 2026-01-28 | Parte 1 | Identificação dinâmica de trimestres | Robustez: verifica disponibilidade no servidor |
| 2026-01-28 | Parte 1 | Detecção operadoras sem movimentação | Validação: zero em todos os trimestres = inconsistente |
| 2026-01-28 | Parte 1 | Manter operadoras sem cadastro | Preservação: 100% dos dados ANS |

---

## Resumo da Parte 1 (Concluída)

**Arquitetura:** Clean Architecture com separação domain/application/infrastructure

**Resultados:**
- ✅ **356.505 registros** processados (contas contábeis 411*/412*)
- ✅ **2.148 registros** consolidados (716 operadoras × 3 trimestres)
- ✅ **Q3/2025, Q2/2025, Q1/2025** identificados dinamicamente
- ✅ **98% match** com cadastro ANS (1.110 operadoras encontradas)
- ✅ **25 operadoras** sem movimentação detectadas
- ✅ **17 operadoras** sem cadastro preservadas

**Validações implementadas:**
- StatusConsistencia: CONSISTENTE | CNPJ_DUPLICADO_RAZAO_DIVERGENTE | OPERADORA_SEM_MOVIMENTACAO
- FlagValorSuspeito: OK | VALOR_ZERO | VALOR_NEGATIVO

**Arquivo gerado:** `consolidado_despesas.zip` (CSV com colunas: CNPJ, RazaoSocial, Trimestre, Ano, ValorDespesas, StatusConsistencia, FlagValorSuspeito)