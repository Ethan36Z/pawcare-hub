<script setup>
const navItems = [
  { label: 'Home', to: '/' },
  { label: 'Services', to: '/services' },
  { label: 'Pets', to: '/pets' },
  { label: 'My Bookings', to: '/bookings' },
  { label: 'Profile', to: '/profile' },
]
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
      <router-link to="/login">Login</router-link>
      <router-link to="/register" class="app-header__button">Register</router-link>
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
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--pc-border);
  backdrop-filter: blur(16px);
}

.app-header__brand {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--pc-primary);
}

.app-header__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.app-header__link,
.app-header__actions a {
  color: var(--pc-muted);
  font-weight: 500;
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-header__button {
  padding: 10px 16px;
  border-radius: 999px;
  background: var(--pc-primary);
  color: white !important;
}

@media (max-width: 900px) {
  .app-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
