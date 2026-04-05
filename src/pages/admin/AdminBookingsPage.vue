<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminBookingsApi } from '@/api/adminBookings'
import { useAuthStore } from '@/stores/auth'

const bookings = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const confirmingBookingId = ref(null)
const cancellingBookingId = ref(null)
const completingBookingId = ref(null)
const isOutcomeDialogOpen = ref(false)
const isSavingOutcome = ref(false)
const outcomeErrorMessage = ref('')
const selectedOutcomeBooking = ref(null)
const outcomeForm = ref({
  visitSummary: '',
  diagnosisAssessment: '',
  treatmentRecommendation: '',
  followUpNote: '',
})
const filters = ref({
  status: '',
  service: '',
  owner: '',
  sort: 'latest',
})
const currentPage = ref(1)
const pageSize = ref(10)
const authStore = useAuthStore()
const canManageBookingQueue = computed(() => authStore.isAdmin || authStore.isFrontDesk)
const canCompleteVisits = computed(() => authStore.isAdmin || authStore.isDoctor)
const paginatedBookings = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  return bookings.value.slice(startIndex, startIndex + pageSize.value)
})

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getStatusTagType(status) {
  if (status === 'Confirmed' || status === 'Completed') {
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

function resetOutcomeForm() {
  selectedOutcomeBooking.value = null
  outcomeErrorMessage.value = ''
  outcomeForm.value = {
    visitSummary: '',
    diagnosisAssessment: '',
    treatmentRecommendation: '',
    followUpNote: '',
  }
}

async function loadBookings() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminBookingsApi.list({
      status: filters.value.status || undefined,
      service: filters.value.service || undefined,
      owner: filters.value.owner || undefined,
      sort: filters.value.sort,
    })
    bookings.value = data
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load admin bookings right now.')
    bookings.value = []
  } finally {
    isLoading.value = false
  }
}

async function handleConfirmBooking(booking) {
  if (!booking?.id) {
    return
  }

  confirmingBookingId.value = booking.id
  errorMessage.value = ''

  try {
    await adminBookingsApi.confirm(booking.id)
    await loadBookings()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to confirm this booking right now.')
  } finally {
    confirmingBookingId.value = null
  }
}

async function handleCancelBooking(booking) {
  if (!booking?.id) {
    return
  }

  cancellingBookingId.value = booking.id
  errorMessage.value = ''

  try {
    await adminBookingsApi.cancel(booking.id)
    await loadBookings()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to cancel this booking right now.')
  } finally {
    cancellingBookingId.value = null
  }
}

function openOutcomeDialog(booking) {
  selectedOutcomeBooking.value = booking
  outcomeErrorMessage.value = ''
  outcomeForm.value = {
    visitSummary: booking.visitSummary || '',
    diagnosisAssessment: booking.diagnosisAssessment || '',
    treatmentRecommendation: booking.treatmentRecommendation || '',
    followUpNote: booking.followUpNote || '',
  }
  isOutcomeDialogOpen.value = true
}

async function handleSaveOutcome() {
  if (!selectedOutcomeBooking.value?.id) {
    return
  }

  isSavingOutcome.value = true
  completingBookingId.value = selectedOutcomeBooking.value.id
  outcomeErrorMessage.value = ''

  try {
    await adminBookingsApi.complete(selectedOutcomeBooking.value.id, outcomeForm.value)
    isOutcomeDialogOpen.value = false
    resetOutcomeForm()
    await loadBookings()
  } catch (error) {
    outcomeErrorMessage.value = getApiErrorMessage(error, 'Unable to save the visit outcome right now.')
  } finally {
    isSavingOutcome.value = false
    completingBookingId.value = null
  }
}

function handleApplyFilters() {
  loadBookings()
}

function handleResetFilters() {
  filters.value = {
    status: '',
    service: '',
    owner: '',
    sort: 'latest',
  }
  loadBookings()
}

