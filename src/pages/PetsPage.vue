<script setup>
import { computed, onMounted, ref } from 'vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { petsApi } from '@/api/pets'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const sexOptions = ['Male', 'Female', 'Unknown']

function createEmptyPetForm() {
  return {
    name: '',
    species: '',
    breed: '',
    age: '',
    weight: '',
    note: 'No care summary added yet.',
    sex: '',
    dateOfBirth: '',
    color: '',
    microchipNumber: '',
    allergies: '',
    chronicConditions: '',
    medications: '',
    vaccinationNotes: '',
    generalMedicalNotes: '',
    status: 'Healthy',
  }
}

const pets = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const isCreateDialogOpen = ref(false)
const isCreating = ref(false)
const createErrorMessage = ref('')
const isEditDialogOpen = ref(false)
const isEditing = ref(false)
const editErrorMessage = ref('')
const editingPetId = ref(null)
const isProfileDialogOpen = ref(false)
const isLoadingProfile = ref(false)
const profileErrorMessage = ref('')
const selectedPet = ref(null)
const isDeletingPetId = ref(null)
const createForm = ref(createEmptyPetForm())
const editForm = ref(createEmptyPetForm())

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function getPetStatusTagType(status) {
  if (status === 'Upcoming visit') {
    return 'primary'
  }

  if (status === 'Healthy') {
    return 'success'
  }

  if (status === 'Needs attention') {
    return 'warning'
  }

  return 'info'
}

function getSummaryMedicalLine(pet) {
  return pet.generalMedicalNotes || pet.note
}

function formatOptionalValue(value, fallback = 'Not added yet') {
  return value || fallback
}

function formatMedicalNoteDate(value) {
  if (!value) {
    return 'No date'
  }

  const parsedDate = new Date(`${value}T00:00:00`)
  return Number.isNaN(parsedDate.getTime())
    ? value
    : parsedDate.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
}

function formatMedicalNoteTimestamp(value) {
  if (!value) {
    return 'Timestamp unavailable'
  }

  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime())
    ? value
    : parsed.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    })
}

function getMedicalNoteTitle(noteItem) {
  if (noteItem.relatedVisit?.service) {
    return noteItem.relatedVisit.service
  }

  if (noteItem.relatedBookingId) {
    return `Visit-linked note #${noteItem.relatedBookingId}`
  }

  return 'Care update'
}

function getMedicalNoteMeta(noteItem) {
  const author = formatOptionalValue(
    noteItem.relatedVisit?.staff || noteItem.author,
    'Clinic team'
  )
  const recordedAt = formatMedicalNoteTimestamp(noteItem.createdAt)
  const updatedAt = noteItem.updatedAt && noteItem.updatedAt !== noteItem.createdAt
    ? `Updated ${formatMedicalNoteTimestamp(noteItem.updatedAt)}`
    : null

  return {
    author,
    recordedAt,
    updatedAt,
  }
}

function getVisitContextLine(noteItem) {
  if (!noteItem.relatedVisit) {
    return null
  }

  const visit = noteItem.relatedVisit
  return `${visit.date} at ${visit.time} • ${visit.staff}`
}

function getVisitOutcomeItems(noteItem) {
  if (!noteItem.relatedVisit) {
    return []
  }

  const visit = noteItem.relatedVisit
  return [
    { label: 'Visit summary', value: visit.visitSummary },
    { label: 'Assessment', value: visit.diagnosisAssessment },
    { label: 'Treatment', value: visit.treatmentRecommendation },
    { label: 'Follow-up', value: visit.followUpNote },
  ].filter((item) => item.value)
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
    const { data } = await petsApi.me()
    pets.value = data
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load pet profiles right now.')
    pets.value = []
  } finally {
    isLoading.value = false
  }
}

async function loadPetDetail(petId) {
  if (!authStore.user?.email || !petId) {
    return
  }

  isLoadingProfile.value = true
  profileErrorMessage.value = ''

  try {
    const { data } = await petsApi.detail(petId)
    selectedPet.value = data
  } catch (error) {
    profileErrorMessage.value = getApiErrorMessage(error, 'Unable to load this pet profile right now.')
    selectedPet.value = null
  } finally {
    isLoadingProfile.value = false
  }
}

onMounted(() => {
  loadPets()
})

function openCreateDialog() {
  createErrorMessage.value = ''
  isCreateDialogOpen.value = true
}

