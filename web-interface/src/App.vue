<script setup>
import { ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
const sidebarOpen = ref(false)

const menuItems = [
  { icon: 'dashboard', label: 'Dashboard', path: '/' },
  { icon: 'list', label: 'Operadoras', path: '/operadoras' }
]

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}
</script>

<template>
  <div id="app">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'sidebar--open': sidebarOpen }">
      <div class="sidebar__header">
        <div class="sidebar__logo">
          <div class="sidebar__logo-icon">IC</div>
          <span>Intuitive Care</span>
        </div>
      </div>

      <nav class="sidebar__nav">
        <div class="sidebar__section">
          <div class="sidebar__section-title">Menu Principal</div>
          <RouterLink
            v-for="item in menuItems"
            :key="item.path"
            :to="item.path"
            class="sidebar__item"
            :class="{ 'sidebar__item--active': route.path === item.path }"
            @click="sidebarOpen = false"
          >
            <svg class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <template v-if="item.icon === 'dashboard'">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
              </template>
              <template v-else-if="item.icon === 'list'">
                <line x1="8" y1="6" x2="21" y2="6" />
                <line x1="8" y1="12" x2="21" y2="12" />
                <line x1="8" y1="18" x2="21" y2="18" />
                <line x1="3" y1="6" x2="3.01" y2="6" />
                <line x1="3" y1="12" x2="3.01" y2="12" />
                <line x1="3" y1="18" x2="3.01" y2="18" />
              </template>
            </svg>
            {{ item.label }}
          </RouterLink>
        </div>

        <div class="sidebar__section">
          <div class="sidebar__section-title">Dados</div>
          <div class="sidebar__item" style="cursor: default; opacity: 0.7;">
            <svg class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
            Fonte: ANS
          </div>
        </div>
      </nav>

      <div class="sidebar__footer">
        <div class="sidebar__item" style="cursor: default;">
          <svg class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span class="text-sm text-secondary">v1.0.0</span>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="main">
      <!-- Header -->
      <header class="header">
        <button class="btn btn--ghost mobile-menu-btn" @click="toggleSidebar">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <h1 class="header__title">{{ route.meta.title || 'Dashboard' }}</h1>
        <div class="header__actions">
          <span class="badge">ANS Data</span>
        </div>
      </header>

      <!-- Content -->
      <div class="content">
        <RouterView v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </div>
    </div>

    <!-- Mobile Overlay -->
    <div 
      v-if="sidebarOpen" 
      class="sidebar-overlay" 
      @click="sidebarOpen = false"
    />
  </div>
</template>

<style scoped>
.mobile-menu-btn {
  display: none;
}

.sidebar-overlay {
  display: none;
}

@media (max-width: 768px) {
  .mobile-menu-btn {
    display: flex;
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: calc(var(--z-sticky) - 1);
  }
}
</style>
