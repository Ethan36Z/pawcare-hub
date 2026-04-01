import { createRouter, createWebHistory } from 'vue-router'

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
        meta: { title: 'Pets' },
      },
      {
        path: 'bookings',
        name: 'my-bookings',
        component: () => import('@/pages/MyBookingsPage.vue'),
        meta: { title: 'My Bookings' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/pages/ProfilePage.vue'),
        meta: { title: 'Profile' },
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
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/pages/admin/AdminDashboardPage.vue'),
        meta: { title: 'Admin Dashboard' },
      },
      {
        path: 'services',
        name: 'admin-services',
        component: () => import('@/pages/admin/AdminServicesPage.vue'),
        meta: { title: 'Admin Services' },
      },
      {
        path: 'bookings',
        name: 'admin-bookings',
        component: () => import('@/pages/admin/AdminBookingsPage.vue'),
        meta: { title: 'Admin Bookings' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/pages/admin/AdminUsersPage.vue'),
        meta: { title: 'Admin Users' },
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

router.afterEach((to) => {
  document.title = to.meta.title ? `PawCare Hub | ${to.meta.title}` : 'PawCare Hub'
})

export default router
