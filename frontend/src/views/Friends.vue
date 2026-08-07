<template>
  <div class="friends-page" :class="{ 'is-night': isNight }">
    <div class="friends-container">
      <!-- Header -->
      <div class="friends-header fade-in-scroll">
        <h1 class="friends-title">{{ $t('friends.title') }}</h1>
        <p class="friends-subtitle">{{ $t('friends.subtitle') }}</p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="friends-loading">
        <div v-for="i in 6" :key="i" class="friend-card-skeleton">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-line short"></div>
          <div class="skeleton-line long"></div>
        </div>
      </div>

      <!-- Grid -->
      <div v-else class="friends-grid">
        <a v-for="(friend, idx) in friends" :key="friend.id"
          :href="friend.url" target="_blank" rel="noopener noreferrer"
          class="friend-card fade-in-scroll"
          :style="{ animationDelay: idx * 100 + 'ms' }">
          <!-- Theme glow -->
          <div class="card-glow" :style="{ background: friend.themeColor || 'rgba(59,130,246,0.5)' }"></div>
          <!-- Avatar + Name -->
          <div class="card-header">
            <div class="avatar-ring">
              <img v-if="friend.avatar" :src="ossImg(friend.avatar)" :alt="friend.name" class="avatar-img" />
              <span v-else class="avatar-fallback">{{ friend.name?.charAt(0) }}</span>
            </div>
            <div class="card-info">
              <div class="card-name">{{ friend.name }}</div>
              <div class="card-status">
                <span class="status-dot"></span>
                <span class="status-text">{{ $t('friends.online') }}</span>
              </div>
            </div>
          </div>
          <!-- Description -->
          <p v-if="friend.description" class="card-desc">{{ friend.description }}</p>
        </a>
      </div>

      <!-- Apply section -->
      <div class="apply-section fade-in-scroll">
        <h2 class="apply-title">{{ $t('friends.applyTitle') }}</h2>
        <p class="apply-hint">{{ $t('friends.applyHint') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import { ossImg } from '../utils/oss'

const { t } = useI18n()

const friends = ref([])
const loading = ref(true)
const isNight = ref(document.body.classList.contains('body-night'))

async function loadFriends() {
  loading.value = true
  try {
    const data = await request.get('/api/friend-links')
    friends.value = data || []
  } catch {
    friends.value = []
  } finally {
    loading.value = false
  }
}

let nightObserver = null

onMounted(() => {
  loadFriends()
  nightObserver = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  nightObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  if (nightObserver) { nightObserver.disconnect(); nightObserver = null }
})
</script>

<style scoped>
.friends-page {
  min-height: 100vh;
  padding: 80px 16px 60px;
}

.friends-container {
  max-width: 1000px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* ===== Header ===== */
.friends-header {
  text-align: center;
  margin-bottom: 40px;
}

.friends-title {
  font-size: 1.75rem;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  margin-bottom: 8px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.friends-subtitle {
  font-size: 0.85rem;
  color: #64748b;
  font-family: Georgia, serif;
}

/* ===== Grid ===== */
.friends-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

/* ===== Card ===== */
.friend-card {
  display: block;
  position: relative;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: all 0.5s ease;
}

.friend-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

/* Theme glow */
.card-glow {
  position: absolute;
  bottom: -40px;
  right: -40px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  filter: blur(30px);
  opacity: 0;
  transition: opacity 0.7s ease;
  pointer-events: none;
}

.friend-card:hover .card-glow {
  opacity: 1;
}

/* Card header */
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 1;
  margin-bottom: 10px;
}

/* Avatar ring */
.avatar-ring {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  padding: 2px;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  flex-shrink: 0;
  transition: transform 1s ease-in-out;
  overflow: hidden;
}

.friend-card:hover .avatar-ring {
  transform: rotate(360deg);
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  background: #fff;
  display: block;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 700;
}

/* Name */
.card-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f172a;
  transition: color 0.3s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-card:hover .card-name {
  color: #2563eb;
}

/* Status */
.card-status {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
  animation: pulse 2s infinite;
}

.status-text {
  font-size: 0.65rem;
  font-weight: 700;
  color: rgba(59, 130, 246, 0.7);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Description */
.card-desc {
  font-size: 0.78rem;
  color: #475569;
  font-family: Georgia, serif;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* ===== Loading skeleton ===== */
.friends-loading {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.friend-card-skeleton {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  padding: 16px;
}

.skeleton-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(120deg, #e0e7ff 30%, #eef2ff 38%, #eef2ff 40%, #e0e7ff 48%);
  background-size: 200% 100%;
  animation: shimmer 2s infinite;
  margin-bottom: 10px;
}

.skeleton-line {
  height: 10px;
  border-radius: 5px;
  background: linear-gradient(120deg, #e0e7ff 30%, #eef2ff 38%, #eef2ff 40%, #e0e7ff 48%);
  background-size: 200% 100%;
  animation: shimmer 2s infinite;
}

.skeleton-line.short { width: 60%; margin-bottom: 8px; }
.skeleton-line.long { width: 90%; }

@keyframes shimmer {
  100% { background-position: -100% 0; }
}

/* ===== Apply section ===== */
.apply-section {
  margin-top: 56px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  padding: 28px;
  max-width: 640px;
  margin-left: auto;
  margin-right: auto;
  text-align: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.apply-title {
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 8px;
}

.apply-hint {
  font-size: 0.8rem;
  color: #64748b;
  margin: 0;
}

/* ===== Desktop ===== */
@media (min-width: 768px) {
  .friends-page {
    padding: 100px 32px 80px;
  }

  .friends-title {
    font-size: 2.25rem;
    margin-bottom: 12px;
  }

  .friends-subtitle {
    font-size: 1rem;
  }

  .friends-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  .friend-card {
    border-radius: 20px;
    padding: 24px;
  }

  .friend-card:hover {
    transform: translateY(-8px) scale(1.02);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
  }

  .card-glow {
    width: 140px;
    height: 140px;
    bottom: -50px;
    right: -50px;
  }

  .avatar-ring {
    width: 60px;
    height: 60px;
    padding: 3px;
  }

  .card-name {
    font-size: 1.1rem;
  }

  .card-desc {
    font-size: 0.85rem;
    -webkit-line-clamp: 3;
  }

  .friends-loading {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  .apply-section {
    border-radius: 24px;
    padding: 40px;
  }

  .apply-title {
    font-size: 1.5rem;
  }
}

@media (max-width: 768px) {
  .card-name { white-space: normal; }
}
@media (max-width: 480px) {
  .friends-page { padding: 60px 12px 40px; }
  .friends-title { font-size: 1.5rem; }
  .friends-grid { grid-template-columns: 1fr 1fr; gap: 10px; }
  .friend-card { padding: 12px; border-radius: 14px; }
  .avatar-ring { width: 44px; height: 44px; }
  .card-name { font-size: 0.85rem; }
  .card-desc { font-size: 0.72rem; -webkit-line-clamp: 2; }
  .apply-section { padding: 20px; border-radius: 16px; }
  .apply-code { font-size: 0.68rem; padding: 10px; }
}

/* ===== Dark mode ===== */
.is-night .friends-page {
  background: transparent;
}

.is-night .friends-title {
  color: #f1f5f9;
}

.is-night .friends-subtitle {
  color: #94a3b8;
}

.is-night .friend-card {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-night .card-name {
  color: #f1f5f9;
}

.is-night .friend-card:hover .card-name {
  color: #60a5fa;
}

.is-night .card-desc {
  color: #cbd5e1;
}

.is-night .friend-card-skeleton {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-night .skeleton-avatar,
.is-night .skeleton-line {
  background: linear-gradient(120deg, #1e293b 30%, #334155 38%, #334155 40%, #1e293b 48%);
  background-size: 200% 100%;
}

.is-night .apply-section {
  background: rgba(30, 41, 59, 0.4);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-night .apply-title {
  color: #f1f5f9;
}

.is-night .apply-hint {
  color: #94a3b8;
}
</style>
