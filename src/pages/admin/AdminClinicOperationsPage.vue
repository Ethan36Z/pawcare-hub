<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { adminStaffApi } from '@/api/adminStaff'
import { clinicOperationsApi } from '@/api/clinicOperations'

const EXCEPTION_MODE_OPTIONS = [
  { label: 'Unavailable all day', value: 'unavailable' },
  { label: 'Custom working hours', value: 'custom-hours' },
]

const staffRecords = ref([])
const selectedStaffId = ref(null)
const selectedDate = ref('')
const scheduleExceptions = ref([])
const isLoadingStaff = ref(false)
const isLoadingExceptions = ref(false)
const errorMessage = ref('')
const isExceptionDialogOpen = ref(false)
const isSavingException = ref(false)
const exceptionFormErrorMessage = ref('')
const editingExceptionId = ref(null)
const deletingExceptionId = ref(null)
const exceptionForm = ref(buildDefaultExceptionForm())

const selectedStaff = computed(() =>
  staffRecords.value.find((staff) => staff.id === selectedStaffId.value) || null
)
const selectedDateException = computed(() =>
  scheduleExceptions.value.find((item) => item.date === selectedDate.value) || null
)
const sortedExceptions = computed(() =>
  [...scheduleExceptions.value].sort((left, right) => left.date.localeCompare(right.date))
)
const exceptionDialogTitle = computed(() =>
  editingExceptionId.value ? 'Edit Schedule Exception' : 'Add Schedule Exception'
)

function buildDefaultExceptionForm() {
  return {
    date: '',
    mode: 'unavailable',
    customStartTime: '09:00',
    customEndTime: '17:00',
  }
}

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function formatExceptionDate(date) {
  if (!date) {
    return ''
  }

  const parsedDate = new Date(`${date}T00:00:00`)
  return parsedDate.toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
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

function getExceptionSummary(exceptionItem) {
  if (!exceptionItem.available) {
    return 'Unavailable all day'
  }

  return `${formatTime(exceptionItem.customStartTime)} - ${formatTime(exceptionItem.customEndTime)}`
}

function isPastDateDisabled(date) {
  const candidateDate = new Date(date)
  candidateDate.setHours(0, 0, 0, 0)

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return candidateDate < today
}

async function loadStaff() {
  isLoadingStaff.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminStaffApi.listForOperations()
    staffRecords.value = data

    if (!selectedStaffId.value && data.length) {
      selectedStaffId.value = data[0].id
    }
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load clinic operations right now.')
    staffRecords.value = []
  } finally {
    isLoadingStaff.value = false
  }
}

async function loadScheduleExceptions() {
  if (!selectedStaffId.value) {
    scheduleExceptions.value = []
    return
  }

  isLoadingExceptions.value = true
  errorMessage.value = ''

  try {
    const { data } = await clinicOperationsApi.listStaffScheduleExceptions(selectedStaffId.value)
    scheduleExceptions.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load schedule exceptions right now.')
    scheduleExceptions.value = []
  } finally {
    isLoadingExceptions.value = false
  }
}

function openCreateExceptionDialog() {
  if (!selectedStaffId.value || !selectedDate.value) {
    return
  }

  editingExceptionId.value = null
  exceptionFormErrorMessage.value = ''
  exceptionForm.value = {
    ...buildDefaultExceptionForm(),
    date: selectedDate.value,
  }
  isExceptionDialogOpen.value = true
}

function openEditExceptionDialog(exceptionItem) {
  editingExceptionId.value = exceptionItem.id
  exceptionFormErrorMessage.value = ''
  exceptionForm.value = {
    date: exceptionItem.date,
    mode: exceptionItem.available ? 'custom-hours' : 'unavailable',
    customStartTime: exceptionItem.customStartTime ? exceptionItem.customStartTime.slice(0, 5) : '09:00',
    customEndTime: exceptionItem.customEndTime ? exceptionItem.customEndTime.slice(0, 5) : '17:00',
  }
  isExceptionDialogOpen.value = true
}

function resetExceptionForm() {
  editingExceptionId.value = null
  exceptionFormErrorMessage.value = ''
  exceptionForm.value = buildDefaultExceptionForm()
}

