/**
 * API Service - Cliente HTTP para comunicação com backend FastAPI
 *
 * Trade-off: Axios vs Fetch nativo
 * - Escolha: Axios
 * - Justificativa:
 *   - Interceptors para tratamento global de erros
 *   - Timeout automático
 *   - Transformação automática de JSON
 *   - Cancelamento de requests
 *   - Melhor suporte a navegadores antigos (não relevante aqui)
 */

import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8000";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000, // 10 segundos
  headers: {
    "Content-Type": "application/json",
  },
});

// Interceptor para tratamento de erros
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Servidor retornou erro (4xx, 5xx)
      console.error("API Error:", error.response.status, error.response.data);
    } else if (error.request) {
      // Request foi feito mas sem resposta
      console.error("Network Error:", error.message);
    } else {
      // Erro na configuração do request
      console.error("Request Error:", error.message);
    }
    return Promise.reject(error);
  },
);

export default {
  // GET /api/operadoras - Lista paginada com filtros
  async getOperadoras(params = {}) {
    const response = await apiClient.get("/api/operadoras", { params });
    return response.data;
  },

  // GET /api/operadoras/{cnpj} - Detalhes da operadora
  async getOperadoraDetails(cnpj) {
    const response = await apiClient.get(`/api/operadoras/${cnpj}`);
    return response.data;
  },

  // GET /api/operadoras/{cnpj}/despesas - Histórico de despesas
  async getOperadoraDespesas(cnpj) {
    const response = await apiClient.get(`/api/operadoras/${cnpj}/despesas`);
    return response.data;
  },

  // GET /api/estatisticas - Estatísticas agregadas
  async getEstatisticas() {
    const response = await apiClient.get("/api/estatisticas");
    return response.data;
  },
};
