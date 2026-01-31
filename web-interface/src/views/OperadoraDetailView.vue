<script setup>
/**
 * Operadora Detail View - Intuitive Care Design
 * Detalhes de uma operadora especifica
 * Adapted to real API response structure
 */
import { onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOperadorasStore } from '../stores/operadoras'

const route = useRoute()
const router = useRouter()
const store = useOperadorasStore()

const props = defineProps({
  cnpj: {
    type: String,
    required: true
  }
})

onMounted(() => {
  store.fetchOperadoraDetails(props.cnpj)
  store.fetchDespesas(props.cnpj)
})

function goBack() {
  router.push({ name: 'operadoras' })
}

function formatCNPJ(cnpj) {
  if (!cnpj) return ''
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
}

function formatCurrency(value) {
  if (!value) return 'R$ 0,00'
  const numValue = Number(value)
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(numValue)
}

function formatCompactBR(value) {
  if (!value) return 'R$ 0'
  const numValue = Number(value)
  if (numValue >= 1000000000) {
    return `R$ ${(numValue / 1000000000).toFixed(1)} bi`
  } else if (numValue >= 1000000) {
    return `R$ ${(numValue / 1000000).toFixed(1)} mi`
  } else if (numValue >= 1000) {
    return `R$ ${(numValue / 1000).toFixed(1)} mil`
  }
  return formatCurrency(numValue)
}

const despesasPorAno = computed(() => {
  if (!store.despesas || store.despesas.length === 0) return {}
  
  const grouped = {}
  store.despesas.forEach(despesa => {
    if (!grouped[despesa.ano]) {
      grouped[despesa.ano] = []
    }
    grouped[despesa.ano].push(despesa)
  })
  
  // Sort by trimestre descending within each year
  Object.keys(grouped).forEach(ano => {
    grouped[ano].sort((a, b) => b.trimestre - a.trimestre)
  })
  
  return grouped
})

const anos = computed(() => {
  return Object.keys(despesasPorAno.value).sort((a, b) => b - a)
})

// Total using the correct field: valor_despesas
const totalDespesas = computed(() => {
  if (!store.despesas) return 0
  return store.despesas.reduce((sum, d) => sum + Number(d.valor_despesas || 0), 0)
})
</script>

