<script setup>
import { computed, ref, watch } from 'vue'
import { bookingsApi } from '@/api/bookings'
import { servicesApi } from '@/api/services'
import { staffApi } from '@/api/staff'
import { useAuthStore } from '@/stores/auth'

const CLINIC_OPEN_HOUR = 8
const CLINIC_CLOSE_HOUR = 17
const TIME_SLOT_INTERVAL_MINUTES = 30
const APPOINTMENT_BUFFER_MINUTES = 60
const TIME_SLOT_OPTIONS = buildTimeSlotOptions()

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  initialServiceId: {
    type: [Number, String],
    default: null,
  },
  services: {
    type: Array,
    default: () => [],
  },
  staffRecords: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'created'])

const authStore = useAuthStore()
const localServices = ref([])
const localStaffRecords = ref([])
const availabilitySlots = ref([])
const isCreating = ref(false)
const errorMessage = ref('')
const createForm = ref(getDefaultCreateForm())

const dialogOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const availableServices = computed(() => (
  props.services.length ? props.services : localServices.value
))

const availableStaffRecords = computed(() => (
  props.staffRecords.length ? props.staffRecords : localStaffRecords.value
))

const serviceOptions = computed(() => availableServices.value.map((service) => ({
  label: `${service.name} - ${service.category}`,
  value: service.id,
})))

const staffOptions = computed(() => availableStaffRecords.value.map((staff) => ({
  label: `${staff.name} - ${staff.role}`,
  value: staff.id,
})))

const timeOptions = computed(() => buildAvailableTimeOptions(
  createForm.value.date,
  availabilitySlots.value,
  createForm.value.staffId,
))

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getDefaultCreateForm() {
  return {
    petName: '',
    serviceId: null,
    service: '',
    date: '',
    time: '',
    status: 'Upcoming',
    clinic: 'PawCare Hub Clinic',
    ownerNote: '',
    staffId: null,
    staff: '',
  }
}

function getTodayLocalDate() {
  return formatDateObjectForInput(new Date())
}

function formatDateObjectForInput(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildTimeSlotOptions() {
  const options = []

  for (
    let minutes = CLINIC_OPEN_HOUR * 60;
    minutes <= CLINIC_CLOSE_HOUR * 60;
    minutes += TIME_SLOT_INTERVAL_MINUTES
  ) {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    const labelHours = hours % 12 || 12
    const labelMinutes = `${mins}`.padStart(2, '0')
    const meridiem = hours >= 12 ? 'PM' : 'AM'

    options.push({
      value: `${labelHours}:${labelMinutes} ${meridiem}`,
      minutes,
    })
  }

  return options
}

function formatDateInputForApi(dateInput) {
  if (!dateInput) {
    return ''
  }

  const [year, month, day] = dateInput.split('-').map(Number)
  if (!year || !month || !day) {
    return dateInput
  }

  const displayDate = new Date(year, month - 1, day)
  return displayDate.toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

function getBufferThreshold() {
  return new Date(Date.now() + APPOINTMENT_BUFFER_MINUTES * 60 * 1000)
}

function isDateDisabled(date) {
  const candidateDate = new Date(date)
  candidateDate.setHours(0, 0, 0, 0)

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return candidateDate < today
}

function isTimeSlotDisabled(selectedDate, slotMinutes) {
  if (!selectedDate) {
    return false
  }

  if (selectedDate !== getTodayLocalDate()) {
    return false
  }

  const threshold = getBufferThreshold()
  return slotMinutes < (threshold.getHours() * 60) + threshold.getMinutes()
}

function getMinutesFromAvailabilityTime(time) {
  if (!time) {
    return null
  }

  const [hours, minutes] = time.split(':').map(Number)
  if (Number.isNaN(hours) || Number.isNaN(minutes)) {
    return null
  }

  return (hours * 60) + minutes
}

function isWithinAvailability(slotMinutes, slots) {
  return slots.some((slot) => {
    const startMinutes = getMinutesFromAvailabilityTime(slot.startTime)
    const endMinutes = getMinutesFromAvailabilityTime(slot.endTime)

    if (startMinutes === null || endMinutes === null) {
      return false
    }

    return slotMinutes >= startMinutes && slotMinutes < endMinutes
  })
}

function buildAvailableTimeOptions(selectedDate, slots, staffId) {
  return TIME_SLOT_OPTIONS
    .filter((slot) => !staffId || !selectedDate || isWithinAvailability(slot.minutes, slots))
    .map((slot) => ({
      ...slot,
      disabled: isTimeSlotDisabled(selectedDate, slot.minutes),
    }))
}

function ensureValidSelectedTime() {
  const matchingOption = timeOptions.value.find((option) => option.value === createForm.value.time)

  if (!matchingOption || matchingOption.disabled) {
    createForm.value = {
      ...createForm.value,
      time: '',
    }
  }
}

async function ensureServicesLoaded() {
  if (props.services.length || localServices.value.length) {
    return
  }

  try {
    const { data } = await servicesApi.list()
    localServices.value = data
  } catch {
    localServices.value = []
  }
}

async function ensureStaffLoaded() {
  if (props.staffRecords.length || localStaffRecords.value.length) {
    return
  }

  try {
    const { data } = await staffApi.list()
    localStaffRecords.value = data
  } catch {
    localStaffRecords.value = []
  }
}

function applyDefaultStaffSelection() {
  if (createForm.value.staffId || !availableStaffRecords.value.length) {
    return
  }

  const defaultStaff = availableStaffRecords.value[0]
  createForm.value = {
    ...createForm.value,
    staffId: defaultStaff.id,
    staff: defaultStaff.name,
  }
}

function applyInitialServiceSelection() {
  const normalizedServiceId = Number(props.initialServiceId)
  if (!normalizedServiceId || !availableServices.value.length) {
    return
  }

  const selectedService = availableServices.value.find((service) => service.id === normalizedServiceId)
  if (!selectedService) {
    return
  }

  createForm.value = {
    ...createForm.value,
    serviceId: normalizedServiceId,
    service: selectedService.name,
  }
}

async function prepareDialog() {
  errorMessage.value = ''
  await Promise.all([ensureServicesLoaded(), ensureStaffLoaded()])
  applyInitialServiceSelection()
  applyDefaultStaffSelection()
}

function resetCreateForm() {
  availabilitySlots.value = []
  errorMessage.value = ''
  createForm.value = getDefaultCreateForm()
}

async function loadAvailabilityForBooking(date, staffId) {
  if (!date || !staffId) {
    availabilitySlots.value = []
    return
  }

  try {
    const { data } = await staffApi.availability(staffId, date)
    availabilitySlots.value = data
  } catch {
    availabilitySlots.value = []
  }
}

async function handleCreateBooking() {
  if (!authStore.user?.email) {
    return
  }

  if (!createForm.value.staffId && availableStaffRecords.value.length) {
    applyDefaultStaffSelection()
  }

  isCreating.value = true
  errorMessage.value = ''

  try {
    const selectedService = availableServices.value.find((service) => service.id === createForm.value.serviceId)
    const selectedStaff = availableStaffRecords.value.find((staff) => staff.id === createForm.value.staffId)

    const { data } = await bookingsApi.create({
      ...createForm.value,
      date: formatDateInputForApi(createForm.value.date),
      serviceId: createForm.value.serviceId,
      service: selectedService?.name || createForm.value.service,
      staffId: createForm.value.staffId,
      staff: selectedStaff?.name || createForm.value.staff,
    })
    dialogOpen.value = false
    resetCreateForm()
    emit('created', data)
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to create a booking right now.')
  } finally {
    isCreating.value = false
  }
}

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      prepareDialog()
    }
  },
  { immediate: true },
)

