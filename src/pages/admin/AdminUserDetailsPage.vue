<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { usersApi } from '@/api/users'

const route = useRoute()
const router = useRouter()

const user = ref(null)
const isLoading = ref(false)
const isUpdatingAccountStatus = ref(false)
const isUpdatingRole = ref(false)
const isDeactivateDialogOpen = ref(false)
const isReactivateDialogOpen = ref(false)
const isRoleDialogOpen = ref(false)
const errorMessage = ref('')
const selectedRole = ref('user')

const roleOptions = [
  { label: 'User', value: 'user' },
  { label: 'Doctor', value: 'doctor' },
  { label: 'Front desk', value: 'front_desk' },
  { label: 'Administrator', value: 'admin' },
]

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

function getAccountStatusTagType(isActive) {
  return isActive ? 'success' : 'danger'
}

function getStatusBadgeClass(status) {
  const normalizedStatus = String(status || 'neutral').trim().toLowerCase().replace(/\s+/g, '-')
  if (normalizedStatus === 'needs-attention') {
    return ['admin-badge', 'admin-badge--warning']
  }

  return ['admin-badge', `admin-badge--${normalizedStatus}`]
}

function getAccountStatusBadgeClass(isActive) {
  return ['admin-badge', isActive ? 'admin-badge--active' : 'admin-badge--deactivated']
}

function getRoleLabel(role) {
  return roleOptions.find((option) => option.value === role)?.label || 'User'
}

function getRoleBadgeClass(role) {
  const normalizedRole = String(role || 'user').toLowerCase().replace('_', '-')
  return ['admin-badge', `admin-badge--${normalizedRole}`]
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
    { label: 'Account status', value: user.value.active ? 'Active' : 'Deactivated' },
    { label: 'Phone', value: user.value.phone || 'Not provided' },
    { label: 'Address', value: user.value.address || 'Not provided' },
    {
      label: 'Preferred contact',
      value: user.value.preferredContactMethod || 'Not set',
    },
  ]
})

const accountStatusExplanation = computed(() => {
  if (!user.value) {
    return ''
  }

  return user.value.active
    ? 'This user can sign in and access their account.'
    : 'This user cannot sign in or access protected account features. Historical records remain available.'
})

const canSaveRole = computed(() => user.value && selectedRole.value !== user.value.role && !isUpdatingRole.value)

const roleConfirmationText = computed(() => {
  if (selectedRole.value === 'admin') {
    return 'This grants full administrative access to PawCareHub, including user management, role management, services, staff, and clinic operations.'
  }

  return 'The user may need to sign in again or refresh their session before the new access is reflected in the app.'
})

async function loadUserDetails() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await usersApi.getById(route.params.id)
    user.value = data
    selectedRole.value = data.role || 'user'
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load this user right now.')
    user.value = null
  } finally {
    isLoading.value = false
  }
}

function openDeactivateDialog() {
  errorMessage.value = ''
  isDeactivateDialogOpen.value = true
}

function openReactivateDialog() {
  errorMessage.value = ''
  isReactivateDialogOpen.value = true
}

function openRoleDialog() {
  if (!canSaveRole.value) {
    return
  }

  errorMessage.value = ''
  isRoleDialogOpen.value = true
}

async function handleDeactivateAccount() {
  if (!user.value || isUpdatingAccountStatus.value) {
    return
  }

  isUpdatingAccountStatus.value = true
  errorMessage.value = ''

  try {
    const { data } = await usersApi.deactivate(user.value.id)
    user.value = data
    isDeactivateDialogOpen.value = false
    ElMessage.success('Account deactivated.')
    await loadUserDetails()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to deactivate this account right now.')
  } finally {
    isUpdatingAccountStatus.value = false
  }
}

async function handleReactivateAccount() {
  if (!user.value || isUpdatingAccountStatus.value) {
    return
  }

  isUpdatingAccountStatus.value = true
  errorMessage.value = ''

  try {
    const { data } = await usersApi.reactivate(user.value.id)
    user.value = data
    isReactivateDialogOpen.value = false
    ElMessage.success('Account reactivated.')
    await loadUserDetails()
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to reactivate this account right now.')
  } finally {
    isUpdatingAccountStatus.value = false
  }
}

