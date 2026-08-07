<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="close">
        <div class="modal-container" :class="{ 'is-night': isNight }">
          <!-- Close button -->
          <button class="modal-close" @click="close">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>

          <!-- Cover image -->
          <div class="modal-cover">
            <img v-if="cover" :src="cover" alt="cover" class="modal-cover-img" />
            <div v-else class="modal-cover-default"></div>
            <div class="modal-cover-overlay"></div>
          </div>

          <!-- Avatar + Title -->
          <div class="modal-header">
            <div class="modal-avatar">
              <img v-if="avatar" :src="avatar" alt="avatar" />
              <div v-else class="modal-avatar-placeholder">{{ initial }}</div>
            </div>
            <h1 class="modal-name">{{ name }}</h1>
            <p class="modal-subtitle">{{ subtitle }}</p>
          </div>

          <!-- Tab switcher -->
          <div class="modal-tabs">
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'intro' }"
              @click="activeTab = 'intro'"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <span>{{ $t('about.tabIntro') }}</span>
            </button>
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'activity' }"
              @click="activeTab = 'activity'"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
              <span>{{ $t('about.tabActivity') }}</span>
            </button>
          </div>

          <!-- Tab content -->
          <div class="modal-body">
            <!-- Intro tab -->
            <div v-if="activeTab === 'intro'" class="tab-content fade-in">
              <div v-if="bioHtml" class="bio-content markdown-body" v-html="bioHtml"></div>
              <div v-else class="bio-empty">{{ $t('about.defaultBio') }}</div>
            </div>

            <!-- Activity tab -->
            <div v-if="activeTab === 'activity'" class="tab-content tab-content--activity fade-in">
              <div v-if="loading" class="activity-loading">
                <div class="skeleton" style="width: 100%; height: 16px; margin-bottom: 12px; border-radius: 4px;" />
                <div class="skeleton" style="width: 80%; height: 16px; margin-bottom: 12px; border-radius: 4px;" />
                <div class="skeleton" style="width: 90%; height: 16px; border-radius: 4px;" />
              </div>
              <div v-else-if="activities.length === 0" class="activity-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                <span>{{ $t('about.noActivity') }}</span>
              </div>
              <template v-else>
                <!-- Search & Filter toolbar -->
                <div class="activity-toolbar">
                  <div class="search-row">
                    <div class="search-input-wrap">
                      <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                      <input
                        v-model="searchQuery"
                        type="text"
                        :placeholder="$t('about.searchPlaceholder')"
                        class="search-input"
                        @input="onSearchInput"
                      />
                    </div>
                    <button class="ai-search-btn" :class="{ active: aiMode }" @click="toggleAiMode">
                      <span class="ai-icon-wrap">
                        <div v-if="aiSearching" class="loader">
                          <svg width="100" height="100" viewBox="0 0 100 100"><defs><mask id="clipping"><polygon points="0,0 100,0 100,100 0,100" fill="black"/><polygon points="25,25 75,25 50,75" fill="white"/><polygon points="50,25 75,75 25,75" fill="white"/><polygon points="35,35 65,35 50,65" fill="white"/><polygon points="35,35 65,35 50,65" fill="white"/><polygon points="35,35 65,35 50,65" fill="white"/><polygon points="35,35 65,35 50,65" fill="white"/></mask></defs></svg>
                          <div class="box"></div>
                        </div>
                        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M12 2a4 4 0 0 1 4 4v1a2 2 0 0 1 2 2v1a2 2 0 0 1-2 2h-1l2 7H7l2-7H8a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2V6a4 4 0 0 1 4-4z"/></svg>
                      </span>
                      <span>{{ $t('about.aiSearch') }}</span>
                    </button>
                    <button class="filter-toggle-btn" :class="{ active: showFilters, 'has-filters': hasFilters }" @click="showFilters = !showFilters">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
                      <span>{{ $t('about.filter') }}</span>
                      <span v-if="hasFilters" class="filter-dot"></span>
                    </button>
                  </div>
                  <div v-if="aiMode" class="ai-search-row">
                    <input
                      v-model="aiQuery"
                      type="text"
                      :placeholder="$t('about.aiSearchPlaceholder')"
                      class="search-input ai-input"
                      @keydown.enter="doAiSearch"
                    />
                    <button class="ai-go-btn" :disabled="aiSearching || !aiQuery.trim()" @click="doAiSearch">
                      {{ aiSearching ? $t('about.parsing') : '→' }}
                    </button>
                  </div>
                  <Transition name="filter-expand">
                    <div v-if="showFilters" class="filter-panel">
                      <div class="filter-row">
                        <div class="filter-field">
                          <label class="filter-label">{{ $t('about.dateFrom') }}</label>
                          <DatePicker v-model="dateFrom" :placeholder="$t('about.dateFrom')" :max="dateTo || undefined" @change="currentPage = 1" />
                        </div>
                        <div class="filter-field">
                          <label class="filter-label">{{ $t('about.dateTo') }}</label>
                          <DatePicker v-model="dateTo" :placeholder="$t('about.dateTo')" :min="dateFrom || undefined" @change="currentPage = 1" />
                        </div>
                      </div>
                      <div v-if="hasFilters" class="filter-actions">
                        <button class="clear-btn" @click="clearFilters">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                          {{ $t('about.clear') }}
                        </button>
                      </div>
                    </div>
                  </Transition>
                  <div class="filter-status">
                    <span class="result-count">{{ $t('about.totalActivities', { count: filteredActivities.length }) }}</span>
                    <span v-if="aiMatchedIds.size > 0" class="ai-badge">AI</span>
                  </div>
                </div>

                <!-- Timeline -->
                <div v-if="paginatedActivities.length === 0" class="activity-empty" style="padding: 24px 0;">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="32" height="32"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  <span>{{ $t('about.noResults') }}</span>
                </div>
                <div v-else class="activity-timeline">
                  <div
                    v-for="(item, i) in paginatedActivities"
                    :key="item.id + '-' + item.type"
                    class="timeline-item"
                  >
                    <div class="timeline-dot" :class="typeClass(item.type)"></div>
                    <div class="timeline-content">
                      <div class="timeline-meta">
                        <img v-if="avatar" :src="avatar" class="timeline-avatar" alt="avatar" />
                        <div v-else class="timeline-avatar-placeholder">{{ initial }}</div>
                        <span class="timeline-type" :class="typeClass(item.type)">{{ typeLabel(item.type) }}</span>
                        <span class="timeline-date">{{ formatDate(item.date) }}</span>
                      </div>
                      <div class="timeline-title" @click="goTo(item.url)">
                        <template v-if="item.type === 'moment'">{{ item.title }}</template>
                        <template v-else>《{{ item.title }}》</template>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Pagination -->
                <div v-if="totalPages > 1" class="pagination">
                  <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage--">
                    ← {{ $t('about.prev') }}
                  </button>
                  <span class="page-info">{{ $t('about.pageInfo', { current: currentPage, total: totalPages }) }}</span>
                  <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage++">
                    {{ $t('about.next') }} →
                  </button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import request from '../utils/request'
