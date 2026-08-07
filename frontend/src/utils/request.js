import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

function clearAuth() {
  const authStore = useAuthStore()
  authStore.clearTokensAndStopPolling()
  router.push('/admin/login')
}

// Prevent infinite refresh loops
let isRefreshing = false
let refreshQueue = [] // stores { resolve, reject } pairs

request.interceptors.request.use(config => {
  const authStore = useAuthStore()
  config.headers.Accept = 'application/json'
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    const err = new Error(message || '请求失败')
    err.code = code
    return Promise.reject(err)
  },
  async error => {
    const originalConfig = error.config

    if (error.response?.status === 401 && !originalConfig._retried) {
      const authStore = useAuthStore()

      // If the refresh endpoint itself returns 401, logout immediately
      if (originalConfig.url === '/admin/auth/refresh') {
        isRefreshing = false
        refreshQueue.forEach(({ reject }) => reject(error))
        refreshQueue = []
        clearAuth()
        return Promise.reject(error)
      }

      // Try refresh token if available
      if (authStore.refreshToken && !isRefreshing) {
        isRefreshing = true
        originalConfig._retried = true
        try {
          await authStore.refreshAccessToken()
          isRefreshing = false
          // Retry queued requests
          refreshQueue.forEach(({ resolve, config }) => {
            config.headers.Authorization = `Bearer ${authStore.accessToken}`
            config._retried = true
            resolve(request(config))
          })
          refreshQueue = []
          // Retry original request
          originalConfig.headers.Authorization = `Bearer ${authStore.accessToken}`
          return request(originalConfig)
        } catch (refreshError) {
          isRefreshing = false
          // Reject all queued requests
          refreshQueue.forEach(({ reject }) => reject(refreshError))
          refreshQueue = []
          // Refresh failed — fall through to logout
        }
      } else if (isRefreshing) {
        // Queue this request until refresh completes
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject, config: originalConfig })
        })
      }

      // No refresh token or refresh failed — logout and redirect to login
      if (authStore.accessToken) {
        authStore.clearTokensAndStopPolling()
        if (router.currentRoute.value.path.startsWith('/admin')) {
          router.push('/admin/login')
        }
      }
      return Promise.reject(error)
    }
    // 提取服务器返回的真实错误信息（如 Bean Validation 校验信息），让 400/500 不再是笼统的 "status code 400"
    if (error.response && error.response.status !== 401 && error.response.status !== 409) {
      const data = error.response.data
      const serverMsg = data?.message || data?.msg || (typeof data === 'string' ? data : '')
      if (serverMsg) {
        error.message = serverMsg
      }
      return Promise.reject(error)
    }
    if (error.response?.status === 409) {
      // Session conflict — another device logged in
      const conflictMsg = error.response?.data?.message || ''
      const authStore = useAuthStore()
      const currentRole = authStore.role

      // Try to parse as JSON (new format), fallback to legacy regex
      let deviceInfo = '未知设备'
      let loginTime = '未知时间'
      let ip = ''
      let loginMethod = ''
      try {
        const parsed = JSON.parse(conflictMsg)
        deviceInfo = parsed.deviceInfo || deviceInfo
        loginTime = parsed.loginTime || loginTime
        ip = parsed.ip || ''
        loginMethod = parsed.loginMethod || ''
      } catch {
        // Legacy format: "Chrome · Windows · PC（登录时间：2024-01-01 12:00:00）"
        const timeMatch = conflictMsg.match(/登录时间[：:](.+?)）/)
        deviceInfo = conflictMsg.replace(/（登录时间.+$/, '') || deviceInfo
        loginTime = timeMatch ? timeMatch[1] : loginTime
      }

      // Set kick notification BEFORE clearing auth, save token for freeze action
      authStore.kickNotification = {
        deviceInfo, loginTime, role: currentRole, ip, loginMethod,
        token: authStore.accessToken
      }
      // Clear tokens but DON'T redirect — let KickNotification dialog handle it
      authStore.clearTokensAndStopPolling()
      return Promise.reject(error)
    }
    return Promise.reject(error)
  }
)

export default request
