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
  height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  overflow: hidden;
}

.admin-layout__content {
  padding: 32px 24px 48px;
  min-height: 0;
  overflow-y: auto;
}

@media (max-width: 900px) {
  .admin-layout {
    height: auto;
    min-height: 100vh;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .admin-layout__content {
    overflow: visible;
  }
}
</style>
