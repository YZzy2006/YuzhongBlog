import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'
import axios from 'axios'

let storageListenerAttached = false

function isTokenValid(token) {
  if (!token) return false
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 > Date.now()
  } catch {
    return false
  }
}

export const useAuthStore = defineStore('auth', () => {
  const storedToken = localStorage.getItem('accessToken') || ''
  const tokenValid = isTokenValid(storedToken)
  const accessToken = ref(tokenValid ? storedToken : '')
  // Keep refresh token even if access token expired — enables silent refresh on page reload
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  let parsedPermissions = []
  try {
    parsedPermissions = JSON.parse(localStorage.getItem('permissions') || '[]')
  } catch {
    parsedPermissions = []
  }
  const permissions = ref(parsedPermissions)
  const kickNotification = ref(null) // { deviceInfo, loginTime, role }
  const name = ref(localStorage.getItem('name') || '')
  const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')
  const bio = ref(localStorage.getItem('bio') || '')
  let kickPollingAbort = null

  function clearTokensAndStopPolling() {
    if (kickPollingAbort) {
      kickPollingAbort.abort()
      kickPollingAbort = null
    }
    accessToken.value = ''
    refreshToken.value = ''
    username.value = ''
    role.value = ''
    permissions.value = []
    name.value = ''
    avatarUrl.value = ''
    bio.value = ''
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('permissions')
    localStorage.removeItem('name')
    localStorage.removeItem('avatarUrl')
    localStorage.removeItem('bio')
  }

  function clearAuthState() {
    clearTokensAndStopPolling()
    kickNotification.value = null
  }

  // Clean up expired access token on startup (keep refresh token for silent refresh)
  if (storedToken && !tokenValid) {
    localStorage.removeItem('accessToken')
  }

  const isLoggedIn = computed(() => !!accessToken.value)
  const isSuperAdmin = computed(() => role.value === 'super_admin')
  const isVisitor = computed(() => role.value === 'visitor')

  function hasPermission(perm) {
    if (role.value === 'super_admin') return true
    return permissions.value.includes(perm)
  }

  function setLoginData(data) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    username.value = data.username
    role.value = data.role || 'admin'
    permissions.value = data.permissions || []
    name.value = data.name || ''
    avatarUrl.value = data.avatarUrl || ''
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role || 'admin')
    localStorage.setItem('permissions', JSON.stringify(data.permissions || []))
    localStorage.setItem('name', data.name || '')
    localStorage.setItem('avatarUrl', data.avatarUrl || '')
  }

  async function login(usernameVal, password) {
    clearTokensAndStopPolling()
    const data = await request.post('/admin/auth/login', { username: usernameVal, password })
    setLoginData(data)
  }

  async function refreshAccessToken() {
    const data = await request.post('/admin/auth/refresh', { refreshToken: refreshToken.value })
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    role.value = data.role || role.value
    if (data.permissions) permissions.value = data.permissions
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    if (data.role) localStorage.setItem('role', data.role)
    if (data.permissions) localStorage.setItem('permissions', JSON.stringify(data.permissions))
  }

  async function logout() {
    // Stop kick polling
    if (kickPollingAbort) {
      kickPollingAbort.abort()
      kickPollingAbort = null
    }
    // Call server-side logout to clear session store
    try {
      if (accessToken.value) {
        await request.post('/admin/logout', null, {
          headers: { 'Authorization': `Bearer ${accessToken.value}` }
        })
      }
    } catch {
      // Logout may fail if token is invalid/expired — local state is still cleared below
    }
    clearAuthState()
  }

  async function startKickPolling() {
    if (kickPollingAbort) kickPollingAbort.abort()
    kickPollingAbort = new AbortController()
    const signal = kickPollingAbort.signal

    while (accessToken.value && !signal.aborted) {
      try {
        const resp = await axios.get('/admin/session/kick-notification', {
          headers: { 'Authorization': `Bearer ${accessToken.value}` },
          signal,
          timeout: 10000
        })
        if (signal.aborted) return
        const data = resp.data?.data
        if (data) {
          // Save token for potential freeze action before it gets cleared
          data.token = accessToken.value
          kickNotification.value = data
          // Clear tokens so the old tab is effectively logged out (preserve kickNotification)
          clearTokensAndStopPolling()
          return // stop polling after receiving kick event
        }
        // No kick event — wait before next poll
        await new Promise(r => setTimeout(r, 5000))
      } catch (e) {
        if (signal.aborted || axios.isCancel(e)) return
        // 409 = session conflict (another device logged in) — extract kick info from response
        if (e.response?.status === 409) {
          const conflictData = e.response?.data
          let kickData = {}
          try {
            // Try to parse the message as JSON (new format with ip/loginMethod)
            kickData = JSON.parse(conflictData?.message || '{}')
          } catch {
            kickData = { deviceInfo: conflictData?.message || '' }
          }
          kickData.token = accessToken.value
          if (!kickData.role) kickData.role = role.value // fallback: use local role
          kickNotification.value = kickData
          clearTokensAndStopPolling()
          return
        }
        // 401 or network error — stop polling silently
        return
      }
    }
  }

  async function freezeAccount() {
    // Use saved token from kickNotification (current token may already be cleared)
    const savedToken = kickNotification.value?.token
    await axios.post('/admin/session/freeze-account', null, {
      headers: { 'Authorization': `Bearer ${savedToken}` }
    })
    clearAuthState()
  }

  // Listen for cross-tab login conflicts (same browser, different tabs)
  if (!storageListenerAttached) {
  storageListenerAttached = true
  window.addEventListener('storage', async (e) => {
    if (e.key !== 'accessToken') return
    // Another tab changed the token
    if (!e.newValue) {
      // Another tab logged out — sync logout
      if (accessToken.value) {
        clearAuthState()
      }
      return
    }
    if (e.newValue === accessToken.value) return // same token, ignore
    if (!accessToken.value) return // this tab already logged out

    // Another tab logged in with a new token — fetch kick notification using our OLD token
    // The kick event is stored atomically before the login response returns, so no delay needed
    try {
      const resp = await axios.get('/admin/session/kick-notification', {
        headers: { 'Authorization': `Bearer ${accessToken.value}` },
        timeout: 5000
      })
      const data = resp.data?.data
      if (data) {
        data.token = accessToken.value
        kickNotification.value = data
        clearTokensAndStopPolling()
      }
    } catch (e) {
      if (e.response?.status === 409) {
        // Session conflict — preserve kick notification, only clear tokens
        const conflictData = e.response?.data
        let kickData = {}
        try {
          kickData = JSON.parse(conflictData?.message || '{}')
        } catch {
          kickData = { deviceInfo: conflictData?.message || '' }
        }
        kickData.token = accessToken.value
        kickNotification.value = kickData
        clearTokensAndStopPolling()
      } else {
        clearAuthState()
      }
    }
  })
  } // end storageListenerAttached guard

  // Attempt silent token refresh on startup (call before router guard)
  async function trySilentRefresh() {
    if (accessToken.value || !refreshToken.value) return
    try {
      await refreshAccessToken()
    } catch {
      clearAuthState()
    }
  }

  // Validate session on page reload: token in localStorage may be valid JWT
  // but server session cleared (e.g. restart). Proactively check and redirect.
  async function validateSession() {
    if (!accessToken.value) return
    try {
      await request.get('/admin/profile')
    } catch (e) {
      // 401 = token invalid or session gone — already handled by interceptor
      // (interceptor tries refresh, if fails clears auth)
      // If auth was cleared by interceptor, isLoggedIn is now false
    }
  }

  return { accessToken, refreshToken, username, role, permissions, kickNotification, name, avatarUrl, bio, isLoggedIn, isSuperAdmin, isVisitor, hasPermission, login, setLoginData, refreshAccessToken, logout, startKickPolling, freezeAccount, trySilentRefresh, validateSession, clearTokensAndStopPolling }
})
