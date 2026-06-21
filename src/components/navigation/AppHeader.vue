<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDefaultRouteForRole, useAuthStore } from '@/stores/auth'

const navItems = [
  { label: 'Home', to: '/' },
  { label: 'Services', to: '/services' },
  { label: 'Pets', to: '/pets' },
  { label: 'My Bookings', to: '/bookings' },
  { label: 'Profile', to: '/profile' },
]

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const isMenuOpen = ref(false)
const headerRef = ref(null)

const accountLink = computed(() =>
  authStore.isAdmin
    ? { label: 'Admin', to: '/admin' }
    : { label: 'My Account', to: getDefaultRouteForRole(authStore.role) },
)

function closeMenu() {
  isMenuOpen.value = false
}

function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value
}

function isExactActive(path) {
  return route.path === path
}

function handleLogout() {
  closeMenu()
  authStore.logout()
  router.push('/login')
}

function handleDocumentClick(event) {
  if (!isMenuOpen.value || !headerRef.value?.contains(event.target)) {
    closeMenu()
  }
}

function handleEscape(event) {
  if (event.key === 'Escape') {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleEscape)
})
</script>

<template>
  <header ref="headerRef" class="app-header">
    <div class="app-header__brand">
      <router-link to="/" @click="closeMenu">PawCare Hub</router-link>
    </div>

    <nav class="app-header__nav">
      <router-link
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="app-header__link"
        @click="closeMenu"
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

    <button
      type="button"
      class="app-header__menu-button"
      aria-label="Toggle navigation menu"
      :aria-expanded="isMenuOpen"
      aria-controls="mobile-navigation"
      @click.stop="toggleMenu"
    >
      <span aria-hidden="true"></span>
      <span aria-hidden="true"></span>
      <span aria-hidden="true"></span>
    </button>

    <nav
      id="mobile-navigation"
      class="app-header__mobile-menu"
      :class="{ 'is-open': isMenuOpen }"
      aria-label="Mobile navigation"
    >
      <router-link
        v-for="item in navItems"
        :key="`mobile-${item.to}`"
        :to="item.to"
        class="app-header__mobile-link"
        :class="{ 'is-active': isExactActive(item.to) }"
        @click="closeMenu"
      >
        {{ item.label }}
      </router-link>

      <template v-if="authStore.isLoggedIn">
        <button
          type="button"
          class="app-header__mobile-link app-header__mobile-action app-header__mobile-action--logout"
          @click="handleLogout"
        >
          Logout
        </button>
      </template>
      <template v-else>
        <router-link to="/login" class="app-header__mobile-link" @click="closeMenu">Login</router-link>
        <router-link to="/register" class="app-header__mobile-link app-header__mobile-link--primary" @click="closeMenu">
          Register
        </router-link>
      </template>
    </nav>
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

.app-header__brand,
.app-header__actions {
  position: relative;
  z-index: 2;
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

.app-header__menu-button,
.app-header__mobile-menu {
  display: none;
}

.app-header__menu-button {
  position: relative;
  z-index: 2;
  width: 44px;
  height: 40px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 5px;
  padding: 0;
  border: 1px solid var(--pc-border);
  border-radius: 14px;
  background: rgba(250, 249, 245, 0.92);
  color: var(--pc-ink);
  cursor: pointer;
  box-shadow: 0 12px 26px rgba(20, 20, 19, 0.08);
}

.app-header__menu-button span {
  width: 18px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
}

.app-header__mobile-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 16px;
  z-index: 30;
  display: grid;
  gap: 5px;
  width: min(280px, calc(100vw - 48px));
  padding: 12px;
  border: 1px solid rgba(232, 230, 220, 0.92);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 252, 244, 0.98), rgba(246, 239, 225, 0.98));
  box-shadow:
    0 22px 46px rgba(20, 20, 19, 0.13),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  opacity: 0;
  pointer-events: none;
  transform: translateY(-6px) scale(0.98);
  transform-origin: top right;
  visibility: hidden;
  transition:
    opacity 180ms ease,
    transform 180ms ease,
    visibility 0s linear 180ms;
}

.app-header__mobile-menu.is-open {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0) scale(1);
  visibility: visible;
  transition:
    opacity 180ms ease,
    transform 180ms ease,
    visibility 0s;
}

.app-header__mobile-link {
  display: flex;
  align-items: center;
  min-height: 44px;
  width: 100%;
  padding: 10px 14px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: var(--pc-text);
  font: inherit;
  font-size: 0.96rem;
  font-weight: 650;
  text-align: left;
  cursor: pointer;
  transition:
    background 150ms ease,
    color 150ms ease;
}

.app-header__mobile-link:hover,
.app-header__mobile-link:focus-visible,
.app-header__mobile-link.is-active {
  background: var(--pc-primary-soft);
  color: var(--pc-primary);
  outline: none;
}

.app-header__mobile-link.is-active {
  box-shadow: inset 0 0 0 1px rgba(201, 100, 66, 0.13);
}

.app-header__mobile-link--primary {
  margin-top: 4px;
  background: var(--pc-primary);
  color: white;
}

.app-header__mobile-link--primary:hover,
.app-header__mobile-link--primary:focus-visible {
  background: var(--pc-primary-hover);
  color: white;
}

.app-header__mobile-action {
  font-family: inherit;
}

.app-header__mobile-action--logout {
  justify-content: center;
  margin-top: 6px;
  background: var(--pc-primary);
  color: white;
  box-shadow: 0 12px 22px rgba(201, 100, 66, 0.16);
}

.app-header__mobile-action--logout:hover,
.app-header__mobile-action--logout:focus-visible {
  background: var(--pc-primary-hover);
  color: white;
}

@media (max-width: 1024px) {
  .app-header {
    padding: 14px 16px;
  }

  .app-header__nav,
  .app-header__actions {
    display: none;
  }

  .app-header__menu-button {
    display: flex;
  }

  .app-header__mobile-menu {
    display: grid;
  }
}

@media (max-width: 420px) {
  .app-header__brand {
    font-size: 1.12rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-header__mobile-menu {
    transition: none;
    transform: none;
  }
}
</style>
