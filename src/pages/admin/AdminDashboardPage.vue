<script setup>
import FeatureCard from '@/components/common/FeatureCard.vue'

const metrics = [
  {
    title: 'Clinic overview',
    description: 'Reserved for operational summaries like daily appointments, active services, and customer activity.',
  },
  {
    title: 'Booking queue',
    description: 'A future dashboard panel for reviewing pending appointments and clinic capacity.',
  },
  {
    title: 'Team workflow',
    description: 'A placeholder space for staff assignments, status tracking, and admin alerts.',
  },
]
</script>

<template>
  <div>
    <div class="admin-hero">
      <p>Admin workspace</p>
      <h1>Dashboard</h1>
      <span>Placeholder admin landing page for clinic operations.</span>
    </div>

    <section class="admin-grid">
      <FeatureCard
        v-for="metric in metrics"
        :key="metric.title"
        :title="metric.title"
        :description="metric.description"
      />
    </section>
  </div>
</template>

<style scoped>
.admin-hero {
  padding: 28px;
  border-radius: 28px;
  background: linear-gradient(135deg, #1f4d7c 0%, #0e7490 100%);
  color: white;
}

.admin-hero p,
.admin-hero span {
  margin: 0;
  opacity: 0.82;
}

.admin-hero h1 {
  margin: 8px 0 10px;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-top: 24px;
}
</style>
