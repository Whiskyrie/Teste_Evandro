<script setup>
/**
 * Dashboard View - Intuitive Care Design
 * Statistics overview for ANS health operators
 * Using real API data only
 */
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useOperadorasStore } from '../stores/operadoras'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const store = useOperadorasStore()
const chartCanvas = ref(null)
let chartInstance = null

// Computed KPIs - REAL DATA ONLY from API
const kpis = computed(() => {
  if (!store.estatisticas) return []
  
  return [
    {
      label: 'Total de Operadoras',
      value: formatNumber(store.estatisticas.total_operadoras),
      period: 'Operadoras ativas',
      icon: 'building'
    },
    {
      label: 'Despesas Totais',
      value: formatCompactBR(store.estatisticas.total_despesas),
      period: 'Todos os períodos',
      icon: 'dollar'
    },
    {
      label: 'Média por Operadora',
      value: formatCompactBR(store.estatisticas.media_despesas_por_operadora),
      period: 'Média acumulada',
      icon: 'calculator'
    }
  ]
})

// Top operadoras list - REAL DATA ONLY from API
const topOperadoras = computed(() => {
  if (!store.estatisticas?.top_5_operadoras) return []
  return store.estatisticas.top_5_operadoras
})

// Regional distribution - REAL DATA from API
const regionalDistribution = computed(() => {
  if (!store.estatisticas?.despesas_por_uf) return []
  
  const total = store.estatisticas.despesas_por_uf.reduce((sum, item) => sum + Number(item.total_despesas), 0)
  
  return store.estatisticas.despesas_por_uf.slice(0, 5).map((item, index) => {
    const percentage = ((Number(item.total_despesas) / total) * 100).toFixed(0)
    const colors = ['#DC2626', '#F87171', '#FCA5A5', '#FECACA', '#FEE2E2']
    return {
      name: item.uf,
      value: `${percentage}%`,
      color: colors[index] || '#FEE2E2'
    }
  })
})

onMounted(async () => {
  await store.fetchEstatisticas()
  if (store.estatisticas) {
    createChart()
  }
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.destroy()
  }
})

watch(() => store.estatisticas, (newVal) => {
  if (newVal) {
    createChart()
  }
})

function createChart() {
  if (!chartCanvas.value || !store.estatisticas?.despesas_por_uf) return

  if (chartInstance) {
    chartInstance.destroy()
  }

  const ctx = chartCanvas.value.getContext('2d')
  const data = store.estatisticas.despesas_por_uf.slice(0, 8)

  chartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: data.map(item => item.uf),
      datasets: [{
        label: 'Total de Despesas',
        data: data.map(item => item.total_despesas),
        backgroundColor: '#DC2626',
        borderColor: '#DC2626',
        borderWidth: 0,
        borderRadius: 4,
        barThickness: 32,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          backgroundColor: '#1A1A1A',
          titleColor: '#FFFFFF',
          bodyColor: '#FFFFFF',
          borderColor: 'transparent',
          borderWidth: 0,
          padding: 12,
          cornerRadius: 8,
          displayColors: false,
          titleFont: {
            family: "'Inter', sans-serif",
            size: 14,
            weight: 600
          },
          bodyFont: {
            family: "'Inter', sans-serif",
            size: 14
          },
          callbacks: {
            title: (items) => items[0].label,
            label: (item) => formatCurrency(item.raw)
          }
        }
      },
      scales: {
        x: {
          grid: {
            display: false
          },
          border: {
            display: false
          },
          ticks: {
            color: '#9CA3AF',
            font: {
              family: "'Inter', sans-serif",
              size: 12
            }
          }
        },
        y: {
          grid: {
            color: '#F4F4F5',
            drawBorder: false
          },
          border: {
            display: false
          },
          ticks: {
            color: '#9CA3AF',
            font: {
              family: "'Inter', sans-serif",
              size: 11
            },
            callback: (value) => formatCompact(value)
          }
        }
      }
    }
  })
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

function formatCompactBR(value) {
  if (!value) return 'R$ 0'
  const numValue = Number(value)
  if (numValue >= 1000000000000) {
    return `R$ ${(numValue / 1000000000000).toFixed(2)} tri`
  } else if (numValue >= 1000000000) {
    return `R$ ${(numValue / 1000000000).toFixed(1)} bi`
  } else if (numValue >= 1000000) {
    return `R$ ${(numValue / 1000000).toFixed(1)} mi`
  } else if (numValue >= 1000) {
    return `R$ ${(numValue / 1000).toFixed(1)} mil`
  }
  return formatCurrency(numValue)
}

function formatNumber(value) {
  if (!value) return '0'
  return new Intl.NumberFormat('pt-BR').format(value)
}
</script>

