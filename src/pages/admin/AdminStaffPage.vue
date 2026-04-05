<script setup>
import { onMounted, ref } from 'vue'
import { adminStaffApi } from '@/api/adminStaff'

const staffRecords = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const togglingStaffId = ref(null)
const isCreateDialogOpen = ref(false)
const isCreating = ref(false)
const createErrorMessage = ref('')
const isEditDialogOpen = ref(false)
const isEditing = ref(false)
const editErrorMessage = ref('')
const editingStaffId = ref(null)
const createForm = ref({
  name: '',
  role: '',
  active: true,
})
const editForm = ref({
  name: '',
  role: '',
  active: true,
})

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

function openCreateDialog() {
  createErrorMessage.value = ''
  isCreateDialogOpen.value = true
}

function openEditDialog(staff) {
  editErrorMessage.value = ''
  editingStaffId.value = staff.id
  editForm.value = {
    name: staff.name,
    role: staff.role,
    active: staff.active,
  }
  isEditDialogOpen.value = true
}

function resetCreateForm() {
  createForm.value = {
    name: '',
    role: '',
    active: true,
  }
}

function resetEditForm() {
  editingStaffId.value = null
  editErrorMessage.value = ''
  editForm.value = {
    name: '',
    role: '',
    active: true,
  }
}

async function handleCreateStaff() {
  isCreating.value = true
  createErrorMessage.value = ''

  try {
    await adminStaffApi.create(createForm.value)
    isCreateDialogOpen.value = false
    resetCreateForm()
    await loadStaff()
  } catch (error) {
    createErrorMessage.value = getApiErrorMessage(error, 'Unable to create this staff member right now.')
  } finally {
    isCreating.value = false
  }
}

async function handleEditStaff() {
  if (!editingStaffId.value) {
    return
  }

  isEditing.value = true
  editErrorMessage.value = ''

  try {
    await adminStaffApi.update(editingStaffId.value, editForm.value)
    isEditDialogOpen.value = false
    resetEditForm()
    await loadStaff()
  } catch (error) {
    editErrorMessage.value = getApiErrorMessage(error, 'Unable to update this staff member right now.')
  } finally {
    isEditing.value = false
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
      <el-button type="primary" @click="openCreateDialog">Add Staff</el-button>
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
              :loading="togglingStaffId === row.id"
              @click="handleToggleStaff(row)"
            >
              {{ row.active ? 'Disable' : 'Enable' }}
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="isCreateDialogOpen"
      title="Add Staff"
      width="min(520px, 92vw)"
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
          <el-input v-model="createForm.name" placeholder="Staff member name" />
        </el-form-item>
        <el-form-item label="Role">
          <el-input v-model="createForm.role" placeholder="Veterinarian, Nurse, Reception..." />
        </el-form-item>
        <el-form-item label="Available for bookings">
          <el-switch v-model="createForm.active" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isCreateDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isCreating" @click="handleCreateStaff">
          Save Staff
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="isEditDialogOpen"
      title="Edit Staff"
      width="min(520px, 92vw)"
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
          <el-input v-model="editForm.name" placeholder="Staff member name" />
        </el-form-item>
        <el-form-item label="Role">
          <el-input v-model="editForm.role" placeholder="Veterinarian, Nurse, Reception..." />
        </el-form-item>
        <el-form-item label="Available for bookings">
          <el-switch v-model="editForm.active" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isEditDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isEditing" @click="handleEditStaff">
          Save Changes
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

.actions-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
