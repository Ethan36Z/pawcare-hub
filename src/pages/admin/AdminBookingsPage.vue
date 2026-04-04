<script setup>
import { onMounted, ref } from 'vue'
import { adminBookingsApi } from '@/api/adminBookings'

const bookings = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getStatusTagType(status) {
  if (status === 'Confirmed') {
    return 'success'
  }

  if (status === 'Upcoming') {
    return 'primary'
  }

  if (status === 'Cancelled') {
    return 'danger'
  }

  return 'info'
}

async function loadBookings() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminBookingsApi.list()
    bookings.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load admin bookings right now.')
    bookings.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadBookings()
})
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__header">
      <div>
        <h1>Admin Bookings</h1>
        <p>Live booking records from the MySQL-backed bookings table.</p>
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
      v-else-if="!bookings.length"
      description="No bookings found yet."
    />

    <el-table v-else :data="bookings" stripe>
      <el-table-column prop="id" label="ID" min-width="80" />
      <el-table-column prop="petName" label="Pet" min-width="140" />
      <el-table-column prop="service" label="Service" min-width="180" />
      <el-table-column label="Schedule" min-width="190">
        <template #default="{ row }">
          {{ row.date }} at {{ row.time }}
        </template>
      </el-table-column>
      <el-table-column label="Status" min-width="130">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" effect="plain">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="staff" label="Staff" min-width="160" />
      <el-table-column prop="clinic" label="Clinic" min-width="180" />
      <el-table-column label="Owner" min-width="240">
        <template #default="{ row }">
          <div class="owner-cell">
            <strong>{{ row.ownerName }}</strong>
            <span>{{ row.ownerEmail }}</span>
          </div>
        </template>
      </el-table-column>
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

.owner-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.owner-cell strong {
  color: var(--pc-text);
}

.owner-cell span {
  color: var(--pc-muted);
  word-break: break-word;
}
</style>
