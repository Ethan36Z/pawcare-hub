<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { adminBookingsApi } from '@/api/adminBookings'
import BookingDetailsDrawer from '@/components/admin/BookingDetailsDrawer.vue'
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
const isDetailsDrawerOpen = ref(false)
const selectedDetailsBooking = ref(null)
const openActionMenuId = ref(null)
const openActionMenuBooking = ref(null)
const actionMenuStyle = ref({})
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
const activeActionMenuItems = computed(() => {
  const booking = openActionMenuBooking.value
  const items = []

  if (!booking) {
    return items
  }

  if (canConfirmBooking(booking)) {
    items.push({ key: 'confirm', label: 'Confirm' })
  }

  if (canOpenOutcomeDialog(booking)) {
    items.push({
      key: 'outcome',
      label: getOutcomeActionLabel(booking),
    })
  }

  if (canCancelBooking(booking)) {
    items.push({
      key: 'cancel',
      label: 'Cancel booking',
      danger: true,
      separated: items.length > 0,
    })
  }

  return items
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

function isBookingStatus(booking, status) {
  return booking?.status === status
}

function canConfirmBooking(booking) {
  return canManageBookingQueue.value
    && !isBookingStatus(booking, 'Confirmed')
    && !isBookingStatus(booking, 'Cancelled')
    && !isBookingStatus(booking, 'Completed')
}

function canOpenOutcomeDialog(booking) {
  return canCompleteVisits.value && !isBookingStatus(booking, 'Cancelled')
}

function canCancelBooking(booking) {
  return canManageBookingQueue.value
    && !isBookingStatus(booking, 'Cancelled')
    && !isBookingStatus(booking, 'Completed')
}

function shouldShowActionsMenu(booking) {
  return canConfirmBooking(booking) || canOpenOutcomeDialog(booking) || canCancelBooking(booking)
}

function getOutcomeActionLabel(booking) {
  return isBookingStatus(booking, 'Completed') ? 'Edit outcome' : 'Complete visit'
}

function getActionMenuId(booking) {
  return `booking-actions-menu-${booking?.id ?? 'unknown'}`
}

function closeActionMenu() {
  openActionMenuId.value = null
  openActionMenuBooking.value = null
  actionMenuStyle.value = {}
}

async function toggleActionMenu(booking, event) {
  if (!booking?.id) {
    return
  }

  if (openActionMenuId.value === booking.id) {
    closeActionMenu()
    return
  }

  openActionMenuId.value = booking.id
  openActionMenuBooking.value = booking
  await nextTick()
  positionActionMenu(event?.currentTarget)
}

function positionActionMenu(anchorElement) {
  if (!anchorElement) {
    return
  }

  const rect = anchorElement.getBoundingClientRect()
  const menuWidth = 184
  const menuGap = 6
  const menuPadding = 8
  const estimatedMenuHeight = Math.max(46, (activeActionMenuItems.value.length * 38) + 14)
  const hasRoomBelow = window.innerHeight - rect.bottom >= estimatedMenuHeight + menuGap + menuPadding
  const shouldOpenAbove = !hasRoomBelow && rect.top > estimatedMenuHeight + menuGap
  const top = shouldOpenAbove
    ? Math.max(menuPadding, rect.top - estimatedMenuHeight - menuGap)
    : Math.min(rect.bottom + menuGap, window.innerHeight - estimatedMenuHeight - menuPadding)
  const left = Math.min(
    Math.max(menuPadding, rect.right - menuWidth),
    window.innerWidth - menuWidth - menuPadding,
  )

  actionMenuStyle.value = {
    top: `${Math.max(menuPadding, top)}px`,
    left: `${left}px`,
    width: `${menuWidth}px`,
  }
}

function handleActionMenuKeydown(event) {
  if (event.key === 'Escape') {
    closeActionMenu()
  }
}

function handleDocumentClick() {
  closeActionMenu()
}

function handleViewportChange() {
  closeActionMenu()
}

function handleActionMenuItem(item) {
  const booking = openActionMenuBooking.value

  if (!booking) {
    closeActionMenu()
    return
  }

  if (item.key === 'confirm') {
    handleConfirmBooking(booking)
    return
  }

  if (item.key === 'outcome') {
    openOutcomeDialog(booking)
    return
  }

  if (item.key === 'cancel') {
    handleCancelBooking(booking)
  }
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

function openDetailsDrawer(booking) {
  closeActionMenu()
  selectedDetailsBooking.value = booking
  isDetailsDrawerOpen.value = true
}

function resetDetailsDrawer() {
  selectedDetailsBooking.value = null
}

async function loadBookings() {
  closeActionMenu()
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

  closeActionMenu()
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

  closeActionMenu()
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
  closeActionMenu()
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
  closeActionMenu()
  loadBookings()
}

function handleResetFilters() {
  closeActionMenu()
  filters.value = {
    status: '',
    service: '',
    owner: '',
    sort: 'latest',
  }
  loadBookings()
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleActionMenuKeydown)
  window.addEventListener('resize', handleViewportChange)
  window.addEventListener('scroll', handleViewportChange, true)
  loadBookings()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleActionMenuKeydown)
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('scroll', handleViewportChange, true)
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
          <el-table-column label="Actions" min-width="190" fixed="right" class-name="bookings-actions-column">
            <template #default="{ row }">
              <div class="actions-cell actions-cell--booking">
                <el-button
                  plain
                  size="small"
                  native-type="button"
                  class="booking-action-button booking-action-button--secondary"
                  @click="openDetailsDrawer(row)"
                >
                  Details
                </el-button>
                <button
                  v-if="shouldShowActionsMenu(row)"
                  type="button"
                  class="booking-action-button booking-action-button--primary booking-actions-trigger"
                  aria-label="Booking actions"
                  aria-haspopup="menu"
                  :aria-expanded="openActionMenuId === row.id"
                  :aria-controls="getActionMenuId(row)"
                  @click.stop="toggleActionMenu(row, $event)"
                >
                  <span>Actions</span>
                  <span aria-hidden="true" class="booking-actions-trigger__chevron"></span>
                </button>
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

    <BookingDetailsDrawer
      v-model="isDetailsDrawerOpen"
      :booking="selectedDetailsBooking"
      @closed="resetDetailsDrawer"
    />

    <Teleport to="body">
      <div
        v-if="openActionMenuBooking && activeActionMenuItems.length"
        :id="getActionMenuId(openActionMenuBooking)"
        class="booking-action-menu__panel"
        role="menu"
        aria-label="Booking actions"
        :style="actionMenuStyle"
        @click.stop
      >
        <button
          v-for="item in activeActionMenuItems"
          :key="item.key"
          type="button"
          :class="[
            'booking-action-menu__item',
            {
              'booking-action-menu__item--danger': item.danger,
              'booking-action-menu__item--separated': item.separated,
            },
          ]"
          role="menuitem"
          @click="handleActionMenuItem(item)"
        >
          {{ item.label }}
        </button>
      </div>
    </Teleport>
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
  position: relative;
  align-items: center;
  gap: 7px;
  min-width: 160px;
}

