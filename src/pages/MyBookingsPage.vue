<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { bookingsApi } from '@/api/bookings'
import { staffApi } from '@/api/staff'
import CreateBookingDialog from '@/components/bookings/CreateBookingDialog.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'

const CLINIC_OPEN_HOUR = 8
const CLINIC_CLOSE_HOUR = 17
const TIME_SLOT_INTERVAL_MINUTES = 30
const APPOINTMENT_BUFFER_MINUTES = 60
const TIME_SLOT_OPTIONS = buildTimeSlotOptions()

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const bookings = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const isCreateDialogOpen = ref(false)
const createInitialServiceId = ref(null)
const staffRecords = ref([])
const rescheduleAvailabilitySlots = ref([])
const isCancellingBookingId = ref(null)
const isDetailsDialogOpen = ref(false)
const isLoadingDetails = ref(false)
const detailsErrorMessage = ref('')
const selectedBooking = ref(null)
const isRescheduleDialogOpen = ref(false)
const isRescheduling = ref(false)
const rescheduleErrorMessage = ref('')
const rescheduleBookingId = ref(null)
const rescheduleForm = ref({
  date: '',
  time: '',
  staffId: null,
  staff: '',
})
const ownerName = computed(() => authStore.user?.fullName || 'your account')
const staffOptions = computed(() => staffRecords.value.map((staff) => ({
  label: `${staff.name} - ${staff.role}`,
  value: staff.id,
})))

const rescheduleTimeOptions = computed(() => buildAvailableTimeOptions(
  rescheduleForm.value.date,
  rescheduleAvailabilitySlots.value,
  rescheduleForm.value.staffId,
))

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getNormalizedBookingStatus(status) {
  return String(status || '').trim().toUpperCase()
}

function getBookingStatusLabel(status) {
  const normalizedStatus = getNormalizedBookingStatus(status)

  if (normalizedStatus === 'CANCELLED') {
    return 'Cancelled'
  }

  if (normalizedStatus === 'COMPLETED') {
    return 'Completed'
  }

  return status || 'Unknown'
}

function isBookingCancelled(booking) {
  return getNormalizedBookingStatus(booking?.status) === 'CANCELLED'
}

function isBookingCompleted(booking) {
  return getNormalizedBookingStatus(booking?.status) === 'COMPLETED'
}

function canModifyBooking(booking) {
  return !isBookingCancelled(booking) && !isBookingCompleted(booking)
}

