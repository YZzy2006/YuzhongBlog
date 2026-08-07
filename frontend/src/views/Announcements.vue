<template>
  <div class="announcements-page">
    <h1 class="announcements-title fade-in-up">{{ $t('announcements.title') }}</h1>
    <p class="announcements-subtitle fade-in-up">{{ $t('announcements.subtitle') }}</p>

    <!-- Search bar (same pattern as Articles) -->
    <div class="search-bar fade-in-up">
      <div class="search-inputs">
        <label class="cir-search" :class="{ 'ai-active': aiMode }">
          <svg class="cir-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="m21 21-4.34-4.34"></path>
          </svg>
          <input v-model="keyword" class="cir-search__field" type="search"
            :placeholder="aiMode ? $t('announcements.aiSearchPlaceholder') : $t('announcements.searchPlaceholder')"
            @keyup.enter="aiMode ? doAiSearch() : doSearch()" @input="onKeywordInput" />
          <button class="ai-toggle" :class="{ active: aiMode }" @click="aiMode = !aiMode" :title="$t('announcements.aiToggle')">
            <div class="announcements-ai-loader">
              <svg width="100" height="100" viewBox="0 0 100 100">
                <defs>
                  <mask id="announcements-ai-clipping">
                    <polygon points="0,0 100,0 100,100 0,100" fill="black"></polygon>
                    <polygon points="25,25 75,25 50,75" fill="white"></polygon>
                    <polygon points="50,25 75,75 25,75" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                  </mask>
                </defs>
              </svg>
              <div class="announcements-ai-loader-box"></div>
            </div>
          </button>
          <kbd class="cir-search__kbd">Enter</kbd>
        </label>
        <button class="filter-toggle" :class="{ active: showFilters || activeFilterCount > 0 }" @click="showFilters = !showFilters" :title="$t('announcements.filter')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
          </svg>
          <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
        </button>
        <button class="search-btn" @click="aiMode ? doAiSearch() : doSearch()" :disabled="aiLoading">
          {{ aiLoading ? $t('announcements.parsing') : (aiMode ? $t('announcements.aiSearch') : $t('announcements.search')) }}
        </button>
        <button v-if="hasFilter" class="clear-btn" @click="clearSearch">{{ $t('announcements.clear') }}</button>
      </div>

      <!-- Filter Panel -->
      <Transition name="filter-slide">
        <div v-if="showFilters" class="filter-panel">
          <div class="filter-row">
            <label class="filter-label">{{ $t('announcements.type') }}</label>
            <DropdownMenu v-model="filterType" :items="typeOptions" @change="onFilterChange" />
          </div>
          <div class="filter-row">
            <label class="filter-label">{{ $t('announcements.sort') }}</label>
            <DropdownMenu v-model="sortBy" :items="sortOptions" @change="onFilterChange" />
          </div>
          <div class="filter-row">
            <label class="filter-label">{{ $t('announcements.tag') }}</label>
            <DropdownMenu v-model="filterTag" :items="tagOptions" @change="onFilterChange" />
          </div>
        </div>
      </Transition>

      <div v-if="hasFilter" class="result-count">
        {{ t('announcements.found', { count: totalElements }) }}
      </div>
      <div v-if="aiMode" class="ai-hint">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
        {{ $t('announcements.aiHint') }}
      </div>
    </div>

    <!-- AI Search Result -->
    <Transition name="filter-slide">
      <div v-if="aiMode && aiResult" class="ai-search-result">
        <div class="ai-result-header">
          <span class="ai-result-label">{{ $t('announcements.aiAnswer') }}</span>
          <button v-if="aiLoading" class="ai-cancel-btn" @click="cancelAi">{{ $t('announcements.cancel') }}</button>
        </div>
        <div class="ai-result-content">{{ aiResult }}</div>
      </div>
    </Transition>
    <div v-if="aiMode && aiLoading && !aiResult" class="ai-typing">
      <span></span><span></span><span></span>
    </div>
    <div v-if="aiMode && aiError" class="ai-error-msg">{{ aiError }}</div>

    <!-- Skeleton loading -->
    <div v-if="loading" class="announcements-list">
      <div v-for="n in 4" :key="n" class="announcement-card" style="cursor: default;">
        <div style="display: flex; gap: 10px; margin-bottom: 10px;">
          <div class="skeleton" style="width: 48px; height: 18px; border-radius: 10px;" />
          <div class="skeleton" style="width: 70px; height: 14px;" />
        </div>
        <div class="skeleton" style="width: 65%; height: 18px; margin-bottom: 10px;" />
        <div class="skeleton" style="width: 100%; height: 14px; margin-bottom: 6px;" />
        <div class="skeleton" style="width: 80%; height: 14px;" />
      </div>
    </div>

    <!-- Announcement List -->
    <div v-if="!loading" class="announcements-list">
      <div v-for="n in filteredNotices" :key="n.id" class="announcement-card" :class="`announcement-card--${colorKey(n)}`" @click="showDetail(n)">
        <div class="announcement-card-header">
          <span class="announcement-tag" :class="`announcement-tag--${colorKey(n)}`">{{ noticeTag(n) }}</span>
          <span class="announcement-date">{{ relativeDate(n.createdAt) }}</span>
        </div>
        <h3 class="announcement-card-title">{{ noticeTitle(n) }}</h3>
        <p class="announcement-preview">{{ noticeContent(n) ? noticeContent(n).slice(0, 120) + (noticeContent(n).length > 120 ? '...' : '') : '' }}</p>
      </div>
      <div v-if="!filteredNotices.length" class="announcements-empty">
        <p>{{ keyword || hasFilter ? $t('announcements.noResults') : $t('announcements.noAnnouncements') }}</p>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="filteredNotices.length > 0">
      <div class="page-size-selector">
        <span class="page-size-label">{{ $t('announcements.perPage') }}</span>
        <DropdownMenu :modelValue="pageSize" :items="pageSizeItems" @change="v => setPageSize(v)" />
      </div>
      <div class="page-nav" v-if="totalPages > 1">
        <button class="page-nav-btn" :disabled="page === 0" @click="prevPage">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <button v-for="p in pageNumbers" :key="p" class="page-num-btn" :class="{ active: p === page + 1 }" @click="goToPage(p)">{{ p }}</button>
        <button class="page-nav-btn" :disabled="page >= totalPages - 1" @click="nextPage">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>
      <span class="page-total">{{ t('announcements.total', { count: totalElements }) }}</span>
    </div>

    <!-- Detail Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="detailNotice" class="notice-detail-mask" @mousedown.self="detailNotice = null">
          <div class="notice-detail-modal">
            <div class="notice-detail-header">
              <span class="announcement-tag" :class="`announcement-tag--${colorKey(detailNotice)}`">{{ noticeTag(detailNotice) }}</span>
              <button class="notice-detail-close" @click="detailNotice = null">&times;</button>
            </div>
            <h2 class="notice-detail-title">{{ noticeTitle(detailNotice) }}</h2>
            <p class="notice-detail-date">{{ relativeDate(detailNotice.createdAt) }}</p>
            <div class="notice-detail-content">{{ noticeContent(detailNotice) }}</div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import { aiChatStream } from '../utils/ai'
