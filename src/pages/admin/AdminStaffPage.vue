<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminStaffApi } from '@/api/adminStaff'

const DAY_OPTIONS = [
  { label: 'Monday', value: 'MONDAY' },
  { label: 'Tuesday', value: 'TUESDAY' },
  { label: 'Wednesday', value: 'WEDNESDAY' },
  { label: 'Thursday', value: 'THURSDAY' },
  { label: 'Friday', value: 'FRIDAY' },
  { label: 'Saturday', value: 'SATURDAY' },
  { label: 'Sunday', value: 'SUNDAY' },
]

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
const isAvailabilityDialogOpen = ref(false)
const selectedStaffForAvailability = ref(null)
const availabilityRows = ref([])
const availabilityErrorMessage = ref('')
const isLoadingAvailability = ref(false)
const togglingAvailabilityId = ref(null)
const deletingAvailabilityId = ref(null)
const isAvailabilityFormDialogOpen = ref(false)
const isSavingAvailability = ref(false)
const availabilityFormErrorMessage = ref('')
const editingAvailabilityId = ref(null)
const createForm = ref({
  name: '',
  role: '',
  active: true,
  displayName: '',
  specialty: '',
  bio: '',
  photoUrl: '',
  showOnHomepage: false,
})
const editForm = ref({
  name: '',
  role: '',
  active: true,
  displayName: '',
  specialty: '',
  bio: '',
  photoUrl: '',
  showOnHomepage: false,
})
const availabilityForm = ref({
  dayOfWeek: 'MONDAY',
  startTime: '09:00',
  endTime: '17:00',
  active: true,
})
const availabilityDialogTitle = computed(() =>
  editingAvailabilityId.value ? 'Edit Availability' : 'Add Availability'
)
const currentPage = ref(1)
const pageSize = ref(10)
const paginatedStaffRecords = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  return staffRecords.value.slice(startIndex, startIndex + pageSize.value)
})

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function formatDayOfWeek(dayOfWeek) {
  return DAY_OPTIONS.find((option) => option.value === dayOfWeek)?.label || dayOfWeek
}

function formatTime(time) {
  if (!time) {
    return ''
  }

  const [hoursValue, minutesValue] = time.split(':')
  const hours = Number(hoursValue)
  const minutes = Number(minutesValue)
  const meridiem = hours >= 12 ? 'PM' : 'AM'
  const labelHours = hours % 12 || 12
  return `${labelHours}:${`${minutes}`.padStart(2, '0')} ${meridiem}`
}

function formatAvailabilityWindow(row) {
  return `${formatTime(row.startTime)} - ${formatTime(row.endTime)}`
}

async function loadStaff() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminStaffApi.list()
    staffRecords.value = data
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load staff records right now.')
    staffRecords.value = []
  } finally {
    isLoading.value = false
  }
}

async function loadAvailability(staffId) {
  if (!staffId) {
    availabilityRows.value = []
    return
  }

  isLoadingAvailability.value = true
  availabilityErrorMessage.value = ''

  try {
    const { data } = await adminStaffApi.listAvailability(staffId)
    availabilityRows.value = data
  } catch (error) {
    availabilityErrorMessage.value = getApiErrorMessage(error, 'Unable to load availability right now.')
    availabilityRows.value = []
  } finally {
    isLoadingAvailability.value = false
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
    displayName: staff.displayName || '',
    specialty: staff.specialty || '',
    bio: staff.bio || '',
    photoUrl: staff.photoUrl || '',
    showOnHomepage: !!staff.showOnHomepage,
  }
  isEditDialogOpen.value = true
}

function openAvailabilityDialog(staff) {
  selectedStaffForAvailability.value = staff
  availabilityErrorMessage.value = ''
  availabilityRows.value = []
  isAvailabilityDialogOpen.value = true
  loadAvailability(staff.id)
}

function openAvailabilityCreateDialog() {
  availabilityFormErrorMessage.value = ''
  editingAvailabilityId.value = null
  availabilityForm.value = {
    dayOfWeek: 'MONDAY',
    startTime: '09:00',
    endTime: '17:00',
    active: true,
  }
  isAvailabilityFormDialogOpen.value = true
}

function openAvailabilityEditDialog(row) {
  availabilityFormErrorMessage.value = ''
  editingAvailabilityId.value = row.id
  availabilityForm.value = {
    dayOfWeek: row.dayOfWeek,
    startTime: row.startTime.slice(0, 5),
    endTime: row.endTime.slice(0, 5),
    active: row.active,
  }
  isAvailabilityFormDialogOpen.value = true
}

function resetCreateForm() {
  createForm.value = {
    name: '',
    role: '',
    active: true,
    displayName: '',
    specialty: '',
    bio: '',
    photoUrl: '',
    showOnHomepage: false,
  }
}

