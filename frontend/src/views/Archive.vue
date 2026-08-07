<template>
  <div class="archive-page" :class="{ 'is-night': isNight }">
    <h1 class="page-title fade-in-up">{{ $t('archive.title') }}</h1>
    <p v-if="!loading" class="total fade-in-up fade-in-up-delay-1">
      {{ $t('archive.totalArticles', { count: articles.length }) }}
    </p>
    <div v-else class="skeleton" style="width: 150px; height: 16px; margin-bottom: 16px;" />

    <!-- Search + Filter -->
    <div class="search-sort-block fade-in-up">
      <div class="search-row">
        <div class="search-wrap group">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-if="!aiMode" v-model="searchQuery" type="text" class="search-input" :placeholder="$t('archive.searchPlaceholder')" @input="currentPage = 1" />
          <input v-else v-model="aiQuery" type="text" class="search-input ai-input" :placeholder="$t('archive.aiSearchPlaceholder')" @keyup.enter="doAiSearch" />
          <button class="ai-toggle-btn" :class="{ active: aiMode }" @click="toggleAiMode" :title="$t('archive.aiSearch')">
            <svg v-if="!aiSearching" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M12 2l2.09 6.26L21 9.27l-5 4.87L17.18 21 12 17.27 6.82 21 8 14.14l-5-4.87 6.91-1.01z"/></svg>
            <div v-else class="sun-loader"><div class="sun-loader-inner"></div></div>
          </button>
          <button v-if="aiMode" class="ai-search-btn" @click="doAiSearch" :disabled="aiSearching">{{ $t('archive.aiSearch') }}</button>
        </div>
        <button class="filter-toggle-btn" :class="{ active: showFilters || hasFilters }" @click="showFilters = !showFilters">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
          {{ $t('archive.filter') }}
          <span v-if="hasFilters" class="filter-badge"></span>
        </button>
      </div>

      <!-- Collapsible Filter Panel -->
      <Transition name="filter-slide">
        <div v-if="showFilters" class="filter-panel">
          <div class="filter-row">
            <label class="filter-label">{{ $t('archive.dateFrom') }}</label>
            <DatePicker v-model="dateFrom" :placeholder="$t('archive.dateFrom')" :max="dateTo || undefined" @change="currentPage = 1" />
          </div>
          <div class="filter-row">
            <label class="filter-label">{{ $t('archive.dateTo') }}</label>
            <DatePicker v-model="dateTo" :placeholder="$t('archive.dateTo')" :min="dateFrom || undefined" @change="currentPage = 1" />
          </div>
          <button v-if="hasFilters" class="clear-filters-btn" @click="clearFilters">{{ $t('archive.clear') }}</button>
        </div>
      </Transition>

      <p v-if="aiMode" class="ai-search-tip">{{ $t('archive.aiSearchTip') }}</p>
    </div>

    <!-- View Mode Toggle -->
    <div class="view-mode-toggle">
      <button class="mode-btn" :class="{ active: viewMode === 'list' }" @click="setViewMode('list')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
        <span>{{ $t('archive.listMode') }}</span>
      </button>
      <button class="mode-btn" :class="{ active: viewMode === 'timeline' }" @click="setViewMode('timeline')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7h5l2 3h6l2-3h5"/><circle cx="12" cy="17" r="4"/><path d="M12 15v4"/></svg>
        <span>{{ $t('archive.timelineMode') }}</span>
      </button>
      <button class="mode-btn" :class="{ active: viewMode === 'grid' }" @click="setViewMode('grid')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
        <span>{{ $t('archive.cardMode') }}</span>
      </button>
    </div>

    <!-- Result count -->
    <div v-if="!loading && !loadError && articles.length > 0" class="result-count">
      {{ $t('archive.totalArticles', { count: filteredArticles.length }) }}
    </div>

    <!-- Empty state -->
    <div v-if="!loading && filteredArticles.length === 0 && !loadError" class="empty-state fade-in-up">
      <div class="empty-icon">&#128197;</div>
      <p>{{ searchQuery || aiQuery ? $t('archive.noResults') : $t('archive.noEntries') }}</p>
      <p v-if="searchQuery || aiQuery" class="empty-desc">{{ $t('archive.noResultsDesc') }}</p>
    </div>

    <ResourceError v-if="loadError" :message="$t('archive.loadError')" @retry="loadArchive()" />

    <!-- Skeleton loading -->
    <div v-if="loading && !loadError">
      <div v-for="n in 3" :key="n" class="archive-card" style="cursor: default;">
        <div class="skeleton" style="width: 100px; height: 20px; margin-bottom: 16px;" />
        <div v-for="m in 4" :key="m" style="display: flex; gap: 12px; padding: 8px 0; align-items: center;">
          <div class="skeleton" style="width: 50px; height: 13px; flex-shrink: 0;" />
          <div class="skeleton" style="width: 60%; height: 15px;" />
        </div>
      </div>
    </div>

    <!-- Default List View -->
    <div v-if="!loading && paginatedArticles.length > 0 && viewMode === 'list'" class="list-container">
      <div v-for="[ym, group] in paginatedGrouped" :key="ym" class="list-group">
        <div class="list-year-label">{{ ym }}</div>
        <div v-for="(article, idx) in group" :key="article.id"
          class="list-item"
          :ref="el => { if (el) observeTargets.push({ el, delay: idx * 60 }) }">
          <span class="list-date">{{ formatDate(article.createdAt) }}</span>
          <router-link :to="'/articles/' + article.slug" class="list-title">
            {{ article.title }}
          </router-link>
          <span v-if="article.categoryName" class="list-category">{{ article.categoryName }}</span>
        </div>
      </div>
    </div>

    <!-- Timeline View (中枢链路) -->
    <div v-if="!loading && paginatedArticles.length > 0 && viewMode === 'timeline'" class="timeline-container">
      <div class="timeline-line"></div>
      <div v-for="(article, idx) in paginatedArticles" :key="article.id" class="timeline-node" :class="{ reverse: idx % 2 === 0 }">
        <div class="timeline-spacer"></div>
        <div class="timeline-dot"></div>
        <router-link :to="'/articles/' + article.slug" class="timeline-card">
          <div class="timeline-card-date">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            {{ formatDate(article.createdAt) }}
          </div>
          <h3 class="timeline-card-title">{{ article.title }}</h3>
          <span v-if="article.categoryName" class="timeline-card-tag">#{{ article.categoryName }}</span>
        </router-link>
      </div>
    </div>

    <!-- Grid View (矩阵网格) -->
    <div v-if="!loading && paginatedArticles.length > 0 && viewMode === 'grid'" class="grid-container">
      <div class="grid-scroll">
        <div class="grid-layout">
          <router-link v-for="(article, idx) in paginatedArticles" :key="article.id"
            :to="'/articles/' + article.slug"
            class="grid-card"
            :style="{ animationDelay: idx * 50 + 'ms' }">
            <div class="grid-card-header">
              <span class="grid-card-date">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                {{ formatDate(article.createdAt) }}
              </span>
            </div>
            <div class="grid-card-body">
              <h3 class="grid-card-title">{{ article.title }}</h3>
              <span v-if="article.categoryName" class="grid-card-tag">#{{ article.categoryName }}</span>
            </div>
          </router-link>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="!loading && totalPages > 1" class="pagination-wrap">
      <button class="page-btn" :disabled="currentPage <= 1" @click="currentPage--">{{ $t('archive.prev') }}</button>
      <span class="page-info">{{ $t('archive.pageInfo', { current: currentPage, total: totalPages }) }}</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage++">{{ $t('archive.next') }}</button>
    </div>

    <!-- Back to top -->
    <Transition name="fade">
      <button v-if="showBackToTop" class="back-to-top" @click="scrollToTop" :title="$t('archive.backToTop')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20"><polyline points="18 15 12 9 6 15"/></svg>
      </button>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import DatePicker from '../components/DatePicker.vue'
