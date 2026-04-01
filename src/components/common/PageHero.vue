<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <div class="page-hero">
    <p class="page-hero__eyebrow">PawCare Hub</p>
    <h2>{{ title }}</h2>
    <p>{{ subtitle }}</p>
    <slot />
  </div>
</template>

<style scoped>
.page-hero {
  padding: 28px;
  border: 1px solid var(--pc-border);
  border-radius: 28px;
  background: linear-gradient(135deg, white 0%, #eff6ff 100%);
  box-shadow: var(--pc-shadow);
}

.page-hero__eyebrow {
  margin: 0 0 10px;
  color: var(--pc-primary);
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-hero h2 {
  margin: 0 0 10px;
  font-size: clamp(2rem, 4vw, 3rem);
}

.page-hero p {
  margin: 0;
  max-width: 700px;
  color: var(--pc-muted);
  line-height: 1.6;
}
</style>
