<template>
  <div class="main-layout" :class="{ night: isNight, 'is-cosmos': isCosmos, 'is-not-found': isNotFound }">
    <MidnightSky v-if="!isCosmos" :isNight="isNight" />
    <Transition name="back-fade">
      <button v-show="canGoBack" class="back-btn" @click="goBack" :title="$t('nav.back')">
        <div class="back-btn-box">
          <span class="back-btn-elem">
            <svg viewBox="0 0 46 40" xmlns="http://www.w3.org/2000/svg"><path d="M46 20.038c0-.7-.3-1.5-.8-2.1l-16-17c-1.1-1-3.2-1.4-4.4-.3-1.2 1.1-1.2 3.3 0 4.4l11.3 11.9H3c-1.7 0-3 1.3-3 3s1.3 3 3 3h33.1l-11.3 11.9c-1 1-1.2 3.3 0 4.4 1.2 1.1 3.3.8 4.4-.3l16-17c.5-.5.8-1.1.8-1.9z"/></svg>
          </span>
          <span class="back-btn-elem">
            <svg viewBox="0 0 46 40" xmlns="http://www.w3.org/2000/svg"><path d="M46 20.038c0-.7-.3-1.5-.8-2.1l-16-17c-1.1-1-3.2-1.4-4.4-.3-1.2 1.1-1.2 3.3 0 4.4l11.3 11.9H3c-1.7 0-3 1.3-3 3s1.3 3 3 3h33.1l-11.3 11.9c-1 1-1.2 3.3 0 4.4 1.2 1.1 3.3.8 4.4-.3l16-17c.5-.5.8-1.1.8-1.9z"/></svg>
          </span>
        </div>
      </button>
    </Transition>
    <header class="header">
      <div class="header-accent"></div>
      <div class="container">
        <div class="logo">
          <GhostAnimation />
          <router-link to="/" class="logo-text">{{ $t('nav.brand') }}</router-link>
        </div>
        <router-link v-if="authStore.isLoggedIn" to="/admin" class="admin-entry-btn" :title="$t('nav.goToAdmin')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 3v18"/><path d="m16 15-3-3 3-3"/></svg>
          <span>{{ $t('nav.adminPanel') }}</span>
        </router-link>
        <button class="mobile-toggle" @click="mobileOpen = !mobileOpen" :class="{ active: mobileOpen }">
          <span></span><span></span><span></span>
        </button>
        <nav class="nav" :class="{ open: mobileOpen }">
          <div class="nav-tabs">
            <router-link v-for="(tab, i) in navTabs" :key="tab.to" :to="tab.to"
              class="nav-tab" :class="{ active: activeTab === i }"
              @click="mobileOpen = false">
              {{ tab.label }}
            </router-link>
            <span class="nav-glider" :style="{ transform: `translateX(${activeTab * 100}%)` }"></span>
          </div>
          <router-link to="/articles" class="nav-search" @click="mobileOpen = false" :title="$t('nav.search')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          </router-link>
          <button class="lang-switch" @click="toggleLocale" :title="$t('nav.switchLang')">
            {{ locale === 'zh-CN' ? 'EN' : '中' }}
          </button>
          <label class="theme-switch" :title="$t('nav.toggleTheme')">
            <input type="checkbox" :checked="!isNight" @change="toggleNight" />
            <span class="theme-slider">
              <div class="theme-star theme-star_1"></div>
              <div class="theme-star theme-star_2"></div>
              <div class="theme-star theme-star_3"></div>
              <svg viewBox="0 0 16 16" class="theme-cloud">
                <path transform="matrix(.77976 0 0 .78395-299.99-418.63)" fill="#fff" d="m391.84 540.91c-.421-.329-.949-.524-1.523-.524-1.351 0-2.451 1.084-2.485 2.435-1.395.526-2.388 1.88-2.388 3.466 0 1.874 1.385 3.423 3.182 3.667v.034h12.73v-.006c1.775-.104 3.182-1.584 3.182-3.395 0-1.747-1.309-3.186-2.994-3.379.007-.106.011-.214.011-.322 0-2.707-2.271-4.901-5.072-4.901-2.073 0-3.856 1.202-4.643 2.925"></path>
              </svg>
            </span>
          </label>
        </nav>
      </div>
    </header>
    <main class="main">
      <div class="container">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <keep-alive :include="cachedPages" :max="8">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </main>
    <footer v-if="!isCosmos" class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-brand">
            <span class="footer-logo">{{ $t('nav.brand') }}</span>
            <p>{{ $t('about.intro') }}</p>
          </div>
          <div class="footer-links">
            <div class="footer-col">
              <h4>{{ $t('nav.home') }}</h4>
              <router-link to="/">{{ $t('nav.home') }}</router-link>
              <router-link to="/articles">{{ $t('nav.articles') }}</router-link>
              <router-link to="/photowall">{{ $t('nav.photowall') }}</router-link>
              <router-link to="/projects">{{ $t('nav.projects') }}</router-link>
            </div>
            <div class="footer-col">
              <h4>{{ $t('nav.about') }}</h4>
              <router-link to="/archive">{{ $t('nav.archive') }}</router-link>
              <router-link to="/moments">{{ $t('nav.moments') }}</router-link>
              <router-link to="/about">{{ $t('nav.about') }}</router-link>
            </div>
          </div>
        </div>
      </div>
    </footer>
    <FloatingPlayer v-if="!isCosmos" />
    <AiAssistant v-if="!isCosmos" />
    <CommandPalette v-if="!isCosmos" />
    <KickNotification />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
