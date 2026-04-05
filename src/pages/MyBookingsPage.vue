<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { bookingsApi } from '@/api/bookings'
import { servicesApi } from '@/api/services'
import { staffApi } from '@/api/staff'
import PageContainer from '@/components/common/PageContainer.vue'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const bookings = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const isCreateDialogOpen = ref(false)
const isCreating = ref(false)
const createErrorMessage = ref('')
const services = ref([])
const staffRecords = ref([])
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
const createForm = ref({
  petName: '',
  serviceId: null,
  service: '',
  date: '',
  time: '',
  status: 'Upcoming',
  clinic: 'PawCare Hub Clinic',
  staffId: null,
  staff: '',
})
const ownerName = computed(() => authStore.user?.fullName || 'your account')
const staffOptions = computed(() => staffRecords.value.map((staff) => ({
  label: `${staff.name} - ${staff.role}`,
  value: staff.id,
})))
const serviceOptions = computed(() => services.value.map((service) => ({
  label: `${service.name} · ${service.category}`,
  value: service.id,
})))

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
  if (!authStore.user?.email) {
    bookings.value = []
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await bookingsApi.me(authStore.user.email)
    bookings.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load bookings right now.')
    bookings.value = []
  } finally {
    isLoading.value = false
  }
}

async function loadServices() {
  try {
    const { data } = await servicesApi.list()
    services.value = data
    applyBookingQuerySelection()
  } catch {
    services.value = []
  }
}

async function loadStaff() {
  try {
    const { data } = await staffApi.list()
    staffRecords.value = data
    applyDefaultStaffSelection()
  } catch {
    staffRecords.value = []
  }
}

function openCreateDialog() {
  createErrorMessage.value = ''
  applyDefaultStaffSelection()
  isCreateDialogOpen.value = true
}

function openCreateDialogForService(serviceId) {
  const normalizedServiceId = Number(serviceId)
  if (!normalizedServiceId) {
    return
  }

  const selectedService = services.value.find((service) => service.id === normalizedServiceId)
  if (!selectedService) {
    return
  }

  createErrorMessage.value = ''
  createForm.value = {
    ...createForm.value,
    serviceId: normalizedServiceId,
    service: selectedService.name,
  }
  applyDefaultStaffSelection()
  isCreateDialogOpen.value = true
}

function openRescheduleDialog(booking) {
  rescheduleErrorMessage.value = ''
  rescheduleBookingId.value = booking.id
  rescheduleForm.value = {
    date: booking.date,
    time: booking.time,
    staffId: resolveStaffIdForBooking(booking),
    staff: booking.staff,
  }
  isRescheduleDialogOpen.value = true
}

function resetCreateForm() {
  createForm.value = {
    petName: '',
    serviceId: null,
    service: '',
    date: '',
    time: '',
    status: 'Upcoming',
    clinic: 'PawCare Hub Clinic',
    staffId: null,
    staff: '',
  }
}

