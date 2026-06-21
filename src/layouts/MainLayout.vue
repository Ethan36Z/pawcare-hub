<script setup>
import AppHeader from '@/components/navigation/AppHeader.vue'
</script>

<template>
  <div class="page-shell">
    <AppHeader />
    <main class="main-layout__content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.main-layout__content {
  padding: 32px 20px 56px;
}
</style>