import ResourceError from '../components/ResourceError.vue'

const { t } = useI18n()

const articles = ref([])
const loading = ref(true)
const loadError = ref(false)
const isNight = ref(document.body.classList.contains('body-night'))
const showBackToTop = ref(false)

// Search + filter + pagination
const searchQuery = ref('')
const aiMode = ref(false)
const aiQuery = ref('')
const aiSearching = ref(false)
const aiMatchedIds = ref(new Set())
const dateFrom = ref('')
const dateTo = ref('')
const currentPage = ref(1)
const pageSize = 30
const showFilters = ref(false)
const viewMode = ref('list')

function setViewMode(mode) {
  viewMode.value = mode
  currentPage.value = 1
}

const hasFilters = computed(() =>
  searchQuery.value || aiMatchedIds.value.size > 0 || dateFrom.value || dateTo.value
)

const filteredArticles = computed(() => {
  let list = articles.value
  // AI match filter
  if (aiMatchedIds.value.size > 0) {
    list = list.filter(a => aiMatchedIds.value.has(String(a.id)))
  }
  // Search filter
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(a =>
      (a.title && a.title.toLowerCase().includes(q)) ||
      (a.categoryName && a.categoryName.toLowerCase().includes(q))
    )
  }
  // Date range filter
  if (dateFrom.value) {
    list = list.filter(a => (a.createdAt || '').substring(0, 10) >= dateFrom.value)
  }
  if (dateTo.value) {
    list = list.filter(a => (a.createdAt || '').substring(0, 10) <= dateTo.value)
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredArticles.value.length / pageSize)))
const paginatedArticles = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredArticles.value.slice(start, start + pageSize)
})

