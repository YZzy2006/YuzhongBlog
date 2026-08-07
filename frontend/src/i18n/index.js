import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: { 'zh-CN': zhCN, 'en-US': enUS }
})

// Separate storage keys for admin and frontend
const STORAGE_KEY = 'locale'
const ADMIN_STORAGE_KEY = 'adminLocale'

export function getLocale(isAdmin) {
  const key = isAdmin ? ADMIN_STORAGE_KEY : STORAGE_KEY
  return localStorage.getItem(key) || 'zh-CN'
}

export function setLocale(locale, isAdmin) {
  const key = isAdmin ? ADMIN_STORAGE_KEY : STORAGE_KEY
  localStorage.setItem(key, locale)
  i18n.global.locale.value = locale
}

export function toggleLocale(isAdmin) {
  const current = i18n.global.locale.value
  const next = current === 'zh-CN' ? 'en-US' : 'zh-CN'
  setLocale(next, isAdmin)
  return next
}

export function initLocale(isAdmin) {
  const locale = getLocale(isAdmin)
  i18n.global.locale.value = locale
  return locale
}

export default i18n
