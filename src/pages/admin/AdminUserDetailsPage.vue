<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usersApi } from '@/api/users'

const route = useRoute()
const router = useRouter()

const user = ref(null)
const isLoading = ref(false)
const errorMessage = ref('')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getStatusTagType(status) {
  if (status === 'Healthy' || status === 'Confirmed') {
    return 'success'
  }

  if (status === 'Needs Attention' || status === 'Upcoming') {
    return 'warning'
  }

  if (status === 'Cancelled') {
    return 'danger'
  }

  return 'info'
}

const reminderItems = computed(() => {
  if (!user.value) {
    return []
  }

  return [
    {
      label: 'Email reminders',
      value: user.value.emailRemindersEnabled ? 'Enabled' : 'Disabled',
    },
    {
      label: 'Text reminders',
      value: user.value.textRemindersEnabled ? 'Enabled' : 'Disabled',
    },
  ]
})

const profileItems = computed(() => {
  if (!user.value) {
    return []
  }

  return [
    { label: 'Role', value: user.value.role || 'User' },
    { label: 'Phone', value: user.value.phone || 'Not provided' },
    { label: 'Address', value: user.value.address || 'Not provided' },
    {
      label: 'Preferred contact',
      value: user.value.preferredContactMethod || 'Not set',
    },
  ]
})

async function loadUserDetails() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await usersApi.getById(route.params.id)
    user.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load this user right now.')
    user.value = null
  } finally {
    isLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    loadUserDetails()
  }
)

onMounted(() => {
  loadUserDetails()
})
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__header">
      <div>
        <el-button plain @click="router.push({ name: 'admin-users' })">
          Back to users
        </el-button>
        <h1>User Details</h1>
        <p>Real profile, pets, and recent booking activity from the database.</p>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="admin-page__alert"
    />

    <el-skeleton v-if="isLoading" :rows="8" animated />

    <el-empty
      v-else-if="!user"
      description="User details are not available."
    />

    <div v-else class="details-layout">
      <section class="details-card details-card--hero">
        <p class="details-card__eyebrow">User #{{ user.id }}</p>
        <h2>{{ user.name }}</h2>
        <span>{{ user.email }}</span>
      </section>

      <section class="details-grid">
        <article class="details-card">
          <h3>Profile</h3>
          <dl class="details-list">
            <div v-for="item in profileItems" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </article>

        <article class="details-card">
          <h3>Overview</h3>
          <div class="stats-grid">
            <div class="stat-tile">
              <strong>{{ user.petCount }}</strong>
              <span>Pets</span>
            </div>
            <div class="stat-tile">
              <strong>{{ user.bookingCount }}</strong>
              <span>Bookings</span>
            </div>
          </div>
          <dl class="details-list details-list--compact">
            <div v-for="item in reminderItems" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </article>
      </section>

      <section class="details-card">
        <div class="section-header">
          <div>
            <h3>Pets</h3>
            <p>Current pets connected to this user account.</p>
          </div>
        </div>

        <el-empty
          v-if="!user.pets.length"
          description="No pets found for this user."
        />

        <el-table v-else :data="user.pets" stripe>
          <el-table-column prop="id" label="ID" min-width="80" />
          <el-table-column prop="name" label="Name" min-width="160" />
          <el-table-column prop="species" label="Species" min-width="140" />
          <el-table-column prop="breed" label="Breed" min-width="180" />
          <el-table-column label="Status" min-width="140">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="details-card">
        <div class="section-header">
          <div>
            <h3>Recent Bookings</h3>
            <p>Latest booking activity for this user.</p>
          </div>
        </div>

        <el-empty
          v-if="!user.recentBookings.length"
          description="No bookings found for this user."
        />

        <el-table v-else :data="user.recentBookings" stripe>
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
        </el-table>
      </section>
    </div>
  </section>
</template>

<style scoped>
.admin-page {
  padding: 28px;
  border-radius: 24px;
  background: white;
  border: 1px solid var(--pc-border);
}

.admin-page__header {
  margin-bottom: 20px;
}

.admin-page__header h1 {
  margin: 14px 0 8px;
}

.admin-page__header p {
  margin: 0;
  color: var(--pc-muted);
}

.admin-page__alert {
  margin-bottom: 16px;
}

.details-layout {
  display: grid;
  gap: 20px;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.details-card {
  padding: 22px;
  border-radius: 20px;
  background: var(--pc-surface);
  border: 1px solid var(--pc-border);
}

.details-card--hero {
  background: linear-gradient(135deg, #1f4d7c 0%, #0e7490 100%);
  color: white;
}

.details-card--hero h2,
.details-card--hero p,
.details-card--hero span {
  margin: 0;
}

.details-card--hero h2 {
  margin-top: 10px;
  margin-bottom: 8px;
}

.details-card__eyebrow {
  opacity: 0.78;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.78rem;
}

.details-card h3 {
  margin-top: 0;
  margin-bottom: 16px;
}

.details-list {
  display: grid;
  gap: 14px;
  margin: 0;
}

.details-list--compact {
  margin-top: 18px;
}

.details-list div {
  display: grid;
  gap: 4px;
}

.details-list dt {
  color: var(--pc-muted);
  font-size: 0.92rem;
}

.details-list dd {
  margin: 0;
  color: var(--pc-text);
  word-break: break-word;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.stat-tile {
  padding: 16px;
  border-radius: 16px;
  background: white;
  border: 1px solid var(--pc-border);
  display: grid;
  gap: 4px;
}

.stat-tile strong {
  font-size: 1.5rem;
}

.stat-tile span,
.section-header p {
  color: var(--pc-muted);
}

.section-header {
  margin-bottom: 16px;
}

.section-header h3,
.section-header p {
  margin: 0;
}

.section-header h3 {
  margin-bottom: 6px;
}

@media (max-width: 900px) {
  .details-grid {
    grid-template-columns: 1fr;
  }
}
</style>
