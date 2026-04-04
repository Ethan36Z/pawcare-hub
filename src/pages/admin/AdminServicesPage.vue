<script setup>
import { onMounted, ref } from 'vue'
import { adminServicesApi } from '@/api/adminServices'

const services = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const togglingServiceId = ref(null)
const isCreateDialogOpen = ref(false)
const isCreating = ref(false)
const createErrorMessage = ref('')
const createForm = ref({
  name: '',
  category: '',
  description: '',
  duration: '',
  price: '',
  active: true,
})

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

function openCreateDialog() {
  createErrorMessage.value = ''
  isCreateDialogOpen.value = true
}

function resetCreateForm() {
  createForm.value = {
    name: '',
    category: '',
    description: '',
    duration: '',
    price: '',
    active: true,
  }
}

async function handleCreateService() {
  isCreating.value = true
  createErrorMessage.value = ''

  try {
    await adminServicesApi.create(createForm.value)
    isCreateDialogOpen.value = false
    resetCreateForm()
    await loadServices()
  } catch (error) {
    createErrorMessage.value = getApiErrorMessage(error, 'Unable to create this service right now.')
  } finally {
    isCreating.value = false
  }
}

async function handleToggleService(service) {
  if (!service?.id) {
    return
  }

  togglingServiceId.value = service.id
  errorMessage.value = ''

  try {
    await adminServicesApi.toggle(service.id)
    await loadServices()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to update this service right now.')
  } finally {
    togglingServiceId.value = null
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
      <el-button type="primary" @click="openCreateDialog">Add Service</el-button>
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
      <el-table-column label="Status" min-width="140">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" effect="plain">
            {{ row.active ? 'Active' : 'Inactive' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="Description" min-width="320" show-overflow-tooltip />
      <el-table-column label="Actions" min-width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            plain
            size="small"
            :loading="togglingServiceId === row.id"
            @click="handleToggleService(row)"
          >
            {{ row.active ? 'Disable' : 'Enable' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="isCreateDialogOpen"
      title="Add Service"
      width="min(560px, 92vw)"
      @closed="resetCreateForm"
    >
      <el-alert
        v-if="createErrorMessage"
        :title="createErrorMessage"
        type="error"
        :closable="false"
        class="admin-page__alert"
      />

      <el-form :model="createForm" label-position="top">
        <el-form-item label="Name">
          <el-input v-model="createForm.name" placeholder="Service name" />
        </el-form-item>
        <el-form-item label="Category">
          <el-input v-model="createForm.category" placeholder="Wellness, Dental, Vaccines..." />
        </el-form-item>
        <el-form-item label="Duration">
          <el-input v-model="createForm.duration" placeholder="e.g. 30 min" />
        </el-form-item>
        <el-form-item label="Price">
          <el-input v-model="createForm.price" placeholder="e.g. $85 or From $35" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="4"
            placeholder="Short service description"
          />
        </el-form-item>
        <el-form-item label="Available for users">
          <el-switch v-model="createForm.active" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isCreateDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isCreating" @click="handleCreateService">
          Save Service
        </el-button>
      </template>
    </el-dialog>
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
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

.admin-page :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.admin-page :deep(.el-switch.is-checked .el-switch__core) {
  border-color: #3f725d;
  background-color: #3f725d;
}

@media (max-width: 640px) {
  .admin-page__header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