function openEditDialog(pet) {
  editErrorMessage.value = ''
  editingPetId.value = pet.id
  editForm.value = {
    name: pet.name,
    species: pet.species,
    breed: pet.breed,
    age: pet.age,
    weight: pet.weight,
    note: pet.note,
    sex: pet.sex || '',
    dateOfBirth: pet.dateOfBirth || '',
    color: pet.color || '',
    microchipNumber: pet.microchipNumber || '',
    allergies: pet.allergies || '',
    chronicConditions: pet.chronicConditions || '',
    medications: pet.medications || '',
    vaccinationNotes: pet.vaccinationNotes || '',
    generalMedicalNotes: pet.generalMedicalNotes || '',
    status: pet.status,
  }
  isEditDialogOpen.value = true
}

function resetCreateForm() {
  createForm.value = createEmptyPetForm()
  createErrorMessage.value = ''
}

function resetEditForm() {
  editingPetId.value = null
  editErrorMessage.value = ''
  editForm.value = createEmptyPetForm()
}

function resetSelectedPet() {
  selectedPet.value = null
  profileErrorMessage.value = ''
}

async function handleCreatePet() {
  if (!authStore.user?.email) {
    return
  }

  isCreating.value = true
  createErrorMessage.value = ''

  try {
    await petsApi.create(createForm.value)
    isCreateDialogOpen.value = false
    resetCreateForm()
    await loadPets()
  } catch (error) {
    createErrorMessage.value = getApiErrorMessage(error, 'Unable to add pet right now.')
  } finally {
    isCreating.value = false
  }
}

async function handleViewProfile(pet) {
  if (!pet?.id) {
    return
  }

  isProfileDialogOpen.value = true
  selectedPet.value = null
  await loadPetDetail(pet.id)
}

async function handleEditPet() {
  if (!authStore.user?.email || !editingPetId.value) {
    return
  }

  isEditing.value = true
  editErrorMessage.value = ''
  errorMessage.value = ''

  try {
    const { data } = await petsApi.update(editingPetId.value, editForm.value)
    isEditDialogOpen.value = false
    await loadPets()

    if (selectedPet.value?.id === data.id) {
      await loadPetDetail(data.id)
    }
  } catch (error) {
    editErrorMessage.value = getApiErrorMessage(error, 'Unable to update this pet right now.')
  } finally {
    isEditing.value = false
  }
}