import DropdownMenu from '../components/DropdownMenu.vue'

const { t, locale } = useI18n()
const isEn = computed(() => locale.value === 'en-US')
function noticeTitle(n) { return n && isEn.value && n.titleEn ? n.titleEn : (n?.title || '') }
function noticeContent(n) { return n && isEn.value && n.contentEn ? n.contentEn : (n?.content || '') }
function noticeTag(n) { return n && isEn.value && n.tagEn ? n.tagEn : (n?.tag || '') }

const allNotices = ref([])
const filteredNotices = ref([])
const keyword = ref('')
const detailNotice = ref(null)
const loading = ref(true)

// Pagination
const page = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSizeItems = computed(() => [
  { value: 5, label: `5 ${t('announcements.items')}` },
  { value: 10, label: `10 ${t('announcements.items')}` },
  { value: 20, label: `20 ${t('announcements.items')}` },
  { value: 50, label: `50 ${t('announcements.items')}` }
])

// Filter state
const showFilters = ref(false)
const filterType = ref('')
const filterTag = ref('')
const sortBy = ref('default')

// AI search
const aiMode = ref(false)
const aiResult = ref('')
const aiLoading = ref(false)
const aiError = ref('')
let abortFn = null
let aiDebounceTimer = null

const pageNumbers = computed(() => {
  const total = totalPages.value
  const current = page.value + 1
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = [1]
  if (current > 3) pages.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) pages.push(i)
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

onMounted(async () => {
  await Promise.all([loadAllForFilters(), loadPage()])
  loading.value = false
})

onBeforeUnmount(() => {
  clearTimeout(aiDebounceTimer)
  if (abortFn) abortFn()
})

async function loadAllForFilters() {
  try {
    allNotices.value = await request.get('/api/announcements')
  } catch (e) {
    console.error('Failed to load announcements', e)
  }
}

async function loadPage() {
  try {
    const params = new URLSearchParams({ page: page.value, size: pageSize.value, sort: sortBy.value })
    if (filterType.value) params.set('type', filterType.value)
    if (filterTag.value) params.set('tag', filterTag.value)
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const data = await request.get(`/api/announcements/page?${params}`)
    filteredNotices.value = data.content || []
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch (e) {
    console.error('Failed to load announcements page', e)
  }
}

// Filter options (derived from full list)
const typeOptions = computed(() => {
  const types = [...new Set(allNotices.value.map(n => n.type).filter(Boolean))]
  return [{ value: '', label: t('announcements.allTypes') }, ...types.map(tp => ({ value: tp, label: tp }))]
})
const sortOptions = computed(() => [
  { value: 'default', label: t('announcements.defaultSort') },
  { value: 'newest', label: t('announcements.newest') },
  { value: 'oldest', label: t('announcements.oldest') }
])
const tagOptions = computed(() => {
  const tags = [...new Set(allNotices.value.map(n => n.tag).filter(Boolean))]
  return [{ value: '', label: t('announcements.allTags') }, ...tags.map(tg => ({ value: tg, label: tg }))]
})

const activeFilterCount = computed(() => {
  let c = 0
  if (filterType.value) c++
  if (filterTag.value) c++
  return c
})

const hasFilter = computed(() => keyword.value.trim() || activeFilterCount.value > 0)

function colorKey(n) {
  if (n.level && n.level !== 'info') return n.level
  const t = (n.type || '').toLowerCase()
  if (t === 'feature') return 'success'
  if (t === 'update') return 'warning'
  return 'info'
}

function onFilterChange() {
  page.value = 0
  loadPage()
}

function doSearch() {
  cancelAi()
  aiResult.value = ''
  aiError.value = ''
  page.value = 0
  loadPage()
}

function onKeywordInput() {
  if (!aiMode.value) return
  clearTimeout(aiDebounceTimer)
  if (!keyword.value.trim()) {
    aiResult.value = ''
    aiError.value = ''
    return
  }
  aiDebounceTimer = setTimeout(() => doAiSearch(), 400)
}

function doAiSearch() {
  const q = keyword.value.trim()
  if (!q) return
  cancelAi()
  aiResult.value = ''
  aiError.value = ''
  aiLoading.value = true

  const context = allNotices.value.slice(0, 50).map(n =>
    `[${noticeTag(n)}] ${noticeTitle(n)}\n${(noticeContent(n) || '').slice(0, 200)}`
  ).join('\n---\n')

  const msg = `${t('announcements.aiSystemPrompt')}

${t('announcements.aiUserQuestion')}${q}

${t('announcements.aiNoticeList')}
${context}`

  abortFn = aiChatStream(msg, {
    onChunk(content) { aiResult.value += content },
    onDone() { aiLoading.value = false; abortFn = null },
    onError(err) { aiError.value = err; aiLoading.value = false; abortFn = null }
  })
}

function cancelAi() {
  if (abortFn) { abortFn(); abortFn = null }
  aiLoading.value = false
}

function clearSearch() {
  keyword.value = ''
  filterType.value = ''
  filterTag.value = ''
  sortBy.value = 'default'
  aiResult.value = ''
  aiError.value = ''
  cancelAi()
  page.value = 0
  loadPage()
}

function setPageSize(size) {
  pageSize.value = size
  page.value = 0
  loadPage()
}
function goToPage(p) {
  if (p === '...' || p === page.value + 1) return
  page.value = p - 1
  loadPage()
}
function prevPage() { if (page.value > 0) { page.value--; loadPage() } }
function nextPage() { if (page.value < totalPages.value - 1) { page.value++; loadPage() } }

function relativeDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const diff = Date.now() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return t('announcements.justNow')
  if (mins < 60) return t('announcements.minutesAgo', { n: mins })
  const hours = Math.floor(mins / 60)
  if (hours < 24) return t('announcements.hoursAgo', { n: hours })
  const days = Math.floor(hours / 24)
  if (days < 30) return t('announcements.daysAgo', { n: days })
  return d.toLocaleDateString(locale.value === 'en-US' ? 'en-US' : 'zh-CN')
}

function showDetail(n) {
  detailNotice.value = n
}
</script>

<style scoped>
.announcements-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem 1rem;
}
.announcements-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 0.35rem;
}
.announcements-subtitle {
  font-size: 0.9rem;
  color: var(--color-text-secondary);
  margin-bottom: 1.5rem;
}

