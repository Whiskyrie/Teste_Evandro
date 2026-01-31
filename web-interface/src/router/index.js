/**
 * Vue Router - Configuração de rotas
 * Design: Spark Pixel Dashboard
 */

import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "dashboard",
      component: () => import("../views/DashboardView.vue"),
      meta: { title: "Dashboard" },
    },
    {
      path: "/operadoras",
      name: "operadoras",
      component: () => import("../views/HomeView.vue"),
      meta: { title: "Operadoras" },
    },
    {
      path: "/operadoras/:cnpj",
      name: "operadora-detail",
      component: () => import("../views/OperadoraDetailView.vue"),
      props: true,
      meta: { title: "Detalhes da Operadora" },
    },
  ],
});

// Atualizar título da página
router.beforeEach((to, from, next) => {
  document.title = to.meta.title
    ? `${to.meta.title} | ANS Dashboard`
    : "ANS Dashboard";
  next();
});

export default router;