import DatePicker from '../components/DatePicker.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const router = useRouter()
const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const activeTab = ref('intro')
const isNight = ref(document.body.classList.contains('body-night'))
const siteName = ref('')
const avatar = ref('')
const cover = ref('')
const subtitle = ref('')
const bio = ref('')
const loading = ref(false)
const activities = ref([])

// Search, filter, pagination
const searchQuery = ref('')
const aiQuery = ref('')
const aiMode = ref(false)
const aiSearching = ref(false)
const aiMatchedIds = ref(new Set())
const dateFrom = ref('')
const dateTo = ref('')
const currentPage = ref(1)
const pageSize = 30
const showFilters = ref(false)

const name = computed(() => siteName.value || t('nav.brand'))
const initial = computed(() => (name.value || '?')[0].toUpperCase())
const bioHtml = computed(() => {
  if (!bio.value) return ''
  return DOMPurify.sanitize(marked.parse(bio.value))
})

const hasFilters = computed(() =>
  searchQuery.value || aiMatchedIds.value.size > 0 || dateFrom.value || dateTo.value
)

const filteredActivities = computed(() => {
  let items = activities.value

  // AI search takes priority
  if (aiMatchedIds.value.size > 0) {
    items = items.filter(item => aiMatchedIds.value.has(item.id + '-' + item.type))
  }

  // Traditional text search
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    items = items.filter(item => (item.title || '').toLowerCase().includes(q))
  }

  // Date range filter
  if (dateFrom.value) {
    items = items.filter(item => (item.date || '').substring(0, 10) >= dateFrom.value)
  }
  if (dateTo.value) {
    items = items.filter(item => (item.date || '').substring(0, 10) <= dateTo.value)
  }

  return items
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredActivities.value.length / pageSize)))

