<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import PageContainer from '@/components/common/PageContainer.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const profile = reactive({
  fullName: '',
  email: '',
  phone: '',
  address: '',
  emailReminders: false,
  textReminders: false,
  contactMethod: '',
})

const editForm = reactive({
  phone: '',
  address: '',
  contactMethod: '',
})

const contactOptions = ['Email', 'Text message', 'Phone call']

const isLoading = ref(false)
const isSavingProfile = ref(false)
const isSavingPreferences = ref(false)
const isEditDialogOpen = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const displayName = computed(() => authStore.user?.fullName || profile.fullName || 'Pet Owner')
const displayEmail = computed(() => authStore.user?.email || profile.email || '')
const displayPhone = computed(() => profile.phone || 'Not added yet')
const displayAddress = computed(() => profile.address || 'Not added yet')
const displayContactMethod = computed(() => profile.contactMethod || 'Not set')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function applyProfileData(data) {
  profile.fullName = data?.name || ''
  profile.email = data?.email || ''
  profile.phone = data?.phone || ''
  profile.address = data?.address || ''
  profile.emailReminders = Boolean(data?.emailReminders)
  profile.textReminders = Boolean(data?.textReminders)
  profile.contactMethod = data?.preferredContactMethod || ''
}

function resetEditForm() {
  editForm.phone = profile.phone
  editForm.address = profile.address
  editForm.contactMethod = profile.contactMethod
}

async function loadProfile() {
  if (!authStore.user?.email) {
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await authApi.profile(authStore.user.email)
    applyProfileData(data)
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load your profile right now.')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadProfile()
})

function openEditDialog() {
  successMessage.value = ''
  resetEditForm()
  isEditDialogOpen.value = true
}

async function handleSaveProfile() {
  if (!authStore.user?.email) {
    return
  }

  isSavingProfile.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const { data } = await authApi.updateProfile(authStore.user.email, {
      phone: editForm.phone,
      address: editForm.address,
      preferredContactMethod: editForm.contactMethod,
      emailReminders: profile.emailReminders,
      textReminders: profile.textReminders,
    })

    applyProfileData(data)
    isEditDialogOpen.value = false
    successMessage.value = 'Profile details updated.'
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to save your profile right now.')
  } finally {
    isSavingProfile.value = false
  }
}

async function handleSavePreferences() {
  if (!authStore.user?.email) {
    return
  }

  isSavingPreferences.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const { data } = await authApi.updateProfile(authStore.user.email, {
      phone: profile.phone,
      address: profile.address,
      preferredContactMethod: profile.contactMethod,
      emailReminders: profile.emailReminders,
      textReminders: profile.textReminders,
    })

    applyProfileData(data)
    successMessage.value = 'Preferences updated.'
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to save your preferences right now.')
  } finally {
    isSavingPreferences.value = false
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <PageContainer>
    <div class="profile-page">
      <section class="profile-header">
        <div class="profile-header__copy">
          <span class="eyebrow">Account settings</span>
          <h1>Manage your account details and communication preferences.</h1>
          <p>
            Keep your contact information up to date so PawCare Hub can help you stay prepared for
            bookings, reminders, and clinic follow-up.
          </p>
        </div>
      </section>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
      />

      <el-alert
        v-if="successMessage"
        :title="successMessage"
        type="success"
        :closable="false"
      />

      <section v-if="isLoading" class="profile-grid">
        <el-card class="settings-card">
          <el-skeleton :rows="5" animated />
        </el-card>
      </section>

      <section v-else class="profile-grid">
        <el-card class="settings-card" shadow="hover">
          <div class="card-header">
            <div>
              <h2>Account information</h2>
              <p>Your basic details used for visits, booking updates, and clinic communication.</p>
            </div>
            <el-button type="primary" @click="openEditDialog">Edit Profile</el-button>
          </div>

          <div class="details-grid">
            <div>
              <span>Full name</span>
              <strong>{{ displayName }}</strong>
            </div>
            <div>
              <span>Email</span>
              <strong>{{ displayEmail }}</strong>
            </div>
            <div>
              <span>Phone number</span>
              <strong>{{ displayPhone }}</strong>
            </div>
            <div>
              <span>Address</span>
              <strong>{{ displayAddress }}</strong>
            </div>
          </div>
        </el-card>

        <el-card class="settings-card" shadow="hover">
          <div class="card-header card-header--stacked">
            <div>
              <h2>Preferences</h2>
              <p>Choose how you want to receive updates about visits and reminders.</p>
            </div>
          </div>

          <div class="preferences-list">
            <div class="preference-row">
              <div>
                <strong>Email reminders</strong>
                <p>Receive appointment confirmations and reminder emails.</p>
              </div>
              <el-switch v-model="profile.emailReminders" />
            </div>

            <div class="preference-row">
              <div>
                <strong>Text reminders</strong>
                <p>Get a quick text before upcoming visits when enabled.</p>
              </div>
              <el-switch v-model="profile.textReminders" />
            </div>

            <div class="preference-contact">
              <span>Preferred contact method</span>
              <el-select v-model="profile.contactMethod" placeholder="Select a contact method">
                <el-option
                  v-for="option in contactOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
              <p class="preference-contact__current">Current: {{ displayContactMethod }}</p>
            </div>

            <div class="preferences-actions">
              <el-button type="primary" :loading="isSavingPreferences" @click="handleSavePreferences">
                Save Preferences
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card class="settings-card" shadow="hover">
          <div class="card-header card-header--stacked">
            <div>
              <h2>Security and account</h2>
              <p>Simple account actions for access, password updates, and account management.</p>
            </div>
          </div>

          <div class="account-actions">
            <div class="action-row">
              <div>
                <strong>Change password</strong>
                <p>Update your sign-in password for better account security.</p>
              </div>
              <el-button plain>Change Password</el-button>
            </div>

            <div class="action-row">
              <div>
                <strong>Sign out</strong>
                <p>End your current session on this device.</p>
              </div>
              <el-button plain @click="handleLogout">Sign Out</el-button>
            </div>

            <div class="action-row action-row--subtle">
              <div>
                <strong>Delete account</strong>
                <p>Request permanent account removal if you no longer need PawCare Hub.</p>
              </div>
              <el-button text class="danger-link">Delete Account</el-button>
            </div>
          </div>
        </el-card>
      </section>

      <el-dialog
        v-model="isEditDialogOpen"
        title="Edit Profile"
        width="min(560px, 92vw)"
        @closed="resetEditForm"
      >
        <el-form label-position="top">
          <el-form-item label="Phone number">
            <el-input v-model="editForm.phone" placeholder="Phone number" />
          </el-form-item>
          <el-form-item label="Address">
            <el-input v-model="editForm.address" type="textarea" :rows="3" placeholder="Address" />
          </el-form-item>
          <el-form-item label="Preferred contact method">
            <el-select v-model="editForm.contactMethod" placeholder="Select a contact method">
              <el-option
                v-for="option in contactOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="isEditDialogOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="isSavingProfile" @click="handleSaveProfile">
            Save Changes
          </el-button>
        </template>
      </el-dialog>
    </div>
  </PageContainer>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 30px;
  padding: 28px 0 56px;
}

