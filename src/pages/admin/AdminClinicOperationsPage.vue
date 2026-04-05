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
const weeklyAvailability = ref([])
const scheduleExceptions = ref([])
const isLoadingStaff = ref(false)
const isLoadingWeeklyAvailability = ref(false)
const isLoadingExceptions = ref(false)
const errorMessage = ref('')
const isExceptionDialogOpen = ref(false)
const isSavingException = ref(false)
const exceptionFormErrorMessage = ref('')
const isWeeklyDialogOpen = ref(false)
const isSavingWeeklyAvailability = ref(false)
const weeklyFormErrorMessage = ref('')
const editingExceptionId = ref(null)
const deletingExceptionId = ref(null)
const exceptionForm = ref(buildDefaultExceptionForm())
const weeklyForm = ref(buildDefaultWeeklyForm())
const editingWeeklyDay = ref(null)

const selectedStaff = computed(() =>
  staffRecords.value.find((staff) => staff.id === selectedStaffId.value) || null
)
const selectedDateException = computed(() =>
  scheduleExceptions.value.find((item) => item.date === selectedDate.value) || null
)
const selectedDateWeekday = computed(() => {
  if (!selectedDate.value) {
    return ''
  }

  return new Date(`${selectedDate.value}T00:00:00`).toLocaleDateString('en-US', {
    weekday: 'long',
  })
})
const weeklyAvailabilityByDay = computed(() => {
  const dayOrder = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
  const labels = {
    MONDAY: 'Monday',
    TUESDAY: 'Tuesday',
    WEDNESDAY: 'Wednesday',
    THURSDAY: 'Thursday',
    FRIDAY: 'Friday',
    SATURDAY: 'Saturday',
    SUNDAY: 'Sunday',
  }

  return dayOrder.map((dayKey) => {
    const slots = weeklyAvailability.value
      .filter((item) => item.dayOfWeek === dayKey)
      .sort((left, right) => left.startTime.localeCompare(right.startTime))

    return {
      key: dayKey,
      label: labels[dayKey],
      slots,
      hasActiveSlots: slots.some((slot) => slot.active),
    }
  })
})
const selectedDateWeeklySlots = computed(() => {
  if (!selectedDate.value) {
    return []
  }

  const dayKey = new Date(`${selectedDate.value}T00:00:00`)
    .toLocaleDateString('en-US', { weekday: 'long' })
    .toUpperCase()

  return weeklyAvailability.value
    .filter((item) => item.dayOfWeek === dayKey && item.active)
    .sort((left, right) => left.startTime.localeCompare(right.startTime))
})
const activeRuleSummary = computed(() => {
  if (selectedDateException.value) {
    return {
      tagType: selectedDateException.value.available ? 'warning' : 'danger',
      tagLabel: selectedDateException.value.available ? 'Custom working hours' : 'Unavailable all day',
      description: selectedDateException.value.available
        ? `Override active for ${selectedDateWeekday.value}: ${getExceptionSummary(selectedDateException.value)}`
        : `Override active for ${selectedDateWeekday.value}: this staff member is unavailable all day.`,
      detail: 'This exception takes priority over the weekly template.',
    }
  }

  if (selectedDateWeeklySlots.value.length) {
    return {
      tagType: 'success',
      tagLabel: 'Weekly fallback',
      description: `Using the weekly default for ${selectedDateWeekday.value}: ${formatAvailabilitySlots(selectedDateWeeklySlots.value)}`,
      detail: 'No date-specific override exists for the selected date.',
    }
  }

  if (selectedDate.value) {
    return {
      tagType: 'info',
      tagLabel: 'No working hours',
      description: `No active weekly availability is set for ${selectedDateWeekday.value}.`,
      detail: 'Add a weekly template or a date-specific override to open time on this date.',
    }
  }

  return {
    tagType: 'info',
    tagLabel: 'Select a date',
    description: 'Choose a date to see which scheduling rule applies.',
    detail: '',
  }
})
const sortedExceptions = computed(() =>
  [...scheduleExceptions.value].sort((left, right) => left.date.localeCompare(right.date))
)
const exceptionDialogTitle = computed(() =>
  editingExceptionId.value ? 'Edit Schedule Exception' : 'Add Schedule Exception'
)
const weeklyDialogTitle = computed(() =>
  editingWeeklyDay.value ? `Edit ${editingWeeklyDay.value.label} Template` : 'Edit Weekly Template'
)

