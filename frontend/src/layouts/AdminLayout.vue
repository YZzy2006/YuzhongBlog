<template>
  <div class="admin-layout">
    <!-- Background orbs -->
    <div class="bg-orb bg-orb--top"></div>
    <div class="bg-orb bg-orb--bottom"></div>

    <!-- Mobile overlay -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <aside class="sidebar" :class="{ 'sidebar-open': sidebarOpen }">
      <!-- Profile card -->
      <div class="sidebar-profile">
        <div class="profile-avatar-ring">
          <span class="profile-avatar" :style="authStore.avatarUrl ? { background: `url(${authStore.avatarUrl}) center/cover` } : {}">
            <span v-if="!authStore.avatarUrl">{{ avatarLetter }}</span>
          </span>
        </div>
        <div class="profile-name">{{ authStore.name || authStore.username }}</div>
        <div class="profile-role">{{ authStore.isSuperAdmin ? 'Super Admin' : 'Admin' }}</div>
      </div>

      <!-- Nav card -->
      <div class="sidebar-nav-card">
        <nav class="sidebar-nav">
          <router-link to="/admin" @click="sidebarOpen = false">
            <span class="nav-icon">&#9632;</span> {{ $t('admin.dashboard') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('article:view')" to="/admin/articles" @click="sidebarOpen = false">
            <span class="nav-icon">&#9998;</span> {{ $t('admin.articles') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('category:view')" to="/admin/categories" @click="sidebarOpen = false">
            <span class="nav-icon">&#9776;</span> {{ $t('admin.categories') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('tag:view')" to="/admin/tags" @click="sidebarOpen = false">
            <span class="nav-icon">&#9830;</span> {{ $t('admin.tags') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('project:view')" to="/admin/projects" @click="sidebarOpen = false">
            <span class="nav-icon">&#9881;</span> {{ $t('admin.projects') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('announcement:view')" to="/admin/announcements" @click="sidebarOpen = false">
            <span class="nav-icon">&#128227;</span> {{ $t('admin.announcements') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('photowall:view')" to="/admin/photowall" @click="sidebarOpen = false">
            <span class="nav-icon">&#128247;</span> {{ $t('admin.photowall') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('project:view')" to="/admin/timeline" @click="sidebarOpen = false">
            <span class="nav-icon">&#128197;</span> {{ $t('admin.timeline') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('project:view')" to="/admin/friend-links" @click="sidebarOpen = false">
            <span class="nav-icon">&#128279;</span> {{ $t('admin.friendLinks') }}
          </router-link>
          <router-link v-if="authStore.isSuperAdmin" to="/admin/settings" @click="sidebarOpen = false">
            <span class="nav-icon">&#9881;</span> {{ $t('admin.siteSettings') }}
          </router-link>
          <router-link v-if="authStore.isSuperAdmin" to="/admin/music" @click="sidebarOpen = false">
            <span class="nav-icon">&#127925;</span> {{ $t('admin.music') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('user:view')" to="/admin/users" @click="sidebarOpen = false">
            <span class="nav-icon">&#128100;</span> {{ $t('admin.users') }}
          </router-link>
          <router-link v-if="authStore.isSuperAdmin" to="/admin/ai-settings" @click="sidebarOpen = false">
            <span class="nav-icon">&#129302;</span> {{ $t('admin.aiSettings') }}
          </router-link>
          <router-link v-if="authStore.isSuperAdmin" to="/admin/weather-settings" @click="sidebarOpen = false">
            <span class="nav-icon">&#127780;</span> {{ $t('admin.weatherSettings') }}
          </router-link>
          <router-link v-if="authStore.isSuperAdmin" to="/admin/oss-settings" @click="sidebarOpen = false">
            <span class="nav-icon">&#9729;</span> {{ $t('admin.ossSettings') }}
          </router-link>
          <router-link v-if="authStore.isSuperAdmin" to="/admin/backup" @click="sidebarOpen = false">
            <span class="nav-icon">&#128190;</span> {{ $t('admin.backup') }}
          </router-link>
          <router-link v-if="authStore.isSuperAdmin" to="/admin/reviews" @click="sidebarOpen = false">
            <span class="nav-icon">&#9878;</span> {{ $t('admin.reviews') }}
          </router-link>
          <router-link v-if="authStore.hasPermission('log:view') || authStore.isSuperAdmin" to="/admin/login-logs" @click="sidebarOpen = false">
            <span class="nav-icon">&#128197;</span> {{ $t('admin.loginLogs') }}
          </router-link>
          <router-link to="/admin/account-security" @click="sidebarOpen = false">
            <span class="nav-icon">&#128274;</span> {{ $t('admin.accountSecurity') }}
          </router-link>
          <router-link to="/admin/profile" @click="sidebarOpen = false">
            <span class="nav-icon">&#128100;</span> {{ $t('admin.profile') }}
          </router-link>
        </nav>
      </div>
    </aside>

    <div class="main-wrapper">
      <header class="header">
        <span class="hamburger" @click="sidebarOpen = !sidebarOpen">&#9776;</span>
        <div class="breadcrumb">
          <router-link to="/admin" class="breadcrumb-home">{{ $t('nav.home') }}</router-link>
          <span v-if="currentTitle" class="breadcrumb-sep">/</span>
          <span v-if="currentTitle" class="breadcrumb-current">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <router-link to="/" class="lang-switch site-visit-btn" :title="$t('nav.goToFrontend')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/></svg>
            <span>{{ $t('nav.goToFrontend') }}</span>
          </router-link>
          <button class="lang-switch" @click="toggleLocale" :title="$t('nav.switchLang')">
            {{ locale === 'zh-CN' ? 'EN' : '中' }}
          </button>
          <div class="user-info" @mouseenter="onAvatarHover" @mouseleave="onAvatarLeave">
            <span class="user-avatar" :style="authStore.avatarUrl ? { background: `url(${authStore.avatarUrl}) center/cover` } : {}">
              <span v-if="!authStore.avatarUrl">{{ avatarLetter }}</span>
            </span>
            <span class="user-name">{{ authStore.name || authStore.username }}</span>
            <!-- Hover card -->
            <transition name="card-fade">
              <div v-if="showCard" class="avatar-hover-card" @mouseenter="cardHovering = true" @mouseleave="onAvatarLeave">
                <template v-if="cardLoading">
                  <div class="card_load"></div>
                  <div class="card_load_extreme_title"></div>
                  <div class="card_load_extreme_descripion"></div>
                </template>
                <template v-else>
                  <div class="card-avatar" :style="cardData.avatarUrl ? { background: `url(${cardData.avatarUrl}) center/cover` } : {}">
                    <span v-if="!cardData.avatarUrl">{{ avatarLetter }}</span>
                  </div>
                  <div class="card-info">
                    <div class="card-name">{{ cardData.name || cardData.username }}</div>
                    <div class="card-bio">{{ cardData.bio || $t('adminProfile.noBio') }}</div>
                  </div>
                </template>
              </div>
            </transition>
          </div>
          <button class="logout-btn" @click="handleLogout">
            <div class="logout-sign">
              <svg viewBox="0 0 512 512"><path d="M377.9 105.9L500.7 228.7c7.2 7.2 11.3 17.1 11.3 27.3s-4.1 20.1-11.3 27.3L377.9 406.1c-6.4 6.4-15 9.9-24 9.9c-18.7 0-33.9-15.2-33.9-33.9l0-62.1-128 0c-17.7 0-32-14.3-32-32l0-64c0-17.7 14.3-32 32-32l128 0 0-62.1c0-18.7 15.2-33.9 33.9-33.9c9 0 17.6 3.6 24 9.9zM160 96L96 96c-17.7 0-32 14.3-32 32l0 256c0 17.7 14.3 32 32 32l64 0c17.7 0 32 14.3 32 32s-14.3 32-32 32l-64 0c-53 0-96-43-96-96L0 128C0 75 43 32 96 32l64 0c17.7 0 32 14.3 32 32s-14.3 32-32 32z"></path></svg>
            </div>
            <div class="logout-text">{{ locale === 'zh-CN' ? '退出' : 'Logout' }}</div>
          </button>
        </div>
      </header>

      <main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="admin-page" mode="out-in">
            <keep-alive :include="cachedAdminPages" :max="12">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </main>
    </div>
    <CommandPalette />
    <KickNotification />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'