.profile-header {
  padding: 30px 28px 28px;
  border: 1px solid rgba(28, 60, 88, 0.1);
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba(255, 239, 194, 0.88), transparent 34%),
    radial-gradient(circle at right center, rgba(244, 228, 196, 0.42), transparent 26%),
    linear-gradient(180deg, #fff9ee 0%, #f8efdc 100%);
}

.profile-header__copy {
  max-width: 720px;
}

.eyebrow {
  display: inline-block;
  color: #7f7356;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-header h1 {
  margin: 14px 0 0;
  color: #173047;
  font-size: clamp(2.15rem, 4vw, 3.3rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.profile-header p {
  max-width: 640px;
  margin: 16px 0 0;
  color: #6b7480;
  line-height: 1.75;
}

.profile-grid {
  display: grid;
  gap: 22px;
}

.settings-card {
  border: 1px solid rgba(28, 60, 88, 0.12);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 32px rgba(21, 40, 61, 0.06);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.settings-card:hover {
  transform: translateY(-2px);
  border-color: rgba(63, 114, 93, 0.16);
  box-shadow: 0 20px 34px rgba(21, 40, 61, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}

.card-header--stacked {
  margin-bottom: 18px;
}

.card-header h2 {
  margin: 0;
  color: #173047;
  font-size: 1.22rem;
}

.card-header p {
  margin: 8px 0 0;
  color: #6b7480;
  line-height: 1.68;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.details-grid span,
.preference-contact span {
  display: block;
  color: #7a817f;
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.details-grid strong {
  display: block;
  margin-top: 8px;
  color: #173047;
  font-size: 1rem;
  line-height: 1.55;
}

.preferences-list,
.account-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preference-row,
.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 16px 0;
  border-top: 1px solid rgba(28, 60, 88, 0.08);
}

.preference-row:first-child,
.action-row:first-child {
  border-top: 0;
  padding-top: 0;
}

.preference-row strong,
.action-row strong {
  color: #173047;
}

.preference-row p,
.action-row p,
.preference-contact__current {
  margin: 6px 0 0;
  color: #6b7480;
  line-height: 1.68;
}

.preference-contact {
  padding-top: 8px;
}

.preference-contact :deep(.el-select) {
  width: 100%;
  margin-top: 10px;
}

.preference-contact :deep(.el-input__wrapper),
:deep(.el-dialog .el-input__wrapper),
:deep(.el-dialog .el-textarea__inner) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: none;
  border: 1px solid rgba(132, 125, 104, 0.16);
  background: rgba(255, 255, 255, 0.88);
}

.preferences-actions {
  padding-top: 8px;
}

.settings-card :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.settings-card :deep(.el-button.is-plain) {
  --el-button-text-color: #5f685e;
  --el-button-bg-color: rgba(255, 251, 244, 0.86);
  --el-button-border-color: rgba(132, 125, 104, 0.16);
  --el-button-hover-text-color: #355f4d;
  --el-button-hover-bg-color: rgba(255, 252, 246, 1);
  --el-button-hover-border-color: rgba(63, 114, 93, 0.32);
}

.settings-card :deep(.el-switch.is-checked .el-switch__core) {
  border-color: #3f725d;
  background-color: #3f725d;
}

.danger-link {
  color: #9a675b;
}

.danger-link:hover {
  color: #85584d;
}

.action-row--subtle {
  align-items: flex-start;
}

@media (max-width: 760px) {
  .card-header,
  .preference-row,
  .action-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header :deep(.el-button),
  .action-row :deep(.el-button),
  .preferences-actions :deep(.el-button) {
    width: 100%;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .profile-page {
    gap: 24px;
    padding-bottom: 44px;
  }

  .profile-header {
    padding: 22px 18px 20px;
    border-radius: 24px;
  }
}
</style>