async function handleDeletePet(pet) {
  if (!authStore.user?.email || !pet?.id) {
    return
  }

  const confirmed = window.confirm(`Delete ${pet.name}?`)
  if (!confirmed) {
    return
  }

  isDeletingPetId.value = pet.id
  errorMessage.value = ''

  try {
    await petsApi.remove(pet.id)
    await loadPets()

    if (selectedPet.value?.id === pet.id) {
      isProfileDialogOpen.value = false
      resetSelectedPet()
    }
  } catch (error) {
    errorMessage.value = getApiErrorMessage(
      error,
      'Unable to delete this pet right now.'
    )
  } finally {
    isDeletingPetId.value = null
  }
}
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

        <el-button type="primary" size="large" @click="openCreateDialog">Add Pet</el-button>
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
          :key="pet.id ?? pet.name"
          class="pet-card"
          shadow="hover"
        >
          <div class="pet-card__top">
            <div>
              <h2>{{ pet.name }}</h2>
              <p class="pet-subtitle">{{ pet.species }} | {{ pet.breed }}</p>
            </div>
            <el-tag :type="getPetStatusTagType(pet.displayStatus)" effect="plain">
              {{ pet.displayStatus }}
            </el-tag>
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
            <div>
              <span>Sex</span>
              <strong>{{ formatOptionalValue(pet.sex) }}</strong>
            </div>
            <div>
              <span>Microchip</span>
              <strong>{{ formatOptionalValue(pet.microchipNumber) }}</strong>
            </div>
          </div>

          <div class="pet-note">
            <span>Medical summary</span>
            <p>{{ getSummaryMedicalLine(pet) }}</p>
          </div>

          <div class="pet-actions">
            <el-button type="primary" @click="handleViewProfile(pet)">View Profile</el-button>
            <el-button plain @click="openEditDialog(pet)">Edit</el-button>
            <el-button
              plain
              :loading="isDeletingPetId === pet.id"
              @click="handleDeletePet(pet)"
            >
              Delete
            </el-button>
          </div>
        </el-card>
      </section>

      <section v-else class="empty-state">
        <el-empty description="No pet profiles yet. Add your first pet to get started.">
          <el-button type="primary" @click="openCreateDialog">Add Pet</el-button>
        </el-empty>
      </section>

      <el-dialog
        v-model="isProfileDialogOpen"
        title="Pet Profile"
        width="min(820px, 94vw)"
        @closed="resetSelectedPet"
      >
        <el-alert
          v-if="profileErrorMessage"
          :title="profileErrorMessage"
          type="error"
          :closable="false"
          class="create-alert"
        />

        <el-skeleton v-else-if="isLoadingProfile" :rows="8" animated />

        <div v-else-if="selectedPet" class="profile-layout">
          <section class="profile-section">
            <div class="profile-section__header">
              <div>
                <h3>Profile Summary</h3>
                <p>Everyday details that help keep visits smooth and familiar.</p>
              </div>
              <el-tag :type="getPetStatusTagType(selectedPet.displayStatus)" effect="plain">
                {{ selectedPet.displayStatus }}
              </el-tag>
            </div>

            <div class="profile-grid">
              <div>
                <span>Name</span>
                <strong>{{ selectedPet.name }}</strong>
              </div>
              <div>
                <span>Species</span>
                <strong>{{ selectedPet.species }}</strong>
              </div>
              <div>
                <span>Breed</span>
                <strong>{{ selectedPet.breed }}</strong>
              </div>
              <div>
                <span>Sex</span>
                <strong>{{ formatOptionalValue(selectedPet.sex) }}</strong>
              </div>
              <div>
                <span>Age</span>
                <strong>{{ selectedPet.age }}</strong>
              </div>
              <div>
                <span>Date of birth</span>
                <strong>{{ formatOptionalValue(selectedPet.dateOfBirth) }}</strong>
              </div>
              <div>
                <span>Weight</span>
                <strong>{{ selectedPet.weight }}</strong>
              </div>
              <div>
                <span>Color</span>
                <strong>{{ formatOptionalValue(selectedPet.color) }}</strong>
              </div>
              <div class="profile-grid__full">
                <span>Care summary</span>
                <p>{{ selectedPet.note }}</p>
              </div>
            </div>
          </section>

          <section class="profile-section">
            <div class="profile-section__header">
              <div>
                <h3>Medical Record</h3>
                <p>Lightweight clinic details for continuity of care.</p>
              </div>
            </div>

            <div class="profile-grid">
              <div>
                <span>Microchip number</span>
                <strong>{{ formatOptionalValue(selectedPet.microchipNumber) }}</strong>
              </div>
              <div>
                <span>Current status</span>
                <strong>{{ selectedPet.status }}</strong>
              </div>
              <div class="profile-grid__full">
                <span>Allergies</span>
                <p>{{ formatOptionalValue(selectedPet.allergies) }}</p>
              </div>
              <div class="profile-grid__full">
                <span>Chronic conditions</span>
                <p>{{ formatOptionalValue(selectedPet.chronicConditions) }}</p>
              </div>
              <div class="profile-grid__full">
                <span>Medications</span>
                <p>{{ formatOptionalValue(selectedPet.medications) }}</p>
              </div>
              <div class="profile-grid__full">
                <span>Vaccination notes</span>
                <p>{{ formatOptionalValue(selectedPet.vaccinationNotes) }}</p>
              </div>
              <div class="profile-grid__full">
                <span>General medical notes</span>
                <p>{{ formatOptionalValue(selectedPet.generalMedicalNotes) }}</p>
              </div>
            </div>
          </section>

          <section class="profile-section">
            <div class="profile-section__header">
              <div>
                <h3>Visit Notes</h3>
                <p>A simple timeline for follow-ups, observations, and care updates.</p>
              </div>
            </div>

            <div v-if="selectedPet.medicalNotes?.length" class="note-timeline">
              <article
                v-for="noteItem in selectedPet.medicalNotes"
                :key="noteItem.id"
                class="note-card"
                :class="{ 'note-card--visit': noteItem.relatedVisit }"
              >
                <div class="note-card__pin" aria-hidden="true"></div>
                <div class="note-card__meta">
                  <div class="note-card__meta-primary">
                    <strong>{{ formatMedicalNoteDate(noteItem.date) }}</strong>
                    <span class="note-card__title">{{ getMedicalNoteTitle(noteItem) }}</span>
                    <span v-if="getVisitContextLine(noteItem)" class="note-card__visit-line">
                      {{ getVisitContextLine(noteItem) }}
                    </span>
                  </div>
                  <div class="note-card__meta-secondary">
                    <span>{{ getMedicalNoteMeta(noteItem).author }}</span>
                    <span>{{ getMedicalNoteMeta(noteItem).recordedAt }}</span>
                  </div>
                </div>
                <p>{{ noteItem.noteText }}</p>
                <div
                  v-if="getVisitOutcomeItems(noteItem).length"
                  class="note-card__visit-outcomes"
                >
                  <div
                    v-for="item in getVisitOutcomeItems(noteItem)"
                    :key="item.label"
                    class="note-card__visit-outcome"
                  >
                    <span>{{ item.label }}</span>
                    <p>{{ item.value }}</p>
                  </div>
                </div>
                <div class="note-card__footer">
                  <small v-if="noteItem.relatedBookingId">
                    Related booking #{{ noteItem.relatedBookingId }}
                  </small>
                  <small v-if="noteItem.relatedVisit?.status">
                    Visit status: {{ noteItem.relatedVisit.status }}
                  </small>
                  <small v-if="getMedicalNoteMeta(noteItem).updatedAt">
                    {{ getMedicalNoteMeta(noteItem).updatedAt }}
                  </small>
                </div>
              </article>
            </div>
            <el-empty
              v-else
              description="No medical notes have been recorded yet."
            />
          </section>
        </div>
      </el-dialog>

      <el-dialog
        v-model="isEditDialogOpen"
        title="Edit Pet"
        width="min(720px, 94vw)"
        @closed="resetEditForm"
      >
        <el-alert
          v-if="editErrorMessage"
          :title="editErrorMessage"
          type="error"
          :closable="false"
          class="create-alert"
        />

        <el-form :model="editForm" label-position="top">
          <section class="form-section">
            <h3>Basic Profile</h3>
            <p>Update the everyday details you maintain for this pet.</p>
            <div class="form-grid">
              <el-form-item label="Name">
                <el-input v-model="editForm.name" placeholder="Pet name" />
              </el-form-item>
              <el-form-item label="Species">
                <el-input v-model="editForm.species" placeholder="Dog, Cat, etc." />
              </el-form-item>
              <el-form-item label="Breed">
                <el-input v-model="editForm.breed" placeholder="Breed" />
              </el-form-item>
              <el-form-item label="Sex">
                <el-select v-model="editForm.sex" clearable placeholder="Select sex">
                  <el-option
                    v-for="option in sexOptions"
                    :key="option"
                    :label="option"
                    :value="option"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="Age">
                <el-input v-model="editForm.age" placeholder="e.g. 2 years" />
              </el-form-item>
              <el-form-item label="Weight">
                <el-input v-model="editForm.weight" placeholder="e.g. 10 lb" />
              </el-form-item>
              <el-form-item label="Color">
                <el-input v-model="editForm.color" placeholder="Coat color" />
              </el-form-item>
              <el-form-item label="Microchip number">
                <el-input v-model="editForm.microchipNumber" placeholder="Optional microchip ID" />
              </el-form-item>
              <el-form-item label="Allergies">
                <el-input
                  v-model="editForm.allergies"
                  type="textarea"
                  :rows="2"
                  placeholder="Known allergies or sensitivities"
                />
              </el-form-item>
            </div>
          </section>
        </el-form>

        <template #footer>
          <el-button @click="isEditDialogOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="isEditing" @click="handleEditPet">
            Save Changes
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="isCreateDialogOpen"
        title="Add Pet"
        width="min(720px, 94vw)"
        @closed="resetCreateForm"
      >
        <el-alert
          v-if="createErrorMessage"
          :title="createErrorMessage"
          type="error"
          :closable="false"
          class="create-alert"
        />

        <el-form :model="createForm" label-position="top">
          <section class="form-section">
            <h3>Basic Profile</h3>
            <p>Start with the core profile details you need day to day.</p>
            <div class="form-grid">
              <el-form-item label="Name">
                <el-input v-model="createForm.name" placeholder="Pet name" />
              </el-form-item>
              <el-form-item label="Species">
                <el-input v-model="createForm.species" placeholder="Dog, Cat, etc." />
              </el-form-item>
              <el-form-item label="Breed">
                <el-input v-model="createForm.breed" placeholder="Breed" />
              </el-form-item>
              <el-form-item label="Sex">
                <el-select v-model="createForm.sex" clearable placeholder="Select sex">
                  <el-option
                    v-for="option in sexOptions"
                    :key="option"
                    :label="option"
                    :value="option"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="Age">
                <el-input v-model="createForm.age" placeholder="e.g. 2 years" />
              </el-form-item>
              <el-form-item label="Weight">
                <el-input v-model="createForm.weight" placeholder="e.g. 10 lb" />
              </el-form-item>
              <el-form-item label="Color">
                <el-input v-model="createForm.color" placeholder="Coat color" />
              </el-form-item>
              <el-form-item label="Microchip number">
                <el-input v-model="createForm.microchipNumber" placeholder="Optional microchip ID" />
              </el-form-item>
              <el-form-item label="Allergies">
                <el-input
                  v-model="createForm.allergies"
                  type="textarea"
                  :rows="2"
                  placeholder="Known allergies or sensitivities"
                />
              </el-form-item>
            </div>
          </section>
        </el-form>

        <template #footer>
          <el-button @click="isCreateDialogOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="isCreating" @click="handleCreatePet">
            Save Pet
          </el-button>
        </template>
      </el-dialog>
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
.empty-state :deep(.el-button--primary),
.profile-section__header :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.pet-actions :deep(.el-button.is-plain),
.profile-section__header :deep(.el-button.is-plain) {
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

.profile-section__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.pet-card h2,
.profile-section h3,
.form-section h3 {
  margin: 0;
  color: #173047;
}

.field-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.field-label small,
.form-section p {
  color: #6d7680;
  font-size: 0.84rem;
  line-height: 1.45;
}

.pet-card h2 {
  font-size: 1.3rem;
}

.pet-subtitle {
  margin: 8px 0 0;
  color: #6d7680;
}

.pet-details,
.profile-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.pet-details {
  margin-top: 20px;
  padding: 16px 0;
  border-top: 1px solid rgba(28, 60, 88, 0.08);
  border-bottom: 1px solid rgba(28, 60, 88, 0.08);
}

.pet-details span,
.pet-note span,
.profile-grid span {
  display: block;
  color: #7a817f;
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.pet-details strong,
.profile-grid strong {
  display: block;
  margin-top: 6px;
  color: #173047;
  font-size: 1rem;
}

.pet-note {
  margin-top: 18px;
}

.pet-note p,
.profile-grid p,
.note-card p {
  margin: 10px 0 0;
  color: #5f7484;
  line-height: 1.7;
}

.profile-grid__full {
  grid-column: 1 / -1;
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

.profile-layout,
.form-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-section,
.form-section {
  padding: 18px;
  border: 1px solid rgba(28, 60, 88, 0.08);
  border-radius: 20px;
  background: rgba(255, 252, 247, 0.72);
}

.note-timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  padding-left: 12px;
}

.note-timeline::before {
  content: '';
  position: absolute;
  left: 19px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: rgba(63, 114, 93, 0.14);
}

.note-card {
  position: relative;
  padding: 16px;
  border: 1px solid rgba(28, 60, 88, 0.08);
  border-radius: 18px;
  background: #fff;
  margin-left: 18px;
}

.note-card--visit {
  background: linear-gradient(180deg, #fffdfa 0%, #f8fcfb 100%);
  border-color: rgba(63, 114, 93, 0.18);
}

.note-card__pin {
  position: absolute;
  left: -24px;
  top: 22px;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #3f725d;
  box-shadow: 0 0 0 5px #f7fbf8;
}

.note-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  color: #5f685e;
}

.note-card__meta-primary,
.note-card__meta-secondary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.note-card__title {
  color: #355f4d;
  font-size: 0.9rem;
  font-weight: 600;
}

.note-card__visit-line {
  color: #6b7480;
  font-size: 0.86rem;
}

.note-card__meta-secondary {
  text-align: right;
  color: #7a817f;
  font-size: 0.86rem;
}

.note-card__visit-outcomes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.note-card__visit-outcome {
  padding: 12px;
  border-radius: 14px;
  background: rgba(244, 250, 248, 0.96);
  border: 1px solid rgba(63, 114, 93, 0.1);
}

.note-card__visit-outcome span {
  display: block;
  color: #7a817f;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.note-card__visit-outcome p {
  margin-top: 6px;
}

.note-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.note-card small {
  display: block;
  margin-top: 10px;
  color: #7a817f;
}

.form-section {
  margin-bottom: 16px;
}

.form-grid--compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.full-width {
  width: 100%;
}

.create-alert {
  margin-bottom: 16px;
}

@media (max-width: 760px) {
  .pets-header,
  .pet-card__top,
  .profile-section__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .pets-header :deep(.el-button),
  .profile-section__header :deep(.el-button) {
    width: 100%;
  }

  .pets-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .note-card__meta {
    flex-direction: column;
  }

  .note-card__meta-secondary {
    text-align: left;
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

  .profile-grid,
  .form-grid,
  .form-grid--compact,
  .note-card__visit-outcomes {
    grid-template-columns: 1fr;
  }

  .pet-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .pet-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
