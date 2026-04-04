<script setup>
import { onMounted, ref } from 'vue'
import { adminServicesApi } from '@/api/adminServices'

const services = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

async function loadServices() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminServicesApi.list()
    services.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load services right now.')
    services.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadServices()
})
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__header">
      <div>
        <h1>Admin Services</h1>
        <p>Current clinic services from the MySQL-backed services catalog.</p>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="admin-page__alert"
    />

    <el-skeleton v-if="isLoading" :rows="6" animated />

    <el-empty
      v-else-if="!services.length"
      description="No services found yet."
    />

    <el-table v-else :data="services" stripe>
      <el-table-column prop="id" label="ID" min-width="80" />
      <el-table-column prop="name" label="Service" min-width="220" />
      <el-table-column prop="category" label="Category" min-width="140" />
      <el-table-column prop="duration" label="Duration" min-width="120" />
      <el-table-column prop="price" label="Price" min-width="120" />
      <el-table-column prop="description" label="Description" min-width="360" show-overflow-tooltip />
    </el-table>
  </section>
</template>

<style scoped>
.admin-page {
  padding: 28px;
  border-radius: 24px;
  background: white;
  border: 1px solid var(--pc-border);
}

.admin-page__header {
  margin-bottom: 20px;
}

.admin-page__alert {
  margin-bottom: 16px;
}

.admin-page h1 {
  margin-top: 0;
  margin-bottom: 8px;
}

.admin-page p {
  margin-bottom: 0;
  color: var(--pc-muted);
}
</style>