async function handleSaveException() {
  if (!selectedStaffId.value) {
    return
  }

  isSavingException.value = true
  exceptionFormErrorMessage.value = ''

  const payload = {
    date: exceptionForm.value.date,
    available: exceptionForm.value.mode === 'custom-hours',
    customStartTime: exceptionForm.value.mode === 'custom-hours' ? exceptionForm.value.customStartTime : null,
    customEndTime: exceptionForm.value.mode === 'custom-hours' ? exceptionForm.value.customEndTime : null,
  }

  try {
    if (editingExceptionId.value) {
      await clinicOperationsApi.updateStaffScheduleException(
        selectedStaffId.value,
        editingExceptionId.value,
        payload,
      )
    } else {
      await clinicOperationsApi.createStaffScheduleException(selectedStaffId.value, payload)
    }

    isExceptionDialogOpen.value = false
    resetExceptionForm()
    await loadScheduleExceptions()
  } catch (error) {
    exceptionFormErrorMessage.value = getApiErrorMessage(error, 'Unable to save this schedule exception right now.')
  } finally {
    isSavingException.value = false
  }
}

async function handleDeleteException(exceptionItem) {
  if (!selectedStaffId.value || !exceptionItem?.id) {
    return
  }

  const confirmed = window.confirm(`Delete the override for ${formatExceptionDate(exceptionItem.date)}?`)
  if (!confirmed) {
    return
  }

  deletingExceptionId.value = exceptionItem.id
  errorMessage.value = ''

  try {
    await clinicOperationsApi.deleteStaffScheduleException(selectedStaffId.value, exceptionItem.id)
    await loadScheduleExceptions()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to delete this schedule exception right now.')
  } finally {
    deletingExceptionId.value = null
  }
}

watch(selectedStaffId, async () => {
  await loadScheduleExceptions()
})

onMounted(async () => {
  selectedDate.value = new Date().toISOString().slice(0, 10)
  await loadStaff()
  await loadScheduleExceptions()
})
</script>