onMounted(() => {
  loadBookings()
})
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__header">
      <div>
        <h1>Admin Bookings</h1>
        <p>Live booking records from the MySQL-backed bookings table.</p>
      </div>
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
        v-model="filters.status"
        placeholder="Status"
        clearable
        class="admin-filters__control"
      >
        <el-option label="Upcoming" value="Upcoming" />
        <el-option label="Confirmed" value="Confirmed" />
        <el-option label="Completed" value="Completed" />
        <el-option label="Cancelled" value="Cancelled" />
      </el-select>

      <el-input
        v-model="filters.service"
        placeholder="Filter by service"
        clearable
        class="admin-filters__control"
      />

      <el-input
        v-model="filters.owner"
        placeholder="Filter by owner"
        clearable
        class="admin-filters__control"
      />

      <el-select
        v-model="filters.sort"
        class="admin-filters__control"
      >
        <el-option label="Latest first" value="latest" />
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
        v-else-if="!bookings.length"
        description="No bookings match the current filters."
      />

      <template v-else>
        <el-table :data="paginatedBookings" stripe>
          <el-table-column prop="id" label="ID" min-width="80" />
          <el-table-column prop="petName" label="Pet" min-width="140" />
          <el-table-column prop="service" label="Service" min-width="180" />
          <el-table-column label="Schedule" min-width="190">
            <template #default="{ row }">
              {{ row.date }} at {{ row.time }}
            </template>
          </el-table-column>
          <el-table-column label="Status" min-width="130">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="staff" label="Staff" min-width="160" />
          <el-table-column prop="clinic" label="Clinic" min-width="180" />
          <el-table-column label="Owner" min-width="240">
            <template #default="{ row }">
              <div class="owner-cell">
                <strong>{{ row.ownerName }}</strong>
                <span>{{ row.ownerEmail }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Client message" min-width="260">
            <template #default="{ row }">
              <div v-if="row.ownerNote" class="owner-note-cell">
                {{ row.ownerNote }}
              </div>
              <span v-else class="owner-note-cell owner-note-cell--empty">No message</span>
            </template>
          </el-table-column>
          <el-table-column label="Actions" min-width="220" fixed="right">
            <template #default="{ row }">
              <div class="actions-cell actions-cell--booking">
                <el-button
                  v-if="canManageBookingQueue && row.status !== 'Confirmed' && row.status !== 'Cancelled' && row.status !== 'Completed'"
                  plain
                  size="small"
                  class="actions-cell__button"
                  :loading="confirmingBookingId === row.id"
                  @click="handleConfirmBooking(row)"
                >
                  Confirm
                </el-button>
                <el-button
                  v-if="canCompleteVisits && row.status !== 'Cancelled'"
                  plain
                  size="small"
                  class="actions-cell__button"
                  :loading="completingBookingId === row.id"
                  @click="openOutcomeDialog(row)"
                >
                  {{ row.status === 'Completed' ? 'Edit Outcome' : 'Complete Visit' }}
                </el-button>
                <el-button
                  v-if="canManageBookingQueue && row.status !== 'Cancelled' && row.status !== 'Completed'"
                  plain
                  size="small"
                  class="actions-cell__button actions-cell__button--danger"
                  :loading="cancellingBookingId === row.id"
                  @click="handleCancelBooking(row)"
                >
                  Cancel
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="bookings.length > pageSize" class="admin-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="bookings.length"
            :page-sizes="[10, 20, 50]"
          />
        </div>
      </template>
    </div>

    <el-dialog
      v-model="isOutcomeDialogOpen"
      title="Visit Outcome"
      width="min(640px, 94vw)"
      @closed="resetOutcomeForm"
    >
      <el-alert
        v-if="outcomeErrorMessage"
        :title="outcomeErrorMessage"
        type="error"
        :closable="false"
        class="admin-page__alert"
      />

      <div v-if="selectedOutcomeBooking" class="outcome-summary">
        <strong>{{ selectedOutcomeBooking.petName }}</strong>
        <span>{{ selectedOutcomeBooking.service }} | {{ selectedOutcomeBooking.date }} at {{ selectedOutcomeBooking.time }}</span>
        <p v-if="selectedOutcomeBooking.ownerNote" class="outcome-summary__note">
          Client message: {{ selectedOutcomeBooking.ownerNote }}
        </p>
      </div>

      <el-form :model="outcomeForm" label-position="top">
        <el-form-item label="Visit summary">
          <el-input
            v-model="outcomeForm.visitSummary"
            type="textarea"
            :rows="3"
            placeholder="Brief summary of how the visit went"
          />
        </el-form-item>
        <el-form-item label="Diagnosis or assessment">
          <el-input
            v-model="outcomeForm.diagnosisAssessment"
            type="textarea"
            :rows="3"
            placeholder="Clinical impression or assessment"
          />
        </el-form-item>
        <el-form-item label="Treatment or recommendation">
          <el-input
            v-model="outcomeForm.treatmentRecommendation"
            type="textarea"
            :rows="3"
            placeholder="Treatment provided or recommended next steps"
          />
        </el-form-item>
        <el-form-item label="Follow-up note">
          <el-input
            v-model="outcomeForm.followUpNote"
            type="textarea"
            :rows="3"
            placeholder="Optional follow-up reminder or note"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isOutcomeDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="isSavingOutcome" @click="handleSaveOutcome">
          Save and Mark Complete
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

.owner-cell,
.actions-cell {
  display: flex;
}

.outcome-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid var(--pc-border);
  border-radius: 16px;
  background: rgba(255, 251, 244, 0.72);
}

.outcome-summary span {
  color: var(--pc-muted);
  font-size: 0.95rem;
}

.outcome-summary__note {
  margin: 8px 0 0;
  color: var(--pc-text);
  line-height: 1.5;
}

.owner-cell {
  flex-direction: column;
  line-height: 1.35;
}

.owner-cell strong {
  color: var(--pc-text);
}

.owner-cell span {
  color: var(--pc-muted);
  word-break: break-word;
}

.owner-note-cell {
  color: var(--pc-text);
  line-height: 1.45;
  white-space: normal;
  word-break: break-word;
}

.owner-note-cell--empty {
  color: var(--pc-muted);
}

.actions-cell {
  flex-wrap: wrap;
  gap: 8px;
}

.actions-cell--booking {
  flex-direction: column;
  align-items: stretch;
  min-width: 150px;
}

.actions-cell--booking .el-button + .el-button {
  margin-left: 0;
}

.actions-cell__button {
  justify-content: center;
  width: 100%;
}

.actions-cell__button--danger {
  --el-button-text-color: #b42318;
  --el-button-border-color: rgba(180, 35, 24, 0.28);
  --el-button-hover-text-color: #ffffff;
  --el-button-hover-bg-color: #b42318;
  --el-button-hover-border-color: #b42318;
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

@media (max-width: 1100px) {
  .admin-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .admin-filters {
    grid-template-columns: 1fr;
  }
}
</style>
