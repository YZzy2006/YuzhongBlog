<template>
  <div class="moments-page" :class="{ 'is-night': isNight }">
    <!-- Title + Subtitle -->
    <div class="page-header fade-in-up">
      <h1 class="page-title">{{ $t('moments.title') }}</h1>
      <p class="page-subtitle">
        <svg class="subtitle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M12 2l2.09 6.26L21 9.27l-5 4.87L17.18 21 12 17.27 6.82 21 8 14.14l-5-4.87 6.91-1.01z"/></svg>
        {{ $t('moments.description') }}
      </p>
    </div>

    <!-- Search + Sort + Filter -->
    <div class="search-sort-block fade-in-up">
      <div class="search-row">
        <div class="search-wrap group">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-if="!aiMode" v-model="searchQuery" type="text" class="search-input" :placeholder="$t('moments.searchPlaceholder')" @input="currentPage = 1" />
          <input v-else v-model="aiQuery" type="text" class="search-input ai-input" :placeholder="$t('moments.aiSearchPlaceholder')" @keyup.enter="doAiSearch" />
          <button class="ai-toggle-btn" :class="{ active: aiMode }" @click="toggleAiMode" :title="$t('moments.aiSearch')">
            <svg v-if="!aiSearching" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M12 2l2.09 6.26L21 9.27l-5 4.87L17.18 21 12 17.27 6.82 21 8 14.14l-5-4.87 6.91-1.01z"/></svg>
            <div v-else class="sun-loader"><div class="sun-loader-inner"></div></div>
          </button>
          <button v-if="aiMode" class="ai-search-btn" @click="doAiSearch" :disabled="aiSearching">{{ $t('moments.aiSearch') }}</button>
        </div>
        <button class="filter-toggle-btn" :class="{ active: showFilters || hasFilters }" @click="showFilters = !showFilters">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
          {{ $t('moments.filter') }}
          <span v-if="hasFilters" class="filter-badge"></span>
        </button>
        <div class="sort-toggle">
          <button class="sort-btn" :class="{ active: sortOrder === 'desc' }" @click="sortOrder = 'desc'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M3 6h7M3 12h7M3 18h7M14 6l4 4-4 4M14 18h7"/></svg>
            {{ $t('moments.newest') }}
          </button>
          <button class="sort-btn" :class="{ active: sortOrder === 'hot' }" @click="sortOrder = 'hot'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
            {{ $t('moments.hottest') }}
          </button>
        </div>
      </div>

      <!-- Collapsible Filter Panel -->
      <Transition name="filter-slide">
        <div v-if="showFilters" class="filter-panel">
          <div class="filter-row">
            <label class="filter-label">{{ $t('moments.dateFrom') }}</label>
            <DatePicker v-model="dateFrom" :placeholder="$t('moments.dateFrom')" :max="dateTo || undefined" @change="currentPage = 1" />
          </div>
          <div class="filter-row">
            <label class="filter-label">{{ $t('moments.dateTo') }}</label>
            <DatePicker v-model="dateTo" :placeholder="$t('moments.dateTo')" :min="dateFrom || undefined" @change="currentPage = 1" />
          </div>
          <div class="filter-row tag-filter-row">
            <label class="filter-label">{{ $t('moments.tag') }}</label>
            <div class="tag-btns">
              <button class="tag-btn" :class="{ active: !activeTag }" @click="activeTag = null">{{ $t('moments.allTags') }}</button>
              <button v-for="tag in allTags" :key="tag" class="tag-btn" :class="{ active: activeTag === tag }" @click="activeTag = activeTag === tag ? null : tag">{{ tag }}</button>
            </div>
          </div>
          <button v-if="hasFilters" class="clear-filters-btn" @click="clearFilters">{{ $t('moments.clear') }}</button>
        </div>
      </Transition>

      <p v-if="aiMode" class="ai-search-tip">{{ $t('moments.aiSearchTip') }}</p>
    </div>


    <!-- Result count -->
    <div v-if="!loading && !loadError && entries.length > 0" class="result-count">
      {{ $t('moments.total', { count: filteredEntries.length }) }}
    </div>

    <!-- Empty state -->
    <div v-if="!loading && filteredEntries.length === 0 && !loadError" class="empty-state fade-in-scale">
      <div class="empty-card">
        <div class="empty-icon-wrap">
          <div class="empty-glow"></div>
          <svg class="empty-ghost" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48"><path d="M12 2C6.48 2 2 6.48 2 12v8c0 1.1.9 2 2 2h1.5c.83 0 1.5-.67 1.5-1.5v-1c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5v1c0 .83.67 1.5 1.5 1.5h3c.83 0 1.5-.67 1.5-1.5v-1c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5v1c0 .83.67 1.5 1.5 1.5H20c1.1 0 2-.9 2-2v-8c0-5.52-4.48-10-10-10z"/><circle cx="9" cy="10" r="1.5" fill="currentColor"/><circle cx="15" cy="10" r="1.5" fill="currentColor"/></svg>
        </div>
        <h2 class="empty-title">{{ searchQuery || aiQuery ? $t('moments.noResults') : $t('moments.noEntries') }}</h2>
        <p class="empty-desc">{{ searchQuery || aiQuery ? $t('moments.noResultsDesc') : $t('moments.emptyDesc') }}</p>
      </div>
    </div>

    <ResourceError v-if="loadError" :message="$t('moments.loadError')" @retry="loadEntries()" />

    <!-- Skeleton loading -->
    <div v-if="loading && !loadError" class="dual-column">
      <div class="column">
        <div v-for="n in 3" :key="n" class="moment-card skeleton-card">
          <div class="card-header"><div class="skeleton sk-avatar"></div><div><div class="skeleton sk-name"></div><div class="skeleton sk-time"></div></div></div>
          <div class="skeleton sk-text"></div>
          <div class="skeleton sk-text short"></div>
        </div>
      </div>
      <div class="column">
        <div v-for="n in 2" :key="n" class="moment-card skeleton-card">
          <div class="card-header"><div class="skeleton sk-avatar"></div><div><div class="skeleton sk-name"></div><div class="skeleton sk-time"></div></div></div>
          <div class="skeleton sk-text"></div>
        </div>
      </div>
    </div>

    <!-- Cards: dual column -->
    <div v-if="!loading && paginatedEntries.length > 0" class="dual-column">
      <div class="column">
        <div
          v-for="(entry, idx) in evenEntries"
          :key="entry.id"
          class="moment-card fade-in-scroll"
          :style="{ animationDelay: idx * 100 + 'ms', cursor: 'pointer' }"
          @click="goToDetail(entry.id)"
        >
          <!-- Header: avatar + author + time -->
          <div class="card-header">
            <div class="card-avatar" :style="avatarUrl ? { background: `url(${avatarUrl}) center/cover` } : {}">
              <span v-if="!avatarUrl">{{ avatarInitial }}</span>
            </div>
            <div class="card-author-info">
              <h3 class="card-author-name">{{ authorName }}</h3>
              <div class="card-time">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="10" height="10"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <span>{{ timeAgo(entry.entryDate) }}</span>
              </div>
            </div>
          </div>

          <!-- Divider -->
          <div class="card-divider"></div>

          <!-- Content -->
          <p class="card-content">{{ entry.description || entry.title }}</p>

          <!-- Images grid -->
          <div v-if="parseImages(entry).length" class="card-images-wrap">
            <div class="card-images" :class="imgsClass(parseImages(entry).length)">
              <div
                v-for="(img, i) in parseImages(entry).slice(0, 9)"
                :key="i"
                class="card-img-cell"
                @click.stop="openLightbox(parseImages(entry), i)"
              >
                <img :src="ossImg(img)" :alt="entry.title" loading="lazy" />
                <div v-if="i === 8 && parseImages(entry).length > 9" class="img-more-overlay">
                  +{{ parseImages(entry).length - 9 }}
                </div>
              </div>
            </div>
          </div>

          <!-- Bottom bar -->
          <div class="card-bottom">
            <span v-if="entry.category" class="card-category-tag">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
              {{ entry.category }}
            </span>
          </div>
        </div>
      </div>
      <div class="column">
        <div
          v-for="(entry, idx) in oddEntries"
          :key="entry.id"
          class="moment-card fade-in-scroll"
          :style="{ animationDelay: (idx * 100 + 50) + 'ms', cursor: 'pointer' }"
          @click="goToDetail(entry.id)"
        >
          <!-- Header -->
          <div class="card-header">
            <div class="card-avatar" :style="avatarUrl ? { background: `url(${avatarUrl}) center/cover` } : {}">
              <span v-if="!avatarUrl">{{ avatarInitial }}</span>
            </div>
            <div class="card-author-info">
              <h3 class="card-author-name">{{ authorName }}</h3>
              <div class="card-time">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="10" height="10"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <span>{{ timeAgo(entry.entryDate) }}</span>
              </div>
            </div>
          </div>
          <div class="card-divider"></div>
          <p class="card-content">{{ entry.description || entry.title }}</p>
          <div v-if="parseImages(entry).length" class="card-images-wrap">
            <div class="card-images" :class="imgsClass(parseImages(entry).length)">
              <div
                v-for="(img, i) in parseImages(entry).slice(0, 9)"
                :key="i"
                class="card-img-cell"
                @click.stop="openLightbox(parseImages(entry), i)"
              >
                <img :src="ossImg(img)" :alt="entry.title" loading="lazy" />
                <div v-if="i === 8 && parseImages(entry).length > 9" class="img-more-overlay">
                  +{{ parseImages(entry).length - 9 }}
                </div>
              </div>
            </div>
          </div>
          <div class="card-bottom">
            <span v-if="entry.category" class="card-category-tag">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
              {{ entry.category }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="!loading && totalPages > 1" class="pagination-wrap">
      <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage--">{{ $t('moments.prev') }}</button>
      <span class="page-info">{{ $t('moments.pageInfo', { current: currentPage, total: totalPages }) }}</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage++">{{ $t('moments.next') }}</button>
    </div>

    <!-- Back to top -->
    <Transition name="fade">
      <button v-if="showBackToTop" class="back-to-top" @click="scrollToTop" :title="$t('moments.backToTop')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><polyline points="18 15 12 9 6 15"/></svg>
      </button>
    </Transition>

    <!-- Lightbox -->
    <ImageLightbox v-model="lightboxVisible" :images="lightboxImages" :initial-index="lightboxIndex" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import ResourceError from '../components/ResourceError.vue'
import DatePicker from '../components/DatePicker.vue'
import ImageLightbox from '../components/ImageLightbox.vue'
import { ossImg } from '../utils/oss'

const { t } = useI18n()
const router = useRouter()

const entries = ref([])
const loading = ref(true)
const loadError = ref(false)
const isNight = ref(document.body.classList.contains('body-night'))
const showBackToTop = ref(false)
const searchQuery = ref('')
const activeTag = ref(null)
const sortOrder = ref('desc')

// AI search + filter + pagination
const aiMode = ref(false)
const aiQuery = ref('')
const aiSearching = ref(false)
const aiMatchedIds = ref(new Set())
const dateFrom = ref('')
const dateTo = ref('')
const currentPage = ref(1)
const pageSize = 30
const showFilters = ref(false)

const hasFilters = computed(() =>
  searchQuery.value || aiMatchedIds.value.size > 0 || dateFrom.value || dateTo.value || activeTag.value
)

// Lightbox
const lightboxVisible = ref(false)
const lightboxImages = ref([])
const lightboxIndex = ref(0)

const imageCache = new WeakMap()
function parseImages(entry) {
  let cached = imageCache.get(entry)
  if (cached) return cached
  const imgs = []
  if (entry.coverImage) imgs.push(entry.coverImage)
  if (entry.images) {
    try {
      const arr = JSON.parse(entry.images)
      if (Array.isArray(arr)) arr.forEach(u => { if (u && !imgs.includes(u)) imgs.push(u) })
    } catch {}
  }
  imageCache.set(entry, imgs)
  return imgs
}

function openLightbox(images, idx) {
  lightboxImages.value = images
  lightboxIndex.value = idx
  lightboxVisible.value = true
}

function imgsClass(count) {
  if (count <= 1) return 'imgs-1'
  if (count === 2) return 'imgs-2'
  if (count === 3) return 'imgs-3'
  if (count === 4) return 'imgs-4'
  return 'imgs-many'
}

// Site info
const authorName = ref('')
const avatarUrl = ref('')

const avatarInitial = computed(() => {
  const name = authorName.value || '?'
  return name.charAt(0)
})

// === Tags ===
const allTags = computed(() => {
  const tagSet = new Set()
  entries.value.forEach(e => {
    if (e.tags) {
      try {
        const arr = JSON.parse(e.tags)
        if (Array.isArray(arr)) arr.forEach(tag => tagSet.add(tag))
      } catch {}
    }
  })
  return [...tagSet]
})

// === Filtering + Sorting ===
const filteredEntries = computed(() => {
  let list = [...entries.value]
  // AI match filter
  if (aiMatchedIds.value.size > 0) {
    list = list.filter(e => aiMatchedIds.value.has(String(e.id)))
  }
  // Tag filter
  if (activeTag.value) {
    list = list.filter(e => {
      if (!e.tags) return false
      try {
        const arr = JSON.parse(e.tags)
        return Array.isArray(arr) && arr.includes(activeTag.value)
      } catch { return false }
    })
  }
  // Search filter
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(e =>
      (e.title && e.title.toLowerCase().includes(q)) ||
      (e.description && e.description.toLowerCase().includes(q)) ||
      (e.category && e.category.toLowerCase().includes(q))
    )
  }
  // Date range filter
  if (dateFrom.value) {
    list = list.filter(e => (e.entryDate || '').substring(0, 10) >= dateFrom.value)
  }
  if (dateTo.value) {
    list = list.filter(e => (e.entryDate || '').substring(0, 10) <= dateTo.value)
  }
  // Sort
  list.sort((a, b) => {
    if (sortOrder.value === 'hot') {
      const scoreA = (a.viewCount || 0) + (a.likeCount || 0) * 3 + (a.sortOrder || 0) * 10
      const scoreB = (b.viewCount || 0) + (b.likeCount || 0) * 3 + (b.sortOrder || 0) * 10
      return scoreB - scoreA
    }
    const timeA = new Date(a.entryDate).getTime()
    const timeB = new Date(b.entryDate).getTime()
    return sortOrder.value === 'desc' ? timeB - timeA : timeA - timeB
  })
  return list
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredEntries.value.length / pageSize)))
watch([filteredEntries, currentPage], () => {
  const max = totalPages.value
  if (currentPage.value > max) currentPage.value = max
})
const paginatedEntries = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredEntries.value.slice(start, start + pageSize)
})
const evenEntries = computed(() => paginatedEntries.value.filter((_, i) => i % 2 === 0))
const oddEntries = computed(() => paginatedEntries.value.filter((_, i) => i % 2 === 1))

