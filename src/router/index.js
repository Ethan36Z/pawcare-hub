import { createRouter, createWebHistory } from 'vue-router'
import { getDefaultRouteForRole, useAuthStore } from '@/stores/auth'

function getRoleRedirectPath(to, role) {
  if (to.path.startsWith('/admin')) {
    return ['admin', 'front_desk', 'doctor'].includes(role) ? '/admin' : '/pets'
  }

  return getDefaultRouteForRole(role)
}

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/pages/HomePage.vue'),
        meta: { title: 'Home' },
      },
      {
        path: 'services',
        name: 'services',
        component: () => import('@/pages/ServicesPage.vue'),
        meta: { title: 'Services' },
      },
      {
        path: 'pets',
        name: 'pets',
        component: () => import('@/pages/PetsPage.vue'),
        meta: { title: 'Pets', requiresAuth: true, allowedRoles: ['user'] },
      },
      {
        path: 'bookings',
        name: 'my-bookings',
        component: () => import('@/pages/MyBookingsPage.vue'),
        meta: { title: 'My Bookings', requiresAuth: true, allowedRoles: ['user'] },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/pages/ProfilePage.vue'),
        meta: { title: 'Profile', requiresAuth: true, allowedRoles: ['user'] },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/pages/LoginPage.vue'),
        meta: { title: 'Login' },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/pages/RegisterPage.vue'),
        meta: { title: 'Register' },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, allowedRoles: ['admin', 'front_desk', 'doctor'] },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/pages/admin/AdminDashboardPage.vue'),
        meta: { title: 'Admin Dashboard', allowedRoles: ['admin', 'front_desk', 'doctor'] },
      },
      {
        path: 'services',
        name: 'admin-services',
        component: () => import('@/pages/admin/AdminServicesPage.vue'),
        meta: { title: 'Admin Services', allowedRoles: ['admin'] },
      },
      {
        path: 'bookings',
        name: 'admin-bookings',
        component: () => import('@/pages/admin/AdminBookingsPage.vue'),
        meta: { title: 'Admin Bookings', allowedRoles: ['admin', 'front_desk', 'doctor'] },
      },
      {
        path: 'staff',
        name: 'admin-staff',
        component: () => import('@/pages/admin/AdminStaffPage.vue'),
        meta: { title: 'Admin Staff', allowedRoles: ['admin'] },
      },
      {
        path: 'operations',
        name: 'admin-operations',
        component: () => import('@/pages/admin/AdminClinicOperationsPage.vue'),
        meta: { title: 'Clinic Operations', allowedRoles: ['admin', 'front_desk'] },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/pages/admin/AdminUsersPage.vue'),
        meta: { title: 'Admin Users', allowedRoles: ['admin'] },
      },
      {
        path: 'users/:id',
        name: 'admin-user-details',
        component: () => import('@/pages/admin/AdminUserDetailsPage.vue'),
        meta: { title: 'Admin User Details', allowedRoles: ['admin'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const destinationForRole = getDefaultRouteForRole(authStore.role)
  const hasDisallowedRole = to.matched.some((record) => {
    const allowedRoles = record.meta.allowedRoles ?? []
    return allowedRoles.length > 0 && !allowedRoles.includes(authStore.role)
  })

  if (requiresAuth && !authStore.isLoggedIn) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (authStore.isLoggedIn && (to.name === 'login' || to.name === 'register')) {
    return destinationForRole
  }

  if (hasDisallowedRole) {
    return getRoleRedirectPath(to, authStore.role)
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `PawCare Hub | ${to.meta.title}` : 'PawCare Hub'
})

export default router
