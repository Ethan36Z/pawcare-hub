<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authApi } from '@/api/auth'

const form = reactive({
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const router = useRouter()
const isSubmitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

async function handleRegister() {
  errorMessage.value = ''
  successMessage.value = ''

  if (form.password !== form.confirmPassword) {
    errorMessage.value = 'Passwords do not match.'
    return
  }

  isSubmitting.value = true

  try {
    await authApi.register({
      name: form.fullName,
      email: form.email,
      password: form.password,
    })

    successMessage.value = 'Account created successfully. Redirecting to sign in...'

    router.push({
      path: '/login',
      query: {
        registered: '1',
        email: form.email,
      },
    })
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to create your account right now.')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <section class="register-shell">
      <div class="register-brand">
        <div class="brand-mark" aria-hidden="true">P</div>
        <div>
          <span class="brand-name">PawCare Hub</span>
          <p class="brand-note">Pet clinic booking and care coordination</p>
        </div>
      </div>

      <div class="register-header">
        <span class="eyebrow">Create your account</span>
        <h1>Set up your account to book care and manage your pets.</h1>
        <p>
          Create a PawCare Hub account to schedule appointments, review visit details, and keep
          your pet profiles in one place.
        </p>
      </div>

      <el-alert
        v-if="successMessage"
        :title="successMessage"
        type="success"
        :closable="false"
        class="feedback-alert"
      />

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        class="feedback-alert"
      />

      <el-form
        :model="form"
        label-position="top"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="Full name">
          <el-input
            v-model="form.fullName"
            placeholder="Your full name"
            autocomplete="name"
          />
        </el-form-item>

        <el-form-item label="Email">
          <el-input
            v-model="form.email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
          />
        </el-form-item>

        <el-form-item label="Password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Create a password"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="Confirm password">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="Re-enter your password"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          native-type="submit"
          class="create-account-button"
          :loading="isSubmitting"
        >
          Create Account
        </el-button>
      </el-form>

      <p class="login-prompt">
        Already have an account?
        <RouterLink to="/login" class="text-link">Sign in</RouterLink>
      </p>
    </section>
  </div>
</template>

<style scoped>
.register-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 0 56px;
}

.register-shell {
  width: min(540px, 100%);
  padding: 30px 28px 28px;
  border: 1px solid var(--pc-border);
  border-radius: var(--pc-radius-lg);
  background:
    radial-gradient(circle at top left, rgba(250, 249, 245, 0.9), transparent 34%),
    radial-gradient(circle at right center, rgba(201, 100, 66, 0.12), transparent 28%),
    linear-gradient(180deg, #faf9f5 0%, #f1efe7 100%);
  box-shadow: var(--pc-shadow);
}

.register-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: linear-gradient(145deg, var(--pc-primary) 0%, #d4805f 100%);
  box-shadow: 0 10px 22px rgba(201, 100, 66, 0.18);
  color: #ffffff;
  font-size: 1.1rem;
  font-weight: 700;
}

.brand-name {
  display: block;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.08rem;
  font-weight: 500;
}

.brand-note {
  margin: 4px 0 0;
  color: var(--pc-soft-muted);
  font-size: 0.92rem;
}

.register-header {
  margin-top: 26px;
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

.register-header h1 {
  margin: 14px 0 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(2rem, 4vw, 2.8rem);
  line-height: 1.08;
  font-weight: 500;
  letter-spacing: -0.035em;
}

.register-header p {
  margin: 14px 0 0;
  color: var(--pc-muted);
  line-height: 1.75;
}

.register-form {
  margin-top: 26px;
}

.feedback-alert {
  margin-top: 22px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-form :deep(.el-form-item__label) {
  color: var(--pc-ink);
  font-weight: 600;
}

.register-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  box-shadow: none;
  background: rgba(250, 249, 245, 0.9);
  border: 1px solid var(--pc-border);
}

.register-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(201, 100, 66, 0.34);
  box-shadow: 0 0 0 3px rgba(201, 100, 66, 0.08);
}

.create-account-button {
  width: 100%;
}

.create-account-button,
.register-shell :deep(.el-button--primary) {
  --el-button-bg-color: var(--pc-primary);
  --el-button-border-color: var(--pc-primary);
  --el-button-hover-bg-color: var(--pc-primary-hover);
  --el-button-hover-border-color: var(--pc-primary-hover);
  --el-button-active-bg-color: #a94e33;
  --el-button-active-border-color: #a94e33;
  box-shadow: 0 12px 24px rgba(201, 100, 66, 0.16);
}

.text-link {
  color: var(--pc-primary);
  text-decoration: none;
}

.text-link:hover {
  text-decoration: underline;
}

.login-prompt {
  margin: 22px 0 0;
  color: var(--pc-muted);
  text-align: center;
}

@media (max-width: 640px) {
  .register-page {
    padding: 18px 0 40px;
  }

  .register-shell {
    padding: 22px 18px 20px;
    border-radius: 24px;
  }
}
</style>