<template>
  <section class="operations-page">
    <div class="operations-hero">
      <div>
        <p class="operations-eyebrow">Clinic Operations</p>
        <h1>Handle real-world schedule changes without touching the weekly template.</h1>
        <p class="operations-copy">
          Weekly availability stays as the default layer. Use date-specific overrides here for time off,
          half days, and one-date custom hours.
        </p>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="operations-alert"
    />

    <section class="operations-panel">
      <div class="operations-toolbar">
        <el-select
          v-model="selectedStaffId"
          placeholder="Select a staff member"
          filterable
          class="operations-field"
          :loading="isLoadingStaff"
        >
          <el-option
            v-for="staff in staffRecords"
            :key="staff.id"
            :label="`${staff.name} - ${staff.role}`"
            :value="staff.id"
          />
        </el-select>

        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="Select an exception date"
          value-format="YYYY-MM-DD"
          format="MMMM D, YYYY"
          :disabled-date="isPastDateDisabled"
          class="operations-field"
        />

        <el-button
          type="primary"
          :disabled="!selectedStaffId || !selectedDate"
          @click="selectedDateException ? openEditExceptionDialog(selectedDateException) : openCreateExceptionDialog()"
        >
          {{ selectedDateException ? 'Edit Selected Date' : 'Add Override' }}
        </el-button>
      </div>

      <div class="selected-date-card">
        <div>
          <span class="selected-date-card__label">Selected date</span>
          <strong>{{ selectedDate ? formatExceptionDate(selectedDate) : 'Choose a date' }}</strong>
          <p v-if="selectedStaff">
            {{ selectedStaff.name }} · {{ selectedStaff.role }}
          </p>
        </div>

        <div v-if="selectedDateException" class="selected-date-card__status">
          <el-tag :type="selectedDateException.available ? 'warning' : 'danger'" effect="plain">
            {{ selectedDateException.available ? 'Custom working hours' : 'Unavailable all day' }}
          </el-tag>
          <span>{{ getExceptionSummary(selectedDateException) }}</span>
        </div>
        <div v-else class="selected-date-card__status selected-date-card__status--fallback">
          <el-tag type="success" effect="plain">Weekly fallback</el-tag>
          <span>No exception exists for this date. Weekly availability will be used.</span>
        </div>
      </div>

      <el-skeleton v-if="isLoadingExceptions" :rows="5" animated />

      <el-empty
        v-else-if="!sortedExceptions.length"
        description="No schedule exceptions have been added for this staff member yet."
      />

      <el-table v-else :data="sortedExceptions" stripe>
        <el-table-column label="Date" min-width="220">
          <template #default="{ row }">
            {{ formatExceptionDate(row.date) }}
          </template>
        </el-table-column>
        <el-table-column label="Override" min-width="220">
          <template #default="{ row }">
            {{ getExceptionSummary(row) }}
          </template>
        </el-table-column>
        <el-table-column label="Type" min-width="180">
          <template #default="{ row }">
            <el-tag :type="row.available ? 'warning' : 'danger'" effect="plain">
              {{ row.available ? 'Custom hours' : 'Unavailable' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" min-width="190" fixed="right">
          <template #default="{ row }">
            <div class="operations-actions">
              <el-button plain size="small" @click="openEditExceptionDialog(row)">
                Edit
              </el-button>
              <el-button
                plain
                size="small"
                :loading="deletingExceptionId === row.id"
                @click="handleDeleteException(row)"
              >
                Delete
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="isExceptionDialogOpen"
      :title="exceptionDialogTitle"
      width="min(560px, 92vw)"
      @closed="resetExceptionForm"
    >
      <el-alert
        v-if="exceptionFormErrorMessage"
        :title="exceptionFormErrorMessage"
        type="error"
        :closable="false"
        class="operations-alert"
      />

      <el-form :model="exceptionForm" label-position="top">
        <el-form-item label="Date">
          <el-date-picker
            v-model="exceptionForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            format="MMMM D, YYYY"
            :disabled-date="isPastDateDisabled"
            class="operations-field"
          />
        </el-form-item>
        <el-form-item label="Override type">
          <el-radio-group v-model="exceptionForm.mode" class="exception-mode-group">
            <el-radio-button
              v-for="option in EXCEPTION_MODE_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="exceptionForm.mode === 'custom-hours'">
          <p class="exception-helper">
            Only these hours will be available for booking on this date.
          </p>
          <el-form-item label="Custom start time">
            <el-time-select
              v-model="exceptionForm.customStartTime"
              start="06:00"
              step="00:30"
              end="22:00"
              class="operations-field"
            />
          </el-form-item>
          <el-form-item label="Custom end time">
            <el-time-select
              v-model="exceptionForm.customEndTime"
              start="06:30"
              step="00:30"
              end="22:30"
              class="operations-field"
            />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="isExceptionDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isSavingException" @click="handleSaveException">
          Save Override
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.operations-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.operations-hero,
.operations-panel {
  padding: 28px;
  border-radius: 24px;
  border: 1px solid var(--pc-border);
  background: white;
}

.operations-hero {
  background:
    radial-gradient(circle at top left, rgba(214, 237, 229, 0.9), transparent 34%),
    radial-gradient(circle at right center, rgba(255, 234, 205, 0.68), transparent 24%),
    linear-gradient(180deg, #f9fdfb 0%, #fffaf2 100%);
}

.operations-eyebrow {
  margin: 0;
  color: #557a6b;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.operations-hero h1 {
  margin: 10px 0 0;
  color: #173047;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1.08;
}

.operations-copy {
  max-width: 760px;
  margin: 14px 0 0;
  color: #62707b;
  line-height: 1.7;
}

.operations-alert {
  margin-bottom: 16px;
}

.operations-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) minmax(220px, 1fr) auto;
  gap: 14px;
  align-items: end;
  margin-bottom: 18px;
}

.operations-field {
  width: 100%;
}

.selected-date-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px;
  margin-bottom: 18px;
  border-radius: 20px;
  background: #f8faf8;
  border: 1px solid rgba(63, 114, 93, 0.12);
}

.selected-date-card__label {
  display: block;
  margin-bottom: 6px;
  color: #72807c;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.selected-date-card strong {
  display: block;
  color: #173047;
}

.selected-date-card p {
  margin: 8px 0 0;
  color: #66747f;
}

.selected-date-card__status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  color: #5f6a64;
  text-align: right;
}

.selected-date-card__status--fallback {
  color: #67746f;
}

.operations-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.exception-helper {
  margin: 0 0 14px;
  color: #687670;
  font-size: 0.92rem;
  line-height: 1.5;
}

.exception-mode-group {
  display: flex;
  flex-wrap: wrap;
}

.operations-page :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

@media (max-width: 860px) {
  .operations-toolbar {
    grid-template-columns: 1fr;
  }

  .selected-date-card {
    flex-direction: column;
  }

  .selected-date-card__status {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