const paginatedActivities = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredActivities.value.slice(start, start + pageSize)
})

function close() { visible.value = false }

function onEsc(e) {
  if (e.key === 'Escape') close()
}

function typeClass(type) {
  if (type === 'moment') return 'type-moment'
  if (type === 'article') return 'type-article'
  return 'type-announcement'
}

function typeLabel(type) {
  if (type === 'moment') return t('about.activityMoment')
  if (type === 'article') return t('about.activityArticle')
  return t('about.activityAnnouncement')
}

function formatDate(d) {
  if (!d) return ''
  return d.substring(0, 10)
}

function goTo(url) {
  if (!url) return
  close()
  router.push(url)
}

function onSearchInput() {
  aiMatchedIds.value = new Set()
  currentPage.value = 1
}

function toggleAiMode() {
  aiMode.value = !aiMode.value
  if (!aiMode.value) {
    aiQuery.value = ''
    aiMatchedIds.value = new Set()
    currentPage.value = 1
  }
}

function clearFilters() {
  searchQuery.value = ''
  aiQuery.value = ''
  aiMatchedIds.value = new Set()
  dateFrom.value = ''
  dateTo.value = ''
  currentPage.value = 1
  showFilters.value = false
}

async function doAiSearch() {
  const q = aiQuery.value.trim()
  if (!q) return
  aiSearching.value = true
  aiMatchedIds.value = new Set()
  try {
    const summary = activities.value.map((item, i) =>
      `[${i}] ${item.type}: "${item.title}" (${(item.date || '').substring(0, 10)})`
    ).join('\n')

    const prompt = `你是动态搜索助手。以下是博客活动列表，格式为 [序号] 类型: "标题" (日期)。

用户想查找: "${q}"

请找出匹配的动态序号，只返回JSON数组如[0,3,5]，无匹配返回[]。

动态列表:
${summary}`.substring(0, 3900)

    const res = await request.post('/api/ai/chat', {
      message: prompt
    })

    let text = ''
    if (typeof res === 'string') text = res
    else if (res?.response) text = res.response
    else if (res?.reply) text = res.reply
    else if (res?.content) text = res.content
    else text = JSON.stringify(res)

    const match = text.match(/\[[\d\s,]*\]/)
    if (match) {
      const indices = JSON.parse(match[0])
      const ids = new Set()
      indices.forEach(i => {
        if (i >= 0 && i < activities.value.length) {
          const item = activities.value[i]
          ids.add(item.id + '-' + item.type)
        }
      })
      aiMatchedIds.value = ids
      searchQuery.value = ''
      currentPage.value = 1
    }
  } catch {
    // Fallback: do nothing, show all
  } finally {
    aiSearching.value = false
  }
}

async function loadProfile() {
  try {
    const data = await request.get('/api/site/info')
    if (data) {
      siteName.value = data.siteName || ''
      const extra = data.extraSettings || {}
      avatar.value = extra.site_avatar || ''
      cover.value = extra.about_cover || ''
      subtitle.value = extra.about_subtitle || data.siteDescription || ''
      bio.value = extra.about_bio || ''
    }
  } catch {}
}

