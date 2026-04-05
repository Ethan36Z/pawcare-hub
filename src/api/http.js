import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  const userEmail = authStore.user?.email

  if (userEmail) {
    config.headers = config.headers ?? {}
    if (!config.headers['X-User-Email']) {
      config.headers['X-User-Email'] = userEmail
    }
  }

  return config
})

export default http