const CommandPalette = defineAsyncComponent(() => import('../components/CommandPalette.vue'))
const AiAssistant = defineAsyncComponent(() => import('../components/AiAssistant.vue'))
import GhostAnimation from '../components/GhostAnimation.vue'
import MidnightSky from '../components/MidnightSky.vue'
import KickNotification from '../components/KickNotification.vue'
const FloatingPlayer = defineAsyncComponent(() => import('../components/FloatingPlayer.vue'))
import { useAuthStore } from '../stores/auth'

const { locale } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const mobileOpen = ref(false)

const cachedPages = ['Home', 'Articles', 'Archive', 'Moments', 'Friends', 'Projects', 'PhotoWall', 'Music', 'Announcements', 'Search']

function toggleLocale() {
  const next = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = next
  localStorage.setItem('locale', next)
}

// Night mode detection
const isNight = ref(false)
let nightTimer = null

function applyNight(val) {
  isNight.value = val
  document.body.classList.toggle('body-night', val)
}

function checkTime() {
  const saved = localStorage.getItem('theme')
  if (saved === 'dark') { applyNight(true); return }
  if (saved === 'light') { applyNight(false); return }
  const h = new Date().getHours()
  applyNight(h >= 19 || h < 6)
}

function toggleNight(e) {
  const val = !e.target.checked
  applyNight(val)
  localStorage.setItem('theme', val ? 'dark' : 'light')
}

onMounted(() => {
  checkTime()
  nightTimer = setInterval(checkTime, 60000)
  // Start kick notification polling if logged in
  if (authStore.isLoggedIn) {
    authStore.startKickPolling()
  }
})
onBeforeUnmount(() => {
  clearInterval(nightTimer)
  document.body.classList.remove('body-night')
})

const { t } = useI18n()

const navTabs = computed(() => [
  { to: '/', label: t('nav.home') },
  { to: '/articles', label: t('nav.articles') },
  { to: '/music', label: t('nav.music') },
  { to: '/photowall', label: t('nav.photowall') },
  { to: '/projects', label: t('nav.projects') },
  { to: '/archive', label: t('nav.archive') },
  { to: '/moments', label: t('nav.moments') },
  { to: '/friends', label: t('nav.friends') },
  { to: '/about', label: t('nav.about') },
  { to: '/cosmos', label: t('nav.cosmos') },
])

const activeTab = computed(() => {
  const path = route.path
  if (path === '/') return 0
  const idx = navTabs.value.findIndex((t, i) => i > 0 && path.startsWith(t.to))
  return idx >= 0 ? idx : 0
})