function getStatusTagType(status) {
  const normalizedStatus = getNormalizedBookingStatus(status)

  if (normalizedStatus === 'CONFIRMED' || normalizedStatus === 'COMPLETED') {
    return 'success'
  }

  if (normalizedStatus === 'UPCOMING') {
    return 'primary'
  }

  if (normalizedStatus === 'CANCELLED') {
    return 'danger'
  }

  return 'info'
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

function parseDisplayDateToInput(displayDate) {
  if (!displayDate) {
    return ''
  }

  const parsedDate = new Date(displayDate)
  if (Number.isNaN(parsedDate.getTime())) {
    return ''
  }

  return formatDateObjectForInput(parsedDate)
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

function isWithinAvailability(slotMinutes, availabilitySlots) {
  return availabilitySlots.some((slot) => {
    const startMinutes = getMinutesFromAvailabilityTime(slot.startTime)
    const endMinutes = getMinutesFromAvailabilityTime(slot.endTime)

    if (startMinutes === null || endMinutes === null) {
      return false
    }

    return slotMinutes >= startMinutes && slotMinutes < endMinutes
  })
}

function buildAvailableTimeOptions(selectedDate, availabilitySlots, staffId) {
  return TIME_SLOT_OPTIONS
    .filter((slot) => !staffId || !selectedDate || isWithinAvailability(slot.minutes, availabilitySlots))
    .map((slot) => ({
      ...slot,
      disabled: isTimeSlotDisabled(selectedDate, slot.minutes),
    }))
}

function ensureValidSelectedTime(formRef, timeOptions) {
  const matchingOption = timeOptions.value.find((option) => option.value === formRef.value.time)

  if (!matchingOption || matchingOption.disabled) {
    formRef.value = {
      ...formRef.value,
      time: '',
    }
  }
}

async function loadBookings() {
  if (!authStore.user?.email) {
    bookings.value = []
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await bookingsApi.me()
    bookings.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load bookings right now.')
    bookings.value = []
  } finally {
    isLoading.value = false
  }
}

async function loadStaff() {
  try {
    const { data } = await staffApi.list()
    staffRecords.value = data
  } catch {
    staffRecords.value = []
  }
}

function openCreateDialog() {
  createInitialServiceId.value = null
  isCreateDialogOpen.value = true
}

function openCreateDialogForService(serviceId) {
  const normalizedServiceId = Number(serviceId)
  if (!normalizedServiceId) {
    return
  }

  createInitialServiceId.value = normalizedServiceId
  isCreateDialogOpen.value = true
}

function openRescheduleDialog(booking) {
  if (!canModifyBooking(booking)) {
    return
  }

  rescheduleErrorMessage.value = ''
  rescheduleBookingId.value = booking.id
  rescheduleForm.value = {
    date: parseDisplayDateToInput(booking.date),
    time: booking.time,
    staffId: resolveStaffIdForBooking(booking),
    staff: booking.staff,
  }
  isRescheduleDialogOpen.value = true
}

function clearBookingQuerySelection() {
  if (!route.query.bookServiceId) {
    return
  }

  const nextQuery = { ...route.query }
  delete nextQuery.bookServiceId
  router.replace({ query: nextQuery })
}

function applyBookingQuerySelection() {
  const serviceId = route.query.bookServiceId
  if (!serviceId) {
    return
  }

  openCreateDialogForService(serviceId)
  clearBookingQuerySelection()
}

function resetSelectedBooking() {
  selectedBooking.value = null
  detailsErrorMessage.value = ''
}

function resetRescheduleForm() {
  rescheduleBookingId.value = null
  rescheduleErrorMessage.value = ''
  rescheduleAvailabilitySlots.value = []
  rescheduleForm.value = {
    date: '',
    time: '',
    staffId: null,
    staff: '',
  }
}

async function loadAvailabilityForBooking(date, staffId, availabilityRef) {
  if (!date || !staffId) {
    availabilityRef.value = []
    return
  }

  try {
    const { data } = await staffApi.availability(staffId, date)
    availabilityRef.value = data
  } catch {
    availabilityRef.value = []
  }
}

function resolveStaffIdForBooking(booking) {
  if (booking?.staffId) {
    return booking.staffId
  }

  const matchedStaff = staffRecords.value.find((staff) => staff.name === booking?.staff)
  return matchedStaff?.id ?? null
}

async function handleBookingCreated() {
  await loadBookings()
}

async function handleViewDetails(booking) {
  if (!authStore.user?.email || !booking?.id) {
    return
  }

  isDetailsDialogOpen.value = true
  isLoadingDetails.value = true
  detailsErrorMessage.value = ''
  selectedBooking.value = null

  try {
    const { data } = await bookingsApi.detail(booking.id)
    selectedBooking.value = data
  } catch (error) {
    detailsErrorMessage.value = getApiErrorMessage(error, 'Unable to load booking details right now.')
  } finally {
    isLoadingDetails.value = false
  }
}

async function handleRescheduleBooking() {
  if (!authStore.user?.email || !rescheduleBookingId.value) {
    return
  }

  isRescheduling.value = true
  rescheduleErrorMessage.value = ''
  errorMessage.value = ''

  try {
    const selectedStaff = staffRecords.value.find((staff) => staff.id === rescheduleForm.value.staffId)
    const { data } = await bookingsApi.reschedule(rescheduleBookingId.value, {
      ...rescheduleForm.value,
      date: formatDateInputForApi(rescheduleForm.value.date),
      staffId: rescheduleForm.value.staffId,
      staff: selectedStaff?.name || rescheduleForm.value.staff,
    })

    isRescheduleDialogOpen.value = false
    await loadBookings()

    if (selectedBooking.value?.id === data.id) {
      selectedBooking.value = data
    }
  } catch (error) {
    rescheduleErrorMessage.value = getApiErrorMessage(error, 'Unable to reschedule this booking right now.')
  } finally {
    isRescheduling.value = false
  }
}

async function handleCancelBooking(booking) {
  if (!authStore.user?.email || !booking?.id || !canModifyBooking(booking)) {
    return
  }

  const confirmed = window.confirm(`Cancel the booking for ${booking.petName}?`)
  if (!confirmed) {
    return
  }

  isCancellingBookingId.value = booking.id
  errorMessage.value = ''

  try {
    await bookingsApi.cancel(booking.id)
    await loadBookings()

    if (selectedBooking.value?.id === booking.id) {
      selectedBooking.value = {
        ...selectedBooking.value,
        status: 'Cancelled',
      }
    }
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to cancel this booking right now.')
  } finally {
    isCancellingBookingId.value = null
  }
}

watch(
  () => route.query.bookServiceId,
  () => {
    applyBookingQuerySelection()
  }
)

watch(
  [() => rescheduleForm.value.date, () => rescheduleForm.value.staffId],
  async () => {
    await loadAvailabilityForBooking(
      rescheduleForm.value.date,
      rescheduleForm.value.staffId,
      rescheduleAvailabilitySlots,
    )
    ensureValidSelectedTime(rescheduleForm, rescheduleTimeOptions)
  }
)

onMounted(() => {
  loadBookings()
  loadStaff()
  applyBookingQuerySelection()
})
</script>

<template>
  <PageContainer>
    <div class="bookings-page">
      <section class="bookings-header">
        <div class="bookings-header__copy">
          <span class="eyebrow">My bookings</span>
          <h1>Review upcoming visits and past appointments in one place.</h1>
          <p>
            Keep track of {{ ownerName }}'s scheduled care, check visit details, and manage
            appointments when plans change.
          </p>
        </div>

        <el-button type="primary" size="large" @click="openCreateDialog">Book New Visit</el-button>
      </section>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
      />

      <section v-if="isLoading" class="empty-state">
        <el-skeleton :rows="4" animated />
      </section>

      <section v-else-if="bookings.length" class="bookings-list">
        <el-card
          v-for="booking in bookings"
          :key="booking.id ?? `${booking.petName}-${booking.service}-${booking.date}`"
          class="booking-card"
          shadow="hover"
        >
          <div class="booking-card__top">
            <div>
              <h2>{{ booking.service }}</h2>
              <p class="booking-subtitle">{{ booking.petName }} | {{ booking.date }} at {{ booking.time }}</p>
            </div>
            <el-tag :type="getStatusTagType(booking.status)" effect="plain">
              {{ getBookingStatusLabel(booking.status) }}
            </el-tag>
          </div>

          <div class="booking-meta">
            <div>
              <span>Clinic</span>
              <strong>{{ booking.clinic }}</strong>
            </div>
            <div>
              <span>Staff</span>
              <strong>{{ booking.staff }}</strong>
            </div>
            <div>
              <span>Account</span>
              <strong>{{ booking.ownerEmail }}</strong>
            </div>
          </div>

          <div class="booking-actions">
            <el-button type="primary" @click="handleViewDetails(booking)">View Details</el-button>
            <el-button
              v-if="canModifyBooking(booking)"
              plain
              @click="openRescheduleDialog(booking)"
            >
              Reschedule
            </el-button>
            <el-button
              v-if="canModifyBooking(booking)"
              plain
              :loading="isCancellingBookingId === booking.id"
              @click="handleCancelBooking(booking)"
            >
              Cancel
            </el-button>
          </div>
        </el-card>
      </section>

      <section v-else class="empty-state">
        <el-empty description="No visits booked yet. Schedule your first appointment when you're ready.">
          <el-button type="primary" @click="openCreateDialog">Book New Visit</el-button>
        </el-empty>
      </section>

      <el-dialog
        v-model="isDetailsDialogOpen"
        title="Booking Details"
        width="min(560px, 92vw)"
        @closed="resetSelectedBooking"
      >
        <el-alert
          v-if="detailsErrorMessage"
          :title="detailsErrorMessage"
          type="error"
          :closable="false"
          class="create-alert"
        />

        <el-skeleton v-else-if="isLoadingDetails" :rows="6" animated />

        <div v-else-if="selectedBooking" class="details-grid">
          <div>
            <span>Pet name</span>
            <strong>{{ selectedBooking.petName }}</strong>
          </div>
          <div>
            <span>Service</span>
            <strong>{{ selectedBooking.service }}</strong>
          </div>
          <div>
            <span>Staff</span>
            <strong>{{ selectedBooking.staff }}</strong>
          </div>
          <div>
            <span>Clinic</span>
            <strong>{{ selectedBooking.clinic }}</strong>
          </div>
          <div>
            <span>Date</span>
            <strong>{{ selectedBooking.date }}</strong>
          </div>
          <div>
            <span>Time</span>
            <strong>{{ selectedBooking.time }}</strong>
          </div>
          <div>
            <span>Status</span>
            <el-tag :type="getStatusTagType(selectedBooking.status)" effect="plain" class="details-tag">
              {{ getBookingStatusLabel(selectedBooking.status) }}
            </el-tag>
          </div>
          <div>
            <span>Account</span>
            <strong>{{ selectedBooking.ownerEmail }}</strong>
          </div>
          <div
            v-if="selectedBooking.ownerNote"
            class="details-grid__full"
          >
            <span>Message for the clinic</span>
            <p>{{ selectedBooking.ownerNote }}</p>
          </div>
          <div
            v-if="isBookingCompleted(selectedBooking) && selectedBooking.visitSummary"
            class="details-grid__full"
          >
            <span>Visit summary</span>
            <p>{{ selectedBooking.visitSummary }}</p>
          </div>
          <div
            v-if="isBookingCompleted(selectedBooking) && selectedBooking.diagnosisAssessment"
            class="details-grid__full"
          >
            <span>Diagnosis / assessment</span>
            <p>{{ selectedBooking.diagnosisAssessment }}</p>
          </div>
          <div
            v-if="isBookingCompleted(selectedBooking) && selectedBooking.treatmentRecommendation"
            class="details-grid__full"
          >
            <span>Treatment / recommendation</span>
            <p>{{ selectedBooking.treatmentRecommendation }}</p>
          </div>
          <div
            v-if="isBookingCompleted(selectedBooking) && selectedBooking.followUpNote"
            class="details-grid__full"
          >
            <span>Follow-up note</span>
            <p>{{ selectedBooking.followUpNote }}</p>
          </div>
        </div>
      </el-dialog>

      <el-dialog
        v-model="isRescheduleDialogOpen"
        title="Reschedule Booking"
        width="min(560px, 92vw)"
        @closed="resetRescheduleForm"
      >
        <el-alert
          v-if="rescheduleErrorMessage"
          :title="rescheduleErrorMessage"
          type="error"
          :closable="false"
          class="create-alert"
        />

        <el-form :model="rescheduleForm" label-position="top">
          <el-form-item label="Date">
            <el-date-picker
              v-model="rescheduleForm.date"
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
              v-model="rescheduleForm.time"
              placeholder="Select appointment time"
              :disabled="!rescheduleForm.date || !rescheduleTimeOptions.length"
              class="booking-service-select"
            >
              <el-option
                v-for="slot in rescheduleTimeOptions"
                :key="slot.value"
                :label="slot.value"
                :value="slot.value"
                :disabled="slot.disabled"
              />
            </el-select>
            <p
              v-if="rescheduleForm.date && rescheduleForm.staffId && !rescheduleTimeOptions.length"
              class="time-slot-hint"
            >
              No active availability is set for this staff member on the selected day.
            </p>
          </el-form-item>
          <el-form-item label="Staff">
            <el-select
              v-model="rescheduleForm.staffId"
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
        </el-form>

        <template #footer>
          <el-button @click="isRescheduleDialogOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="isRescheduling" @click="handleRescheduleBooking">
            Save Reschedule
          </el-button>
        </template>
      </el-dialog>

      <CreateBookingDialog
        v-model="isCreateDialogOpen"
        :initial-service-id="createInitialServiceId"
        :staff-records="staffRecords"
        @created="handleBookingCreated"
      />
    </div>
  </PageContainer>
</template>

<style scoped>
.bookings-page {
  display: flex;
  flex-direction: column;
  gap: 34px;
  padding: 28px 0 56px;
}

.bookings-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 30px 28px 28px;
  border: 1px solid var(--pc-border);
  border-radius: var(--pc-radius-lg);
  background:
    radial-gradient(circle at top left, rgba(250, 249, 245, 0.9), transparent 34%),
    radial-gradient(circle at right center, rgba(201, 100, 66, 0.12), transparent 28%),
    linear-gradient(180deg, #faf9f5 0%, #f1efe7 100%);
  box-shadow: var(--pc-shadow);
}

.bookings-header__copy {
  max-width: 700px;
}

.eyebrow {
  display: inline-block;
  color: var(--pc-primary);
  font-family: "Courier New", ui-monospace, monospace;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.bookings-header h1 {
  margin: 14px 0 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(2.15rem, 4vw, 3.3rem);
  line-height: 1.08;
  font-weight: 500;
  letter-spacing: -0.035em;
}

.bookings-header p {
  max-width: 620px;
  margin: 16px 0 0;
  color: var(--pc-muted);
  line-height: 1.75;
}

.bookings-header :deep(.el-button--primary),
.booking-actions :deep(.el-button--primary),
.empty-state :deep(.el-button--primary) {
  --el-button-bg-color: var(--pc-primary);
  --el-button-border-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-primary-hover);
  --el-button-hover-border-color: var(--pc-primary-hover);
  --el-button-active-bg-color: #a94e33;
  --el-button-active-border-color: #a94e33;
  box-shadow: 0 10px 22px rgba(201, 100, 66, 0.16);
}

.booking-actions :deep(.el-button.is-plain) {
  --el-button-text-color: var(--pc-text);
  --el-button-bg-color: rgba(250, 249, 245, 0.9);
  --el-button-border-color: var(--pc-border);
  --el-button-hover-text-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-surface);
  --el-button-hover-border-color: rgba(201, 100, 66, 0.34);
}

.bookings-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.booking-card {
  border: 1px solid var(--pc-border-soft);
  border-radius: var(--pc-radius-md);
  background: rgba(250, 249, 245, 0.92);
  box-shadow: var(--pc-shadow-soft);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.booking-card:hover {
  transform: translateY(-3px);
  border-color: rgba(201, 100, 66, 0.22);
  box-shadow: var(--pc-shadow);
}

.booking-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.booking-card h2 {
  margin: 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.38rem;
  font-weight: 500;
}

.booking-subtitle {
  margin: 8px 0 0;
  color: var(--pc-muted);
}

.booking-meta,
.details-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.booking-meta {
  margin-top: 20px;
  padding: 16px 0;
  border-top: 1px solid var(--pc-border);
  border-bottom: 1px solid var(--pc-border);
}

.booking-meta span,
.details-grid span {
  display: block;
  color: var(--pc-soft-muted);
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.booking-meta strong,
.details-grid strong {
  display: block;
  margin-top: 6px;
  color: var(--pc-ink);
  font-size: 1rem;
}

.booking-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.empty-state {
  padding: 34px 24px;
  border: 1px dashed rgba(201, 100, 66, 0.28);
  border-radius: 28px;
  background: rgba(250, 249, 245, 0.72);
}

.create-alert {
  margin-bottom: 16px;
}

.details-grid__full {
  grid-column: 1 / -1;
}

.details-grid__full p {
  margin: 8px 0 0;
  color: var(--pc-muted);
  line-height: 1.7;
}

.time-slot-hint {
  margin: 8px 0 0;
  color: var(--pc-primary);
  font-size: 0.88rem;
}

.details-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.details-tag {
  margin-top: 8px;
}

@media (max-width: 760px) {
  .bookings-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .bookings-header :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .bookings-page {
    gap: 28px;
    padding-bottom: 44px;
  }

  .bookings-header {
    padding: 22px 18px 20px;
    border-radius: 24px;
  }

  .booking-card__top,
  .booking-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .booking-meta,
  .details-grid {
    grid-template-columns: 1fr;
  }

  .booking-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
