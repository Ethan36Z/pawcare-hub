<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminServicesApi } from '@/api/adminServices'

const services = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const togglingServiceId = ref(null)
const isCreateDialogOpen = ref(false)
const isCreating = ref(false)
const createErrorMessage = ref('')
const isEditDialogOpen = ref(false)
const isEditing = ref(false)
const editErrorMessage = ref('')
const editingServiceId = ref(null)
const filters = ref({
  active: '',
  category: '',
  name: '',
  sort: 'name-asc',
})
const currentPage = ref(1)
const pageSize = ref(10)
const createForm = ref({
  name: '',
  category: '',
  description: '',
  duration: '',
  price: '',
  active: true,
})
const editForm = ref({
  name: '',
  category: '',
  description: '',
  duration: '',
  price: '',
  active: true,
})
const paginatedServices = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  return services.value.slice(startIndex, startIndex + pageSize.value)
})

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

async function loadServices() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminServicesApi.list({
      active: filters.value.active === '' ? undefined : filters.value.active,
      category: filters.value.category || undefined,
      name: filters.value.name || undefined,
      sort: filters.value.sort,
    })
    services.value = data
    currentPage.value = 1
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

function openEditDialog(service) {
  editErrorMessage.value = ''
  editingServiceId.value = service.id
  editForm.value = {
    name: service.name,
    category: service.category,
    description: service.description,
    duration: service.duration,
    price: service.price,
    active: service.active,
  }
  isEditDialogOpen.value = true
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

function resetEditForm() {
  editingServiceId.value = null
  editErrorMessage.value = ''
  editForm.value = {
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

async function handleEditService() {
  if (!editingServiceId.value) {
    return
  }

  isEditing.value = true
  editErrorMessage.value = ''

  try {
    await adminServicesApi.update(editingServiceId.value, editForm.value)
    isEditDialogOpen.value = false
    resetEditForm()
    await loadServices()
  } catch (error) {
    editErrorMessage.value = getApiErrorMessage(error, 'Unable to update this service right now.')
  } finally {
    isEditing.value = false
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

function handleApplyFilters() {
  loadServices()
}

function handleResetFilters() {
  filters.value = {
    active: '',
    category: '',
    name: '',
    sort: 'name-asc',
  }
  loadServices()
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

    <div class="admin-filters">
      <el-select
        v-model="filters.active"
        placeholder="Status"
        clearable
        class="admin-filters__control"
      >
        <el-option label="Active" :value="true" />
        <el-option label="Inactive" :value="false" />
      </el-select>

      <el-input
        v-model="filters.category"
        placeholder="Filter by category"
        clearable
        class="admin-filters__control"
      />

      <el-input
        v-model="filters.name"
        placeholder="Search by name"
        clearable
        class="admin-filters__control"
      />

      <el-select
        v-model="filters.sort"
        class="admin-filters__control"
      >
        <el-option label="Name A-Z" value="name-asc" />
        <el-option label="Name Z-A" value="name-desc" />
        <el-option label="Newest first" value="newest" />
        <el-option label="Oldest first" value="oldest" />
      </el-select>

      <div class="admin-filters__actions">
        <el-button plain @click="handleApplyFilters">
          Apply
        </el-button>
        <el-button @click="handleResetFilters">
          Reset
        </el-button>
      </div>
    </div>

    <div class="admin-table-section">
      <el-skeleton v-if="isLoading" :rows="6" animated />

      <el-empty
        v-else-if="!services.length"
        description="No services match the current filters."
      />

      <template v-else>
        <el-table :data="paginatedServices" stripe>
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
          <el-table-column label="Actions" min-width="220" fixed="right">
            <template #default="{ row }">
              <div class="actions-cell">
                <el-button
                  plain
                  size="small"
                  @click="openEditDialog(row)"
                >
                  Edit
                </el-button>
                <el-button
                  plain
                  size="small"
                  :loading="togglingServiceId === row.id"
                  @click="handleToggleService(row)"
                >
                  {{ row.active ? 'Disable' : 'Enable' }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="services.length > pageSize" class="admin-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="services.length"
            :page-sizes="[10, 20, 50]"
          />
        </div>
      </template>
    </div>

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

    <el-dialog
      v-model="isEditDialogOpen"
      title="Edit Service"
      width="min(560px, 92vw)"
      @closed="resetEditForm"
    >
      <el-alert
        v-if="editErrorMessage"
        :title="editErrorMessage"
        type="error"
        :closable="false"
        class="admin-page__alert"
      />

      <el-form :model="editForm" label-position="top">
        <el-form-item label="Name">
          <el-input v-model="editForm.name" placeholder="Service name" />
        </el-form-item>
        <el-form-item label="Category">
          <el-input v-model="editForm.category" placeholder="Wellness, Dental, Vaccines..." />
        </el-form-item>
        <el-form-item label="Duration">
          <el-input v-model="editForm.duration" placeholder="e.g. 30 min" />
        </el-form-item>
        <el-form-item label="Price">
          <el-input v-model="editForm.price" placeholder="e.g. $85 or From $35" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            placeholder="Short service description"
          />
        </el-form-item>
        <el-form-item label="Available for users">
          <el-switch v-model="editForm.active" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isEditDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isEditing" @click="handleEditService">
          Save Changes
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
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

.admin-filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
  align-items: end;
}

.admin-filters__control {
  width: 100%;
}

.admin-filters__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.admin-page h1 {
  margin-top: 0;
  margin-bottom: 8px;
}

.admin-page p {
  margin-bottom: 0;
  color: var(--pc-muted);
}

.admin-table-section {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.actions-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 18px;
  min-height: 62px;
}

.admin-pagination :deep(.el-pagination) {
  margin-left: auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  row-gap: 10px;
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

  .admin-filters {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 641px) and (max-width: 1100px) {
  .admin-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