const isCosmos = computed(() => route.path === '/cosmos')
const isNotFound = computed(() => route.name === 'NotFound')
const canGoBack = computed(() => route.path !== '/')

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-accent {
  height: 2px;
  background: linear-gradient(to right, var(--color-primary), #3b82f6, var(--color-primary));
}
.header .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
  height: 56px;
}
.logo {
  font-size: 1.05rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  letter-spacing: -0.01em;
}
.logo-text {
  color: var(--color-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}
.logo-text:hover {
  color: var(--color-primary);
}
.nav {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.nav-tabs {
  display: flex;
  position: relative;
  background: #fff;
  box-shadow: 0 0 1px 0 rgba(24, 94, 224, 0.15), 0 4px 10px 0 rgba(24, 94, 224, 0.1);
  padding: 0.35rem;
  border-radius: 99px;
}
.nav-tabs * {
  z-index: 2;
}
.nav-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  padding: 0 0.85rem;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  font-weight: 500;
  border-radius: 99px;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s ease-in;
  white-space: nowrap;
  position: relative;
  z-index: 2;
}
.nav-tab:hover {
  color: var(--color-text);
}
.nav-tab.active {
  color: #185ee0;
}
.nav-glider {
  position: absolute;
  height: 30px;
  width: calc(100% / 10);
  left: 0;
  top: 0.35rem;
  background: #e6eef9;
  z-index: 1;
  border-radius: 99px;
  transition: transform 0.25s ease-out;
}
.nav-search {
  display: flex;
  align-items: center;
  padding: 0.4rem 0.5rem;
  color: var(--color-text-secondary);
  transition: color var(--transition-fast);
}
.nav-search:hover {
  color: var(--color-primary);
  background: transparent;
}

/* Language Switch */
.lang-switch {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  line-height: 1.4;
}
.lang-switch:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.night .lang-switch {
  border-color: rgba(255, 255, 255, 0.15);
  color: #a0aec0;
}
.night .lang-switch:hover {
  border-color: #5a9bff;
  color: #5a9bff;
}

/* Admin entry button */
.admin-entry-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: all 0.2s;
  white-space: nowrap;
}
.admin-entry-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.night .admin-entry-btn {
  border-color: rgba(255, 255, 255, 0.15);
  color: #a0aec0;
}
.night .admin-entry-btn:hover {
  border-color: #5a9bff;
  color: #5a9bff;
}

/* Theme Switch */
.theme-switch {
  font-size: 17px;
  position: relative;
  display: inline-block;
  width: 4em;
  height: 2.2em;
  border-radius: 30px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  cursor: pointer;
}
.theme-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}
.theme-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #2a2a2a;
  transition: 0.4s;
  border-radius: 30px;
  overflow: hidden;
}
.theme-slider:before {
  position: absolute;
  content: "";
  height: 1.2em;
  width: 1.2em;
  border-radius: 20px;
  left: 0.5em;
  bottom: 0.5em;
  transition: 0.4s;
  transition-timing-function: cubic-bezier(0.81, -0.04, 0.38, 1.5);
  box-shadow: inset 8px -4px 0px 0px #fff;
}
.theme-switch input:checked + .theme-slider {
  background-color: #00a6ff;
}
.theme-switch input:checked + .theme-slider:before {
  transform: translateX(1.8em);
  box-shadow: inset 15px -4px 0px 15px #ffcf48;
}
.theme-star {
  background-color: #fff;
  border-radius: 50%;
  position: absolute;
  width: 5px;
  transition: all 0.4s;
  height: 5px;
}
.theme-star_1 { left: 2.5em; top: 0.5em; }
.theme-star_2 { left: 2.2em; top: 1.2em; }
.theme-star_3 { left: 3em; top: 0.9em; }
.theme-switch input:checked ~ .theme-slider .theme-star {
  opacity: 0;
}
.theme-cloud {
  width: 3.5em;
  position: absolute;
  bottom: -1.4em;
  left: -1.1em;
  opacity: 0;
  transition: all 0.4s;
}
.theme-switch input:checked ~ .theme-slider .theme-cloud {
  opacity: 1;
}