// === Time ago ===
function timeAgo(dateStr) {
  if (!dateStr) return ''
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  if (isNaN(then)) return dateStr.substring(0, 10)
  const diff = now - then
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return t('moments.justNow')
  if (mins < 60) return t('moments.minutesAgo', { n: mins })
  const hours = Math.floor(mins / 60)
  if (hours < 24) return t('moments.hoursAgo', { n: hours })
  const days = Math.floor(hours / 24)
  if (days < 30) return t('moments.daysAgo', { n: days })
  return dateStr.substring(0, 10)
}

function goToDetail(id) {
  router.push(`/moments/${id}`)
}

// === AI Search ===
function toggleAiMode() {
  aiMode.value = !aiMode.value
  if (!aiMode.value) {
    aiQuery.value = ''
    aiMatchedIds.value = new Set()
  } else {
    searchQuery.value = ''
  }
}

async function doAiSearch() {
  const q = aiQuery.value.trim()
  if (!q) return
  aiSearching.value = true
  aiMatchedIds.value = new Set()
  try {
    const summary = entries.value.map((e, i) =>
      `[${i}] "${e.title || ''}" (${(e.entryDate || '').substring(0, 10)})`
    ).join('\n')
    const prompt = `你是动态搜索助手。以下是博客动态列表，格式为 [序号] "标题" (日期)。
用户想查找: "${q}"
请找出匹配的动态序号，只返回JSON数组如[0,3,5]，无匹配返回[]。
动态列表:
${summary}`.substring(0, 3900)
    const res = await request.post('/api/ai/chat', { message: prompt })
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
        if (i >= 0 && i < entries.value.length) {
          ids.add(String(entries.value[i].id))
        }
      })
      aiMatchedIds.value = ids
      searchQuery.value = ''
      currentPage.value = 1
    }
  } catch {
    // Fallback: show all
  } finally {
    aiSearching.value = false
  }
}

