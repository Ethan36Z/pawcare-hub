<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminDashboardApi } from '@/api/adminDashboard'

const stats = ref({
  totalUsers: 0,
  petRecords: 0,
  activeStaff: 0,
  activeServices: 0,
  totalBookings: 0,
  bookingsByStatus: [],
  topServices: [],
  staffWorkload: [],
  upcomingBookings: [],
  recentCompletedVisits: [],
})
const isLoading = ref(false)
const errorMessage = ref('')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getStatusTagType(status) {
  if (status === 'Completed' || status === 'Confirmed') {
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

function getBookingStatusBadgeClass(status) {
  return [
    'admin-badge',
    `admin-badge--${String(status || 'neutral').trim().toLowerCase() || 'neutral'}`,
  ]
}

const summaryCards = computed(() => [
  {
    label: 'Total users',
    value: stats.value.totalUsers,
    hint: 'Registered client accounts',
  },
  {
    label: 'Pet records',
    value: stats.value.petRecords,
    hint: 'Pets tracked in the clinic system',
  },
  {
    label: 'Active staff',
    value: stats.value.activeStaff,
    hint: 'Currently bookable team members',
  },
  {
    label: 'Active services',
    value: stats.value.activeServices,
    hint: 'Services available for booking',
  },
])

const maxStatusCount = computed(() => {
  const counts = stats.value.bookingsByStatus.map((item) => item.count)
  return counts.length ? Math.max(...counts, 1) : 1
})

const statusChartItems = computed(() => stats.value.bookingsByStatus.map((item) => ({
  ...item,
  percentage: `${Math.max((item.count / maxStatusCount.value) * 100, item.count ? 14 : 0)}%`,
})))
const maxServiceUsage = computed(() => {
  const counts = stats.value.topServices.map((item) => item.count)
  return counts.length ? Math.max(...counts, 1) : 1
})
const maxStaffWorkload = computed(() => {
  const counts = stats.value.staffWorkload.map((item) => item.count)
  return counts.length ? Math.max(...counts, 1) : 1
})
const serviceUsageItems = computed(() => stats.value.topServices.map((item) => ({
  ...item,
  percentage: `${Math.max((item.count / maxServiceUsage.value) * 100, item.count ? 16 : 0)}%`,
})))
const staffWorkloadItems = computed(() => stats.value.staffWorkload.map((item) => ({
  ...item,
  percentage: `${Math.max((item.count / maxStaffWorkload.value) * 100, item.count ? 16 : 0)}%`,
})))

async function loadStats() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await adminDashboardApi.stats()
    stats.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load dashboard stats right now.')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<template>
  <div class="dashboard-page">
    <div class="admin-hero">
      <p>Admin workspace</p>
      <h1>Dashboard</h1>
      <span>Live operational activity from the real PawCare Hub system.</span>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="admin-alert"
    />

    <section v-if="isLoading" class="summary-grid">
      <article
        v-for="index in 4"
        :key="index"
        class="dashboard-skeleton-card"
      >
        <el-skeleton :rows="3" animated />
      </article>
    </section>

    <template v-else>
      <section class="summary-grid">
        <article
          v-for="card in summaryCards"
          :key="card.label"
          class="summary-card"
        >
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <p>{{ card.hint }}</p>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="dashboard-panel">
          <div class="panel-header">
            <div>
              <h2>Booking Status Mix</h2>
              <p>Quick view of how appointments are moving through the clinic workflow.</p>
            </div>
            <strong>{{ stats.totalBookings }} total</strong>
          </div>

          <div class="status-chart">
            <div
              v-for="item in statusChartItems"
              :key="item.status"
              class="status-chart__row"
            >
              <div class="status-chart__label">
                <span>{{ item.status }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="status-chart__track">
                <div
                  class="status-chart__bar"
                  :class="`status-chart__bar--${item.status.toLowerCase()}`"
                  :style="{ width: item.percentage }"
                />
              </div>
            </div>
          </div>
        </article>

        <article class="dashboard-panel">
          <div class="panel-header">
            <div>
              <h2>System Snapshot</h2>
              <p>Practical operating totals from the current database-backed environment.</p>
            </div>
          </div>

          <div class="snapshot-grid">
            <div class="snapshot-card">
              <span>Booking-ready resources</span>
              <strong>{{ stats.activeStaff + stats.activeServices }}</strong>
              <p>{{ stats.activeStaff }} staff and {{ stats.activeServices }} services are currently active.</p>
            </div>
            <div class="snapshot-card">
              <span>Client + pet base</span>
              <strong>{{ stats.totalUsers + stats.petRecords }}</strong>
              <p>{{ stats.totalUsers }} user accounts are managing {{ stats.petRecords }} pet records.</p>
            </div>
          </div>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="dashboard-panel">
          <div class="panel-header">
            <div>
              <h2>Upcoming Bookings</h2>
              <p>Latest upcoming or confirmed visits that still need clinic attention.</p>
            </div>
          </div>

          <div v-if="stats.upcomingBookings.length" class="activity-list">
            <div
              v-for="booking in stats.upcomingBookings"
              :key="booking.id"
              class="activity-row"
            >
              <div>
                <strong>{{ booking.petName }} · {{ booking.service }}</strong>
                <p>{{ booking.date }} at {{ booking.time }} · {{ booking.ownerName }}</p>
              </div>
              <div class="activity-row__meta">
                <span>{{ booking.staff }}</span>
                <el-tag
                  :type="getStatusTagType(booking.status)"
                  effect="plain"
                  :class="getBookingStatusBadgeClass(booking.status)"
                >
                  {{ booking.status }}
                </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="No upcoming bookings to highlight right now." />
        </article>

        <article class="dashboard-panel">
          <div class="panel-header">
            <div>
              <h2>Recent Completed Visits</h2>
              <p>Most recent visits marked complete in the clinic workflow.</p>
            </div>
          </div>

          <div v-if="stats.recentCompletedVisits.length" class="activity-list">
            <div
              v-for="booking in stats.recentCompletedVisits"
              :key="booking.id"
              class="activity-row"
            >
              <div>
                <strong>{{ booking.petName }} · {{ booking.service }}</strong>
                <p>{{ booking.date }} at {{ booking.time }} · {{ booking.ownerName }}</p>
              </div>
              <div class="activity-row__meta">
                <span>{{ booking.staff }}</span>
                <el-tag type="success" effect="plain" class="admin-badge admin-badge--completed">
                  Completed
                </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="No completed visits yet." />
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="dashboard-panel">
          <div class="panel-header">
            <div>
              <h2>Top Services</h2>
              <p>Most frequently booked services in the current dataset.</p>
            </div>
          </div>

          <div v-if="serviceUsageItems.length" class="mini-chart">
            <div
              v-for="item in serviceUsageItems"
              :key="item.label"
              class="mini-chart__row"
            >
              <div class="mini-chart__label">
                <span>{{ item.label }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="mini-chart__track">
                <div class="mini-chart__bar mini-chart__bar--services" :style="{ width: item.percentage }" />
              </div>
            </div>
          </div>
          <el-empty v-else description="No service activity to summarize yet." />
        </article>

        <article class="dashboard-panel">
          <div class="panel-header">
            <div>
              <h2>Staff Workload Snapshot</h2>
              <p>Which staff members are carrying the most bookings right now.</p>
            </div>
          </div>

          <div v-if="staffWorkloadItems.length" class="mini-chart">
            <div
              v-for="item in staffWorkloadItems"
              :key="item.label"
              class="mini-chart__row"
            >
              <div class="mini-chart__label">
                <span>{{ item.label }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="mini-chart__track">
                <div class="mini-chart__bar mini-chart__bar--staff" :style="{ width: item.percentage }" />
              </div>
            </div>
          </div>
          <el-empty v-else description="No staff workload data to summarize yet." />
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.admin-hero {
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(255, 224, 164, 0.2), transparent 30%),
    linear-gradient(135deg, #1f4d7c 0%, #0e7490 100%);
  color: white;
}

.admin-hero p,
.admin-hero span {
  margin: 0;
  opacity: 0.82;
}

.admin-hero h1 {
  margin: 8px 0 10px;
}

.admin-alert {
  margin-top: -4px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.dashboard-skeleton-card,
.dashboard-panel {
  border: 1px solid var(--pc-border);
  border-radius: 24px;
  background: white;
  box-shadow: var(--pc-shadow);
}

.summary-card,
.dashboard-skeleton-card {
  padding: 22px;
}

.summary-card span,
.snapshot-card span {
  display: block;
  color: var(--pc-muted);
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.summary-card strong,
.snapshot-card strong {
  display: block;
  margin-top: 10px;
  color: var(--pc-text);
  font-size: clamp(1.8rem, 3vw, 2.3rem);
  line-height: 1;
}

.summary-card p,
.snapshot-card p,
.panel-header p,
.activity-row p {
  margin: 10px 0 0;
  color: var(--pc-muted);
  line-height: 1.6;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.dashboard-panel {
  padding: 22px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.panel-header h2 {
  margin: 0;
  color: var(--pc-text);
}

.panel-header strong {
  color: #1f4d7c;
}

.status-chart {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.status-chart__row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-chart__label {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--pc-text);
}

.status-chart__track {
  height: 12px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.status-chart__bar {
  height: 100%;
  border-radius: 999px;
}

.status-chart__bar--upcoming {
  background: linear-gradient(90deg, #5da9e9 0%, #3d7bd9 100%);
}

.status-chart__bar--confirmed {
  background: linear-gradient(90deg, #5bbf8a 0%, #2f9a68 100%);
}

.status-chart__bar--completed {
  background: linear-gradient(90deg, #3f725d 0%, #2b5444 100%);
}

.status-chart__bar--cancelled {
  background: linear-gradient(90deg, #f28a8a 0%, #d95d5d 100%);
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.snapshot-card {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff 0%, #f4f8fb 100%);
  border: 1px solid rgba(31, 77, 124, 0.08);
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(248, 251, 255, 0.82);
  border: 1px solid rgba(31, 77, 124, 0.08);
}

.activity-row strong {
  color: var(--pc-text);
}

.activity-row__meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
  text-align: right;
  color: var(--pc-muted);
}

.mini-chart {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mini-chart__row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mini-chart__label {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--pc-text);
}

.mini-chart__track {
  height: 10px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.mini-chart__bar {
  height: 100%;
  border-radius: 999px;
}

.mini-chart__bar--services {
  background: linear-gradient(90deg, #e9b44c 0%, #d48f22 100%);
}

.mini-chart__bar--staff {
  background: linear-gradient(90deg, #4da7a7 0%, #1f7c8c 100%);
}

@media (max-width: 1100px) {
  .summary-grid,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .summary-grid,
  .dashboard-grid,
  .snapshot-grid {
    grid-template-columns: 1fr;
  }

  .panel-header,
  .activity-row {
    flex-direction: column;
  }

  .activity-row__meta {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