import CommandPalette from '../components/CommandPalette.vue'
import KickNotification from '../components/KickNotification.vue'
import '../styles/admin.css'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { t, locale } = useI18n()

const sidebarOpen = ref(false)

const cachedAdminPages = [
  'Dashboard', 'Articles', 'Categories', 'Tags', 'Projects',
  'Announcements', 'PhotoWall', 'TimelineEntries', 'FriendLinks',
  'Settings', 'Music', 'Users', 'AiSettings', 'WeatherSettings',
  'OssSettings', 'Reviews', 'LoginLogs', 'AccountSecurity', 'Profile',
  'Backup'
]

// Avatar hover card
const showCard = ref(false)
const cardLoading = ref(false)
const cardHovering = ref(false)
const cardData = ref({ username: '', name: '', bio: '', avatarUrl: '' })
let hoverTimer = null

function onAvatarHover() {
  clearTimeout(hoverTimer)
  hoverTimer = setTimeout(async () => {
    showCard.value = true
    cardHovering.value = true
    // If we already have bio in store, use it
    if (authStore.bio) {
      cardData.value = { username: authStore.username, name: authStore.name, bio: authStore.bio, avatarUrl: authStore.avatarUrl }
      cardLoading.value = false
      return
    }
    cardLoading.value = true
    try {
      const data = await request.get('/admin/profile')
      cardData.value = data
      authStore.bio = data.bio || ''
      localStorage.setItem('bio', data.bio || '')
      if (data.avatarUrl) {
        authStore.avatarUrl = data.avatarUrl
        localStorage.setItem('avatarUrl', data.avatarUrl)
      }
      if (data.name) {
        authStore.name = data.name
        localStorage.setItem('name', data.name)
      }
    } catch {
      cardData.value = { username: authStore.username, name: authStore.name, bio: '', avatarUrl: authStore.avatarUrl }
    } finally {
      cardLoading.value = false
    }
  }, 300)
}

