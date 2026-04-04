<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usersApi } from '@/api/users'

const users = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const router = useRouter()

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

async function loadUsers() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await usersApi.list()
    users.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load users right now.')
    users.value = []
  } finally {
    isLoading.value = false
  }
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

    <el-skeleton v-if="isLoading" :rows="5" animated />

    <el-empty
      v-else-if="!users.length"
      description="No users found yet."
    />

    <el-table v-else :data="users" stripe>
      <el-table-column prop="id" label="ID" min-width="80" />
      <el-table-column prop="name" label="Name" min-width="180" />
      <el-table-column prop="email" label="Email" min-width="240" />
      <el-table-column prop="role" label="Role" min-width="120" />
      <el-table-column label="Actions" min-width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            plain
            size="small"
            @click="router.push({ name: 'admin-user-details', params: { id: row.id } })"
          >
            View details
          </el-button>
        </template>
      </el-table-column>
    </el-table>
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

.admin-page__alert {
  margin-bottom: 16px;
}

.admin-page h1 {
  margin-top: 0;
  margin-bottom: 8px;
}

.admin-page p {
  margin-bottom: 0;
  color: var(--pc-muted);
}
</style>
