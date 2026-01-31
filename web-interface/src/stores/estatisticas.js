/**
 * Pinia Store - Gerenciamento de estatísticas
 */

import { defineStore } from "pinia";
import { ref } from "vue";
import api from "../services/api";

export const useEstatisticasStore = defineStore("estatisticas", () => {
  // State
  const stats = ref(null);
  const loading = ref(false);
  const error = ref(null);

  // Actions
  async function fetchEstatisticas() {
    loading.value = true;
    error.value = null;

    try {
      stats.value = await api.getEstatisticas();
    } catch (err) {
      error.value = err.message || "Erro ao carregar estatísticas";
      stats.value = null;
    } finally {
      loading.value = false;
    }
  }

  return {
    stats,
    loading,
    error,
    fetchEstatisticas,
  };
});
