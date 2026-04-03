<script setup>
import { computed, onMounted, ref } from 'vue'
import { bookingsApi } from '@/api/bookings'
import PageContainer from '@/components/common/PageContainer.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const bookings = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const isCreateDialogOpen = ref(false)
const isCreating = ref(false)
const createErrorMessage = ref('')
const isCancellingBookingId = ref(null)
const createForm = ref({
  petName: '',
  service: '',
  date: '',
  time: '',
  status: 'Upcoming',
  clinic: 'PawCare Hub Clinic',
  staff: '',
})
const ownerName = computed(() => authStore.user?.fullName || 'your account')

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

onMounted(() => {
  loadBookings()
})

function openCreateDialog() {
  createErrorMessage.value = ''
  isCreateDialogOpen.value = true
}

function resetCreateForm() {
  createForm.value = {
    petName: '',
    service: '',
    date: '',
    time: '',
    status: 'Upcoming',
    clinic: 'PawCare Hub Clinic',
    staff: '',
  }
}

async function handleCreateBooking() {
  if (!authStore.user?.email) {
    return
  }

  isCreating.value = true
  createErrorMessage.value = ''

  try {
    await bookingsApi.create(authStore.user.email, createForm.value)
    isCreateDialogOpen.value = false
    resetCreateForm()
    await loadBookings()
  } catch (error) {
    createErrorMessage.value = getApiErrorMessage(error, 'Unable to create a booking right now.')
  } finally {
    isCreating.value = false
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
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to cancel this booking right now.')
  } finally {
    isCancellingBookingId.value = null
  }
}
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
            <el-button type="primary">View Details</el-button>
            <el-button plain>Reschedule</el-button>
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
            <el-input v-model="createForm.service" placeholder="Service" />
          </el-form-item>
          <el-form-item label="Date">
            <el-input v-model="createForm.date" placeholder="e.g. May 10, 2026" />
          </el-form-item>
          <el-form-item label="Time">
            <el-input v-model="createForm.time" placeholder="e.g. 10:30 AM" />
          </el-form-item>
          <el-form-item label="Status">
            <el-input v-model="createForm.status" placeholder="Upcoming" />
          </el-form-item>
          <el-form-item label="Clinic">
            <el-input v-model="createForm.clinic" placeholder="Clinic name" />
          </el-form-item>
          <el-form-item label="Staff">
            <el-input v-model="createForm.staff" placeholder="Assigned staff" />
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

.booking-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
  padding: 16px 0;
  border-top: 1px solid rgba(28, 60, 88, 0.08);
  border-bottom: 1px solid rgba(28, 60, 88, 0.08);
}

.booking-meta span {
  display: block;
  color: #7a817f;
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.booking-meta strong {
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

  .booking-meta {
    grid-template-columns: 1fr;
  }

  .booking-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
