<script setup>
/**
 * App Layout - Intuitive Care Dashboard
 * ANS Health Operators Analytics
 */
import { ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
const sidebarOpen = ref(false)

const menuItems = [
  { icon: 'dashboard', label: 'Dashboard', path: '/' },
  { icon: 'building', label: 'Operadoras', path: '/operadoras' }
]

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}
</script>

<template>
  <div id="app">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ 'sidebar--open': sidebarOpen }">
      <nav class="sidebar__nav">
        <div class="sidebar__section">
          <RouterLink
            v-for="item in menuItems"
            :key="item.path"
            :to="item.disabled ? '#' : item.path"
            class="sidebar__item"
            :class="{ 
              'sidebar__item--active': route.path === item.path,
              'sidebar__item--disabled': item.disabled
            }"
            @click="sidebarOpen = false"
          >
            <!-- Dashboard Icon -->
            <svg v-if="item.icon === 'dashboard'" class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="9" rx="1" />
              <rect x="14" y="3" width="7" height="5" rx="1" />
              <rect x="14" y="12" width="7" height="9" rx="1" />
              <rect x="3" y="16" width="7" height="5" rx="1" />
            </svg>
            <!-- Building Icon -->
            <svg v-else-if="item.icon === 'building'" class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="4" y="2" width="16" height="20" rx="2"/>
              <path d="M9 22v-4h6v4"/>
              <path d="M8 6h.01"/>
              <path d="M16 6h.01"/>
              <path d="M12 6h.01"/>
              <path d="M12 10h.01"/>
              <path d="M12 14h.01"/>
              <path d="M16 10h.01"/>
              <path d="M16 14h.01"/>
              <path d="M8 10h.01"/>
              <path d="M8 14h.01"/>
            </svg>
            <!-- Chart Icon -->
            <svg v-else-if="item.icon === 'chart'" class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 3v18h18"/>
              <path d="M18 17V9"/>
              <path d="M13 17V5"/>
              <path d="M8 17v-3"/>
            </svg>
            <!-- File Icon -->
            <svg v-else-if="item.icon === 'file'" class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <line x1="10" y1="9" x2="8" y2="9"/>
            </svg>
            <!-- Map Icon -->
            <svg v-else-if="item.icon === 'map'" class="sidebar__item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
              <circle cx="12" cy="10" r="3"/>
            </svg>
            <span>{{ item.label }}</span>
          </RouterLink>
        </div>
      </nav>
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

.sidebar__item--disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