<template>
  <div class="detail-view">
    <!-- Back Button -->
    <button class="back-btn" @click="goBack">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="m15 18-6-6 6-6"/>
      </svg>
      Voltar para lista
    </button>

    <!-- Loading State -->
    <div v-if="store.loading" class="loading-container">
      <div class="spinner"></div>
      <p class="text-secondary">Carregando detalhes...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="store.error" class="card error-card">
      <div class="error-content">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <p class="mt-4">{{ store.error }}</p>
        <button class="btn btn--primary mt-4" @click="goBack">
          Voltar para lista
        </button>
      </div>
    </div>

    <!-- Detail Content -->
    <div v-else-if="store.currentOperadora">
      <!-- Detail Header Card -->
      <div class="detail-header-card">
        <div class="detail-header__info">
          <h1 class="detail-header__title">{{ store.currentOperadora.razao_social }}</h1>
          <p class="detail-header__cnpj">{{ formatCNPJ(store.currentOperadora.cnpj) }}</p>
          
          <div class="detail-header__meta">
            <div class="detail-header__meta-item">
              <span class="detail-header__meta-label">Registro ANS</span>
              <span class="detail-header__meta-value">{{ store.currentOperadora.registro_ans || '-' }}</span>
            </div>
            <div class="detail-header__meta-item">
              <span class="detail-header__meta-label">Modalidade</span>
              <span class="detail-header__meta-value">{{ store.currentOperadora.modalidade || '-' }}</span>
            </div>
            <div class="detail-header__meta-item">
              <span class="detail-header__meta-label">UF</span>
              <span class="detail-header__meta-value">{{ store.currentOperadora.uf || '-' }}</span>
            </div>
            <div class="detail-header__meta-item">
              <span class="detail-header__meta-label">Status CNPJ</span>
              <span 
                class="status"
                :class="{
                  'status--valid': store.currentOperadora.status_validacao === 'VALIDO',
                  'status--invalid': store.currentOperadora.status_validacao === 'INVALIDO',
                  'status--pending': store.currentOperadora.status_validacao === 'NAO_APLICAVEL'
                }"
              >
                <span class="status__dot"></span>
                {{ store.currentOperadora.status_validacao || 'N/A' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Stats Cards -->
      <div class="detail-stats-grid">
        <div class="card stat-card stat-card--accent">
          <span class="stat-card__label">Total de Despesas</span>
          <span class="stat-card__value">{{ formatCompactBR(totalDespesas) }}</span>
          <span class="stat-card__period">Todos os períodos</span>
        </div>

        <div class="card stat-card">
          <span class="stat-card__label">Trimestres Registrados</span>
          <span class="stat-card__value">{{ store.despesas?.length || 0 }}</span>
          <span class="stat-card__period">Períodos com dados</span>
        </div>

        <div class="card stat-card">
          <span class="stat-card__label">Anos com Dados</span>
          <span class="stat-card__value">{{ anos.length }}</span>
          <span class="stat-card__period">{{ anos.length > 0 ? `${anos[anos.length - 1]} - ${anos[0]}` : '-' }}</span>
        </div>
      </div>

      <!-- Despesas History -->
      <div class="section">
        <div class="section__header">
          <h2 class="section__title">Histórico de Despesas</h2>
          <span class="badge">Por Trimestre</span>
        </div>

        <div v-if="store.despesas && store.despesas.length > 0">
          <div v-for="ano in anos" :key="ano" class="year-section">
            <div class="year-header">
              <span class="year-badge">{{ ano }}</span>
            </div>

            <div class="data-table-card">
              <div class="data-table-card__body">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Trimestre</th>
                      <th style="text-align: right;">Valor Total de Despesas</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="despesa in despesasPorAno[ano]" :key="`${despesa.ano}-${despesa.trimestre}`">
                      <td>
                        <span class="font-medium">{{ despesa.trimestre }}º Trimestre</span>
                      </td>
                      <td style="text-align: right;">
                        <span class="font-semibold text-accent">{{ formatCurrency(despesa.valor_despesas) }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else class="empty-state card">
          <svg class="empty-state__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <h3 class="empty-state__title">Sem registros de despesas</h3>
          <p class="empty-state__description">
            Esta operadora não possui dados de despesas registrados no sistema.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-view {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--text-body-sm);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-bottom: var(--space-4);
}

.back-btn:hover {
  background: rgba(220, 38, 38, 0.05);
  color: var(--color-accent);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16);
}

.error-card {
  max-width: 400px;
  margin: var(--space-16) auto;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  color: var(--color-error);
}

/* Detail Header Card */
.detail-header-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  padding: var(--space-6);
  margin-bottom: var(--space-6);
}

.detail-header__title {
  font-size: var(--text-heading-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-1) 0;
}

.detail-header__cnpj {
  font-size: var(--text-body-md);
  color: var(--color-text-tertiary);
  font-family: var(--font-mono);
  margin: 0 0 var(--space-4) 0;
}

.detail-header__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-6);
}

.detail-header__meta-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.detail-header__meta-label {
  font-size: 11px;
  font-weight: var(--font-medium);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-header__meta-value {
  font-size: var(--text-body-md);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

/* Stats Grid for Detail Page */
.detail-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--card-gap);
  margin-bottom: var(--space-6);
}

@media (max-width: 768px) {
  .detail-stats-grid {
    grid-template-columns: 1fr;
  }
}

/* Section */
.section {
  margin-bottom: var(--space-6);
}

.section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.section__title {
  font-size: var(--text-heading-md);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

/* Year Section */
.year-section {
  margin-bottom: var(--space-6);
}

.year-section:last-child {
  margin-bottom: 0;
}

.year-header {
  margin-bottom: var(--space-3);
}

.year-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-1) var(--space-3);
  background: linear-gradient(135deg, var(--color-accent), #B91C1C);
  color: #FFFFFF;
  border-radius: var(--radius-md);
  font-size: var(--text-body-sm);
  font-weight: var(--font-semibold);
}

/* Text accent color */
.text-accent {
  color: var(--color-accent);
}
</style>
