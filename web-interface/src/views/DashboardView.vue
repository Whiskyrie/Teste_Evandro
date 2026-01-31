<script setup>
/**
 * Dashboard View - Spark Pixel Design
 * Estatisticas gerais das operadoras ANS
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useOperadorasStore } from '../stores/operadoras'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

const store = useOperadorasStore()
const chartCanvas = ref(null)
let chartInstance = null

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
  const data = store.estatisticas.despesas_por_uf

  chartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: data.map(item => item.uf),
      datasets: [{
        label: 'Total de Despesas',
        data: data.map(item => item.total_despesas),
        backgroundColor: '#1A1A1A',
        borderColor: '#1A1A1A',
        borderWidth: 0,
        borderRadius: 4,
        barThickness: 24,
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
          backgroundColor: '#FFFFFF',
          titleColor: '#1A1A1A',
          bodyColor: '#6B6B6B',
          borderColor: '#E8E6E1',
          borderWidth: 1,
          padding: 12,
          displayColors: false,
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
          ticks: {
            color: '#6B6B6B',
            font: {
              family: "'Inter', sans-serif",
              size: 12
            }
          }
        },
        y: {
          grid: {
            color: '#E8E6E1',
            drawBorder: false
          },
          ticks: {
            color: '#6B6B6B',
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
      <p class="text-secondary mt-md">Carregando estatisticas...</p>
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
        <button class="btn btn--primary mt-md" @click="store.fetchEstatisticas()">
          Tentar novamente
        </button>
      </div>
    </div>

    <!-- Dashboard Content -->
    <template v-else-if="store.estatisticas">
      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="card stat-card">
          <span class="stat-card__label">Total de Operadoras</span>
          <span class="stat-card__value">{{ formatNumber(store.estatisticas.total_operadoras) }}</span>
          <div class="stat-card__trend stat-card__trend--positive">
            Cadastradas na ANS
          </div>
        </div>

        <div class="card stat-card">
          <span class="stat-card__label">Total de Despesas</span>
          <span class="stat-card__value">{{ formatCompact(store.estatisticas.total_despesas) }}</span>
          <div class="stat-card__trend">
            {{ formatCurrency(store.estatisticas.total_despesas) }}
          </div>
        </div>

        <div class="card stat-card">
          <span class="stat-card__label">Media por Operadora</span>
          <span class="stat-card__value">{{ formatCompact(store.estatisticas.media_despesas) }}</span>
          <div class="stat-card__trend">
            {{ formatCurrency(store.estatisticas.media_despesas) }}
          </div>
        </div>

        <div class="card stat-card">
          <span class="stat-card__label">Total de Trimestres</span>
          <span class="stat-card__value">{{ store.estatisticas.total_trimestres }}</span>
          <div class="stat-card__trend">
            Periodos analisados
          </div>
        </div>
      </div>

      <!-- Top 5 Operadoras -->
      <div class="section">
        <div class="section__header">
          <h2 class="section__title">Top 5 Operadoras</h2>
          <span class="badge">Maiores Despesas</span>
        </div>

        <div class="data-table-card">
          <div class="data-table-card__body">
            <table class="table">
              <thead>
                <tr>
                  <th style="width: 60px;">Rank</th>
                  <th>Razao Social</th>
                  <th>CNPJ</th>
                  <th style="text-align: right;">Total de Despesas</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(op, index) in store.estatisticas.top_5_operadoras" :key="op.cnpj">
                  <td>
                    <span class="rank-badge" :class="`rank-badge--${index + 1}`">
                      {{ index + 1 }}
                    </span>
                  </td>
                  <td>
                    <span class="font-medium">{{ op.razao_social }}</span>
                  </td>
                  <td>
                    <span class="value-display">{{ op.cnpj }}</span>
                  </td>
                  <td style="text-align: right;">
                    <span class="font-semibold">{{ formatCurrency(op.total_despesas) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Chart Section -->
      <div class="section">
        <div class="section__header">
          <h2 class="section__title">Despesas por UF</h2>
          <span class="badge">Top 10 Estados</span>
        </div>

        <div class="card">
          <div class="chart-container">
            <canvas ref="chartCanvas"></canvas>
          </div>
        </div>
      </div>

      <!-- UF Table -->
      <div class="section">
        <div class="section__header">
          <h2 class="section__title">Detalhamento por Estado</h2>
        </div>

        <div class="data-table-card">
          <div class="data-table-card__body">
            <table class="table">
              <thead>
                <tr>
                  <th>UF</th>
                  <th style="text-align: right;">Operadoras</th>
                  <th style="text-align: right;">Total Despesas</th>
                  <th style="text-align: right;">Media</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="uf in store.estatisticas.despesas_por_uf" :key="uf.uf">
                  <td>
                    <span class="font-semibold">{{ uf.uf }}</span>
                  </td>
                  <td style="text-align: right;">
                    {{ formatNumber(uf.qtd_operadoras) }}
                  </td>
                  <td style="text-align: right;">
                    <span class="font-medium">{{ formatCurrency(uf.total_despesas) }}</span>
                  </td>
                  <td style="text-align: right;">
                    <span class="text-secondary">{{ formatCurrency(uf.media_por_operadora) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
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

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
}

.rank-badge--1 {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: #000;
}

.rank-badge--2 {
  background: linear-gradient(135deg, #C0C0C0, #A0A0A0);
  color: #000;
}

.rank-badge--3 {
  background: linear-gradient(135deg, #CD7F32, #A0522D);
  color: #FFF;
}

.rank-badge--4,
.rank-badge--5 {
  background: var(--color-bg-main);
  color: var(--color-text-secondary);
}

.chart-container {
  height: 350px;
  padding: var(--space-md);
}

@media (max-width: 768px) {
  .chart-container {
    height: 280px;
  }
}
</style>