<template>
  <div class="dashboard">
    <!-- Loading State -->
    <div v-if="store.loading" class="loading-container">
      <div class="spinner"></div>
      <p class="text-secondary mt-4">Carregando estatísticas...</p>
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
        <button class="btn btn--primary mt-4" @click="store.fetchEstatisticas()">
          Tentar novamente
        </button>
      </div>
    </div>

    <!-- Dashboard Content -->
    <template v-else>

      <!-- KPI Cards Grid - 3 cards with real data -->
      <div class="stats-grid stats-grid--3">
        <div 
          v-for="(kpi, index) in kpis" 
          :key="index"
          class="card stat-card"
          :class="{ 'stat-card--accent': index === 0 }"
        >
          <span class="stat-card__label">{{ kpi.label }}</span>
          <span class="stat-card__value">{{ kpi.value }}</span>
          <span class="stat-card__period">{{ kpi.period }}</span>
        </div>
      </div>

      <!-- Charts Section -->
      <div class="charts-grid">
        <!-- Main Chart Card -->
        <div class="chart-card">
          <div class="chart-card__header">
            <div>
              <h3 class="chart-card__title">Despesas por UF</h3>
              <p class="chart-card__subtitle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
                Distribuição de despesas por estado (Top 8)
              </p>
            </div>
          </div>
          <div class="chart-card__content">
            <div class="chart-card__chart chart-card__chart--full">
              <canvas ref="chartCanvas"></canvas>
            </div>
          </div>
        </div>

        <!-- Regional Distribution Card - with real API data -->
        <div class="card" style="padding: 0;">
          <div class="list-card__header" style="padding: var(--space-5);">
            <h3 class="list-card__title">Distribuição por UF</h3>
          </div>
          <div style="padding: var(--space-5); padding-top: 0;">
            <!-- Region summary with real data -->
            <div v-if="regionalDistribution.length > 0" class="region-stats">
              <div class="region-stat" v-for="region in regionalDistribution" :key="region.name">
                <div class="region-stat__bar" :style="{ background: region.color, width: region.value }"></div>
                <div class="region-stat__info">
                  <span class="region-stat__name">{{ region.name }}</span>
                  <span class="region-stat__value">{{ region.value }}</span>
                </div>
              </div>
            </div>
            <div v-else class="empty-state-small">
              <p>Carregando dados...</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom Cards Grid - Only Top 5 Maiores Despesas with real data -->
      <div class="bottom-grid bottom-grid--single">
        <!-- Maiores Despesas - REAL DATA -->
        <div class="list-card">
          <div class="list-card__header">
            <h3 class="list-card__title">Top 5 Maiores Despesas</h3>
          </div>
          <div v-if="topOperadoras.length > 0">
            <div 
              v-for="(op, index) in topOperadoras" 
              :key="op.cnpj || index"
              class="list-card__item"
            >
              <div class="list-card__item-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="4" y="2" width="16" height="20" rx="2"/>
                  <path d="M9 22v-4h6v4"/>
                  <path d="M8 6h.01"/>
                  <path d="M16 6h.01"/>
                  <path d="M12 6h.01"/>
                  <path d="M12 10h.01"/>
                  <path d="M12 14h.01"/>
                </svg>
              </div>
              <div class="list-card__item-content">
                <span class="list-card__item-title">{{ op.razao_social }}</span>
                <span class="list-card__item-subtitle">{{ op.uf }}</span>
              </div>
              <span class="list-card__item-value">{{ formatCompactBR(op.total_despesas) }}</span>
            </div>
          </div>
          <div v-else class="empty-state-small">
            <p>Nenhum dado disponível</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard {
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

/* Stats Grid - 3 columns */
.stats-grid--3 {
  grid-template-columns: repeat(3, 1fr) !important;
}

@media (max-width: 768px) {
  .stats-grid--3 {
    grid-template-columns: 1fr !important;
  }
}

/* Chart full width */
.chart-card__chart--full {
  width: 100%;
  flex: 1;
}

/* Bottom grid single card */
.bottom-grid--single {
  grid-template-columns: 1fr !important;
}

/* Region Stats */
.region-stats {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.region-stat {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.region-stat__bar {
  height: 8px;
  border-radius: var(--radius-full);
  transition: width 0.5s ease;
}

.region-stat__info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.region-stat__name {
  font-size: var(--text-body-sm);
  color: var(--color-text-secondary);
}

.region-stat__value {
  font-size: var(--text-body-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

/* Empty state small */
.empty-state-small {
  padding: var(--space-4);
  text-align: center;
  color: var(--color-text-tertiary);
}

@media (max-width: 768px) {
  .chart-card__content {
    flex-direction: column;
  }
}
</style>
