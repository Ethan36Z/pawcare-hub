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
  newEmail: '',
  currentPassword: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})

const contactOptions = ['Email', 'Text message', 'Phone call']

const isLoading = ref(false)
const isSavingProfile = ref(false)
const isSavingPreferences = ref(false)
const isChangingPassword = ref(false)
const isDeletingAccount = ref(false)
const isEditDialogOpen = ref(false)
const isPasswordDialogOpen = ref(false)
const passwordErrorMessage = ref('')
const editErrorMessage = ref('')
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
  editForm.newEmail = ''
  editForm.currentPassword = ''
  editErrorMessage.value = ''
}

function resetPasswordForm() {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmNewPassword = ''
  passwordErrorMessage.value = ''
}

async function loadProfile() {
  if (!authStore.user?.email) {
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const { data } = await authApi.profile()
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

function openPasswordDialog() {
  successMessage.value = ''
  resetPasswordForm()
  isPasswordDialogOpen.value = true
}

async function handleSaveProfile() {
  if (!authStore.user?.email) {
    return
  }

  isSavingProfile.value = true
  errorMessage.value = ''
  editErrorMessage.value = ''
  successMessage.value = ''

  try {
    const { data } = await authApi.updateProfile({
      phone: editForm.phone,
      address: editForm.address,
      preferredContactMethod: profile.contactMethod,
      emailReminders: profile.emailReminders,
      textReminders: profile.textReminders,
    })

    applyProfileData(data)

    const nextEmail = editForm.newEmail.trim()

    if (nextEmail) {
      const response = await authApi.changeEmail({
        currentPassword: editForm.currentPassword,
        newEmail: nextEmail,
      })

      isEditDialogOpen.value = false
      authStore.logout()
      router.push({
        path: '/login',
        query: {
          email: nextEmail,
          emailChanged: '1',
        },
      })
      return response
    }

    isEditDialogOpen.value = false
    successMessage.value = 'Profile details updated.'
  } catch (error) {
    editErrorMessage.value = getApiErrorMessage(error, 'Unable to save your profile right now.')
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
    const { data } = await authApi.updateProfile({
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

async function handleChangePassword() {
  if (!authStore.user?.email) {
    return
  }

  passwordErrorMessage.value = ''
  errorMessage.value = ''
  successMessage.value = ''

  if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
    passwordErrorMessage.value = 'New passwords do not match.'
    return
  }

  isChangingPassword.value = true

  try {
    await authApi.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })

    isPasswordDialogOpen.value = false
    resetPasswordForm()
    successMessage.value = 'Password updated successfully.'
  } catch (error) {
    passwordErrorMessage.value = getApiErrorMessage(error, 'Unable to change your password right now.')
  } finally {
    isChangingPassword.value = false
  }
}

async function handleDeleteAccount() {
  if (!authStore.user?.email || isDeletingAccount.value) {
    return
  }

  const confirmed = window.confirm(
    'Delete this account? You will immediately lose access to PawCare Hub, and you will not be able to log in again.'
  )

  if (!confirmed) {
    return
  }

  isDeletingAccount.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await authApi.deleteAccount()
    authStore.logout()
    router.push('/login')
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to delete your account right now.')
  } finally {
    isDeletingAccount.value = false
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
              <el-button plain @click="openPasswordDialog">Change Password</el-button>
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
                <p>Deactivate this account and immediately sign out of PawCare Hub.</p>
              </div>
              <el-button
                text
                class="danger-link"
                :loading="isDeletingAccount"
                @click="handleDeleteAccount"
              >
                Delete Account
              </el-button>
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
        <el-alert
          v-if="editErrorMessage"
          :title="editErrorMessage"
          type="error"
          :closable="false"
          class="password-alert"
        />

        <el-form label-position="top">
          <el-form-item label="Phone number">
            <el-input v-model="editForm.phone" placeholder="Phone number" />
          </el-form-item>
          <el-form-item label="Address">
            <el-input v-model="editForm.address" type="textarea" :rows="3" placeholder="Address" />
          </el-form-item>
          <el-form-item label="New email">
            <el-input
              v-model="editForm.newEmail"
              type="email"
              placeholder="Enter a new email address"
            />
            <p class="dialog-helper">Leave blank if you are not changing your email.</p>
          </el-form-item>
          <el-form-item label="Current password">
            <el-input
              v-model="editForm.currentPassword"
              type="password"
              show-password
              placeholder="Required to change your email"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="isEditDialogOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="isSavingProfile" @click="handleSaveProfile">
            Save Changes
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="isPasswordDialogOpen"
        title="Change Password"
        width="min(560px, 92vw)"
        @closed="resetPasswordForm"
      >
        <el-alert
          v-if="passwordErrorMessage"
          :title="passwordErrorMessage"
          type="error"
          :closable="false"
          class="password-alert"
        />

        <el-form label-position="top">
          <el-form-item label="Current password">
            <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              show-password
              placeholder="Enter your current password"
            />
          </el-form-item>
          <el-form-item label="New password">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="Enter a new password"
            />
          </el-form-item>
          <el-form-item label="Confirm new password">
            <el-input
              v-model="passwordForm.confirmNewPassword"
              type="password"
              show-password
              placeholder="Re-enter your new password"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="isPasswordDialogOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="isChangingPassword" @click="handleChangePassword">
            Update Password
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
  border: 1px solid var(--pc-border);
  border-radius: var(--pc-radius-lg);
  background:
    radial-gradient(circle at top left, rgba(250, 249, 245, 0.9), transparent 34%),
    radial-gradient(circle at right center, rgba(201, 100, 66, 0.12), transparent 28%),
    linear-gradient(180deg, #faf9f5 0%, #f1efe7 100%);
  box-shadow: var(--pc-shadow);
}

.profile-header__copy {
  max-width: 720px;
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

.profile-header h1 {
  margin: 14px 0 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(2.15rem, 4vw, 3.3rem);
  line-height: 1.08;
  font-weight: 500;
  letter-spacing: -0.035em;
}

.profile-header p {
  max-width: 640px;
  margin: 16px 0 0;
  color: var(--pc-muted);
  line-height: 1.75;
}

.profile-grid {
  display: grid;
  gap: 22px;
}

.settings-card {
  border: 1px solid var(--pc-border-soft);
  border-radius: var(--pc-radius-md);
  background: rgba(250, 249, 245, 0.92);
  box-shadow: var(--pc-shadow-soft);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.settings-card:hover {
  transform: translateY(-2px);
  border-color: rgba(201, 100, 66, 0.22);
  box-shadow: var(--pc-shadow);
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
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.38rem;
  font-weight: 500;
}

.card-header p {
  margin: 8px 0 0;
  color: var(--pc-muted);
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
  color: var(--pc-soft-muted);
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.details-grid strong {
  display: block;
  margin-top: 8px;
  color: var(--pc-ink);
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
  border-top: 1px solid var(--pc-border);
}

.preference-row:first-child,
.action-row:first-child {
  border-top: 0;
  padding-top: 0;
}

.preference-row strong,
.action-row strong {
  color: var(--pc-ink);
}

.preference-row p,
.action-row p,
.preference-contact__current {
  margin: 6px 0 0;
  color: var(--pc-muted);
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
  border: 1px solid var(--pc-border);
  background: rgba(250, 249, 245, 0.88);
}

.preferences-actions {
  padding-top: 8px;
}

.password-alert {
  margin-bottom: 16px;
}

.dialog-helper {
  margin: 8px 0 0;
  color: var(--pc-muted);
  font-size: 0.92rem;
  line-height: 1.55;
}

.settings-card :deep(.el-button--primary) {
  --el-button-bg-color: var(--pc-primary);
  --el-button-border-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-primary-hover);
  --el-button-hover-border-color: var(--pc-primary-hover);
  --el-button-active-bg-color: #a94e33;
  --el-button-active-border-color: #a94e33;
  box-shadow: 0 10px 22px rgba(201, 100, 66, 0.16);
}

.settings-card :deep(.el-button.is-plain) {
  --el-button-text-color: var(--pc-text);
  --el-button-bg-color: rgba(250, 249, 245, 0.9);
  --el-button-border-color: var(--pc-border);
  --el-button-hover-text-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-surface);
  --el-button-hover-border-color: rgba(201, 100, 66, 0.34);
}

.settings-card :deep(.el-switch.is-checked .el-switch__core) {
  border-color: var(--pc-primary);
  background-color: var(--pc-primary);
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