function onAvatarLeave() {
  clearTimeout(hoverTimer)
  // Delay hide to allow mouse to move to card
  hoverTimer = setTimeout(() => {
    if (!cardHovering.value) {
      showCard.value = false
    }
  }, 200)
  cardHovering.value = false
}

function toggleLocale() {
  const next = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = next
  localStorage.setItem('adminLocale', next)
}

const titleMap = computed(() => ({
  '/admin': t('admin.dashboard'),
  '/admin/articles': t('admin.articles'),
  '/admin/categories': t('admin.categories'),
  '/admin/tags': t('admin.tags'),
  '/admin/projects': t('admin.projects'),
  '/admin/settings': t('admin.siteSettings'),
  '/admin/ai-settings': t('admin.aiSettings'),
  '/admin/weather-settings': t('admin.weatherSettings'),
  '/admin/oss-settings': t('admin.ossSettings'),
  '/admin/backup': t('admin.backup'),
  '/admin/announcements': t('admin.announcements'),
  '/admin/photowall': t('admin.photowall'),
  '/admin/reviews': t('admin.reviews'),
  '/admin/music': t('admin.music'),
  '/admin/timeline': t('admin.timeline'),
  '/admin/friend-links': t('admin.friendLinks'),
  '/admin/users': t('admin.users'),
  '/admin/login-logs': t('admin.loginLogs'),
  '/admin/account-security': t('admin.accountSecurity'),
  '/admin/profile': t('admin.profile'),
}))

const currentTitle = computed(() => {
  const path = route.path
  if (titleMap.value[path]) return titleMap.value[path]
  if (path.startsWith('/admin/articles/')) return t('admin.articleEdit')
  return ''
})

const avatarLetter = computed(() => {
  const name = authStore.username || '?'
  return name.charAt(0).toUpperCase()
})

