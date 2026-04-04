<script setup>
import { computed, onMounted, ref } from 'vue'
import FeatureCard from '@/components/common/FeatureCard.vue'
import { adminDashboardApi } from '@/api/adminDashboard'

const stats = ref({
  totalUsers: 0,
  totalPets: 0,
  totalBookings: 0,
  totalServices: 0,
  confirmedBookings: 0,
  cancelledBookings: 0,
})
const isLoading = ref(false)
const errorMessage = ref('')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

const metrics = computed(() => [
  {
    title: 'Clinic overview',
    description: `${stats.value.totalUsers} users, ${stats.value.totalPets} pets, and ${stats.value.totalServices} services are currently stored in the database.`,
  },
  {
    title: 'Booking queue',
    description: `${stats.value.totalBookings} total bookings, with ${stats.value.confirmedBookings} confirmed and ${stats.value.cancelledBookings} cancelled appointments.`,
  },
  {
    title: 'Team workflow',
    description: `The admin workspace is now pulling real summary totals from the live database instead of placeholder dashboard copy.`,
  },
])

async function loadStats() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminDashboardApi.stats()
    stats.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load dashboard stats right now.')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<template>
  <div>
    <div class="admin-hero">
      <p>Admin workspace</p>
      <h1>Dashboard</h1>
      <span>Live operational totals from the MySQL-backed PawCare Hub database.</span>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="admin-alert"
    />

    <section v-if="isLoading" class="admin-grid">
      <article
        v-for="index in 3"
        :key="index"
        class="admin-skeleton-card"
      >
        <el-skeleton :rows="3" animated />
      </article>
    </section>

    <section v-else class="admin-grid">
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

.admin-alert {
  margin-top: 18px;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  margin-top: 24px;
}

.admin-skeleton-card {
  padding: 22px;
  border-radius: 22px;
  background: var(--pc-surface);
  border: 1px solid var(--pc-border);
  box-shadow: var(--pc-shadow);
}
</style>
