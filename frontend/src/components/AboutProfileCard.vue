<template>
  <div class="profile-card" :class="{ 'is-night': isNight }" @click="$emit('open')">
    <div class="profile-card-left">
      <div class="profile-avatar">
        <img v-if="avatar" :src="ossImg(avatar)" alt="avatar" />
        <div v-else class="avatar-placeholder">{{ initial }}</div>
      </div>
      <div class="profile-info">
        <div class="profile-name">{{ name }}</div>
        <div class="profile-tagline">{{ tagline }}</div>
      </div>
    </div>
    <button class="profile-view-btn" @click.stop="$emit('open')">
      <span>{{ $t('about.viewProfile') }}</span>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><polyline points="9 18 15 12 9 6"/></svg>
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import { ossImg } from '../utils/oss'

const { t } = useI18n()

defineEmits(['open'])

const siteName = ref('')
const avatar = ref('')
const tagline = ref('')
const isNight = ref(document.body.classList.contains('body-night'))

const name = computed(() => siteName.value || t('nav.brand'))
const initial = computed(() => (name.value || '?')[0].toUpperCase())

async function loadSiteInfo() {
  try {
    const data = await request.get('/api/site/info')
    if (data) {
      siteName.value = data.siteName || ''
      const extra = data.extraSettings || {}
      avatar.value = extra.site_avatar || ''
      tagline.value = extra.site_tagline || data.siteDescription || ''
    }
  } catch {}
}

let observer = null
onMounted(() => {
  loadSiteInfo()
  observer = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  observer.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})
onUnmounted(() => { observer?.disconnect() })
</script>

<style scoped>
.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 28px;
}
.profile-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.profile-card.is-night {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}
.profile-card.is-night:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.profile-card-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.profile-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 3px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.profile-card.is-night .profile-avatar {
  border-color: rgba(255, 255, 255, 0.1);
}
.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  font-size: 22px;
  font-weight: 700;
}

.profile-info {
  min-width: 0;
}
.profile-name {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.profile-card.is-night .profile-name { color: #e2e8f0; }

.profile-tagline {
  font-size: 13px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.profile-view-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 20px;
  color: #3b82f6;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}
.profile-view-btn:hover {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.4);
}
.profile-card.is-night .profile-view-btn {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.25);
  color: #60a5fa;
}

@media (max-width: 480px) {
  .profile-card { padding: 12px 14px; }
  .profile-avatar { width: 44px; height: 44px; }
  .profile-name { font-size: 14px; }
  .profile-tagline { font-size: 12px; }
}
</style>
