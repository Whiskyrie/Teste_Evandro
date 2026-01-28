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
| 2026-01-28 | Parte 1 | Manter valores suspeitos | Análise completa dos dados |
