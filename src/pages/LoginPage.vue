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
  border: 1px solid rgba(28, 60, 88, 0.1);
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba(255, 239, 194, 0.88), transparent 34%),
    radial-gradient(circle at right center, rgba(244, 228, 196, 0.38), transparent 26%),
    linear-gradient(180deg, #fff9ee 0%, #f8efdc 100%);
  box-shadow: 0 20px 40px rgba(21, 40, 61, 0.08);
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
  background: linear-gradient(145deg, #2f6b52 0%, #4f8a70 100%);
  box-shadow: 0 10px 22px rgba(72, 92, 63, 0.12);
  color: #ffffff;
  font-size: 1.1rem;
  font-weight: 700;
}

.brand-name {
  display: block;
  color: #385145;
  font-size: 1.08rem;
  font-weight: 700;
}

.brand-note {
  margin: 4px 0 0;
  color: #7a7468;
  font-size: 0.92rem;
}

.login-header {
  margin-top: 26px;
}

.eyebrow {
  display: inline-block;
  color: #7f7356;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-header h1 {
  margin: 14px 0 0;
  color: #173047;
  font-size: clamp(2rem, 4vw, 2.8rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.login-header p {
  margin: 14px 0 0;
  color: #6b7480;
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
  color: #385145;
  font-weight: 600;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(132, 125, 104, 0.16);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(63, 114, 93, 0.28);
  box-shadow: 0 0 0 3px rgba(63, 114, 93, 0.08);
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 6px 0 22px;
}

.login-options :deep(.el-checkbox__label) {
  color: #5f685e;
}

.login-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #3f725d;
  border-color: #3f725d;
}

.sign-in-button {
  width: 100%;
}

.sign-in-button,
.login-shell :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.text-link {
  color: #355f4d;
  text-decoration: none;
}

.text-link:hover {
  text-decoration: underline;
}

.register-prompt {
  margin: 22px 0 0;
  color: #6b7480;
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
