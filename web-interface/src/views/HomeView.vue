<script setup>
/**
 * Operadoras View - Spark Pixel Design
 * Lista de operadoras com filtros e paginacao
 */
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOperadorasStore } from '../stores/operadoras'

const router = useRouter()
const store = useOperadorasStore()

const localSearch = ref('')
const localUF = ref('')

const ufs = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA',
  'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN',
  'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
]

const hasActiveFilters = computed(() => store.searchQuery || store.selectedUF)

const paginationPages = computed(() => {
  const current = store.pagination.page
  const total = store.pagination.total_pages
  const pages = []
  
  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 3) {
      pages.push(1, 2, 3, 4, '...', total)
    } else if (current >= total - 2) {
      pages.push(1, '...', total - 3, total - 2, total - 1, total)
    } else {
      pages.push(1, '...', current - 1, current, current + 1, '...', total)
    }
  }
  
  return pages
})

onMounted(() => {
  store.fetchOperadoras()
})

function applyFilters() {
  store.setSearchQuery(localSearch.value)
  store.setSelectedUF(localUF.value)
  store.fetchOperadoras(1)
}

function clearFilters() {
  localSearch.value = ''
  localUF.value = ''
  store.clearFilters()
  store.fetchOperadoras(1)
}

function goToPage(page) {
  if (typeof page === 'number' && page >= 1 && page <= store.pagination.total_pages) {
    store.fetchOperadoras(page, store.pagination.limit)
  }
}

function viewDetails(cnpj) {
  router.push({ name: 'operadora-detail', params: { cnpj } })
}

function formatCNPJ(cnpj) {
  if (!cnpj) return ''
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
}
</script>

<template>
  <div class="operadoras-view">
    <!-- Filters Bar -->
    <div class="filters-bar">
      <div class="filters-bar__search">
        <div class="search-input">
          <svg class="search-input__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.35-4.35"/>
          </svg>
          <input
            v-model="localSearch"
            type="text"
            class="input"
            placeholder="Buscar por razao social ou CNPJ..."
            @keyup.enter="applyFilters"
          />
        </div>
      </div>

      <div class="filters-bar__select">
        <select v-model="localUF" class="input">
          <option value="">Todos os Estados</option>
          <option v-for="uf in ufs" :key="uf" :value="uf">{{ uf }}</option>
        </select>
      </div>

      <button class="btn btn--primary" @click="applyFilters">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <path d="m21 21-4.35-4.35"/>
        </svg>
        Buscar
      </button>

      <button v-if="hasActiveFilters" class="btn btn--ghost" @click="clearFilters">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 6 6 18"/>
          <path d="m6 6 12 12"/>
        </svg>
        Limpar
      </button>
    </div>

    <!-- Active Filters Badge -->
    <div v-if="hasActiveFilters" class="active-filters">
      <span class="badge" v-if="store.searchQuery">
        Busca: "{{ store.searchQuery }}"
        <button class="badge-close" @click="localSearch = ''; applyFilters()">x</button>
      </span>
      <span class="badge" v-if="store.selectedUF">
        UF: {{ store.selectedUF }}
        <button class="badge-close" @click="localUF = ''; applyFilters()">x</button>
      </span>
    </div>

    <!-- Loading State -->
    <div v-if="store.loading" class="loading-container">
      <div class="spinner"></div>
      <p class="text-secondary mt-md">Carregando operadoras...</p>
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
        <button class="btn btn--primary mt-md" @click="store.fetchOperadoras()">
          Tentar novamente
        </button>
      </div>
    </div>

    <!-- Data Table -->
    <div v-else-if="store.hasOperadoras" class="data-table-card">
      <div class="data-table-card__header">
        <h3 class="data-table-card__title">Lista de Operadoras</h3>
        <span class="badge">{{ store.pagination.total }} registros</span>
      </div>

      <div class="data-table-card__body">
        <table class="table">
          <thead>
            <tr>
              <th>CNPJ</th>
              <th>Razao Social</th>
              <th>Registro ANS</th>
              <th>Modalidade</th>
              <th>UF</th>
              <th>Status</th>
              <th style="width: 100px;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="op in store.operadoras" :key="op.cnpj" @click="viewDetails(op.cnpj)" style="cursor: pointer;">
              <td>
                <span class="value-display">{{ formatCNPJ(op.cnpj) }}</span>
              </td>
              <td>
                <span class="font-medium">{{ op.razao_social }}</span>
              </td>
              <td>{{ op.registro_ans || '-' }}</td>
              <td>
                <span class="text-secondary text-sm">{{ op.modalidade || '-' }}</span>
              </td>
              <td>
                <span class="font-semibold">{{ op.uf || '-' }}</span>
              </td>
              <td>
                <span 
                  class="status"
                  :class="{
                    'status--valid': op.status_validacao === 'VALIDO',
                    'status--invalid': op.status_validacao === 'INVALIDO',
                    'status--pending': op.status_validacao === 'NAO_APLICAVEL'
                  }"
                >
                  <span class="status__dot"></span>
                  {{ op.status_validacao || 'N/A' }}
                </span>
              </td>
              <td>
                <button class="btn btn--secondary btn--sm" @click.stop="viewDetails(op.cnpj)">
                  Ver
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="data-table-card__footer">
        <span class="text-secondary text-sm">
          Mostrando {{ store.operadoras.length }} de {{ store.pagination.total }}
        </span>

        <div class="pagination">
          <button 
            class="pagination__btn"
            :disabled="!store.pagination.has_previous"
            @click="goToPage(store.pagination.page - 1)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m15 18-6-6 6-6"/>
            </svg>
          </button>

          <button
            v-for="(page, i) in paginationPages"
            :key="i"
            class="pagination__btn"
            :class="{ 'pagination__btn--active': page === store.pagination.page }"
            :disabled="page === '...'"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>

          <button 
            class="pagination__btn"
            :disabled="!store.pagination.has_next"
            @click="goToPage(store.pagination.page + 1)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m9 18 6-6-6-6"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state card">
      <svg class="empty-state__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
      </svg>
      <h3 class="empty-state__title">Nenhuma operadora encontrada</h3>
      <p class="empty-state__description">
        Tente ajustar os filtros ou limpar a busca para ver mais resultados.
      </p>
      <button class="btn btn--primary mt-md" @click="clearFilters">
        Limpar filtros
      </button>
    </div>
  </div>
</template>

<style scoped>
.operadoras-view {
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

.active-filters {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
}

.badge-close {
  background: transparent;
  border: none;
  margin-left: var(--space-xs);
  cursor: pointer;
  font-size: var(--text-xs);
  opacity: 0.7;
}

.badge-close:hover {
  opacity: 1;
}

.table tbody tr:hover {
  background: var(--color-bg-hover);
}

@media (max-width: 768px) {
  .filters-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filters-bar__search,
  .filters-bar__select {
    min-width: 100%;
  }
}
</style>
