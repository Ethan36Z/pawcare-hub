<template>
  <div class="auth-layout">
    <div class="auth-layout__panel">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-layout__panel {
  width: min(100%, 480px);
}
</style>
