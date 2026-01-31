<script setup>
/**
 * Operadora Detail View - Spark Pixel Design
 * Detalhes de uma operadora especifica
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
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value)
}

function formatCompact(value) {
  if (!value) return '0'
  return new Intl.NumberFormat('pt-BR', {
    notation: 'compact',
    compactDisplay: 'short'
  }).format(value)
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
  
  return grouped
})

const anos = computed(() => {
  return Object.keys(despesasPorAno.value).sort((a, b) => b - a)
})

const totalDespesas = computed(() => {
  if (!store.despesas) return 0
  return store.despesas.reduce((sum, d) => sum + (d.total_despesas || 0), 0)
})
</script>

<template>
  <div class="detail-view">
    <!-- Back Button -->
    <button class="btn btn--ghost mb-lg" @click="goBack">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="m15 18-6-6 6-6"/>
      </svg>
      Voltar para lista
    </button>

    <!-- Loading State -->
    <div v-if="store.loading" class="loading-container">
      <div class="spinner"></div>
      <p class="text-secondary mt-md">Carregando detalhes...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="store.error" class="card error-card">
      <div class="error-content">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <p class="mt-md">{{ store.error }}</p>
        <button class="btn btn--primary mt-md" @click="goBack">
          Voltar para lista
        </button>
      </div>
    </div>

    <!-- Detail Content -->
    <div v-else-if="store.currentOperadora">
      <!-- Detail Header -->
      <div class="detail-header">
        <div class="detail-header__info">
          <h1 class="detail-header__title">{{ store.currentOperadora.razao_social }}</h1>
          <p class="detail-header__subtitle">
            <span class="value-display">{{ formatCNPJ(store.currentOperadora.cnpj) }}</span>
          </p>
          
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
      <div class="stats-grid">
        <div class="card stat-card">
          <span class="stat-card__label">Total de Despesas</span>
          <span class="stat-card__value">{{ formatCompact(totalDespesas) }}</span>
          <div class="stat-card__trend">
            {{ formatCurrency(totalDespesas) }}
          </div>
        </div>

        <div class="card stat-card">
          <span class="stat-card__label">Trimestres Registrados</span>
          <span class="stat-card__value">{{ store.despesas?.length || 0 }}</span>
          <div class="stat-card__trend">
            Periodos com dados
          </div>
        </div>

        <div class="card stat-card">
          <span class="stat-card__label">Anos com Dados</span>
          <span class="stat-card__value">{{ anos.length }}</span>
          <div class="stat-card__trend">
            {{ anos.length > 0 ? `${anos[anos.length - 1]} - ${anos[0]}` : '-' }}
          </div>
        </div>
      </div>

      <!-- Despesas History -->
      <div class="section">
        <div class="section__header">
          <h2 class="section__title">Historico de Despesas</h2>
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
                      <th style="text-align: right;">Despesas Assistenciais</th>
                      <th style="text-align: right;">Despesas Administrativas</th>
                      <th style="text-align: right;">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="despesa in despesasPorAno[ano]" :key="`${despesa.ano}-${despesa.trimestre}`">
                      <td>
                        <span class="font-medium">{{ despesa.trimestre }}o Trimestre</span>
                      </td>
                      <td style="text-align: right;">
                        {{ formatCurrency(despesa.despesas_assistenciais) }}
                      </td>
                      <td style="text-align: right;">
                        {{ formatCurrency(despesa.despesas_administrativas) }}
                      </td>
                      <td style="text-align: right;">
                        <span class="font-semibold">{{ formatCurrency(despesa.total_despesas) }}</span>
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
            Esta operadora nao possui dados de despesas registrados no sistema.
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

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-3xl);
}

.error-card {
  max-width: 400px;
  margin: var(--space-3xl) auto;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  color: var(--color-text-error);
}

.year-section {
  margin-bottom: var(--space-xl);
}

.year-section:last-child {
  margin-bottom: 0;
}

.year-header {
  margin-bottom: var(--space-md);
}

.year-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-xs) var(--space-md);
  background: var(--color-primary-main);
  color: #FFFFFF;
  border-radius: var(--radius-md);
  font-size: var(--text-md);
  font-weight: var(--font-semibold);
}
</style>