async function handleSaveRole() {
  if (!user.value || isUpdatingRole.value) {
    return
  }

  isUpdatingRole.value = true
  errorMessage.value = ''

  try {
    const { data } = await usersApi.updateRole(user.value.id, selectedRole.value)
    user.value = data
    selectedRole.value = data.role || 'user'
    isRoleDialogOpen.value = false
    ElMessage.success('Access role updated.')
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to update this access role right now.')
  } finally {
    isUpdatingRole.value = false
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
        <el-button plain class="admin-button admin-button--ghost" @click="router.push({ name: 'admin-users' })">
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
      <section
        class="details-card details-card--hero"
        :class="{ 'details-card--hero-inactive': !user.active }"
      >
        <p class="details-card__eyebrow">User #{{ user.id }}</p>
        <h2>{{ user.name }}</h2>
        <span>{{ user.email }}</span>
        <div class="hero-status">
          <el-tag
            :type="getAccountStatusTagType(user.active)"
            effect="plain"
            :class="getAccountStatusBadgeClass(user.active)"
          >
            {{ user.active ? 'Active account' : 'Deactivated account' }}
          </el-tag>
        </div>
        <p v-if="!user.active" class="hero-status-message">
          This user cannot sign in or access protected account features.
        </p>
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

      <section class="details-card account-access-card">
        <div class="section-header section-header--split">
          <div>
            <h3>Account access</h3>
            <p>{{ accountStatusExplanation }}</p>
          </div>
          <el-tag
            :type="getAccountStatusTagType(user.active)"
            effect="plain"
            :class="getAccountStatusBadgeClass(user.active)"
          >
            {{ user.active ? 'Active' : 'Deactivated' }}
          </el-tag>
        </div>

        <div class="account-access-card__actions">
          <el-button
            v-if="user.active"
            class="admin-button admin-button--danger"
            :loading="isUpdatingAccountStatus"
            @click="openDeactivateDialog"
          >
            Deactivate account
          </el-button>
          <el-button
            v-else
            type="primary"
            class="admin-button admin-button--primary"
            :loading="isUpdatingAccountStatus"
            @click="openReactivateDialog"
          >
            Reactivate account
          </el-button>
        </div>
      </section>

      <section class="details-card access-role-card">
        <div class="section-header section-header--split">
          <div>
            <h3>Access role</h3>
            <p>
              Access roles control which PawCareHub pages and operations this account can use.
              Staff profiles and scheduling are managed separately.
            </p>
          </div>
          <el-tag effect="plain" :class="getRoleBadgeClass(user.role)">
            {{ getRoleLabel(user.role) }}
          </el-tag>
        </div>

        <div class="access-role-card__form">
          <el-select v-model="selectedRole" class="admin-control access-role-card__select">
            <el-option
              v-for="option in roleOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-button
            type="primary"
            class="admin-button admin-button--primary"
            :disabled="!canSaveRole"
            :loading="isUpdatingRole"
            @click="openRoleDialog"
          >
            Save role
          </el-button>
        </div>
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
              <el-tag :type="getStatusTagType(row.status)" effect="plain" :class="getStatusBadgeClass(row.status)">
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
              <el-tag :type="getStatusTagType(row.status)" effect="plain" :class="getStatusBadgeClass(row.status)">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="staff" label="Staff" min-width="160" />
          <el-table-column prop="clinic" label="Clinic" min-width="180" />
        </el-table>
      </section>

      <el-dialog
        v-model="isDeactivateDialogOpen"
        title="Deactivate account"
        width="520px"
        :close-on-click-modal="!isUpdatingAccountStatus"
        :close-on-press-escape="!isUpdatingAccountStatus"
      >
        <div v-if="user" class="account-status-dialog">
          <p>
            Deactivate access for <strong>{{ user.name }}</strong> at <strong>{{ user.email }}</strong>?
          </p>
          <ul>
            <li>This user will no longer be able to sign in.</li>
            <li>Pets, bookings, and historical records will not be deleted.</li>
            <li>This action can be reversed later.</li>
          </ul>
        </div>
        <template #footer>
          <el-button :disabled="isUpdatingAccountStatus" @click="isDeactivateDialogOpen = false">
            Cancel
          </el-button>
          <el-button
            type="danger"
            class="admin-button admin-button--danger"
            :loading="isUpdatingAccountStatus"
            @click="handleDeactivateAccount"
          >
            Deactivate account
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="isReactivateDialogOpen"
        title="Reactivate account"
        width="480px"
        :close-on-click-modal="!isUpdatingAccountStatus"
        :close-on-press-escape="!isUpdatingAccountStatus"
      >
        <div v-if="user" class="account-status-dialog">
          <p>
            Reactivate access for <strong>{{ user.name }}</strong> at <strong>{{ user.email }}</strong>?
          </p>
          <p>They will be able to sign in and use protected account features again.</p>
        </div>
        <template #footer>
          <el-button :disabled="isUpdatingAccountStatus" @click="isReactivateDialogOpen = false">
            Cancel
          </el-button>
          <el-button
            type="primary"
            class="admin-button admin-button--primary"
            :loading="isUpdatingAccountStatus"
            @click="handleReactivateAccount"
          >
            Reactivate account
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="isRoleDialogOpen"
        title="Change access role"
        width="540px"
        :close-on-click-modal="!isUpdatingRole"
        :close-on-press-escape="!isUpdatingRole"
      >
        <div v-if="user" class="account-status-dialog">
          <p>
            Change access for <strong>{{ user.name }}</strong> at
            <strong>{{ user.email }}</strong>?
          </p>
          <dl class="role-change-summary">
            <div>
              <dt>Current role</dt>
              <dd>{{ getRoleLabel(user.role) }}</dd>
            </div>
            <div>
              <dt>New role</dt>
              <dd>{{ getRoleLabel(selectedRole) }}</dd>
            </div>
          </dl>
          <p>{{ roleConfirmationText }}</p>
          <p v-if="selectedRole === 'admin'" class="role-warning">
            Confirm this only for accounts that should have full administrative access.
            The user may need to sign in again or refresh their session.
          </p>
        </div>
        <template #footer>
          <el-button :disabled="isUpdatingRole" @click="isRoleDialogOpen = false">
            Cancel
          </el-button>
          <el-button
            type="primary"
            class="admin-button admin-button--primary"
            :loading="isUpdatingRole"
            @click="handleSaveRole"
          >
            Save role
          </el-button>
        </template>
      </el-dialog>
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

.details-card--hero-inactive {
  background: linear-gradient(135deg, #6b3a3a 0%, #8b4a4a 100%);
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

.hero-status {
  margin-top: 14px;
}

.hero-status-message {
  margin-top: 12px;
  max-width: 560px;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.5;
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

.section-header--split {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-header h3,
.section-header p {
  margin: 0;
}

.section-header h3 {
  margin-bottom: 6px;
}

.account-access-card {
  display: grid;
  gap: 18px;
}

.account-access-card__actions {
  display: flex;
  justify-content: flex-start;
}

.access-role-card {
  display: grid;
  gap: 18px;
}

.access-role-card__form {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.access-role-card__select {
  width: min(260px, 100%);
}

.account-status-dialog {
  display: grid;
  gap: 12px;
  color: var(--pc-text);
  line-height: 1.55;
}

.account-status-dialog p,
.account-status-dialog ul {
  margin: 0;
}

.account-status-dialog ul {
  padding-left: 20px;
}

.account-status-dialog li + li {
  margin-top: 8px;
}

.role-change-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.role-change-summary div {
  padding: 12px;
  border: 1px solid var(--pc-border);
  border-radius: 12px;
  background: var(--pc-surface);
}

.role-change-summary dt {
  color: var(--pc-muted);
  font-size: 0.88rem;
}

.role-change-summary dd {
  margin: 4px 0 0;
  color: var(--pc-text);
  font-weight: 700;
}

.role-warning {
  color: #9f3a28;
  font-weight: 600;
}

@media (max-width: 900px) {
  .details-grid {
    grid-template-columns: 1fr;
  }

  .section-header--split {
    display: grid;
  }
}
</style>
