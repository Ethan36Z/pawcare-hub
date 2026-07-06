<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usersApi } from '@/api/users'

const users = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const router = useRouter()
const filters = ref({
  search: '',
  role: '',
  active: '',
})
const currentPage = ref(1)
const pageSize = ref(10)
const paginatedUsers = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  return users.value.slice(startIndex, startIndex + pageSize.value)
})

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getStatusTagType(isActive) {
  return isActive ? 'success' : 'danger'
}

function getRoleBadgeClass(role) {
  const normalizedRole = String(role || 'user').toLowerCase().replace('_', '-')
  return ['admin-badge', `admin-badge--${normalizedRole}`]
}

function getUserRowClassName({ row }) {
  return row?.active ? '' : 'admin-users__row--inactive'
}

async function loadUsers() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await usersApi.list({
      search: filters.value.search || undefined,
      role: filters.value.role || undefined,
      active: filters.value.active === '' ? undefined : filters.value.active,
    })
    users.value = data
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load users right now.')
    users.value = []
  } finally {
    isLoading.value = false
  }
}

function handleApplyFilters() {
  loadUsers()
}

function handleResetFilters() {
  filters.value = {
    search: '',
    role: '',
    active: '',
  }
  loadUsers()
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__header">
      <div>
        <h1>Admin Users</h1>
        <p>Current users from the MySQL-backed auth system.</p>
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
      <el-input
        v-model="filters.search"
        placeholder="Search by name or email"
        clearable
        class="admin-filters__control admin-control"
      />

      <el-select
        v-model="filters.role"
        placeholder="Role"
        clearable
        class="admin-filters__control admin-control"
      >
        <el-option label="Admin" value="admin" />
        <el-option label="Front Desk" value="front_desk" />
        <el-option label="Doctor" value="doctor" />
        <el-option label="User" value="user" />
      </el-select>

      <el-select
        v-model="filters.active"
        placeholder="Status"
        clearable
        class="admin-filters__control admin-control"
      >
        <el-option label="Active" :value="true" />
        <el-option label="Deactivated" :value="false" />
      </el-select>

      <div class="admin-filters__actions">
        <el-button native-type="button" class="admin-button admin-button--primary admin-button--toolbar" @click="handleApplyFilters">
          Apply
        </el-button>
        <el-button native-type="button" class="admin-button admin-button--secondary admin-button--toolbar" @click="handleResetFilters">
          Reset
        </el-button>
      </div>
    </div>

    <div class="admin-table-section">
      <el-skeleton v-if="isLoading" :rows="5" animated />

      <el-empty
        v-else-if="!users.length"
        description="No users match the current filters."
      />

      <template v-else>
        <el-table
          :data="paginatedUsers"
          stripe
          :row-class-name="getUserRowClassName"
        >
          <el-table-column prop="id" label="ID" min-width="80" />
          <el-table-column label="Name" min-width="200">
            <template #default="{ row }">
              <div class="user-name-cell">
                <strong>{{ row.name }}</strong>
                <span v-if="!row.active">Inactive account</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="email" label="Email" min-width="240" />
          <el-table-column label="Role" min-width="130">
            <template #default="{ row }">
              <el-tag effect="plain" :class="getRoleBadgeClass(row.role)">
                {{ row.role }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Status" min-width="140">
            <template #default="{ row }">
              <el-tag
                :type="getStatusTagType(row.active)"
                effect="plain"
                :class="['admin-badge', row.active ? 'admin-badge--active' : 'admin-badge--deactivated']"
              >
                {{ row.active ? 'Active' : 'Deactivated' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" min-width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                plain
                size="small"
                class="admin-button admin-button--secondary"
                @click="router.push({ name: 'admin-user-details', params: { id: row.id } })"
              >
                View details
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="users.length > pageSize" class="admin-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="users.length"
            :page-sizes="[10, 20, 50]"
          />
        </div>
      </template>
    </div>
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
  grid-template-columns: minmax(0, 1.4fr) repeat(2, minmax(0, 1fr)) auto;
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

.user-name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.35;
}

.user-name-cell strong {
  color: var(--pc-text);
}

.user-name-cell span {
  color: #b42318;
  font-size: 0.82rem;
  font-weight: 600;
}

.admin-page :deep(.admin-users__row--inactive) {
  --el-table-tr-bg-color: #fff7f7;
}

.admin-page :deep(.admin-users__row--inactive td) {
  color: #7a5a5a;
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

@media (max-width: 960px) {
  .admin-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-filters {
    grid-template-columns: 1fr;
  }
}
</style>
