<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const adminLinks = computed(() => {
  if (authStore.isAdmin) {
    return [
      { label: 'Dashboard', to: '/admin' },
      { label: 'Services', to: '/admin/services' },
      { label: 'Bookings', to: '/admin/bookings' },
      { label: 'Staff', to: '/admin/staff' },
      { label: 'Clinic Operations', to: '/admin/operations' },
      { label: 'Users', to: '/admin/users' },
    ]
  }

  if (authStore.isFrontDesk) {
    return [
      { label: 'Dashboard', to: '/admin' },
      { label: 'Bookings', to: '/admin/bookings' },
      { label: 'Clinic Operations', to: '/admin/operations' },
    ]
  }

  if (authStore.isDoctor) {
    return [
      { label: 'Dashboard', to: '/admin' },
      { label: 'Bookings', to: '/admin/bookings' },
    ]
  }

  return []
})

const sidebarEyebrow = computed(() => (authStore.isAdmin ? 'Admin' : 'Clinic team'))

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <aside class="app-sidebar">
    <div>
      <p class="app-sidebar__eyebrow">{{ sidebarEyebrow }}</p>
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
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 22px 18px 18px;
  background: #16324f;
  color: white;
  overflow: hidden;
  box-sizing: border-box;
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
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
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
    position: static;
    height: auto;
    overflow: visible;
    gap: 16px;
    padding: 18px 16px 12px;
  }

  .app-sidebar__nav {
    overflow: visible;
    padding-right: 0;
  }
}
</style>