.mobile-toggle {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  padding: 4px;
}
.mobile-toggle span {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--color-text);
  border-radius: 1px;
  transition: all var(--transition-fast);
}
.mobile-toggle.active span:nth-child(1) {
  transform: rotate(45deg) translate(5px, 5px);
}
.mobile-toggle.active span:nth-child(2) {
  opacity: 0;
}
.mobile-toggle.active span:nth-child(3) {
  transform: rotate(-45deg) translate(5px, -5px);
}
.main {
  flex: 1;
  padding: 1.5rem 0 3rem;
  position: relative;
  z-index: 1;
}
.main .container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}
/* Back button */
.back-btn {
  position: fixed;
  top: 72px;
  left: calc((100vw - 1200px) / 2 - 52px);
  z-index: 90;
  display: block;
  width: 48px;
  height: 48px;
  overflow: hidden;
  outline: none;
  background-color: transparent;
  cursor: pointer;
  border: 0;
  padding: 0;
}
.back-btn:before,
.back-btn:after {
  content: "";
  position: absolute;
  border-radius: 50%;
  inset: 6px;
}
.back-btn:before {
  border: 3px solid var(--color-border);
  transition: opacity 0.4s cubic-bezier(0.77, 0, 0.175, 1) 80ms,
    transform 0.5s cubic-bezier(0.455, 0.03, 0.515, 0.955) 80ms;
}
.back-btn:after {
  border: 3px solid var(--color-primary);
  transform: scale(1.3);
  transition: opacity 0.4s cubic-bezier(0.165, 0.84, 0.44, 1),
    transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0;
}
.back-btn:hover:before,
.back-btn:focus:before {
  opacity: 0;
  transform: scale(0.7);
  transition: opacity 0.4s cubic-bezier(0.165, 0.84, 0.44, 1),
    transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.back-btn:hover:after,
.back-btn:focus:after {
  opacity: 1;
  transform: scale(1);
  transition: opacity 0.4s cubic-bezier(0.77, 0, 0.175, 1) 80ms,
    transform 0.5s cubic-bezier(0.455, 0.03, 0.515, 0.955) 80ms;
}
.back-btn-box {
  display: flex;
  position: absolute;
  top: 0;
  left: 0;
}
.back-btn-elem {
  display: block;
  width: 18px;
  height: 18px;
  margin: 15px 15px 0 15px;
  transform: rotate(180deg);
  fill: var(--color-text-secondary);
  transition: fill var(--transition-fast);
}
.back-btn:hover .back-btn-elem {
  fill: var(--color-primary);
}
.back-btn:hover .back-btn-box,
.back-btn:focus .back-btn-box {
  transition: 0.4s;
  transform: translateX(-48px);
}
.back-fade-enter-active,
.back-fade-leave-active {
  transition: opacity 0.25s ease;
}
.back-fade-enter-from,
.back-fade-leave-to {
  opacity: 0;
}
@media (max-width: 1200px) {
  .back-btn {
    left: 12px;
    top: 68px;
  }
}
@media (max-width: 768px) {
  .back-btn {
    display: none;
  }
}

/* Footer */
.footer {
  background: var(--color-footer-bg);
  color: var(--color-footer-text);
  padding: 0;
  margin-top: auto;
}
.footer .container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}
.footer-content {
  display: flex;
  justify-content: space-between;
  padding: 2.5rem 0 2rem;
  gap: 2rem;
}
.footer-brand {
  max-width: 320px;
}
.footer-logo {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-footer-heading);
  display: block;
  margin-bottom: 0.5rem;
}
.footer-brand p {
  font-size: 0.825rem;
  line-height: 1.6;
  color: var(--color-footer-text);
}
.footer-links {
  display: flex;
  gap: 3.5rem;
}
.footer-col h4 {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-footer-heading);
  margin-bottom: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.footer-col a {
  display: block;
  color: var(--color-footer-text);
  font-size: 0.85rem;
  padding: 0.25rem 0;
  transition: color var(--transition-fast);
}
.footer-col a:hover {
  color: var(--color-footer-heading);
}
@media (max-width: 768px) {
  .mobile-toggle {
    display: flex;
  }
  .nav {
    display: none;
    position: absolute;
    top: 58px;
    left: 0;
    right: 0;
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    flex-direction: column;
    align-items: center;
    padding: 0.75rem;
    box-shadow: var(--shadow-md);
    border-bottom: 1px solid var(--color-border-light);
    gap: 0.5rem;
    max-height: calc(100vh - 58px);
    overflow-y: auto;
  }
  .nav.open {
    display: flex;
  }
  .nav-tabs {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    padding: 0.3rem;
    justify-content: flex-start;
  }
  .nav-tabs::-webkit-scrollbar {
    display: none;
  }
  .nav-tab {
    flex: 0 0 auto;
    font-size: 0.78rem;
    padding: 0 0.7rem;
    height: 30px;
    white-space: nowrap;
  }
  .nav-glider {
    display: none;
  }
  .header .container {
    padding: 0 1rem;
  }
  .main .container {
    padding: 0 0.5rem;
  }
  .footer-content {
    flex-direction: column;
    gap: 1.5rem;
  }
  .footer-links {
    gap: 2rem;
  }
}