function resetEditForm() {
  editingStaffId.value = null
  editErrorMessage.value = ''
  editForm.value = {
    name: '',
    role: '',
    active: true,
    displayName: '',
    specialty: '',
    bio: '',
    photoUrl: '',
    showOnHomepage: false,
  }
}

function resetAvailabilityDialog() {
  selectedStaffForAvailability.value = null
  availabilityRows.value = []
  availabilityErrorMessage.value = ''
  togglingAvailabilityId.value = null
  deletingAvailabilityId.value = null
}

function resetAvailabilityForm() {
  editingAvailabilityId.value = null
  availabilityFormErrorMessage.value = ''
  availabilityForm.value = {
    dayOfWeek: 'MONDAY',
    startTime: '09:00',
    endTime: '17:00',
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

async function handleSaveAvailability() {
  if (!selectedStaffForAvailability.value?.id) {
    return
  }

  isSavingAvailability.value = true
  availabilityFormErrorMessage.value = ''

  try {
    if (editingAvailabilityId.value) {
      await adminStaffApi.updateAvailability(
        selectedStaffForAvailability.value.id,
        editingAvailabilityId.value,
        availabilityForm.value,
      )
    } else {
      await adminStaffApi.createAvailability(selectedStaffForAvailability.value.id, availabilityForm.value)
    }

    isAvailabilityFormDialogOpen.value = false
    resetAvailabilityForm()
    await loadAvailability(selectedStaffForAvailability.value.id)
  } catch (error) {
    availabilityFormErrorMessage.value = getApiErrorMessage(error, 'Unable to save this availability slot right now.')
  } finally {
    isSavingAvailability.value = false
  }
}

async function handleToggleAvailability(row) {
  if (!selectedStaffForAvailability.value?.id || !row?.id) {
    return
  }

  togglingAvailabilityId.value = row.id
  availabilityErrorMessage.value = ''

  try {
    await adminStaffApi.toggleAvailability(selectedStaffForAvailability.value.id, row.id)
    await loadAvailability(selectedStaffForAvailability.value.id)
  } catch (error) {
    availabilityErrorMessage.value = getApiErrorMessage(error, 'Unable to update this availability slot right now.')
  } finally {
    togglingAvailabilityId.value = null
  }
}

async function handleDeleteAvailability(row) {
  if (!selectedStaffForAvailability.value?.id || !row?.id) {
    return
  }

  const confirmed = window.confirm(`Delete the ${formatDayOfWeek(row.dayOfWeek)} availability slot?`)
  if (!confirmed) {
    return
  }

  deletingAvailabilityId.value = row.id
  availabilityErrorMessage.value = ''

  try {
    await adminStaffApi.deleteAvailability(selectedStaffForAvailability.value.id, row.id)
    await loadAvailability(selectedStaffForAvailability.value.id)
  } catch (error) {
    availabilityErrorMessage.value = getApiErrorMessage(error, 'Unable to delete this availability slot right now.')
  } finally {
    deletingAvailabilityId.value = null
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
        <p>Manage real staff records and set the weekly hours they can accept bookings.</p>
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

    <el-table v-else :data="paginatedStaffRecords" stripe>
      <el-table-column prop="id" label="ID" min-width="80" />
      <el-table-column prop="name" label="Name" min-width="220" />
      <el-table-column prop="role" label="Role" min-width="180" />
      <el-table-column label="Homepage" min-width="160">
        <template #default="{ row }">
          <el-tag :type="row.showOnHomepage && row.active ? 'success' : 'info'" effect="plain">
            {{ row.showOnHomepage && row.active ? 'Visible' : 'Hidden' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Status" min-width="140">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" effect="plain">
            {{ row.active ? 'Active' : 'Inactive' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" min-width="280" fixed="right">
        <template #default="{ row }">
          <div class="actions-cell">
            <el-button plain size="small" @click="openEditDialog(row)">
              Edit
            </el-button>
            <el-button plain size="small" @click="openAvailabilityDialog(row)">
              Manage Availability
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

    <div v-if="staffRecords.length > pageSize" class="admin-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        layout="total, prev, pager, next, sizes"
        :total="staffRecords.length"
        :page-sizes="[10, 20, 50]"
      />
    </div>

    <el-dialog
      v-model="isCreateDialogOpen"
      title="Add Staff"
      width="min(640px, 92vw)"
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
        <el-form-item label="Public display name">
          <el-input v-model="createForm.displayName" placeholder="Shown on the homepage team section" />
        </el-form-item>
        <el-form-item label="Title or specialty">
          <el-input v-model="createForm.specialty" placeholder="Small Animal Veterinarian" />
        </el-form-item>
        <el-form-item label="Short bio">
          <el-input
            v-model="createForm.bio"
            type="textarea"
            :rows="3"
            maxlength="280"
            show-word-limit
            placeholder="A short introduction for families visiting the clinic website"
          />
        </el-form-item>
        <el-form-item label="Photo URL">
          <el-input v-model="createForm.photoUrl" placeholder="https://example.com/staff-photo.jpg" />
        </el-form-item>
        <el-form-item label="Available for bookings">
          <el-switch v-model="createForm.active" />
        </el-form-item>
        <el-form-item label="Show on homepage">
          <el-switch v-model="createForm.showOnHomepage" />
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
      width="min(640px, 92vw)"
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
        <el-form-item label="Public display name">
          <el-input v-model="editForm.displayName" placeholder="Shown on the homepage team section" />
        </el-form-item>
        <el-form-item label="Title or specialty">
          <el-input v-model="editForm.specialty" placeholder="Small Animal Veterinarian" />
        </el-form-item>
        <el-form-item label="Short bio">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="3"
            maxlength="280"
            show-word-limit
            placeholder="A short introduction for families visiting the clinic website"
          />
        </el-form-item>
        <el-form-item label="Photo URL">
          <el-input v-model="editForm.photoUrl" placeholder="https://example.com/staff-photo.jpg" />
        </el-form-item>
        <el-form-item label="Available for bookings">
          <el-switch v-model="editForm.active" />
        </el-form-item>
        <el-form-item label="Show on homepage">
          <el-switch v-model="editForm.showOnHomepage" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isEditDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isEditing" @click="handleEditStaff">
          Save Changes
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="isAvailabilityDialogOpen"
      :title="selectedStaffForAvailability ? `Availability for ${selectedStaffForAvailability.name}` : 'Manage Availability'"
      width="min(880px, 96vw)"
      @closed="resetAvailabilityDialog"
    >
      <div class="availability-header">
        <div>
          <strong v-if="selectedStaffForAvailability">{{ selectedStaffForAvailability.role }}</strong>
          <p>Weekly slots are used to filter booking time choices for this staff member.</p>
        </div>
        <el-button type="primary" @click="openAvailabilityCreateDialog">Add Availability</el-button>
      </div>

      <el-alert
        v-if="availabilityErrorMessage"
        :title="availabilityErrorMessage"
        type="error"
        :closable="false"
        class="admin-page__alert"
      />

      <el-skeleton v-if="isLoadingAvailability" :rows="4" animated />

      <el-empty
        v-else-if="!availabilityRows.length"
        description="No weekly availability has been set for this staff member yet."
      />

      <el-table v-else :data="availabilityRows" stripe>
        <el-table-column label="Day" min-width="160">
          <template #default="{ row }">
            {{ formatDayOfWeek(row.dayOfWeek) }}
          </template>
        </el-table-column>
        <el-table-column label="Hours" min-width="220">
          <template #default="{ row }">
            {{ formatAvailabilityWindow(row) }}
          </template>
        </el-table-column>
        <el-table-column label="Status" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" effect="plain">
              {{ row.active ? 'Active' : 'Inactive' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" min-width="260" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button plain size="small" @click="openAvailabilityEditDialog(row)">
                Edit
              </el-button>
              <el-button
                plain
                size="small"
                :loading="togglingAvailabilityId === row.id"
                @click="handleToggleAvailability(row)"
              >
                {{ row.active ? 'Disable' : 'Enable' }}
              </el-button>
              <el-button
                plain
                size="small"
                :loading="deletingAvailabilityId === row.id"
                @click="handleDeleteAvailability(row)"
              >
                Delete
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="isAvailabilityFormDialogOpen"
      :title="availabilityDialogTitle"
      width="min(520px, 92vw)"
      @closed="resetAvailabilityForm"
    >
      <el-alert
        v-if="availabilityFormErrorMessage"
        :title="availabilityFormErrorMessage"
        type="error"
        :closable="false"
        class="admin-page__alert"
      />

      <el-form :model="availabilityForm" label-position="top">
        <el-form-item label="Day of week">
          <el-select v-model="availabilityForm.dayOfWeek" class="form-select">
            <el-option
              v-for="option in DAY_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Start time">
          <el-time-select
            v-model="availabilityForm.startTime"
            start="06:00"
            step="00:30"
            end="22:00"
            class="form-select"
          />
        </el-form-item>
        <el-form-item label="End time">
          <el-time-select
            v-model="availabilityForm.endTime"
            start="06:30"
            step="00:30"
            end="22:30"
            class="form-select"
          />
        </el-form-item>
        <el-form-item label="Active for bookings">
          <el-switch v-model="availabilityForm.active" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isAvailabilityFormDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isSavingAvailability" @click="handleSaveAvailability">
          Save Availability
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

.admin-page__header,
.availability-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.admin-page__header {
  margin-bottom: 20px;
}

.availability-header {
  margin-bottom: 18px;
}

.availability-header p {
  margin: 8px 0 0;
  color: var(--pc-muted);
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

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.form-select {
  width: 100%;
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
  .admin-page__header,
  .availability-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
