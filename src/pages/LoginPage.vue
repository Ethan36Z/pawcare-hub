<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { getDefaultRouteForRole, useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({
  email: typeof route.query.email === 'string' ? route.query.email : '',
  password: '',
  remember: true,
})

const isSubmitting = ref(false)
const errorMessage = ref('')

const registerSuccessMessage = computed(() =>
  route.query.registered === '1' ? 'Account created successfully. Please sign in.' : '',
)

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

async function handleLogin() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const { data } = await authApi.login({
      email: form.email,
      password: form.password,
    })

    authStore.setAuthenticatedUser({
      email: data.email,
      name: data.name,
      role: data.role,
      token: data.token,
    })

    const redirectTarget =
      typeof route.query.redirect === 'string'
        ? route.query.redirect
        : getDefaultRouteForRole(authStore.role)

    router.push(redirectTarget)
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to sign in right now.')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-shell">
      <div class="login-brand">
        <div class="brand-mark" aria-hidden="true">P</div>
        <div>
          <span class="brand-name">PawCare Hub</span>
          <p class="brand-note">Pet clinic booking and care coordination</p>
        </div>
      </div>

      <div class="login-header">
        <span class="eyebrow">Welcome back</span>
        <h1>Sign in to manage your pets and upcoming visits.</h1>
        <p>
          Access your bookings, review pet profiles, and stay prepared for your next clinic
          appointment.
        </p>
      </div>

      <el-alert
        v-if="registerSuccessMessage"
        :title="registerSuccessMessage"
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

      <el-form :model="form" label-position="top" class="login-form" @submit.prevent="handleLogin">
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
            placeholder="Enter your password"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="form.remember">Remember me</el-checkbox>
          <a href="#" class="text-link">Forgot password?</a>
        </div>

        <el-button
          type="primary"
          size="large"
          native-type="submit"
          class="sign-in-button"
          :loading="isSubmitting"
        >
          Sign In
        </el-button>
      </el-form>

      <p class="register-prompt">
        New to PawCare Hub?
        <RouterLink to="/register" class="text-link">Create an account</RouterLink>
      </p>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 0 56px;
}

.login-shell {
  width: min(520px, 100%);
  padding: 30px 28px 28px;
  border: 1px solid var(--pc-border);
  border-radius: var(--pc-radius-lg);
  background:
    radial-gradient(circle at top left, rgba(250, 249, 245, 0.9), transparent 34%),
    radial-gradient(circle at right center, rgba(201, 100, 66, 0.12), transparent 28%),
    linear-gradient(180deg, #faf9f5 0%, #f1efe7 100%);
  box-shadow: var(--pc-shadow);
}

.login-brand {
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

.login-header {
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

.login-header h1 {
  margin: 14px 0 0;
  color: var(--pc-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(2rem, 4vw, 2.8rem);
  line-height: 1.08;
  font-weight: 500;
  letter-spacing: -0.035em;
}

.login-header p {
  margin: 14px 0 0;
  color: var(--pc-muted);
  line-height: 1.75;
}

.login-form {
  margin-top: 26px;
}

.feedback-alert {
  margin-top: 22px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-form-item__label) {
  color: var(--pc-ink);
  font-weight: 600;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  box-shadow: none;
  background: rgba(250, 249, 245, 0.9);
  border: 1px solid var(--pc-border);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(201, 100, 66, 0.34);
  box-shadow: 0 0 0 3px rgba(201, 100, 66, 0.08);
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 6px 0 22px;
}

.login-options :deep(.el-checkbox__label) {
  color: var(--pc-muted);
}

.login-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--pc-primary);
  border-color: var(--pc-primary);
}

.sign-in-button {
  width: 100%;
}

.sign-in-button,
.login-shell :deep(.el-button--primary) {
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

.register-prompt {
  margin: 22px 0 0;
  color: var(--pc-muted);
  text-align: center;
}

@media (max-width: 640px) {
  .login-page {
    padding: 18px 0 40px;
  }

  .login-shell {
    padding: 22px 18px 20px;
    border-radius: 24px;
  }

  .login-options {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
