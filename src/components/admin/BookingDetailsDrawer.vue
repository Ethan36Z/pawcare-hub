<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  booking: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue', 'closed'])

const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const bookingTitleId = computed(() => {
  const id = normalizeDisplayValue(props.booking?.id, 'selected')
  return `booking-details-title-${id}`
})

const appointmentFields = computed(() => [
  { label: 'Service', value: props.booking?.service },
  { label: 'Date', value: props.booking?.date },
  { label: 'Time', value: props.booking?.time },
  { label: 'Clinic', value: props.booking?.clinic },
  { label: 'Assigned staff', value: props.booking?.staff },
])

const clientFields = computed(() => [
  { label: 'Pet name', value: props.booking?.petName },
  { label: 'Owner name', value: props.booking?.ownerName },
  { label: 'Owner email', value: props.booking?.ownerEmail },
  { label: 'Owner ID', value: props.booking?.ownerId },
])

const outcomeFields = computed(() => [
  { label: 'Visit summary', value: props.booking?.visitSummary },
  { label: 'Diagnosis or assessment', value: props.booking?.diagnosisAssessment },
  { label: 'Treatment recommendation', value: props.booking?.treatmentRecommendation },
  { label: 'Follow-up note', value: props.booking?.followUpNote },
])

const isCompletedBooking = computed(() => {
  return String(props.booking?.status || '').trim().toLowerCase() === 'completed'
})

const hasOutcomeContent = computed(() => {
  return outcomeFields.value.some((field) => hasDisplayValue(field.value))
})

function closeDrawer() {
  isOpen.value = false
}

function handleClosed() {
  emit('closed')
}

function hasDisplayValue(value) {
  if (value === null || value === undefined) {
    return false
  }

  if (typeof value === 'string') {
    return value.trim().length > 0
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return true
  }

  return false
}

function normalizeDisplayValue(value, fallback = 'Not provided') {
  if (!hasDisplayValue(value)) {
    return fallback
  }

  if (typeof value === 'string') {
    return value.trim()
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }

  return fallback
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

function getStatusBadgeClass(status) {
  return [
    'admin-badge',
    `admin-badge--${String(status || 'neutral').trim().toLowerCase() || 'neutral'}`,
  ]
}
</script>

<template>
  <el-drawer
    v-model="isOpen"
    direction="rtl"
    size="min(520px, 96vw)"
    :with-header="false"
    :append-to-body="true"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    custom-class="booking-details-drawer-shell"
    @closed="handleClosed"
  >
    <aside
      v-if="booking"
      class="booking-details-drawer"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="bookingTitleId"
    >
      <header class="booking-details-drawer__header">
        <div>
          <p class="booking-details-drawer__eyebrow">Booking #{{ normalizeDisplayValue(booking.id) }}</p>
          <h2 :id="bookingTitleId">Booking details</h2>
          <el-tag
            :type="getStatusTagType(booking.status)"
            effect="plain"
            :class="['booking-details-drawer__status', ...getStatusBadgeClass(booking.status)]"
          >
            {{ normalizeDisplayValue(booking.status) }}
          </el-tag>
        </div>
        <el-button
          circle
          plain
          native-type="button"
          aria-label="Close booking details"
          class="booking-details-drawer__close"
          @click="closeDrawer"
        >
          <span aria-hidden="true">&times;</span>
        </el-button>
      </header>

      <div class="booking-details-drawer__content">
        <section class="booking-details-section" aria-labelledby="booking-appointment-heading">
          <h3 id="booking-appointment-heading">Appointment</h3>
          <dl class="booking-details-grid">
            <div v-for="field in appointmentFields" :key="field.label">
              <dt>{{ field.label }}</dt>
              <dd>{{ normalizeDisplayValue(field.value) }}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>
                <el-tag
                  :type="getStatusTagType(booking.status)"
                  effect="plain"
                  :class="getStatusBadgeClass(booking.status)"
                >
                  {{ normalizeDisplayValue(booking.status) }}
                </el-tag>
              </dd>
            </div>
          </dl>
        </section>

        <section class="booking-details-section" aria-labelledby="booking-client-heading">
          <h3 id="booking-client-heading">Client and pet</h3>
          <dl class="booking-details-grid">
            <div v-for="field in clientFields" :key="field.label">
              <dt>{{ field.label }}</dt>
              <dd>{{ normalizeDisplayValue(field.value) }}</dd>
            </div>
          </dl>
        </section>

        <section class="booking-details-section" aria-labelledby="booking-note-heading">
          <h3 id="booking-note-heading">Client note</h3>
          <p :class="['booking-details-note', { 'booking-details-note--empty': !hasDisplayValue(booking.ownerNote) }]">
            {{ normalizeDisplayValue(booking.ownerNote) }}
          </p>
        </section>

        <section
          v-if="isCompletedBooking && hasOutcomeContent"
          class="booking-details-section"
          aria-labelledby="booking-outcome-heading"
        >
          <h3 id="booking-outcome-heading">Visit outcome</h3>
          <dl class="booking-details-stack">
            <div v-for="field in outcomeFields" :key="field.label">
              <template v-if="hasDisplayValue(field.value)">
                <dt>{{ field.label }}</dt>
                <dd>{{ normalizeDisplayValue(field.value) }}</dd>
              </template>
            </div>
          </dl>
        </section>
      </div>
    </aside>
  </el-drawer>
</template>

<style scoped>
.booking-details-drawer {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background: #ffffff;
}

.booking-details-drawer__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  padding: 24px 24px 20px;
  border-bottom: 1px solid var(--pc-border);
  background: var(--pc-surface);
}

.booking-details-drawer__eyebrow {
  margin: 0 0 8px;
  color: var(--pc-soft-muted);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.booking-details-drawer h2 {
  margin: 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.62rem;
  font-weight: 500;
}

.booking-details-drawer__status {
  margin-top: 12px;
}

.booking-details-drawer__close {
  flex: 0 0 auto;
  font-size: 1.24rem;
}

.booking-details-drawer__close:focus-visible {
  outline: 3px solid rgba(201, 100, 66, 0.32);
  outline-offset: 2px;
}

.booking-details-drawer__content {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 18px;
  overflow-y: auto;
  padding: 22px 24px 28px;
}

.booking-details-section {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--pc-border-soft);
}

.booking-details-section:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.booking-details-section h3 {
  margin: 0 0 14px;
  color: var(--pc-ink);
  font-size: 1rem;
}

.booking-details-grid,
.booking-details-stack {
  display: grid;
  gap: 14px;
  margin: 0;
}

.booking-details-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.booking-details-grid div,
.booking-details-stack div {
  min-width: 0;
}

.booking-details-grid dt,
.booking-details-stack dt {
  color: var(--pc-soft-muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.booking-details-grid dd,
.booking-details-stack dd,
.booking-details-note {
  margin: 6px 0 0;
  color: var(--pc-text);
  line-height: 1.55;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.booking-details-stack dd {
  padding: 12px 14px;
  border: 1px solid var(--pc-border-soft);
  border-radius: 12px;
  background: rgba(250, 249, 245, 0.72);
}

.booking-details-note {
  padding: 12px 14px;
  border: 1px solid var(--pc-border-soft);
  border-radius: 12px;
  background: rgba(250, 249, 245, 0.72);
}

.booking-details-note--empty {
  color: var(--pc-muted);
  font-style: italic;
}

@media (max-width: 560px) {
  .booking-details-drawer__header,
  .booking-details-drawer__content {
    padding-left: 18px;
    padding-right: 18px;
  }

  .booking-details-grid {
    grid-template-columns: 1fr;
  }
}
</style>
