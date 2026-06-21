<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getDefaultRouteForRole, useAuthStore } from '@/stores/auth'

const navItems = [
  { label: 'Home', to: '/' },
  { label: 'Services', to: '/services' },
  { label: 'Pets', to: '/pets' },
  { label: 'My Bookings', to: '/bookings' },
  { label: 'Profile', to: '/profile' },
]

const authStore = useAuthStore()
const router = useRouter()

const accountLink = computed(() =>
  authStore.isAdmin
    ? { label: 'Admin', to: '/admin' }
    : { label: 'My Account', to: getDefaultRouteForRole(authStore.role) },
)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <router-link to="/">PawCare Hub</router-link>
    </div>

    <nav class="app-header__nav">
      <router-link
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="app-header__link"
      >
        {{ item.label }}
      </router-link>
    </nav>

    <div class="app-header__actions">
      <template v-if="authStore.isLoggedIn">
        <router-link :to="accountLink.to">{{ accountLink.label }}</router-link>
        <button type="button" class="app-header__button app-header__button--logout" @click="handleLogout">
          Logout
        </button>
      </template>
      <template v-else>
        <router-link to="/login">Login</router-link>
        <router-link to="/register" class="app-header__button">Register</router-link>
      </template>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  background: rgba(250, 249, 245, 0.88);
  border-bottom: 1px solid var(--pc-border);
  backdrop-filter: blur(16px);
}

.app-header__brand {
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.24rem;
  font-weight: 500;
}

.app-header__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.app-header__link,
.app-header__actions a,
.app-header__actions button {
  color: var(--pc-muted);
  font-size: 0.94rem;
  font-weight: 650;
  transition: color 150ms ease;
}

.app-header__link:hover,
.app-header__actions a:hover,
.app-header__actions button:hover {
  color: var(--pc-primary);
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-header__button {
  padding: 10px 16px;
  border-radius: 12px;
  background: var(--pc-primary);
  border: 0;
  color: white !important;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(201, 100, 66, 0.16);
}

.app-header__button--logout {
  font: inherit;
}

@media (max-width: 900px) {
  .app-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