async function handleLogout() {
  await authStore.logout()
  router.push('/admin/login')
}

onMounted(() => {
  // Start kick polling when entering admin area (handles both fresh login and page refresh)
  if (authStore.isLoggedIn && !authStore.kickNotification) {
    authStore.startKickPolling()
  }
})

onUnmounted(() => {
  if (hoverTimer) { clearTimeout(hoverTimer); hoverTimer = null }
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: #f8fafc;
}

/* ===== Background Orbs ===== */
.bg-orb {
  position: fixed;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
  filter: blur(80px);
  will-change: transform;
}
.bg-orb--top {
  top: -10%;
  left: -10%;
  width: 30vw;
  height: 30vw;
  background: rgba(255, 255, 255, 0.3);
}
.bg-orb--bottom {
  bottom: -10%;
  right: -10%;
  width: 30vw;
  height: 30vw;
  background: rgba(59, 130, 246, 0.1);
}

/* ===== Sidebar ===== */
.sidebar {
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 16px;
  flex-shrink: 0;
  overflow-y: auto;
  position: relative;
  z-index: 2;
}

/* Profile card */
.sidebar-profile {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  padding: 24px 16px;
  text-align: center;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.profile-avatar-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, #3b82f6, #a855f7);
  margin: 0 auto 12px;
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
}

.profile-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  border: 3px solid #fff;
}

.profile-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 2px;
}

.profile-role {
  font-size: 0.75rem;
  color: #94a3b8;
  font-weight: 500;
}

/* Nav card */
.sidebar-nav-card {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  padding: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  flex: 1;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-nav a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  color: #64748b;
  text-decoration: none;
  font-size: 0.85rem;
  font-weight: 600;
  border-radius: 16px;
  transition: all 0.3s ease;
  position: relative;
}

.sidebar-nav a:hover {
  background: rgba(255, 255, 255, 0.5);
  color: #334155;
  transform: translateX(4px);
}

.sidebar-nav a.router-link-exact-active {
  background: #3b82f6;
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transform: translateX(8px);
}

.sidebar-nav a.router-link-exact-active:hover {
  background: #2563eb;
  color: #fff;
}

.nav-icon {
  font-size: 0.85rem;
  width: 1.2rem;
  text-align: center;
  opacity: 0.7;
}

.sidebar-nav a.router-link-exact-active .nav-icon {
  opacity: 1;
}

/* ===== Main wrapper ===== */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
  z-index: 1;
}

/* ===== Header ===== */
.header {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  position: sticky;
  top: 0;
  z-index: 10;
}

.hamburger {
  display: none;
  font-size: 20px;
  cursor: pointer;
  color: #334155;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.hamburger:hover {
  background: rgba(0, 0, 0, 0.05);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.875rem;
}

.breadcrumb-home {
  color: #94a3b8;
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-home:hover {
  color: #3b82f6;
}

.breadcrumb-sep {
  color: #cbd5e1;
}

.breadcrumb-current {
  color: #1e293b;
  font-weight: 600;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
  cursor: pointer;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #a855f7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}

.user-name {
  font-size: 0.85rem;
  color: #334155;
  font-weight: 600;
}

.lang-switch {
  padding: 5px 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.lang-switch:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.05);
}

.site-visit-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  text-decoration: none;
}

