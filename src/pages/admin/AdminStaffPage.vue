<script setup>
import { onMounted, ref } from 'vue'
import { adminStaffApi } from '@/api/adminStaff'

const staffRecords = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const togglingStaffId = ref(null)

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

async function loadStaff() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminStaffApi.list()
    staffRecords.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load staff records right now.')
    staffRecords.value = []
  } finally {
    isLoading.value = false
  }
}

async function handleToggleStaff(staff) {
  if (!staff?.id) {
    return
  }

  togglingStaffId.value = staff.id
  errorMessage.value = ''

  try {
    await adminStaffApi.toggle(staff.id)
    await loadStaff()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to update this staff member right now.')
  } finally {
    togglingStaffId.value = null
  }
}

onMounted(() => {
  loadStaff()
})
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__header">
      <div>
        <h1>Admin Staff</h1>
        <p>Clinic staff records are now stored as real backend data for upcoming booking improvements.</p>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="admin-page__alert"
    />

    <el-skeleton v-if="isLoading" :rows="5" animated />

    <el-empty
      v-else-if="!staffRecords.length"
      description="No staff records are available yet."
    />

    <el-table v-else :data="staffRecords" stripe>
      <el-table-column prop="id" label="ID" min-width="80" />
      <el-table-column prop="name" label="Name" min-width="220" />
      <el-table-column prop="role" label="Role" min-width="180" />
      <el-table-column label="Status" min-width="140">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" effect="plain">
            {{ row.active ? 'Active' : 'Inactive' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" min-width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            plain
            size="small"
            :loading="togglingStaffId === row.id"
            @click="handleToggleStaff(row)"
          >
            {{ row.active ? 'Disable' : 'Enable' }}
          </el-button>
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
</style>