function clearFilters() {
  searchQuery.value = ''
  aiQuery.value = ''
  aiMatchedIds.value = new Set()
  dateFrom.value = ''
  dateTo.value = ''
  activeTag.value = null
  currentPage.value = 1
}

// === Back to top ===
function onScroll() {
  showBackToTop.value = window.scrollY > 400
}
function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// === Night mode ===
let nightObserver = null

// === Load site info ===
async function loadSiteInfo() {
  try {
    const data = await request.get('/api/site/info')
    if (data) {
      if (data.siteName) authorName.value = data.siteName
      if (data.extraSettings?.site_avatar) avatarUrl.value = data.extraSettings.site_avatar
    }
  } catch {}
}

// === Load entries ===
async function loadEntries() {
  loadError.value = false
  loading.value = true
  try {
    const data = await request.get('/api/timeline-entries')
    entries.value = data || []
  } catch (e) {
    console.error('Failed to load moments:', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSiteInfo()
  loadEntries()
  window.addEventListener('scroll', onScroll, { passive: true })
  nightObserver = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  nightObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  if (nightObserver) { nightObserver.disconnect(); nightObserver = null }
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.moments-page {
  width: 95%;
  max-width: 1152px;
  margin: 0 auto;
  padding: 24px 0 128px;
  min-height: 85vh;
  display: flex;
  flex-direction: column;
}
@media (min-width: 768px) {
  .moments-page { width: 90%; padding: 40px 0 128px; }
}

/* ===== Page Header ===== */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}
@media (min-width: 768px) {
  .page-header { margin-bottom: 56px; }
}
.page-title {
  font-size: 1.875rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 8px;
  letter-spacing: -0.04em;
}
@media (min-width: 768px) {
  .page-title { font-size: 3rem; margin-bottom: 16px; }
}
.is-night .page-title { color: #f1f5f9; }
.page-subtitle {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  font-style: italic;
  opacity: 0.8;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
@media (min-width: 768px) {
  .page-subtitle { font-size: 14px; gap: 8px; }
}
.is-night .page-subtitle { color: #94a3b8; }
.subtitle-icon { color: #3b82f6; flex-shrink: 0; }

/* ===== Search + Sort Block ===== */
.search-sort-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}
@media (min-width: 768px) {
  .search-sort-block { gap: 32px; margin-bottom: 40px; }
}
.search-wrap {
  position: relative;
  width: 100%;
  max-width: 512px;
}
.search-icon {
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  pointer-events: none;
  transition: color 0.2s;
  z-index: 2;
}
@media (min-width: 768px) {
  .search-icon { left: 20px; }
}
.search-wrap:focus-within .search-icon { color: #3b82f6; }
.search-input {
  width: 100%;
  padding: 12px 20px 12px 48px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}
@media (min-width: 768px) {
  .search-input { padding: 16px 24px 16px 56px; border-radius: 16px; font-size: 16px; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08); }
}
.search-input:focus {
  outline: none;

  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.5);
}
.search-input::placeholder { color: #94a3b8; }
.is-night .search-input {
  background: rgba(30, 41, 59, 0.4);
  border-color: rgba(255, 255, 255, 0.05);
  color: #f1f5f9;
}
.is-night .search-input:focus { box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.5); }

/* Sort toggle */
.sort-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  padding: 4px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 1;
}
@media (min-width: 768px) {
  .sort-toggle { padding: 6px; border-radius: 16px; }
}
.is-night .sort-toggle {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}
.sort-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 11px;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
@media (min-width: 768px) {
  .sort-btn { padding: 8px 24px; border-radius: 12px; font-size: 12px; }
}
.sort-btn:hover { color: #3b82f6; }
.sort-btn.active {
  background: #3b82f6;
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transform: scale(1.05);
}
.is-night .sort-btn { color: #94a3b8; }
.is-night .sort-btn:hover { color: #60a5fa; }

/* Tags */
.tag-filter-row {
  flex-wrap: wrap;
}
.tag-btns {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.tag-btn {
  font-size: 11px;
  font-weight: 600;
  padding: 5px 14px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}
.tag-btn:hover { border-color: #3b82f6; color: #3b82f6; }
.tag-btn.active { background: #3b82f6; border-color: #3b82f6; color: white; }
.is-night .tag-btn { background: rgba(30, 41, 59, 0.4); border-color: rgba(255, 255, 255, 0.06); color: #94a3b8; }
.is-night .tag-btn:hover { border-color: #60a5fa; color: #60a5fa; }

/* ===== Total ===== */
.total {
  color: #64748b;
  margin-bottom: 16px;
  font-size: 0.85rem;
  font-weight: 600;
  text-align: center;
}
.is-night .total { color: #94a3b8; }

/* ===== Dual Column Layout ===== */
.dual-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}
@media (min-width: 768px) {
  .dual-column {
    flex-direction: row;
    gap: 32px;
  }
}
.column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}
@media (min-width: 768px) {
  .column { gap: 32px; }
}

/* ===== Moment Card ===== */
.moment-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.4s, transform 0.4s;
  overflow: hidden;
  position: relative;
}
.moment-card:hover {
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.is-night .moment-card {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
}
.is-night .moment-card:hover {
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3);
}
@media (min-width: 768px) {
  .moment-card {
    border-radius: 40px;
    padding: 40px;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
  }
}

/* ===== Card Header ===== */
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  margin-bottom: 0;
}
@media (min-width: 768px) {
  .card-header { gap: 16px; padding-bottom: 24px; }
}
.card-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  background: linear-gradient(135deg, #3b82f6, #3b82f6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 900;
  font-size: 16px;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.is-night .card-avatar { border-color: rgba(51, 65, 85, 0.8); }
@media (min-width: 768px) {
  .card-avatar { width: 56px; height: 56px; border-radius: 16px; font-size: 22px; }
}
.card-author-info { flex: 1; min-width: 0; }
.card-author-name {
  font-size: 15px;
  font-weight: 900;
  color: #576b95;
  margin: 0;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}
.is-night .card-author-name { color: #7f99cc; }
@media (min-width: 768px) {
  .card-author-name { font-size: 18px; }
}
.card-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  margin-top: 2px;
}

/* ===== Card Divider ===== */
.card-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}
.is-night .card-divider { background: rgba(255, 255, 255, 0.05); }
@media (min-width: 768px) {
  .card-divider { margin-bottom: 32px; }
}

/* ===== Card Content ===== */
.card-content {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.7;
  font-weight: 500;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0 0 16px;
}
.is-night .card-content { color: #e2e8f0; }
@media (min-width: 768px) {
  .card-content { font-size: 16px; margin-bottom: 24px; }
}

/* ===== Card Images Grid ===== */
.card-images-wrap {
  width: 100%;
  display: flex;
  justify-content: flex-start;
  margin-bottom: 16px;
}
@media (min-width: 640px) {
  .card-images-wrap { justify-content: center; }
}
@media (min-width: 768px) {
  .card-images-wrap { margin-bottom: 24px; }
}
.card-images {
  display: grid;
  gap: 6px;
  border-radius: 12px;
  overflow: hidden;
  max-width: 80%;
}
.card-images.imgs-1 { grid-template-columns: 1fr; max-width: 80%; }
.card-images.imgs-2 { grid-template-columns: 1fr 1fr; }
.card-images.imgs-3 { grid-template-columns: 1fr 1fr 1fr; }
.card-images.imgs-4 { grid-template-columns: 1fr 1fr; max-width: 210px; }
.card-images.imgs-many { grid-template-columns: 1fr 1fr 1fr; }
.card-img-cell {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  cursor: zoom-in;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(226, 232, 240, 0.2);
}
.is-night .card-img-cell { border-color: rgba(255, 255, 255, 0.1); background: rgba(51, 65, 85, 0.2); }
.card-images.imgs-1 .card-img-cell {
  aspect-ratio: auto;
  max-height: 300px;
  border-radius: 12px;
  background: transparent;
  cursor: zoom-in;
}
.card-images.imgs-1 .card-img-cell img {
  object-fit: contain;
  transition: transform 0.5s;
}
.card-images.imgs-1 .card-img-cell:hover img {
  transform: scale(1.05);
}
.card-img-cell img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s;
}
.card-img-cell:hover img { transform: scale(1.1); }
.img-more-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: 700;
}
.card-images.imgs-1 {
  border: 1px solid rgba(226, 232, 240, 0.5);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}
.is-night .card-images.imgs-1 { border-color: rgba(255, 255, 255, 0.1); }
.is-night .card-images.imgs-1 .card-img-cell { border-color: rgba(255, 255, 255, 0.1); }
@media (min-width: 768px) {
  .card-images { border-radius: 16px; gap: 8px; max-width: 320px; }
  .card-images.imgs-1 { max-width: 280px; max-height: 400px; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.1); }
  .card-images.imgs-1 .card-img-cell { border-radius: 16px; }
  .card-img-cell { border-radius: 12px; }
}

/* ===== Card Bottom ===== */
.card-bottom {
  display: flex;
  align-items: center;
}
.card-category-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  padding: 4px 12px;
  border-radius: 9999px;
}
.is-night .card-category-tag { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }

/* ===== Empty State ===== */
.empty-state {
  display: flex;
  justify-content: center;
  padding: 60px 20px;
}
.empty-card {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(30px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 40px;
  padding: 48px 40px;
  max-width: 420px;
  width: 100%;
  text-align: center;
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.06);
}
.is-night .empty-card {
  background: rgba(30, 41, 59, 0.3);
  border-color: rgba(255, 255, 255, 0.06);
}
.empty-icon-wrap {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-glow {
  position: absolute;
  inset: 0;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 50%;
  filter: blur(20px);
  animation: pulse-glow 2s ease-in-out infinite;
}
.empty-ghost {
  position: relative;
  color: #3b82f6;
  z-index: 1;
}
.empty-title {
  font-size: 1.3rem;
  font-weight: 900;
  color: #1e293b;
  margin: 0 0 8px;
}
.is-night .empty-title { color: #e2e8f0; }
.empty-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}
@keyframes pulse-glow {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.2); }
}

/* ===== Skeleton ===== */
.skeleton {
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 6px;
}
.is-night .skeleton {
  background: linear-gradient(90deg, #334155 25%, #475569 50%, #334155 75%);
  background-size: 200% 100%;
}
@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skeleton-card { pointer-events: none; }
.sk-avatar { width: 40px; height: 40px; border-radius: 12px; flex-shrink: 0; }
.sk-name { width: 80px; height: 14px; margin-bottom: 6px; }
.sk-time { width: 60px; height: 10px; }
.sk-text { height: 14px; margin-bottom: 10px; }
.sk-text.short { width: 60%; }

/* ===== Back to Top ===== */
.back-to-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  color: #3b82f6;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  z-index: 100;
}
.back-to-top:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.2);
  background: #3b82f6;
  color: white;
}
.is-night .back-to-top {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
  color: #60a5fa;
}
.is-night .back-to-top:hover { background: #3b82f6; color: white; }

/* ===== Animations ===== */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.fade-in-scroll {
  opacity: 0;
  transform: translateY(20px);
  animation: card-enter 0.5s ease forwards;
}
@keyframes card-enter {
  to { opacity: 1; transform: translateY(0); }
}
.fade-in-up {
  opacity: 0;
  transform: translateY(20px);
  animation: card-enter 0.6s ease both;
}
.fade-in-scale {
  opacity: 0;
  transform: scale(0.95);
  animation: scale-enter 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
@keyframes scale-enter {
  to { opacity: 1; transform: scale(1); }
}

/* ===== Search Row ===== */
.search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 800px;
}
.search-wrap { flex: 1; position: relative; }
.ai-toggle-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(59, 130, 246, 0.08);
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 2;
}
.ai-toggle-btn:hover { color: #3b82f6; background: rgba(59, 130, 246, 0.15); }
.ai-toggle-btn.active { color: #3b82f6; background: rgba(59, 130, 246, 0.2); }
.is-night .ai-toggle-btn { color: #64748b; background: rgba(59, 130, 246, 0.1); }
.is-night .ai-toggle-btn:hover, .is-night .ai-toggle-btn.active { color: #60a5fa; background: rgba(59, 130, 246, 0.2); }
.ai-input { padding-right: 100px !important; }
.ai-search-btn {
  position: absolute;
  right: 44px;
  top: 50%;
  transform: translateY(-50%);
  padding: 4px 12px;
  border-radius: 8px;
  border: none;
  background: #3b82f6;
  color: white;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 2;
}
.ai-search-btn:hover { background: #2563eb; }
.ai-search-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* Sun loader */
.sun-loader {
  --size: 0.15;
  width: calc(100px * var(--size));
  height: calc(100px * var(--size));
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sun-loader-inner {
  width: 100%;
  height: 100%;
  background: #3b82f6;
  border-radius: 50%;
  position: relative;
  animation: sun-pulse 1.5s ease-in-out infinite;
}
.sun-loader-inner::before {
  content: '';
  position: absolute;
  inset: -30%;
  background: radial-gradient(circle, rgba(59,130,246,0.3) 0%, transparent 70%);
  border-radius: 50%;
  animation: sun-glow 1.5s ease-in-out infinite;
}
@keyframes sun-pulse {
  0%, 100% { transform: scale(0.8); opacity: 0.8; }
  50% { transform: scale(1.1); opacity: 1; }
}
@keyframes sun-glow {
  0%, 100% { transform: scale(0.8); opacity: 0.3; }
  50% { transform: scale(1.3); opacity: 0.6; }
}

/* Filter toggle */
.filter-toggle-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  position: relative;
}
.filter-toggle-btn:hover { border-color: #3b82f6; color: #3b82f6; }
.filter-toggle-btn.active { border-color: #3b82f6; color: #3b82f6; background: rgba(59, 130, 246, 0.06); }
.is-night .filter-toggle-btn { background: rgba(30, 41, 59, 0.4); border-color: rgba(255, 255, 255, 0.05); color: #94a3b8; }
.is-night .filter-toggle-btn:hover, .is-night .filter-toggle-btn.active { border-color: #60a5fa; color: #60a5fa; }
.filter-badge {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
}

/* Filter panel */
.filter-panel {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.is-night .filter-panel { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.05); }
.filter-slide-enter-active,
.filter-slide-leave-active {
  transition: all 0.25s ease;
}
.filter-slide-enter-from,
.filter-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
.filter-row {
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
.clear-filters-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.clear-filters-btn:hover { background: rgba(239, 68, 68, 0.2); }
.is-night .clear-filters-btn { background: rgba(239, 68, 68, 0.15); color: #f87171; }
.is-night .clear-filters-btn:hover { background: rgba(239, 68, 68, 0.25); }
.is-night .ai-search-btn { background: #60a5fa; }
.is-night .ai-search-btn:hover { background: #3b82f6; }
.ai-search-tip {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  text-align: center;
}

/* Result count */
.result-count {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
  margin-bottom: 16px;
}
.is-night .result-count { color: #94a3b8; }

/* Pagination */
.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  padding: 16px 0;
}
.page-btn {
  padding: 8px 20px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  color: #3b82f6;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.page-btn:hover:not(:disabled) { background: #3b82f6; color: white; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.is-night .page-btn { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.1); color: #60a5fa; }
.page-info {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}
.is-night .page-info { color: #94a3b8; }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .moments-page { padding: 0 14px 60px; }
  .moments-banner { margin: 0 -14px; height: 170px; border-radius: 0; }
  .banner-title { font-size: 1.35rem; }
  .banner-info { left: 20px; bottom: 20px; }
  .search-row { flex-direction: column; gap: 0.5rem; }
  .sort-toggle { align-self: flex-start; }
  .moment-card { border-radius: 16px; padding: 16px; }
  .card-content { font-size: 0.88rem; }
  .card-images { max-width: 100%; }
  .back-to-top { bottom: 20px; right: 20px; width: 40px; height: 40px; }
  .filter-toggle-btn { align-self: flex-start; }
  .filter-row { flex-direction: column; gap: 0.5rem; }
  .filter-label { white-space: normal; }
  .card-author-name { max-width: 80px; }
  .ai-input { padding-right: 80px !important; font-size: 16px; }
  .ai-search-btn { right: 8px; width: 36px; height: 36px; }
  .ai-toggle-btn { width: 36px; height: 36px; }
  .page-info { font-size: 0.78rem; }
}
@media (max-width: 480px) {
  .moments-page { padding: 0 10px 50px; }
  .moments-banner { margin: 0 -10px; height: 140px; }
  .banner-title { font-size: 1.15rem; }
  .moment-card { padding: 12px; border-radius: 12px; }
  .card-header { gap: 8px; }
  .card-avatar { width: 32px; height: 32px; }
  .card-author { font-size: 0.82rem; }
  .card-time { font-size: 0.75rem; }
  .card-content { font-size: 0.88rem; }
  .card-actions { gap: 0.75rem; }
  .card-action-btn { font-size: 0.82rem; gap: 3px; }
}
</style>