async function loadActivities() {
  loading.value = true
  try {
    const [momentsRes, articlesRes, announcementsRes] = await Promise.allSettled([
      request.get('/api/timeline-entries'),
      request.get('/api/articles'),
      request.get('/api/announcements')
    ])

    const items = []

    if (momentsRes.status === 'fulfilled' && Array.isArray(momentsRes.value)) {
      momentsRes.value.forEach(m => {
        items.push({
          id: m.id,
          type: 'moment',
          title: m.title || '',
          date: m.entryDate || m.createdAt || '',
          url: `/moments/${m.id}`
        })
      })
    }

    if (articlesRes.status === 'fulfilled' && articlesRes.value) {
      const articles = Array.isArray(articlesRes.value) ? articlesRes.value : (articlesRes.value.content || [])
      articles.forEach(a => {
        items.push({
          id: a.id,
          type: 'article',
          title: a.title || '',
          date: a.createdAt || a.updatedAt || '',
          url: `/articles/${a.slug || a.id}`
        })
      })
    }

    if (announcementsRes.status === 'fulfilled' && Array.isArray(announcementsRes.value)) {
      announcementsRes.value.forEach(a => {
        items.push({
          id: a.id,
          type: 'announcement',
          title: a.title || '',
          date: a.createdAt || a.updatedAt || '',
          url: '/announcements'
        })
      })
    }

    items.sort((a, b) => (b.date || '').localeCompare(a.date || ''))
    activities.value = items
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

watch(visible, (v) => {
  if (v) {
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', onEsc)
    activeTab.value = 'intro'
    clearFilters()
    loadProfile()
    loadActivities()
  } else {
    document.body.style.overflow = ''
    document.removeEventListener('keydown', onEsc)
  }
})

let observer = null
onMounted(() => {
  observer = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  observer.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})
onUnmounted(() => {
  observer?.disconnect()
  document.removeEventListener('keydown', onEsc)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.modal-container {
  position: relative;
  width: 100%;
  max-width: 640px;
  max-height: 85vh;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.modal-container.is-night {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background 0.2s;
}
.modal-close:hover { background: rgba(0, 0, 0, 0.5); }

/* Cover */
.modal-cover {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
  border-radius: 32px 32px 0 0;
}
@media (min-width: 640px) {
  .modal-cover { height: 220px; }
}
.modal-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 1s ease;
}
.modal-cover:hover .modal-cover-img {
  transform: scale(1.05);
}
.modal-cover-default {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 50%, #93c5fd 100%);
}
.modal-cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5) 0%, transparent 60%);
}

/* Header */
.modal-header {
  text-align: center;
  padding: 0 24px;
  margin-top: -48px;
  position: relative;
  z-index: 2;
}
.modal-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto;
  border: 4px solid white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}
.is-night .modal-avatar { border-color: #1e293b; }
.modal-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.modal-avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  font-size: 36px;
  font-weight: 700;
}