function buildDefaultExceptionForm() {
  return {
    date: '',
    mode: 'unavailable',
    customStartTime: '09:00',
    customEndTime: '17:00',
  }
}

function buildDefaultWeeklyForm() {
  return {
    isOpen: true,
    startTime: '09:00',
    endTime: '17:00',
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

function formatAvailabilitySlots(slots) {
  if (!slots.length) {
    return 'No active hours'
  }

  return slots
    .filter((slot) => slot.active)
    .map((slot) => `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`)
    .join(', ')
}

function getPrimaryWeeklySlot(day) {
  return day?.slots?.[0] ?? null
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

async function loadWeeklyAvailability() {
  if (!selectedStaffId.value) {
    weeklyAvailability.value = []
    return
  }

  isLoadingWeeklyAvailability.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminStaffApi.listAvailability(selectedStaffId.value)
    weeklyAvailability.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load weekly availability right now.')
    weeklyAvailability.value = []
  } finally {
    isLoadingWeeklyAvailability.value = false
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

function resetWeeklyForm() {
  editingWeeklyDay.value = null
  weeklyFormErrorMessage.value = ''
  weeklyForm.value = buildDefaultWeeklyForm()
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

function openWeeklyTemplateDialog(day) {
  editingWeeklyDay.value = day
  weeklyFormErrorMessage.value = ''

  const primarySlot = getPrimaryWeeklySlot(day)
  weeklyForm.value = {
    isOpen: day.hasActiveSlots,
    startTime: primarySlot?.startTime?.slice(0, 5) || '09:00',
    endTime: primarySlot?.endTime?.slice(0, 5) || '17:00',
  }

  isWeeklyDialogOpen.value = true
}

async function handleSaveWeeklyTemplate() {
  if (!selectedStaffId.value || !editingWeeklyDay.value) {
    return
  }

  isSavingWeeklyAvailability.value = true
  weeklyFormErrorMessage.value = ''

  const primarySlot = getPrimaryWeeklySlot(editingWeeklyDay.value)
  const payload = {
    dayOfWeek: editingWeeklyDay.value.key,
    startTime: weeklyForm.value.startTime,
    endTime: weeklyForm.value.endTime,
    active: weeklyForm.value.isOpen,
  }

  try {
    if (primarySlot?.id) {
      await adminStaffApi.updateAvailability(selectedStaffId.value, primarySlot.id, payload)
    } else {
      await adminStaffApi.createAvailability(selectedStaffId.value, payload)
    }

    isWeeklyDialogOpen.value = false
    resetWeeklyForm()
    await loadWeeklyAvailability()
  } catch (error) {
    weeklyFormErrorMessage.value = getApiErrorMessage(error, 'Unable to save weekly template availability right now.')
  } finally {
    isSavingWeeklyAvailability.value = false
  }
}

watch(selectedStaffId, async () => {
  await loadWeeklyAvailability()
  await loadScheduleExceptions()
})

onMounted(async () => {
  selectedDate.value = new Date().toISOString().slice(0, 10)
  await loadStaff()
  await loadWeeklyAvailability()
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
          <span class="selected-date-card__label">Selected staff and date</span>
          <strong>{{ selectedDate ? formatExceptionDate(selectedDate) : 'Choose a date' }}</strong>
          <p v-if="selectedStaff">
            {{ selectedStaff.name }} · {{ selectedStaff.role }}
          </p>
        </div>

        <div class="selected-date-card__status" :class="{ 'selected-date-card__status--fallback': activeRuleSummary.tagLabel === 'Weekly fallback' }">
          <el-tag :type="activeRuleSummary.tagType" effect="plain">
            {{ activeRuleSummary.tagLabel }}
          </el-tag>
          <strong class="selected-date-card__headline">Active rule</strong>
          <span>{{ activeRuleSummary.description }}</span>
          <small v-if="activeRuleSummary.detail">{{ activeRuleSummary.detail }}</small>
        </div>
      </div>

      <section class="operations-subpanel operations-subpanel--weekly">
        <div class="operations-subpanel__header">
          <div>
            <span class="operations-subpanel__eyebrow">Weekly Default</span>
            <h2>Template availability</h2>
          </div>
          <p v-if="selectedStaff">
            Default working hours for {{ selectedStaff.name }}.
          </p>
        </div>

        <el-skeleton v-if="isLoadingWeeklyAvailability" :rows="6" animated />

        <div v-else class="weekly-availability-list">
          <article
            v-for="day in weeklyAvailabilityByDay"
            :key="day.key"
            class="weekly-availability-card"
            :class="{ 'weekly-availability-card--selected': day.label === selectedDateWeekday }"
          >
            <div>
              <strong>{{ day.label }}</strong>
              <span>
                {{ day.hasActiveSlots ? formatAvailabilitySlots(day.slots) : 'Unavailable by default' }}
              </span>
            </div>
            <div class="weekly-availability-card__actions">
              <el-tag :type="day.hasActiveSlots ? 'success' : 'info'" effect="plain">
                {{ day.hasActiveSlots ? 'Open by default' : 'Closed by default' }}
              </el-tag>
              <el-button plain size="small" @click="openWeeklyTemplateDialog(day)">
                Edit default
              </el-button>
            </div>
          </article>
        </div>
      </section>

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
              {{ row.available ? 'Custom working hours' : 'Unavailable all day' }}
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

    <el-dialog
      v-model="isWeeklyDialogOpen"
      :title="weeklyDialogTitle"
      width="min(520px, 92vw)"
      @closed="resetWeeklyForm"
    >
      <el-alert
        v-if="weeklyFormErrorMessage"
        :title="weeklyFormErrorMessage"
        type="error"
        :closable="false"
        class="operations-alert"
      />

      <el-form :model="weeklyForm" label-position="top">
        <el-form-item label="Default status">
          <el-radio-group v-model="weeklyForm.isOpen" class="exception-mode-group">
            <el-radio-button :value="true">Open</el-radio-button>
            <el-radio-button :value="false">Closed</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="weeklyForm.isOpen">
          <p class="exception-helper">
            These hours will be used as the weekly fallback when no date-specific override exists.
          </p>
          <el-form-item label="Start time">
            <el-time-select
              v-model="weeklyForm.startTime"
              start="06:00"
              step="00:30"
              end="22:00"
              class="operations-field"
            />
          </el-form-item>
          <el-form-item label="End time">
            <el-time-select
              v-model="weeklyForm.endTime"
              start="06:30"
              step="00:30"
              end="22:30"
              class="operations-field"
            />
          </el-form-item>
        </template>

        <p v-else class="exception-helper">
          This weekday will be treated as closed by default unless a date-specific override is added.
        </p>
      </el-form>

      <template #footer>
        <el-button @click="isWeeklyDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isSavingWeeklyAvailability" @click="handleSaveWeeklyTemplate">
          Save Weekly Default
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

.operations-subpanel {
  margin-bottom: 18px;
  border: 1px solid var(--pc-border);
  border-radius: 20px;
  padding: 20px;
  background: #fcfdfc;
}

.operations-subpanel__header {
  margin-bottom: 16px;
}

.operations-subpanel__eyebrow {
  display: block;
  margin-bottom: 6px;
  color: #72807c;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.operations-subpanel__header h2 {
  margin: 0;
  color: #173047;
  font-size: 1.08rem;
}

.operations-subpanel__header p {
  margin: 8px 0 0;
  color: #66747f;
  line-height: 1.55;
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

.selected-date-card__headline {
  color: #173047;
  font-size: 0.95rem;
}

.selected-date-card__status small {
  color: #72807c;
  font-size: 0.82rem;
}

.selected-date-card__status--fallback {
  color: #67746f;
}

.weekly-availability-list {
  display: grid;
  gap: 10px;
}

.weekly-availability-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: white;
  border: 1px solid rgba(63, 114, 93, 0.12);
}

.weekly-availability-card--selected {
  border-color: rgba(63, 114, 93, 0.28);
  background: #f4faf6;
}

.weekly-availability-card strong {
  display: block;
  color: #173047;
}

.weekly-availability-card span {
  display: block;
  margin-top: 6px;
  color: #66747f;
  line-height: 1.45;
}

.weekly-availability-card__actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
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