/* Search bar */
.search-bar {
  margin-bottom: 1.5rem;
}
.search-inputs {
  display: flex;
  gap: 8px;
  align-items: center;
}
.cir-search {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 200px;
  height: 44px;
  padding: 0 8px 0 16px;
  background: var(--color-bg, #fff);
  border: 1px solid var(--color-border, #e3e8ee);
  border-radius: 999px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: border-color 220ms cubic-bezier(0.22, 1, 0.36, 1), box-shadow 220ms cubic-bezier(0.22, 1, 0.36, 1);
}
.cir-search__icon {
  width: 16px;
  height: 16px;
  color: #5b6472;
  flex-shrink: 0;
}
.cir-search__field {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  font: inherit;
  font-size: 14px;
  color: var(--color-text, #0e1116);
}
.cir-search__field::placeholder {
  color: #8a93a3;
}
.cir-search__kbd {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  background: #f3f6fa;
  border: 1px solid #eef2f6;
  border-radius: 999px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 500;
  color: #5b6472;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}
.cir-search:focus-within {
  border-color: var(--color-primary, #2e7def);
  box-shadow: 0 0 0 3px var(--color-primary-glow, rgba(46, 125, 239, 0.22));
}
.cir-search.ai-active {
  border-color: #3b82f6;
}
.cir-search.ai-active:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15);
}

/* AI toggle */
.ai-toggle {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
  border-radius: 0;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.ai-toggle:hover { color: #3b82f6; }
.ai-toggle.active { color: #3b82f6; }

/* AI Loader */
.announcements-ai-loader {
  --color-one: #ffbf48;
  --color-two: #be4a1d;
  --color-three: #ffbf4780;
  --color-four: #bf4a1d80;
  --color-five: #ffbf4740;
  --time-animation: 2s;
  --size: 0.2;
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  transform: scale(var(--size));
  box-shadow: 0 0 25px 0 var(--color-three), 0 20px 50px 0 var(--color-four);
  animation: announcements-colorize calc(var(--time-animation) * 3) ease-in-out infinite;
  flex-shrink: 0;
  pointer-events: none;
}
.announcements-ai-loader::before {
  content: "";
  position: absolute;
  top: 0; left: 0;
  width: 100px; height: 100px;
  border-radius: 50%;
  border-top: solid 1px var(--color-one);
  border-bottom: solid 1px var(--color-two);
  background: linear-gradient(180deg, var(--color-five), var(--color-four));
  box-shadow: inset 0 10px 10px 0 var(--color-three), inset 0 -10px 10px 0 var(--color-four);
}
.announcements-ai-loader-box {
  width: 100px;
  height: 100px;
  background: linear-gradient(180deg, var(--color-one) 30%, var(--color-two) 70%);
  mask: url(#announcements-ai-clipping);
  -webkit-mask: url(#announcements-ai-clipping);
}
.announcements-ai-loader svg { position: absolute; }
.announcements-ai-loader svg #announcements-ai-clipping {
  filter: contrast(15);
  animation: announcements-roundness calc(var(--time-animation) / 2) linear infinite;
}
.announcements-ai-loader svg #announcements-ai-clipping polygon { filter: blur(7px); }
.announcements-ai-loader svg #announcements-ai-clipping polygon:nth-child(1) { transform-origin: 75% 25%; transform: rotate(90deg); }
.announcements-ai-loader svg #announcements-ai-clipping polygon:nth-child(2) { transform-origin: 50% 50%; animation: announcements-rotation var(--time-animation) linear infinite reverse; }
.announcements-ai-loader svg #announcements-ai-clipping polygon:nth-child(3) { transform-origin: 50% 60%; animation: announcements-rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -3); }
.announcements-ai-loader svg #announcements-ai-clipping polygon:nth-child(4) { transform-origin: 40% 40%; animation: announcements-rotation var(--time-animation) linear infinite reverse; }
.announcements-ai-loader svg #announcements-ai-clipping polygon:nth-child(5) { transform-origin: 40% 40%; animation: announcements-rotation var(--time-animation) linear infinite reverse; animation-delay: calc(var(--time-animation) / -2); }
.announcements-ai-loader svg #announcements-ai-clipping polygon:nth-child(6) { transform-origin: 60% 40%; animation: announcements-rotation var(--time-animation) linear infinite; }
.announcements-ai-loader svg #announcements-ai-clipping polygon:nth-child(7) { transform-origin: 60% 40%; animation: announcements-rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -1.5); }
@keyframes announcements-rotation { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
@keyframes announcements-roundness { 0% { filter: contrast(15); } 20% { filter: contrast(3); } 40% { filter: contrast(3); } 60% { filter: contrast(15); } 100% { filter: contrast(15); } }
@keyframes announcements-colorize { 0% { filter: hue-rotate(0deg); } 20% { filter: hue-rotate(-30deg); } 40% { filter: hue-rotate(-60deg); } 60% { filter: hue-rotate(-90deg); } 80% { filter: hue-rotate(-45deg); } 100% { filter: hue-rotate(0deg); } }

/* Buttons */
.search-btn {
  padding: 0 1.25rem;
  height: 44px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  cursor: pointer;
  transition: background var(--transition-fast);
  flex-shrink: 0;
}
.search-btn:hover:not(:disabled) { background: var(--color-primary-hover); }
.search-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.clear-btn {
  padding: 0 1rem;
  height: 44px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}
.clear-btn:hover {
  border-color: var(--color-text-secondary);
  color: var(--color-text);
}

/* Filter toggle */
.filter-toggle {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}
.filter-toggle:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.filter-toggle.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-glow, rgba(46, 125, 239, 0.08));
}
.filter-badge {
  position: absolute;
  top: -4px; right: -4px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--color-primary);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* Filter panel */
.filter-panel {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
  padding: 1rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
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
  color: var(--color-text-secondary);
  white-space: nowrap;
  min-width: 40px;
}
.result-count {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-top: 0.5rem;
}
.ai-hint {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  margin-top: 0.5rem;
}

/* AI Result */
.ai-search-result {
  background: var(--color-bg-muted, #f6f8fa);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 14px;
  margin-bottom: 1rem;
}
.ai-result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.ai-result-label { font-size: 0.75rem; font-weight: 600; color: #3b82f6; }
.ai-cancel-btn {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: transparent;
  font-size: 0.78rem;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}
.ai-cancel-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.ai-result-content { font-size: 0.85rem; color: var(--color-text); line-height: 1.6; white-space: pre-wrap; }
.ai-typing { display: flex; gap: 4px; padding: 8px 0; margin-bottom: 1rem; }
.ai-typing span { width: 6px; height: 6px; border-radius: 50%; background: #3b82f6; animation: ai-bounce 1.2s infinite; }
.ai-typing span:nth-child(2) { animation-delay: 0.2s; }
.ai-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes ai-bounce { 0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); } 40% { opacity: 1; transform: scale(1); } }
.ai-error-msg { font-size: 0.8rem; color: var(--color-error); margin-bottom: 1rem; }

/* Announcement Cards */
.announcements-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.announcement-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 1.1rem 1.25rem;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 4px solid transparent;
  position: relative;
  overflow: hidden;
}
.announcement-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--color-primary);
  opacity: 0.06;
  z-index: 0;
  transition: transform 0.45s ease-out, opacity 0.45s ease-out;
  transform: scaleX(0);
  transform-origin: left center;
}
.announcement-card > * {
  position: relative;
  z-index: 1;
}
.announcement-card:hover {
  transform: translateY(-5px) scale(1.005);
  box-shadow: 0 24px 36px rgba(0,0,0,0.08), 0 24px 46px rgba(59, 130, 246, 0.15);
  border-color: rgba(30, 94, 182, 0.2);
}
.announcement-card:hover::before {
  transform: scaleX(1);
  opacity: 0.08;
}
.announcement-card--info { border-left-color: #3b82f6; }
.announcement-card--success { border-left-color: #22c55e; }
.announcement-card--warning { border-left-color: #f59e0b; }
.announcement-card--error { border-left-color: #ef4444; }

.announcement-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.announcement-tag {
  font-size: 0.68rem;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.announcement-tag--info { background: #dbeafe; color: #2563eb; }
.announcement-tag--success { background: #dcfce7; color: #16a34a; }
.announcement-tag--warning { background: #fef3c7; color: #d97706; }
.announcement-tag--error { background: #fee2e2; color: #dc2626; }
.announcement-date { font-size: 0.72rem; color: var(--color-text-tertiary); }
.announcement-card-title { font-size: 1rem; font-weight: 600; color: var(--color-text); margin-bottom: 6px; }
.announcement-preview { font-size: 0.82rem; color: var(--color-text-secondary); line-height: 1.5; margin: 0; }

.announcements-empty {
  text-align: center;
  padding: 3rem 0;
  color: var(--color-text-tertiary);
  font-size: 0.9rem;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border-light);
  gap: 0.75rem;
  flex-wrap: wrap;
}
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.page-size-label {
  font-size: 0.78rem;
  color: var(--color-text-tertiary);
}
.page-nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.page-nav-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.page-nav-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.page-nav-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-num-btn {
  min-width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 0.78rem;
  font-variant-numeric: tabular-nums;
  transition: all 0.15s;
}
.page-num-btn:hover { background: var(--color-bg-muted); }
.page-num-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  font-weight: 600;
}
.page-total {
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

/* Detail Modal */
.notice-detail-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}
.notice-detail-modal {
  background: var(--color-bg);
  border-radius: 16px;
  max-width: 520px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 2rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.notice-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.notice-detail-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--color-text-tertiary);
  cursor: pointer;
}
.notice-detail-title { font-size: 1.3rem; font-weight: 700; color: var(--color-text); margin-bottom: 0.5rem; }
.notice-detail-date { font-size: 0.78rem; color: var(--color-text-tertiary); margin-bottom: 1rem; }
.notice-detail-content { font-size: 0.9rem; color: var(--color-text-secondary); line-height: 1.7; white-space: pre-wrap; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.3s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }

/* Responsive */
@media (max-width: 768px) {
  .announcements-page { padding: 1rem 0.75rem; }
  .search-inputs { flex-wrap: wrap; gap: 0.4rem; }
  .cir-search { min-width: 0; flex: 1; }
  .cir-search__kbd { display: none; }
  .filter-panel { flex-direction: column; padding: 0.75rem; }
  .filter-label { white-space: normal; }
  .page-title { font-size: 1.2rem; }
  .announcement-card { padding: 0.85rem; border-radius: 10px; }
  .announcement-card-title { font-size: 0.95rem; }
  .announcement-preview { font-size: 0.82rem; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 3; overflow: hidden; }
  .announcement-card-header { font-size: 0.75rem; flex-wrap: wrap; gap: 0.25rem 0.5rem; }
  .pagination { flex-wrap: wrap; gap: 0.25rem; }
}
@media (max-width: 480px) {
  .announcements-page { padding: 0.75rem 0.5rem; }
  .announcement-card { padding: 0.75rem; }
  .announcement-card-title { font-size: 0.88rem; }
}

/* Night mode */
:global(body.body-night) .cir-search__icon { color: #a0aec0; }
:global(body.body-night) .cir-search__field { color: #e0e0e0; }
:global(body.body-night) .cir-search__field::placeholder { color: #718096; }
:global(body.body-night) .cir-search__kbd {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.12);
  color: #a0aec0;
}
:global(body.body-night) .cir-search {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.12);
}
:global(body.body-night) .cir-search:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(9, 105, 218, 0.2);
}
:global(body.body-night) .cir-search.ai-active {
  border-color: #3b82f6;
}
:global(body.body-night) .cir-search.ai-active:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
}
:global(body.body-night) .search-btn {
  background: var(--color-primary);
}
:global(body.body-night) .clear-btn {
  border-color: rgba(255, 255, 255, 0.12);
  color: #a0aec0;
}
:global(body.body-night) .clear-btn:hover {
  border-color: #a0aec0;
  color: #e0e0e0;
}
:global(body.body-night) .filter-toggle {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.12);
  color: #a0aec0;
}
:global(body.body-night) .filter-toggle:hover,
:global(body.body-night) .filter-toggle.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
:global(body.body-night) .filter-panel {
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(255, 255, 255, 0.08);
}
:global(body.body-night) .ai-search-result {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
}
:global(body.body-night) .ai-result-content { color: #a0aec0; }
:global(body.body-night) .ai-cancel-btn {
  border-color: rgba(255, 255, 255, 0.15);
  color: #a0aec0;
}
:global(body.body-night) .ai-cancel-btn:hover {
  border-color: #5a9bff;
  color: #5a9bff;
}
:global(body.body-night) .announcement-card {
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(255, 255, 255, 0.08);
}
:global(body.body-night) .announcement-card:hover { box-shadow: 0 24px 36px rgba(0,0,0,0.3), 0 24px 46px rgba(59, 130, 246, 0.12); }
:global(body.body-night) .announcement-card-title { color: #e0e0e0; }
:global(body.body-night) .announcement-preview { color: #a0aec0; }
:global(body.body-night) .announcement-date { color: #718096; }
:global(body.body-night) .pagination { border-top-color: rgba(255, 255, 255, 0.08); }
:global(body.body-night) .page-nav-btn {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: #a0aec0;
}
:global(body.body-night) .page-nav-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
:global(body.body-night) .page-num-btn:hover { background: rgba(255, 255, 255, 0.06); }
:global(body.body-night) .page-num-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
:global(body.body-night) .notice-detail-mask { background: rgba(0, 0, 0, 0.6); }
:global(body.body-night) .notice-detail-modal { background: #1b2838; }
:global(body.body-night) .notice-detail-title { color: #e0e0e0; }
:global(body.body-night) .notice-detail-content { color: #a0aec0; }
:global(body.body-night) .notice-detail-date { color: #718096; }
:global(body.body-night) .notice-detail-close { color: #a0aec0; }
</style>
