<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CreateBookingDialog from '@/components/bookings/CreateBookingDialog.vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { servicesApi } from '@/api/services'

const services = ref([])
const selectedCategory = ref('All')
const isLoading = ref(false)
const errorMessage = ref('')
const isCreateDialogOpen = ref(false)
const selectedServiceId = ref(null)
const router = useRouter()

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

const categories = computed(() => [
  'All',
  ...new Set(services.value.map((service) => service.category)),
])

const visibleServices = computed(() => {
  if (selectedCategory.value === 'All') {
    return services.value
  }

  return services.value.filter((service) => service.category === selectedCategory.value)
})

async function loadServices() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await servicesApi.list()
    services.value = data

    if (!categories.value.includes(selectedCategory.value)) {
      selectedCategory.value = 'All'
    }
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load services right now.')
    services.value = []
  } finally {
    isLoading.value = false
  }
}

function handleBookNow(service) {
  if (!service?.id) {
    return
  }

  selectedServiceId.value = service.id
  isCreateDialogOpen.value = true
}

function handleBookingCreated() {
  router.push({
    name: 'my-bookings',
  })
}

onMounted(() => {
  loadServices()
})
</script>

<template>
  <PageContainer>
    <div class="services-page">
      <section class="services-intro">
        <div class="services-header">
          <span class="eyebrow">Clinic services</span>
          <h1>Care options that are easy to browse and simple to book.</h1>
          <p>
            Explore common pet clinic visits with clear descriptions, estimated appointment lengths,
            and starting prices.
          </p>
        </div>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          :closable="false"
          class="services-alert"
        />

        <div class="filters-section">
          <div class="filters-header">
            <h2>Browse by category</h2>
            <span>{{ visibleServices.length }} services</span>
          </div>

          <div class="filter-chips">
            <el-button
              v-for="category in categories"
              :key="category"
              :type="selectedCategory === category ? 'primary' : 'default'"
              :plain="selectedCategory !== category"
              round
              @click="selectedCategory = category"
            >
              {{ category }}
            </el-button>
          </div>
        </div>
      </section>

      <section v-if="isLoading" class="services-grid">
        <el-card
          v-for="index in 4"
          :key="index"
          class="service-card"
        >
          <el-skeleton :rows="4" animated />
        </el-card>
      </section>

      <section v-else-if="visibleServices.length" class="services-grid">
        <el-card
          v-for="service in visibleServices"
          :key="service.id ?? service.name"
          class="service-card"
          shadow="hover"
        >
          <div class="service-card__top">
            <el-tag effect="plain">{{ service.category }}</el-tag>
            <span class="service-duration">{{ service.duration }}</span>
          </div>

          <h3>{{ service.name }}</h3>
          <p>{{ service.description }}</p>

          <div class="service-card__footer">
            <div class="service-price">
              <span>Starting at</span>
              <strong>{{ service.price }}</strong>
            </div>

            <el-button type="primary" @click="handleBookNow(service)">Book Now</el-button>
          </div>
        </el-card>
      </section>

      <section v-else class="empty-state">
        <el-empty description="No active services are available right now." />
      </section>

      <CreateBookingDialog
        v-model="isCreateDialogOpen"
        :initial-service-id="selectedServiceId"
        :services="services"
        @created="handleBookingCreated"
      />
    </div>
  </PageContainer>
</template>

<style scoped>
.services-page {
  display: flex;
  flex-direction: column;
  gap: 42px;
  padding: 28px 0 56px;
}

.services-intro {
  padding: 30px 28px 28px;
  border: 1px solid var(--pc-border);
  border-radius: var(--pc-radius-lg);
  background:
    radial-gradient(circle at top left, rgba(250, 249, 245, 0.9), transparent 34%),
    radial-gradient(circle at right center, rgba(201, 100, 66, 0.12), transparent 28%),
    linear-gradient(180deg, #faf9f5 0%, #f1efe7 100%);
  box-shadow: var(--pc-shadow);
}

.services-header {
  max-width: 720px;
  padding: 4px 0 0;
}

.eyebrow {
  display: inline-block;
  color: var(--pc-primary);
  font-family: "Courier New", ui-monospace, monospace;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.services-header h1 {
  margin: 14px 0 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(2.2rem, 4vw, 3.4rem);
  line-height: 1.06;
  font-weight: 500;
  letter-spacing: -0.035em;
}

.services-header p {
  max-width: 620px;
  margin: 16px 0 0;
  color: var(--pc-muted);
  line-height: 1.75;
}

.services-alert {
  margin-top: 20px;
}

.filters-section {
  margin-top: 24px;
  padding: 0;
}

.filters-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.filters-header h2 {
  margin: 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.12rem;
  font-weight: 500;
}

.filters-header span {
  color: var(--pc-soft-muted);
  font-size: 0.95rem;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 20px;
}

.filter-chips :deep(.el-button--primary) {
  --el-button-bg-color: var(--pc-primary);
  --el-button-border-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-primary-hover);
  --el-button-hover-border-color: var(--pc-primary-hover);
  --el-button-active-bg-color: #a94e33;
  --el-button-active-border-color: #a94e33;
  box-shadow: 0 10px 22px rgba(201, 100, 66, 0.16);
  transition: all 180ms ease;
}

.filter-chips :deep(.el-button.is-plain) {
  --el-button-text-color: var(--pc-text);
  --el-button-bg-color: rgba(250, 249, 245, 0.9);
  --el-button-border-color: var(--pc-border);
  --el-button-hover-text-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-surface);
  --el-button-hover-border-color: rgba(201, 100, 66, 0.34);
  transition: all 180ms ease;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.service-card {
  border: 1px solid var(--pc-border-soft);
  border-radius: var(--pc-radius-md);
  background: rgba(250, 249, 245, 0.92);
  box-shadow: var(--pc-shadow-soft);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.service-card:hover {
  transform: translateY(-3px);
  border-color: rgba(201, 100, 66, 0.22);
  box-shadow: var(--pc-shadow);
}

.service-card__top,
.service-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.service-card__top {
  margin-bottom: 16px;
}

.service-duration {
  color: var(--pc-soft-muted);
  font-size: 0.93rem;
}

.service-card h3 {
  margin: 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.35rem;
  font-weight: 500;
}

.service-card p {
  margin: 12px 0 0;
  color: var(--pc-muted);
  line-height: 1.68;
}

.service-card__footer {
  margin-top: 22px;
}

.service-price {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.service-price span {
  color: var(--pc-soft-muted);
  font-size: 0.86rem;
}

.service-price strong {
  color: var(--pc-ink);
  font-size: 1.08rem;
}

.service-card :deep(.el-button--primary) {
  --el-button-bg-color: var(--pc-primary);
  --el-button-border-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-primary-hover);
  --el-button-hover-border-color: var(--pc-primary-hover);
  --el-button-active-bg-color: #a94e33;
  --el-button-active-border-color: #a94e33;
}

.empty-state {
  padding: 34px 24px;
  border: 1px dashed rgba(201, 100, 66, 0.28);
  border-radius: 28px;
  background: rgba(250, 249, 245, 0.72);
}

@media (max-width: 640px) {
  .services-page {
    gap: 28px;
    padding-bottom: 44px;
  }

  .services-intro {
    padding: 22px 18px 20px;
    border-radius: 24px;
  }

  .filters-section {
    padding: 18px;
  }

  .filters-header,
  .service-card__footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .service-card__footer :deep(.el-button) {
    width: 100%;
  }

  .services-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }
}
</style>
