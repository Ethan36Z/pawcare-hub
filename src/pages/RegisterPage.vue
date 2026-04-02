<script setup>
import { reactive } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getDefaultRouteForRole, useAuthStore } from '@/stores/auth'

const form = reactive({
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const authStore = useAuthStore()
const router = useRouter()

function handleRegister() {
  authStore.login({
    fullName: form.fullName,
    email: form.email,
  })

  router.push(getDefaultRouteForRole(authStore.role))
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
  border: 1px solid rgba(28, 60, 88, 0.1);
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba(255, 239, 194, 0.88), transparent 34%),
    radial-gradient(circle at right center, rgba(244, 228, 196, 0.38), transparent 26%),
    linear-gradient(180deg, #fff9ee 0%, #f8efdc 100%);
  box-shadow: 0 20px 40px rgba(21, 40, 61, 0.08);
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

.register-header {
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

.register-header h1 {
  margin: 14px 0 0;
  color: #173047;
  font-size: clamp(2rem, 4vw, 2.8rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.register-header p {
  margin: 14px 0 0;
  color: #6b7480;
  line-height: 1.75;
}

.register-form {
  margin-top: 26px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-form :deep(.el-form-item__label) {
  color: #385145;
  font-weight: 600;
}

.register-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(132, 125, 104, 0.16);
}

.register-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(63, 114, 93, 0.28);
  box-shadow: 0 0 0 3px rgba(63, 114, 93, 0.08);
}

.create-account-button {
  width: 100%;
}

.create-account-button,
.register-shell :deep(.el-button--primary) {
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

.login-prompt {
  margin: 22px 0 0;
  color: #6b7480;
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
