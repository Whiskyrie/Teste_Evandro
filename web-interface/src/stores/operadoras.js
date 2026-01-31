/**
 * Pinia Store - Gerenciamento de estado das operadoras
 * Design: Spark Pixel Dashboard
 */

import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "../api/client";

export const useOperadorasStore = defineStore("operadoras", () => {
  // State
  const operadoras = ref([]);
  const currentOperadora = ref(null);
  const despesas = ref([]);
  const estatisticas = ref(null);
  const pagination = ref({
    total: 0,
    page: 1,
    limit: 20,
    total_pages: 0,
    has_next: false,
    has_previous: false,
  });
  const loading = ref(false);
  const error = ref(null);

  // Filters
  const searchQuery = ref("");
  const selectedUF = ref("");

  // Computed
  const hasOperadoras = computed(() => operadoras.value.length > 0);

  // Actions
  async function fetchOperadoras(page = 1, limit = 20) {
    loading.value = true;
    error.value = null;

    try {
      const params = { page, limit };
      if (searchQuery.value) params.search = searchQuery.value;
      if (selectedUF.value) params.uf = selectedUF.value;

      const response = await api.get("/operadoras", { params });
      const data = response.data;

      operadoras.value = data.items || data.data || [];
      pagination.value = {
        total: data.total,
        page: data.page,
        limit: data.limit,
        total_pages: data.total_pages,
        has_next: data.has_next,
        has_previous: data.has_previous,
      };
    } catch (err) {
      error.value =
        err.response?.data?.detail ||
        err.message ||
        "Erro ao carregar operadoras";
      operadoras.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function fetchOperadoraDetails(cnpj) {
    loading.value = true;
    error.value = null;

    try {
      const response = await api.get(`/operadoras/${cnpj}`);
      currentOperadora.value = response.data;
    } catch (err) {
      error.value = err.response?.data?.detail || "Operadora nao encontrada";
      currentOperadora.value = null;
    } finally {
      loading.value = false;
    }
  }

  async function fetchDespesas(cnpj) {
    loading.value = true;
    error.value = null;

    try {
      const response = await api.get(`/operadoras/${cnpj}/despesas`);
      despesas.value = response.data;
    } catch (err) {
      error.value =
        err.response?.data?.detail ||
        err.message ||
        "Erro ao carregar despesas";
      despesas.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function fetchEstatisticas() {
    loading.value = true;
    error.value = null;

    try {
      const response = await api.get("/estatisticas");
      estatisticas.value = response.data;
    } catch (err) {
      error.value =
        err.response?.data?.detail ||
        err.message ||
        "Erro ao carregar estatisticas";
      estatisticas.value = null;
    } finally {
      loading.value = false;
    }
  }

  function setSearchQuery(query) {
    searchQuery.value = query;
  }

  function setSelectedUF(uf) {
    selectedUF.value = uf;
  }

  function clearFilters() {
    searchQuery.value = "";
    selectedUF.value = "";
  }

  function clearError() {
    error.value = null;
  }

  return {
    // State
    operadoras,
    currentOperadora,
    despesas,
    estatisticas,
    pagination,
    loading,
    error,
    searchQuery,
    selectedUF,

    // Computed
    hasOperadoras,

    // Actions
    fetchOperadoras,
    fetchOperadoraDetails,
    fetchDespesas,
    fetchEstatisticas,
    setSearchQuery,
    setSelectedUF,
    clearFilters,
    clearError,
  };
});
