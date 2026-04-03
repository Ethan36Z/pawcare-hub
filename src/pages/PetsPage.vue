<script setup>
import { computed, onMounted, ref } from 'vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { petsApi } from '@/api/pets'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const pets = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

const ownerName = computed(() => authStore.user?.fullName || 'your pets')

async function loadPets() {
  if (!authStore.user?.email) {
    pets.value = []
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await petsApi.me(authStore.user.email)
    pets.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load pet profiles right now.')
    pets.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadPets()
})
</script>

<template>
  <PageContainer>
    <div class="pets-page">
      <section class="pets-header">
        <div class="pets-header__copy">
          <span class="eyebrow">My pets</span>
          <h1>Keep each pet's profile, notes, and visit details in one place.</h1>
          <p>
            Review {{ ownerName }}'s pet profiles, keep track of care notes, and stay ready for
            upcoming clinic visits.
          </p>
        </div>

        <el-button type="primary" size="large">Add Pet</el-button>
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

      <section v-else-if="pets.length" class="pets-grid">
        <el-card
          v-for="pet in pets"
          :key="pet.name"
          class="pet-card"
          shadow="hover"
        >
          <div class="pet-card__top">
            <div>
              <h2>{{ pet.name }}</h2>
              <p class="pet-subtitle">{{ pet.species }} | {{ pet.breed }}</p>
            </div>
            <el-tag effect="plain">{{ pet.status }}</el-tag>
          </div>

          <div class="pet-details">
            <div>
              <span>Age</span>
              <strong>{{ pet.age }}</strong>
            </div>
            <div>
              <span>Weight</span>
              <strong>{{ pet.weight }}</strong>
            </div>
          </div>

          <div class="pet-note">
            <span>Care note</span>
            <p>{{ pet.note }}</p>
          </div>

          <div class="pet-actions">
            <el-button type="primary">View Profile</el-button>
            <el-button plain>Edit</el-button>
          </div>
        </el-card>
      </section>

      <section v-else class="empty-state">
        <el-empty description="No pet profiles yet. Add your first pet to get started.">
          <el-button type="primary">Add Pet</el-button>
        </el-empty>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped>
.pets-page {
  display: flex;
  flex-direction: column;
  gap: 34px;
  padding: 28px 0 56px;
}

.pets-header {
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

.pets-header__copy {
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

.pets-header h1 {
  margin: 14px 0 0;
  color: #173047;
  font-size: clamp(2.15rem, 4vw, 3.3rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.pets-header p {
  max-width: 620px;
  margin: 16px 0 0;
  color: #6b7480;
  line-height: 1.75;
}

.pets-header :deep(.el-button--primary),
.pet-actions :deep(.el-button--primary),
.empty-state :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.pet-actions :deep(.el-button.is-plain) {
  --el-button-text-color: #5f685e;
  --el-button-bg-color: rgba(255, 251, 244, 0.86);
  --el-button-border-color: rgba(132, 125, 104, 0.16);
  --el-button-hover-text-color: #355f4d;
  --el-button-hover-bg-color: rgba(255, 252, 246, 1);
  --el-button-hover-border-color: rgba(63, 114, 93, 0.32);
}

.pets-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.pet-card {
  border: 1px solid rgba(28, 60, 88, 0.12);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 32px rgba(21, 40, 61, 0.06);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.pet-card:hover {
  transform: translateY(-3px);
  border-color: rgba(63, 114, 93, 0.18);
  box-shadow: 0 20px 34px rgba(21, 40, 61, 0.08);
}

.pet-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.pet-card h2 {
  margin: 0;
  color: #173047;
  font-size: 1.3rem;
}

.pet-subtitle {
  margin: 8px 0 0;
  color: #6d7680;
}

.pet-details {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
  padding: 16px 0;
  border-top: 1px solid rgba(28, 60, 88, 0.08);
  border-bottom: 1px solid rgba(28, 60, 88, 0.08);
}

.pet-details span,
.pet-note span {
  display: block;
  color: #7a817f;
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.pet-details strong {
  display: block;
  margin-top: 6px;
  color: #173047;
  font-size: 1rem;
}

.pet-note {
  margin-top: 18px;
}

.pet-note p {
  margin: 10px 0 0;
  color: #5f7484;
  line-height: 1.7;
}

.pet-actions {
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

@media (max-width: 760px) {
  .pets-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .pets-header :deep(.el-button) {
    width: 100%;
  }

  .pets-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }
}

@media (max-width: 640px) {
  .pets-page {
    gap: 28px;
    padding-bottom: 44px;
  }

  .pets-header {
    padding: 22px 18px 20px;
    border-radius: 24px;
  }

  .pet-card__top,
  .pet-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .pet-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