watch(
  [() => props.initialServiceId, availableServices],
  () => {
    if (props.modelValue) {
      applyInitialServiceSelection()
    }
  },
)

watch(
  availableStaffRecords,
  () => {
    if (props.modelValue) {
      applyDefaultStaffSelection()
    }
  },
)

watch(
  [() => createForm.value.date, () => createForm.value.staffId],
  async () => {
    await loadAvailabilityForBooking(createForm.value.date, createForm.value.staffId)
    ensureValidSelectedTime()
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogOpen"
    title="Book New Visit"
    width="min(560px, 92vw)"
    @closed="resetCreateForm"
  >
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="create-alert"
    />

    <el-form :model="createForm" label-position="top">
      <el-form-item label="Pet name">
        <el-input v-model="createForm.petName" placeholder="Pet name" />
      </el-form-item>
      <el-form-item label="Service">
        <el-select
          v-model="createForm.serviceId"
          placeholder="Select a service"
          filterable
          class="booking-service-select"
        >
          <el-option
            v-for="service in serviceOptions"
            :key="service.value"
            :label="service.label"
            :value="service.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="Date">
        <el-date-picker
          v-model="createForm.date"
          type="date"
          placeholder="Select appointment date"
          format="MMMM D, YYYY"
          value-format="YYYY-MM-DD"
          :disabled-date="isDateDisabled"
          class="booking-service-select"
        />
      </el-form-item>
      <el-form-item label="Time">
        <el-select
          v-model="createForm.time"
          placeholder="Select appointment time"
          :disabled="!createForm.date || !timeOptions.length"
          class="booking-service-select"
        >
          <el-option
            v-for="slot in timeOptions"
            :key="slot.value"
            :label="slot.value"
            :value="slot.value"
            :disabled="slot.disabled"
          />
        </el-select>
        <p
          v-if="createForm.date && createForm.staffId && !timeOptions.length"
          class="time-slot-hint"
        >
          No active availability is set for this staff member on the selected day.
        </p>
      </el-form-item>
      <el-form-item label="Clinic">
        <el-input v-model="createForm.clinic" placeholder="Clinic name" />
      </el-form-item>
      <el-form-item label="Staff">
        <el-select
          v-model="createForm.staffId"
          placeholder="Select a staff member"
          filterable
          class="booking-service-select"
        >
          <el-option
            v-for="staff in staffOptions"
            :key="staff.value"
            :label="staff.label"
            :value="staff.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="Message for the clinic">
        <el-input
          v-model="createForm.ownerNote"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="Optional note to help the clinic prepare for this visit"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogOpen = false">Cancel</el-button>
      <el-button type="primary" :loading="isCreating" @click="handleCreateBooking">
        Save Booking
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.create-alert {
  margin-bottom: 16px;
}

.time-slot-hint {
  margin: 8px 0 0;
  color: var(--pc-primary);
  font-size: 0.88rem;
}
</style>