/* ===== Night Theme ===== */
.night {
  --color-bg: #0d1b2a;
  --color-bg-muted: #1b2838;
  --color-text: #e0e0e0;
  --color-text-secondary: #a0aec0;
  --color-text-tertiary: #718096;
  --color-border: rgba(255, 255, 255, 0.1);
  --color-border-light: rgba(255, 255, 255, 0.06);
  --color-primary: #5a9bff;
  --color-primary-hover: #7db3ff;
  --color-primary-glow: rgba(90, 155, 255, 0.15);
  --color-accent-blue: rgba(90, 155, 255, 0.1);
  --shadow-card-hover: 0 8px 24px rgba(0, 0, 0, 0.3);
}
.night .header {
  background: rgba(13, 27, 42, 0.9);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}
.night .header-accent {
  background: linear-gradient(to right, #2a5298, #3b82f6, #2a5298);
}
.night .logo-text {
  color: #e0e0e0;
}
.night .logo-text:hover {
  color: #5a9bff;
}
.night .nav-tabs {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 1px 0 rgba(255, 255, 255, 0.08), 0 4px 10px 0 rgba(0, 0, 0, 0.2);
}
.night .nav-tab {
  color: #a0aec0;
}
.night .nav-tab:hover {
  color: #e0e0e0;
}
.night .nav-tab.active {
  color: #5a9bff;
}
.night .nav-glider {
  background: rgba(90, 155, 255, 0.15);
}
.night .nav-search {
  color: #a0aec0;
}
.night .nav-search:hover {
  color: #5a9bff;
}
.night .mobile-toggle span {
  background: #a0aec0;
}
.night .main {
  color: #e0e0e0;
}
.night .footer {
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--color-footer-border);
}
.night .back-btn-elem {
  fill: #a0aec0;
}
.night .back-btn:hover .back-btn-elem {
  fill: #5a9bff;
}
.night .back-btn:before {
  border-color: rgba(255, 255, 255, 0.15);
}
.night .back-btn:after {
  border-color: #3b82f6;
}
/* Night nav mobile */
@media (max-width: 768px) {
  .night .nav {
    background: rgba(13, 27, 42, 0.98);
    border-bottom-color: rgba(255, 255, 255, 0.06);
  }
}

/* ===== Cosmos Theme (after .night to override) ===== */
.is-cosmos .main {
  padding: 0;
}
.is-cosmos .main .container {
  max-width: none;
  padding: 0;
}
.is-cosmos .header {
  background: rgba(13, 13, 13, 0.92);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(212, 175, 55, 0.15);
}
.is-cosmos .header-accent {
  background: linear-gradient(to right, #8b6914, #d4af37, #8b6914);
}
.is-cosmos .logo-text {
  color: #d4af37;
}
.is-cosmos .logo-text:hover {
  color: #e8c84a;
}
.is-cosmos .nav-tabs {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 1px 0 rgba(255, 255, 255, 0.08), 0 4px 10px 0 rgba(0, 0, 0, 0.3);
}
.is-cosmos .nav-tab {
  color: #8b6b4a;
}
.is-cosmos .nav-tab:hover {
  color: #e8e4d9;
}
.is-cosmos .nav-tab.active {
  color: #d4af37;
}
.is-cosmos .nav-glider {
  background: rgba(212, 175, 55, 0.15);
}
.is-cosmos .nav-search {
  color: #8b6b4a;
}
.is-cosmos .nav-search:hover {
  color: #d4af37;
}
.is-cosmos .lang-switch {
  border-color: rgba(139, 107, 74, 0.4);
  color: #8b6b4a;
}
.is-cosmos .lang-switch:hover {
  border-color: #d4af37;
  color: #d4af37;
}
.is-cosmos .admin-entry-btn {
  border-color: rgba(139, 107, 74, 0.4);
  color: #8b6b4a;
}
.is-cosmos .admin-entry-btn:hover {
  border-color: #d4af37;
  color: #d4af37;
}
.is-cosmos .theme-switch {
  box-shadow: 0 0 10px rgba(212, 175, 55, 0.15);
}
.is-cosmos .mobile-toggle span {
  background: #8b6b4a;
}

/* ===== 404 Page ===== */
.is-not-found .main {
  padding: 0;
}
.is-not-found .main .container {
  max-width: none;
  padding: 0;
}
.is-not-found .footer {
  display: none;
}
</style>

<style>
/* Page transition - must be unscoped to apply to child components */
.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateY(12px);
}
.page-enter-active {
  transition: opacity 0.3s ease-out, transform 0.3s ease-out;
}
.page-leave-active {
  transition: opacity 0.15s ease-in, transform 0.15s ease-in;
}
</style>
