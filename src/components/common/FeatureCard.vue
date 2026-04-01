<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <article class="feature-card">
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
  </article>
</template>

<style scoped>
.feature-card {
  padding: 22px;
  border-radius: 22px;
  background: var(--pc-surface);
  border: 1px solid var(--pc-border);
  box-shadow: var(--pc-shadow);
}

.feature-card h3 {
  margin: 0 0 8px;
}

.feature-card p {
  margin: 0;
  color: var(--pc-muted);
  line-height: 1.6;
}
</style>