const paginatedGrouped = computed(() => {
  const map = new Map()
  for (const a of paginatedArticles.value) {
    const ym = (a.createdAt || '').substring(0, 7)
    if (!map.has(ym)) map.set(ym, [])
    map.get(ym).push(a)
  }
  return map
})

function formatDate(d) {
  if (!d) return ''
  return d.substring(0, 10)
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
    const summary = articles.value.map((a, i) =>
      `[${i}] "${a.title}" (${(a.createdAt || '').substring(0, 10)})`
    ).join('\n')
    const prompt = `你是文章搜索助手。以下是博客文章列表，格式为 [序号] "标题" (日期)。
用户想查找: "${q}"
请找出匹配的文章序号，只返回JSON数组如[0,3,5]，无匹配返回[]。
文章列表:
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
        if (i >= 0 && i < articles.value.length) {
          ids.add(String(articles.value[i].id))
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
  currentPage.value = 1
}

// === Scroll animation ===
import { onBeforeUpdate, watch } from 'vue'

const observeTargets = []
onBeforeUpdate(() => { observeTargets.length = 0 })
watch([filteredArticles, currentPage], () => {
  const max = totalPages.value
  if (currentPage.value > max) currentPage.value = max
})
let observer = null

function setupObserver() {
  observer = new IntersectionObserver((items) => {
    items.forEach(item => {
      if (item.isIntersecting) {
        const delay = item.target.__animDelay || 0
        setTimeout(() => item.target.classList.add('visible'), delay)
        observer.unobserve(item.target)
      }
    })
  }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' })
}

function observeAll() {
  nextTick(() => {
    if (!observer) return
    observeTargets.forEach(({ el, delay }) => {
      el.__animDelay = delay
      el.classList.add('fade-in-scroll')
      observer.observe(el)
    })
  })
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

// === Data loading ===
async function loadArchive() {
  loadError.value = false
  loading.value = true
  try {
    const data = await request.get('/api/articles?page=0&size=200')
    const list = data?.content || []
    list.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
    articles.value = list
  } catch (e) {
    console.error('Failed to load archive:', e)
    loadError.value = true
  } finally {
    loading.value = false
    observeTargets.length = 0
    observeAll()
  }
}

onMounted(() => {
  setupObserver()
  loadArchive()
  window.addEventListener('scroll', onScroll, { passive: true })
  nightObserver = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  nightObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  if (observer) { observer.disconnect(); observer = null }
  if (nightObserver) { nightObserver.disconnect(); nightObserver = null }
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.archive-page {
  min-height: 100vh;
  padding: 80px 16px 40px;
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 900;
  margin-bottom: 0.5rem;
  color: #1e293b;
  letter-spacing: 0.05em;
}

.total {
  color: #64748b;
  margin-bottom: 1.5rem;
  font-size: 0.9rem;
  font-weight: 500;
}

.is-night .page-title { color: #e2e8f0; }
.is-night .total { color: #64748b; }

/* ========== Empty State ========== */
.empty-state { text-align: center; padding: 80px 20px; }
.empty-icon { font-size: 48px; opacity: 0.3; margin-bottom: 12px; }
.empty-state p { color: #94a3b8; font-size: 14px; }

/* ========== List ========== */
.list-container { padding: 0 0 20px; }

.list-group { margin-bottom: 28px; }

.list-year-label {
  font-size: 13px;
  font-weight: 800;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  padding: 4px 16px;
  border-radius: 999px;
  display: inline-block;
  margin-bottom: 12px;
  letter-spacing: 0.05em;
}

.is-night .list-year-label {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  transition: background 0.2s;
}

.list-item:hover {
  background: rgba(59, 130, 246, 0.03);
  border-radius: 8px;
  padding-left: 8px;
}

.is-night .list-item { border-bottom-color: rgba(255, 255, 255, 0.04); }
.is-night .list-item:hover { background: rgba(59, 130, 246, 0.06); }

.list-date {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  flex-shrink: 0;
  width: 80px;
  font-variant-numeric: tabular-nums;
}

.list-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-decoration: none;
  transition: color 0.2s;
}

.list-title:hover { color: #3b82f6; }

.is-night .list-title { color: #e2e8f0; }
.is-night .list-title:hover { color: #60a5fa; }

.list-category {
  font-size: 11px;
  font-weight: 600;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  padding: 2px 10px;
  border-radius: 999px;
  flex-shrink: 0;
  white-space: nowrap;
}

.is-night .list-category {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

/* ========== Back to Top ========== */
.back-to-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
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
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(255, 255, 255, 0.08);
  color: #60a5fa;
}

.is-night .back-to-top:hover { background: #3b82f6; color: white; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ========== Scroll Animation ========== */
.fade-in-scroll {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.fade-in-scroll.visible {
  opacity: 1;
  transform: translateY(0);
}

/* ========== Entrance Animation ========== */
@keyframes fade-in-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-in-up { animation: fade-in-up 0.6s ease both; }
.fade-in-up-delay-1 { animation-delay: 0.1s; }

/* ========== Skeleton ========== */
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

.archive-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 16px;
}

.is-night .archive-card {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
}

/* ===== Search + Filter ===== */
.search-sort-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 600px;
}
.search-wrap {
  flex: 1;
  position: relative;
}
.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  pointer-events: none;
  transition: color 0.2s;
  z-index: 2;
}
.search-wrap:focus-within .search-icon { color: #3b82f6; }
.search-input {
  width: 100%;
  padding: 12px 20px 12px 44px;
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
  z-index: 1;
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

/* AI toggle */
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
.empty-desc {
  color: #94a3b8;
  font-size: 13px;
  margin-top: 4px;
}

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

/* ========== View Mode Toggle ========== */
.view-mode-toggle {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  padding: 4px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.mode-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.mode-btn:hover { color: #3b82f6; }
.mode-btn.active {
  background: #fff;
  color: #3b82f6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.is-night .view-mode-toggle {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}
.is-night .mode-btn { color: #94a3b8; }
.is-night .mode-btn:hover { color: #60a5fa; }
.is-night .mode-btn.active {
  background: #334155;
  color: #60a5fa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

/* ========== Timeline View (中枢链路) ========== */
.timeline-container {
  position: relative;
  padding: 20px 0 40px;
  min-height: 400px;
}
.timeline-line {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(59, 130, 246, 0.2);
  transform: translateX(-50%);
  border-radius: 1px;
}
.is-night .timeline-line { background: rgba(96, 165, 250, 0.2); }

.timeline-node {
  display: flex;
  align-items: flex-start;
  position: relative;
  margin-bottom: 32px;
}
.timeline-node.reverse { flex-direction: row-reverse; }
.timeline-node.reverse .timeline-card { text-align: right; }
.timeline-node.reverse .timeline-card-date { justify-content: flex-end; }
.timeline-node.reverse .timeline-card-tag { justify-content: flex-end; }

.timeline-spacer {
  flex: 1;
  min-width: 0;
}
.timeline-dot {
  position: relative;
  z-index: 2;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  border: 4px solid #3b82f6;
  box-shadow: 0 0 0 6px rgba(59, 130, 246, 0.15), 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  margin: 16px 16px 0;
}
.is-night .timeline-dot {
  background: #1e293b;
  border-color: #60a5fa;
  box-shadow: 0 0 0 6px rgba(96, 165, 250, 0.2), 0 2px 8px rgba(0, 0, 0, 0.3);
}
.timeline-card {
  flex: 1;
  min-width: 0;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
  text-decoration: none;
  display: block;
}
.timeline-card:hover {
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  transform: scale(1.02);
}
.is-night .timeline-card {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.08);
}
.is-night .timeline-card:hover {
  background: rgba(30, 41, 59, 0.7);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}
.timeline-card-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #3b82f6;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}
.is-night .timeline-card-date { color: #60a5fa; }
.timeline-card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.is-night .timeline-card-title { color: #e2e8f0; }
.timeline-card-tag {
  display: inline-flex;
  font-size: 10px;
  font-weight: 700;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  padding: 2px 10px;
  border-radius: 6px;
  border: 1px solid rgba(59, 130, 246, 0.1);
}
.is-night .timeline-card-tag { color: #60a5fa; background: rgba(96, 165, 250, 0.1); border-color: rgba(96, 165, 250, 0.15); }

/* ========== Grid View (矩阵网格) ========== */
.grid-container {
  position: relative;
}
.grid-scroll {
  max-height: 75vh;
  overflow-y: auto;
  padding-right: 4px;
  padding-bottom: 40px;
  mask-image: linear-gradient(to bottom, transparent 0%, black 3%, black 97%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 3%, black 97%, transparent 100%);
}
.grid-scroll::-webkit-scrollbar { width: 6px; }
.grid-scroll::-webkit-scrollbar-track { background: transparent; }
.grid-scroll::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #60a5fa, #93c5fd);
  border-radius: 3px;
}
.is-night .grid-scroll::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #3b82f6, #a855f7);
}
.grid-layout {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding-top: 8px;
}
@media (min-width: 1024px) {
  .grid-layout { grid-template-columns: repeat(3, 1fr); gap: 20px; }
}
.grid-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  text-decoration: none;
  transition: all 0.3s;
  animation: grid-enter 0.4s ease both;
}
@keyframes grid-enter {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.grid-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(59, 130, 246, 0.12);
}
.is-night .grid-card {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
}
.is-night .grid-card:hover { box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3); }
.grid-card-header {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(59, 130, 246, 0.1));
  padding: 24px 16px 16px;
  position: relative;
}
.is-night .grid-card-header {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(59, 130, 246, 0.15));
}
.grid-card-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #3b82f6;
  font-family: monospace;
}
.is-night .grid-card-date { color: #60a5fa; }
.grid-card-body {
  padding: 12px 16px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.grid-card-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s;
}
.grid-card:hover .grid-card-title { color: #3b82f6; }
.is-night .grid-card-title { color: #e2e8f0; }
.is-night .grid-card:hover .grid-card-title { color: #60a5fa; }
.grid-card-tag {
  display: inline-flex;
  align-self: flex-start;
  font-size: 9px;
  font-weight: 700;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
}
.is-night .grid-card-tag { color: #60a5fa; background: rgba(96, 165, 250, 0.1); }

@media (min-width: 768px) {
  .grid-card-header { padding: 32px 20px 20px; }
  .grid-card-body { padding: 16px 20px 20px; }
  .grid-card-title { font-size: 15px; }
  .grid-card { border-radius: 20px; }
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .archive-page { padding: 80px 16px 40px; }
  .list-date { width: 60px; font-size: 11px; }
  .list-item { gap: 12px; }
  .list-category { display: none; }
  .back-to-top { bottom: 20px; right: 20px; width: 40px; height: 40px; }
  .search-row { flex-direction: column; }
  .filter-toggle-btn { align-self: flex-start; }
  .filter-row { flex-direction: column; }
  .filter-label { white-space: normal; }
  .list-title { white-space: normal; }
  .mode-btn { padding: 6px 12px; font-size: 11px; }
  .mode-btn span { display: none; }
  .timeline-line { left: 12px; }
  .timeline-dot { margin: 12px 12px 0 0; width: 16px; height: 16px; border-width: 3px; box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15); }
  .timeline-node, .timeline-node.reverse { flex-direction: column; }
  .timeline-spacer { display: none; }
  .timeline-card { text-align: left !important; }
  .timeline-card-date { justify-content: flex-start !important; }
  .timeline-card-tag { justify-content: flex-start !important; }
  .timeline-node.reverse .timeline-card { text-align: left; }
  .timeline-node.reverse .timeline-card-date { justify-content: flex-start; }
  .timeline-node.reverse .timeline-card-tag { justify-content: flex-start; }
}

@media (min-width: 769px) {
  .archive-page { padding: 112px 24px 40px; }
}
@media (max-width: 480px) {
  .archive-page { padding: 70px 10px 30px; }
  .page-title { font-size: 1.2rem; }
  .list-date { width: 50px; font-size: 11px; }
  .list-title { font-size: 0.82rem; }
  .timeline-card { padding: 10px; }
  .timeline-card-title { font-size: 0.88rem; }
  .timeline-card-summary { font-size: 0.75rem; -webkit-line-clamp: 2; }
  .grid-card { border-radius: 12px; }
  .grid-card-header { padding: 10px; }
  .grid-card-title { font-size: 0.85rem; }
  .grid-card-body { padding: 8px 10px; }
  .grid-card-summary { font-size: 0.72rem; }
}
</style>