/* Logout button */
.logout-btn {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition-duration: .3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #3b82f6, #a855f7);
}
.logout-sign {
  width: 100%;
  transition-duration: .3s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logout-sign svg {
  width: 15px;
}
.logout-sign svg path {
  fill: white;
}
.logout-text {
  position: absolute;
  right: 0%;
  width: 0%;
  opacity: 0;
  color: white;
  font-size: 1em;
  font-weight: 600;
  transition-duration: .3s;
}
.logout-btn:hover {
  width: 110px;
  border-radius: 20px;
  transition-duration: .3s;
}
.logout-btn:hover .logout-sign {
  width: 30%;
  transition-duration: .3s;
  padding-left: 16px;
}
.logout-btn:hover .logout-text {
  opacity: 1;
  width: 70%;
  transition-duration: .3s;
  padding-right: 10px;
}
.logout-btn:active {
  transform: scale(0.95);
}

/* ===== Avatar Hover Card ===== */
.avatar-hover-card {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 240px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border-radius: 20px;
  padding: 16px;
  z-index: 100;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.card-fade-enter-active,
.card-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.card-fade-enter-from,
.card-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* Skeleton loading */
.card_load {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  background: linear-gradient(120deg, #e0e7ff 30%, #eef2ff 38%, #eef2ff 40%, #e0e7ff 48%);
  border-radius: 50%;
  background-size: 200% 100%;
  background-position: 100% 0;
  animation: skeleton-shimmer 2s infinite;
}

.card_load_extreme_title {
  width: 80px;
  height: 10px;
  border-radius: 5px;
  background: linear-gradient(120deg, #e0e7ff 30%, #eef2ff 38%, #eef2ff 40%, #e0e7ff 48%);
  background-size: 200% 100%;
  background-position: 100% 0;
  animation: skeleton-shimmer 2s infinite;
  margin-top: 8px;
}

.card_load_extreme_descripion {
  width: 120px;
  height: 36px;
  border-radius: 5px;
  background: linear-gradient(120deg, #e0e7ff 30%, #eef2ff 38%, #eef2ff 40%, #e0e7ff 48%);
  background-size: 200% 100%;
  background-position: 100% 0;
  animation: skeleton-shimmer 2s infinite;
  margin-top: 8px;
}

@keyframes skeleton-shimmer {
  100% { background-position: -100% 0; }
}

/* Card loaded content */
.card-avatar {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #a855f7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  font-weight: 700;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.card-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-bio {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== Main content ===== */
.admin-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* Page transition */
.admin-page-enter-active {
  transition: opacity 0.3s ease-out, transform 0.3s ease-out;
}
.admin-page-leave-active {
  transition: opacity 0.15s ease-in, transform 0.15s ease-in;
}
.admin-page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.admin-page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ===== Mobile ===== */
.sidebar-overlay {
  display: none;
}

@media (max-width: 768px) {
  .hamburger {
    display: flex;
  }

  .header {
    padding: 0 12px;
  }

  .breadcrumb {
    display: none;
  }

  .admin-main {
    padding: 12px;
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 99;
    animation: fadeIn 0.2s ease;
  }

  .sidebar {
    position: fixed;
    top: 0;
    left: -280px;
    bottom: 0;
    z-index: 100;
    width: 280px;
    background: rgba(248, 250, 252, 0.98);
    transition: left 0.3s ease;
  }

  .sidebar.sidebar-open {
    left: 0;
  }
}

/* ===== Night mode ===== */
.night .admin-layout {
  background: #0f172a;
}

.night .bg-orb--top {
  background: rgba(59, 130, 246, 0.1);
}

.night .bg-orb--bottom {
  background: rgba(59, 130, 246, 0.1);
}

.night .sidebar-profile,
.night .sidebar-nav-card {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(255, 255, 255, 0.08);
}

.night .profile-name {
  color: #e2e8f0;
}

.night .profile-role {
  color: #64748b;
}

.night .sidebar-nav a {
  color: #94a3b8;
}

.night .sidebar-nav a:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e2e8f0;
}

.night .sidebar-nav a.router-link-exact-active {
  background: #3b82f6;
  color: #fff;
}

.night .header {
  background: rgba(15, 23, 42, 0.85);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

.night .breadcrumb-current {
  color: #e2e8f0;
}

.night .user-name {
  color: #e2e8f0;
}

.night .lang-switch {
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: #94a3b8;
}

.night .lang-switch:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.night .avatar-hover-card {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
}

.night .card-name {
  color: #e2e8f0;
}

.night .card-bio {
  color: #94a3b8;
}

.night .sidebar {
  background: transparent;
}

@media (max-width: 768px) {
  .night .sidebar {
    background: rgba(15, 23, 42, 0.98);
  }
}
</style>

<style>
/* Custom scrollbar - global */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.2);
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.5);
}
</style>