.modal-name {
  font-size: 1.6rem;
  font-weight: 900;
  color: #1e293b;
  margin: 14px 0 6px;
}
.is-night .modal-name { color: #e2e8f0; }

.modal-subtitle {
  font-size: 13px;
  color: #3b82f6;
  letter-spacing: 0.05em;
  margin: 0 0 20px;
}
.is-night .modal-subtitle { color: #60a5fa; }

/* Tabs */
.modal-tabs {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 0 24px;
  margin-bottom: 20px;
}
.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.5);
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}
.is-night .tab-btn {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.06);
  color: #94a3b8;
}
.tab-btn:hover { color: #3b82f6; }
.tab-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* Body */
.modal-body {
  padding: 0 24px 28px;
}
.tab-content--activity {
  padding-bottom: 260px;
}

.fade-in {
  animation: fadeIn 0.3s ease both;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Bio content */
.bio-content {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
}
.is-night .bio-content { color: #cbd5e1; }
.bio-content :deep(h1),
.bio-content :deep(h2),
.bio-content :deep(h3) {
  margin-top: 1.2em;
  margin-bottom: 0.5em;
  font-weight: 700;
  color: #1e293b;
}
.is-night .bio-content :deep(h1),
.is-night .bio-content :deep(h2),
.is-night .bio-content :deep(h3) { color: #e2e8f0; }
.bio-content :deep(p) { margin: 0 0 0.8em; }
.bio-content :deep(code) {
  font-family: 'Fira Code', monospace;
  font-size: 0.9em;
  background: rgba(59, 130, 246, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  color: #3b82f6;
}
.is-night .bio-content :deep(code) {
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
}
.bio-content :deep(pre) {
  background: #1e293b;
  border-radius: 8px;
  padding: 14px;
  overflow-x: auto;
  margin: 0.8em 0;
}
.bio-content :deep(pre code) {
  background: none;
  color: #e2e8f0;
  padding: 0;
  font-size: 13px;
}
.bio-content :deep(a) {
  color: #3b82f6;
  text-decoration: none;
  border-bottom: 1px solid rgba(59, 130, 246, 0.3);
}
.bio-content :deep(ul), .bio-content :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.bio-empty {
  text-align: center;
  padding: 32px 0;
  color: #94a3b8;
  font-size: 14px;
  white-space: pre-line;
}

/* Activity */
.activity-loading {
  padding: 16px 0;
}
.skeleton {
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
}
.is-night .skeleton {
  background: linear-gradient(90deg, #334155 25%, #475569 50%, #334155 75%);
  background-size: 200% 100%;
}
@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.activity-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;
  color: #94a3b8;
  font-size: 14px;
}

.activity-timeline {
  position: relative;
  padding-left: 24px;
}
.activity-timeline::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 1px;
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
}
.timeline-item:last-child { padding-bottom: 0; }

.timeline-dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #3b82f6;
  background: white;
  z-index: 1;
  transition: transform 0.2s;
}
.is-night .timeline-dot { background: #1e293b; }
.timeline-item:hover .timeline-dot { transform: scale(1.3); }
.timeline-dot.type-moment { border-color: #10b981; }
.timeline-dot.type-article { border-color: #3b82f6; }
.timeline-dot.type-announcement { border-color: #f59e0b; }

.timeline-content {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  padding: 12px 16px;
  transition: all 0.2s;
}
.is-night .timeline-content {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.06);
}
.timeline-content:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.timeline-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.timeline-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.timeline-avatar-placeholder {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}
.timeline-type {
  font-size: 12px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 10px;
}
.timeline-type.type-moment {
  color: #10b981;
  background: rgba(16, 185, 129, 0.08);
}
.timeline-type.type-article {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
}
.timeline-type.type-announcement {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.08);
}
.timeline-date {
  font-size: 12px;
  color: #94a3b8;
  font-family: monospace;
}

.timeline-title {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}
.timeline-title:hover { color: #3b82f6; }
.is-night .timeline-title { color: #e2e8f0; }
.is-night .timeline-title:hover { color: #60a5fa; }

/* Search & Filter Toolbar */
.activity-toolbar {
  margin-bottom: 16px;
}
.search-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.search-input-wrap {
  flex: 1;
  position: relative;
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  color: #1e293b;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.search-input:focus {
  border-color: #3b82f6;
}
.is-night .search-input {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
}
.is-night .search-input:focus {
  border-color: #60a5fa;
}
.ai-search-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.06);
  color: #3b82f6;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.ai-search-btn:hover {
  background: rgba(59, 130, 246, 0.12);
}
.ai-search-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

/* AI Sun Loader */
.ai-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  position: relative;
}
.loader {
  --color-one: #ffbf48;
  --color-two: #be4a1d;
  --color-three: #ffbf4780;
  --color-four: #bf4a1d80;
  --color-five: #ffbf4740;
  --time-animation: 2s;
  --size: 0.15;
  position: relative;
  border-radius: 50%;
  transform: scale(var(--size));
  box-shadow: 0 0 25px 0 var(--color-three), 0 20px 50px 0 var(--color-four);
  animation: colorize calc(var(--time-animation) * 3) ease-in-out infinite;
  width: 100px;
  height: 100px;
}
.loader::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border-top: solid 1px var(--color-one);
  border-bottom: solid 1px var(--color-two);
  background: linear-gradient(180deg, var(--color-five), var(--color-four));
  box-shadow: inset 0 10px 10px 0 var(--color-three), inset 0 -10px 10px 0 var(--color-four);
}
.loader .box {
  width: 100px;
  height: 100px;
  background: linear-gradient(180deg, var(--color-one) 30%, var(--color-two) 70%);
  mask: url(#clipping);
  -webkit-mask: url(#clipping);
}
.loader svg {
  position: absolute;
}
.loader svg #clipping {
  filter: contrast(15);
  animation: roundness calc(var(--time-animation) / 2) linear infinite;
}
.loader svg #clipping polygon {
  filter: blur(7px);
}
.loader svg #clipping polygon:nth-child(1) { transform-origin: 75% 25%; transform: rotate(90deg); }
.loader svg #clipping polygon:nth-child(2) { transform-origin: 50% 50%; animation: rotation var(--time-animation) linear infinite reverse; }
.loader svg #clipping polygon:nth-child(3) { transform-origin: 50% 60%; animation: rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -3); }
.loader svg #clipping polygon:nth-child(4) { transform-origin: 40% 40%; animation: rotation var(--time-animation) linear infinite reverse; }
.loader svg #clipping polygon:nth-child(5) { transform-origin: 40% 40%; animation: rotation var(--time-animation) linear infinite reverse; animation-delay: calc(var(--time-animation) / -2); }
.loader svg #clipping polygon:nth-child(6) { transform-origin: 60% 40%; animation: rotation var(--time-animation) linear infinite; }
.loader svg #clipping polygon:nth-child(7) { transform-origin: 60% 40%; animation: rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -1.5); }
@keyframes rotation { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
@keyframes roundness { 0% { filter: contrast(15); } 20% { filter: contrast(3); } 40% { filter: contrast(3); } 60% { filter: contrast(15); } 100% { filter: contrast(15); } }
@keyframes colorize { 0% { filter: hue-rotate(0deg); } 20% { filter: hue-rotate(-30deg); } 40% { filter: hue-rotate(-60deg); } 60% { filter: hue-rotate(-90deg); } 80% { filter: hue-rotate(-45deg); } 100% { filter: hue-rotate(0deg); } }

/* Filter Toggle Button */
.filter-toggle-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.5);
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  position: relative;
}
.is-night .filter-toggle-btn {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.06);
  color: #94a3b8;
}
.filter-toggle-btn:hover { color: #3b82f6; border-color: rgba(59, 130, 246, 0.3); }
.filter-toggle-btn.active {
  background: rgba(59, 130, 246, 0.08);
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.3);
}
.filter-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
  position: absolute;
  top: 4px;
  right: 4px;
}

/* Filter Panel */
.filter-panel {
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 8px;
  position: relative;
  z-index: 100;
}
.is-night .filter-panel {
  background: rgba(30, 41, 59, 0.4);
  border-color: rgba(255, 255, 255, 0.04);
}
.filter-expand-enter-active,
.filter-expand-leave-active {
  transition: all 0.25s ease;
}
.filter-expand-enter-from,
.filter-expand-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
.filter-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
.ai-search-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.ai-input {
  padding-left: 12px;
}
.ai-go-btn {
  padding: 8px 14px;
  border: 1px solid #3b82f6;
  border-radius: 10px;
  background: #3b82f6;
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 44px;
}
.ai-go-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.filter-row {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.filter-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.filter-label {
  font-size: 0.82rem;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
  min-width: 56px;
}
.is-night .filter-label { color: #a0aec0; }
.clear-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto;
}
.clear-btn:hover {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
}
.filter-status {
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-count {
  font-size: 12px;
  color: #94a3b8;
}
.ai-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 6px;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}
.is-night .pagination {
  border-top-color: rgba(255, 255, 255, 0.06);
}
.page-btn {
  padding: 6px 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.5);
  color: #3b82f6;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-btn:not(:disabled):hover {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}
.is-night .page-btn {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
}
.page-info {
  font-size: 12px;
  color: #94a3b8;
  font-family: monospace;
}

/* Transition */
.modal-enter-active, .modal-leave-active { transition: opacity 0.3s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .modal-container { animation: modalSlideIn 0.3s ease; }
.modal-leave-active .modal-container { animation: modalSlideOut 0.2s ease; }
@keyframes modalSlideIn {
  from { opacity: 0; transform: translateY(20px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes modalSlideOut {
  from { opacity: 1; transform: translateY(0) scale(1); }
  to { opacity: 0; transform: translateY(20px) scale(0.96); }
}

@media (max-width: 640px) {
  .modal-overlay { padding: 10px; }
  .modal-container { max-height: 90vh; border-radius: 24px; }
  .modal-cover { border-radius: 24px 24px 0 0; }
  .modal-header { padding: 0 16px; }
  .modal-avatar { width: 72px; height: 72px; }
  .modal-name { font-size: 1.3rem; margin-top: 10px; }
  .modal-tabs { padding: 0 16px; }
  .modal-body { padding: 0 16px 20px; }
  .search-row { flex-wrap: wrap; }
  .search-input-wrap { flex: 1 1 100%; }
  .ai-search-btn, .filter-toggle-btn { flex: 1; }
  .filter-row { flex-wrap: wrap; }
  .filter-field { flex: 1; min-width: 0; }
  .pagination { gap: 8px; }
  .page-btn { padding: 6px 10px; font-size: 11px; }
}
</style>
