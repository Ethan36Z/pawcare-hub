<script setup>
import AppSidebar from '@/components/navigation/AppSidebar.vue'
</script>

<template>
  <div class="admin-layout">
    <AppSidebar />
    <main class="admin-layout__content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
}

.admin-layout__content {
  padding: 32px 24px 48px;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
