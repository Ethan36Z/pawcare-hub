<script setup>
const adminLinks = [
  { label: 'Dashboard', to: '/admin' },
  { label: 'Services', to: '/admin/services' },
  { label: 'Bookings', to: '/admin/bookings' },
  { label: 'Users', to: '/admin/users' },
]
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
  </aside>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 28px 20px;
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
  margin: 8px 0 0;
  font-size: 1.6rem;
}

.app-sidebar__nav {
  display: grid;
  gap: 10px;
}

.app-sidebar__link {
  padding: 12px 14px;
  border-radius: 14px;
  color: rgba(255, 255, 255, 0.92);
  background: rgba(255, 255, 255, 0.06);
}

@media (max-width: 900px) {
  .app-sidebar {
    padding-bottom: 12px;
  }
}
</style>