.actions-cell--booking .el-button + .el-button {
  margin-left: 0;
}

.booking-action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 8px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.actions-cell--booking .booking-action-button--secondary {
  width: 76px;
}

.booking-action-button--primary {
  width: 90px;
  border: 1px solid #1f3f5b;
  font: inherit;
  cursor: pointer;
  --el-button-text-color: #ffffff;
  --el-button-bg-color: #1f3f5b;
  --el-button-border-color: #1f3f5b;
  --el-button-hover-text-color: #ffffff;
  --el-button-hover-bg-color: #173149;
  --el-button-hover-border-color: #173149;
  --el-button-active-bg-color: #102539;
  --el-button-active-border-color: #102539;
  box-shadow: 0 8px 16px rgba(31, 63, 91, 0.14);
}

.booking-action-button--primary:hover {
  background: #173149;
  border-color: #173149;
}

.booking-action-button--secondary {
  --el-button-text-color: #26364a;
  --el-button-bg-color: #ffffff;
  --el-button-border-color: #c8d2dd;
  --el-button-hover-text-color: #173149;
  --el-button-hover-bg-color: #f3f7fb;
  --el-button-hover-border-color: #8fa3b7;
  --el-button-active-text-color: #102539;
  --el-button-active-bg-color: #eaf1f7;
  --el-button-active-border-color: #7890a8;
}

.actions-cell--booking :deep(.el-button:focus-visible),
.booking-actions-trigger:focus-visible {
  outline: 3px solid rgba(31, 63, 91, 0.28);
  outline-offset: 2px;
}

.booking-actions-trigger {
  align-items: center;
  gap: 7px;
  background: #1f3f5b;
  color: #ffffff;
}

.booking-actions-trigger__chevron {
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid currentColor;
  transform: translateY(1px);
}

.booking-action-menu__item:focus-visible {
  outline: 3px solid rgba(31, 63, 91, 0.28);
  outline-offset: 2px;
}

.booking-action-menu__panel {
  position: fixed;
  z-index: 3000;
  padding: 6px;
  border: 1px solid #d6dee8;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 30px rgba(25, 42, 58, 0.14);
}

.booking-action-menu__item {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 36px;
  padding: 8px 10px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #26364a;
  font: inherit;
  font-size: 0.9rem;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}

.booking-action-menu__item:hover {
  background: #f3f7fb;
  color: #173149;
}

.booking-action-menu__item--danger {
  color: #b42318;
}

.booking-action-menu__item--separated {
  margin-top: 5px;
  border-top: 1px solid #eef1f5;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
  padding-top: 10px;
}

.booking-action-menu__item--danger:hover {
  background: #fff1f0;
  color: #912018;
}

.booking-action-menu__item:disabled {
  cursor: wait;
  opacity: 0.62;
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
