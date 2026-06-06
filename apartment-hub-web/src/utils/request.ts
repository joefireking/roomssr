import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { Result } from '@/types'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let isRedirectingToLogin = false

http.interceptors.response.use(
  (response) => {
    const res = response.data as Result<unknown>
    if (res.code !== 200) {
      if (res.code === 401) {
        if (!isRedirectingToLogin) {
          isRedirectingToLogin = true
          const userStore = useUserStore()
          userStore.logout()
          ElMessage.warning('登录已过期，请重新登录')
          router.push('/login')
          setTimeout(() => { isRedirectingToLogin = false }, 1000)
        }
      } else {
        ElMessage.error(res.message || 'Error')
      }
      return Promise.reject(new Error(res.message))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      // Skip if already handled by the success interceptor above
      if (error.response?.data?.code === 401) {
        return Promise.reject(error)
      }
      if (!isRedirectingToLogin) {
        isRedirectingToLogin = true
        const userStore = useUserStore()
        userStore.logout()
        ElMessage.warning('登录已过期，请重新登录')
        router.push('/login')
        setTimeout(() => { isRedirectingToLogin = false }, 1000)
      }
    } else {
      ElMessage.error(error.response?.data?.message || error.message || 'Network Error')
    }
    return Promise.reject(error)
  }
)

export default http