function applyDefaultStaffSelection() {
  if (createForm.value.staffId || !staffRecords.value.length) {
    return
  }

  const defaultStaff = staffRecords.value[0]
  createForm.value = {
    ...createForm.value,
    staffId: defaultStaff.id,
    staff: defaultStaff.name,
  }
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
  if (!serviceId || !services.value.length) {
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
  rescheduleForm.value = {
    date: '',
    time: '',
    staffId: null,
    staff: '',
  }
}

function resolveStaffIdForBooking(booking) {
  if (booking?.staffId) {
    return booking.staffId
  }

  const matchedStaff = staffRecords.value.find((staff) => staff.name === booking?.staff)
  return matchedStaff?.id ?? null
}

async function handleCreateBooking() {
  if (!authStore.user?.email) {
    return
  }

  if (!createForm.value.staffId && staffRecords.value.length) {
    applyDefaultStaffSelection()
  }

  isCreating.value = true
  createErrorMessage.value = ''

  try {
    const selectedService = services.value.find((service) => service.id === createForm.value.serviceId)
    const selectedStaff = staffRecords.value.find((staff) => staff.id === createForm.value.staffId)

    await bookingsApi.create(authStore.user.email, {
      ...createForm.value,
      serviceId: createForm.value.serviceId,
      service: selectedService?.name || createForm.value.service,
      staffId: createForm.value.staffId,
      staff: selectedStaff?.name || createForm.value.staff,
    })
    isCreateDialogOpen.value = false
    resetCreateForm()
    await loadBookings()
  } catch (error) {
    createErrorMessage.value = getApiErrorMessage(error, 'Unable to create a booking right now.')
  } finally {
    isCreating.value = false
  }
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
    const { data } = await bookingsApi.detail(authStore.user.email, booking.id)
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
    const { data } = await bookingsApi.reschedule(
      authStore.user.email,
      rescheduleBookingId.value,
      {
        ...rescheduleForm.value,
        staffId: rescheduleForm.value.staffId,
        staff: selectedStaff?.name || rescheduleForm.value.staff,
      },
    )

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
  if (!authStore.user?.email || !booking?.id) {
    return
  }

  const confirmed = window.confirm(`Cancel the booking for ${booking.petName}?`)
  if (!confirmed) {
    return
  }

  isCancellingBookingId.value = booking.id
  errorMessage.value = ''

  try {
    await bookingsApi.cancel(authStore.user.email, booking.id)
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

onMounted(() => {
  loadBookings()
  loadServices()
  loadStaff()
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
              {{ booking.status }}
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
              v-if="booking.status !== 'Cancelled'"
              plain
              @click="openRescheduleDialog(booking)"
            >
              Reschedule
            </el-button>
            <el-button
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
              {{ selectedBooking.status }}
            </el-tag>
          </div>
          <div>
            <span>Account</span>
            <strong>{{ selectedBooking.ownerEmail }}</strong>
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
            <el-input v-model="rescheduleForm.date" placeholder="e.g. May 10, 2026" />
          </el-form-item>
          <el-form-item label="Time">
            <el-input v-model="rescheduleForm.time" placeholder="e.g. 10:30 AM" />
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

      <el-dialog
        v-model="isCreateDialogOpen"
        title="Book New Visit"
        width="min(560px, 92vw)"
        @closed="resetCreateForm"
      >
        <el-alert
          v-if="createErrorMessage"
          :title="createErrorMessage"
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
            <el-input v-model="createForm.date" placeholder="e.g. May 10, 2026" />
          </el-form-item>
          <el-form-item label="Time">
            <el-input v-model="createForm.time" placeholder="e.g. 10:30 AM" />
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
        </el-form>

        <template #footer>
          <el-button @click="isCreateDialogOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="isCreating" @click="handleCreateBooking">
            Save Booking
          </el-button>
        </template>
      </el-dialog>
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
  border: 1px solid rgba(28, 60, 88, 0.1);
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba(255, 239, 194, 0.88), transparent 34%),
    radial-gradient(circle at right center, rgba(244, 228, 196, 0.42), transparent 26%),
    linear-gradient(180deg, #fff9ee 0%, #f8efdc 100%);
}

.bookings-header__copy {
  max-width: 700px;
}

.eyebrow {
  display: inline-block;
  color: #7f7356;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.bookings-header h1 {
  margin: 14px 0 0;
  color: #173047;
  font-size: clamp(2.15rem, 4vw, 3.3rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.bookings-header p {
  max-width: 620px;
  margin: 16px 0 0;
  color: #6b7480;
  line-height: 1.75;
}

.bookings-header :deep(.el-button--primary),
.booking-actions :deep(.el-button--primary),
.empty-state :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.booking-actions :deep(.el-button.is-plain) {
  --el-button-text-color: #5f685e;
  --el-button-bg-color: rgba(255, 251, 244, 0.86);
  --el-button-border-color: rgba(132, 125, 104, 0.16);
  --el-button-hover-text-color: #355f4d;
  --el-button-hover-bg-color: rgba(255, 252, 246, 1);
  --el-button-hover-border-color: rgba(63, 114, 93, 0.32);
}

.bookings-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.booking-card {
  border: 1px solid rgba(28, 60, 88, 0.12);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 32px rgba(21, 40, 61, 0.06);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.booking-card:hover {
  transform: translateY(-3px);
  border-color: rgba(63, 114, 93, 0.18);
  box-shadow: 0 20px 34px rgba(21, 40, 61, 0.08);
}

.booking-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.booking-card h2 {
  margin: 0;
  color: #173047;
  font-size: 1.22rem;
}

.booking-subtitle {
  margin: 8px 0 0;
  color: #6d7680;
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
  border-top: 1px solid rgba(28, 60, 88, 0.08);
  border-bottom: 1px solid rgba(28, 60, 88, 0.08);
}

.booking-meta span,
.details-grid span {
  display: block;
  color: #7a817f;
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.booking-meta strong,
.details-grid strong {
  display: block;
  margin-top: 6px;
  color: #173047;
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
  border: 1px dashed rgba(63, 114, 93, 0.24);
  border-radius: 28px;
  background: rgba(255, 251, 244, 0.72);
}

.create-alert {
  margin-bottom: 16px;
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
