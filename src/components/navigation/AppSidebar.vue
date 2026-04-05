<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const adminLinks = [
  { label: 'Dashboard', to: '/admin' },
  { label: 'Services', to: '/admin/services' },
  { label: 'Bookings', to: '/admin/bookings' },
  { label: 'Staff', to: '/admin/staff' },
  { label: 'Users', to: '/admin/users' },
]

const authStore = useAuthStore()
const router = useRouter()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <aside class="app-sidebar">
    <div>
      <p class="app-sidebar__eyebrow">Admin</p>
      <h1 class="app-sidebar__title">PawCare Hub</h1>
    </div>

    <nav class="app-sidebar__nav">
      <router-link
        v-for="item in adminLinks"
        :key="item.to"
        :to="item.to"
        class="app-sidebar__link"
      >
        {{ item.label }}
      </router-link>
    </nav>

    <button type="button" class="app-sidebar__logout" @click="handleLogout">Logout</button>
  </aside>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 22px 18px 18px;
  background: #16324f;
  color: white;
}

.app-sidebar__eyebrow {
  margin: 0;
  color: rgba(255, 255, 255, 0.7);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.8rem;
}

.app-sidebar__title {
  margin: 6px 0 0;
  font-size: 1.45rem;
  line-height: 1.15;
}

.app-sidebar__nav {
  display: grid;
  gap: 8px;
  flex: 1;
  align-content: start;
}

.app-sidebar__link {
  padding: 10px 12px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.92);
  background: rgba(255, 255, 255, 0.06);
  line-height: 1.2;
}

.app-sidebar__logout {
  margin-top: auto;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.96);
  font: inherit;
  text-align: left;
  line-height: 1.2;
  cursor: pointer;
}

@media (max-width: 900px) {
  .app-sidebar {
    gap: 16px;
    padding: 18px 16px 12px;
  }
}
</style>
