<template>
  <div class="articles-page">
    <h1 class="page-title fade-in-up">{{ $t('articles.title') }}</h1>

    <!-- Search and filter bar -->
    <div class="search-bar fade-in-up">
      <div class="search-inputs">
        <label class="cir-search" :class="{ 'ai-active': aiMode }">
          <svg class="cir-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="m21 21-4.34-4.34"></path>
          </svg>
          <input v-model="keyword" class="cir-search__field" type="search"
            :placeholder="aiMode ? $t('articles.aiSearchPlaceholder') : $t('articles.searchPlaceholder')"
            @keyup.enter="aiMode ? doAiSearch() : doSearch()" @input="onKeywordInput" />
          <button class="ai-toggle" :class="{ active: aiMode }" @click="aiMode = !aiMode" :title="$t('articles.aiSearchTitle')">
            <div class="articles-ai-loader">
              <svg width="100" height="100" viewBox="0 0 100 100">
                <defs>
                  <mask id="articles-ai-clipping">
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
              <div class="articles-ai-loader-box"></div>
            </div>
          </button>
          <kbd class="cir-search__kbd">Enter</kbd>
        </label>
        <button class="filter-toggle" :class="{ active: showFilters || activeFilterCount > 0 }" @click="showFilters = !showFilters" :title="$t('articles.filterTitle')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
          </svg>
          <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
        </button>
        <button class="search-btn" @click="aiMode ? doAiSearch() : doSearch()" :disabled="aiLoading">
          {{ aiLoading ? $t('articles.parsing') : (aiMode ? $t('articles.aiSearch') : $t('articles.search')) }}
        </button>
        <button v-if="hasFilter" class="clear-btn" @click="clearSearch">{{ $t('articles.clear') }}</button>
        <DropdownMenu v-if="!aiMode" v-model="sortBy" :items="sortOptions" @change="doSearch()" />
        <div class="view-mode-toggle">
          <button class="view-mode-btn" :class="{ active: viewMode === 'list' }" @click="setViewMode('list')" :title="$t('articles.listView')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/>
              <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
            </svg>
          </button>
          <button class="view-mode-btn" :class="{ active: viewMode === 'card' }" @click="setViewMode('card')" :title="$t('articles.cardView')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
              <rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Filter Panel -->
      <Transition name="filter-slide">
        <div v-if="showFilters" class="filter-panel">
          <div class="filter-row">
            <label class="filter-label">{{ $t('articles.category') }}</label>
            <DropdownMenu v-model="categoryId" :items="categoryOptions" @change="doSearch()" />
          </div>
          <div class="filter-row">
            <label class="filter-label">{{ $t('articles.tag') }}</label>
            <DropdownMenu v-model="tagId" :items="tagOptions" @change="doSearch()" />
          </div>
          <div class="filter-row">
            <label class="filter-label">{{ $t('articles.startDate') }}</label>
            <DatePicker v-model="startDate" :placeholder="$t('articles.startDate')" :max="endDate || undefined" @change="onDateChange" />
          </div>
          <div class="filter-row">
            <label class="filter-label">{{ $t('articles.endDate') }}</label>
            <DatePicker v-model="endDate" :placeholder="$t('articles.endDate')" :min="startDate || undefined" @change="onDateChange" />
          </div>
        </div>
      </Transition>

      <div v-if="hasFilter" class="result-count">
        {{ $t('articles.found', { count: totalElements }) }}
      </div>
      <div v-if="aiMode" class="ai-hint">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
        {{ $t('articles.aiHint') }}
      </div>
    </div>

    <ResourceError v-if="loadError" :message="$t('articles.loadError')" @retry="loadArticles()" />

    <!-- Skeleton loading -->
    <div v-if="loading && !loadError" class="article-list">
      <div v-for="n in 6" :key="n" class="article-card skeleton-card-articles">
        <div style="display: flex; gap: 12px; margin-bottom: 10px;">
          <div class="skeleton" style="width: 60px; height: 14px;" />
          <div class="skeleton" style="width: 80px; height: 14px;" />
          <div class="skeleton" style="width: 50px; height: 14px;" />
        </div>
        <div class="skeleton" style="width: 70%; height: 20px; margin-bottom: 10px;" />
        <div class="skeleton" style="width: 100%; height: 14px; margin-bottom: 6px;" />
        <div class="skeleton" style="width: 85%; height: 14px; margin-bottom: 12px;" />
        <div style="display: flex; gap: 8px;">
          <div class="skeleton" style="width: 48px; height: 20px; border-radius: 10px;" />
          <div class="skeleton" style="width: 56px; height: 20px; border-radius: 10px;" />
        </div>
      </div>
    </div>

    <!-- List view -->
    <div v-if="viewMode === 'list' && !loading" class="article-list">
      <div v-for="(article, i) in articles" :key="article.id"
        class="article-card fade-in-up"
        :class="`fade-in-up-delay-${Math.min(i + 1, 5)}`">
        <div class="card-bg" aria-hidden="true">
          <div style="--i:10;--j:2;" class="blub"></div>
          <div style="--i:12;--j:1.8;" class="blub"></div>
          <div style="--i:16;--j:2.2;" class="blub"></div>
          <div style="--i:9;--j:1.5;" class="blub"></div>
          <div style="--i:7;--j:1.7;" class="blub"></div>
          <div style="--i:18;--j:2.5;" class="blub"></div>
          <div style="--i:20;--j:2;" class="blub"></div>
          <div style="--i:16;--j:1.9;" class="blub"></div>
          <div style="--i:21;--j:2.1;" class="blub"></div>
          <div style="--i:5;--j:1.6;" class="blub"></div>
          <div class="moon">
            <div class="crater cr1"></div>
            <div class="crater cr2"></div>
            <div class="crater cr3"></div>
          </div>
        </div>
        <div class="article-meta">
          <span v-if="article.categoryName" class="category">{{ article.categoryName }}</span>
          <span class="date">{{ relativeDate(article.createdAt) }}</span>
          <span class="views">{{ article.viewCount }} {{ $t('articles.reads') }}</span>
        </div>
        <router-link :to="`/articles/${article.slug || article.id}`" class="article-title">
          {{ articleTitle(article) }}
        </router-link>
        <p class="article-summary">{{ stripMarkdown(articleSummary(article)) }}</p>
        <div class="article-tags">
          <span v-for="tag in article.tags" :key="tag.id" class="tag" @click="filterByTag(tag.id)">{{ tag.name }}</span>
        </div>
      </div>
    </div>

    <!-- Card grid view -->
    <div v-if="viewMode !== 'list' && !loading" class="article-card-grid">
      <div v-for="(article, i) in articles" :key="article.id"
        class="grid-card fade-in-up"
        :class="[`fade-in-up-delay-${Math.min(i + 1, 5)}`, `card-style-${article.cardStyle || 0}`]">
        <div class="card-bg" aria-hidden="true">
          <div style="--i:10;--j:2;" class="blub"></div>
          <div style="--i:12;--j:1.8;" class="blub"></div>
          <div style="--i:16;--j:2.2;" class="blub"></div>
          <div style="--i:9;--j:1.5;" class="blub"></div>
          <div style="--i:7;--j:1.7;" class="blub"></div>
          <div style="--i:18;--j:2.5;" class="blub"></div>
          <div style="--i:20;--j:2;" class="blub"></div>
          <div style="--i:16;--j:1.9;" class="blub"></div>
          <div style="--i:21;--j:2.1;" class="blub"></div>
          <div style="--i:5;--j:1.6;" class="blub"></div>
          <div class="moon">
            <div class="crater cr1"></div>
            <div class="crater cr2"></div>
            <div class="crater cr3"></div>
          </div>
        </div>
        <!-- Style 18: physical card tabs -->
        <div class="card-tab" aria-hidden="true"></div>
        <div class="card-btn card-btn-top" aria-hidden="true"></div>
        <div class="card-btn card-btn-bottom" aria-hidden="true"></div>
        <!-- Style 20: corner elements + scan line + particles -->
        <div class="card-corners" aria-hidden="true">
          <span></span><span></span><span></span><span></span>
        </div>
        <div class="card-scanline" aria-hidden="true"></div>
        <div class="card-cyber-particles" aria-hidden="true">
          <span></span><span></span><span></span><span></span><span></span><span></span>
        </div>
        <div class="card-cyber-glow" aria-hidden="true"></div>
        <!-- Style 26: light streak -->
        <div class="card-light-streak" aria-hidden="true"></div>
        <!-- Style 22: education circle + overlay -->
        <div class="card-edu-overlay" aria-hidden="true"></div>
        <div class="card-edu-circle" aria-hidden="true">
          <svg viewBox="29 14 71 76" height="56" width="50" fill="none">
            <g transform="translate(30,14)" fill-rule="evenodd" fill="none" stroke-width="1" stroke="none">
              <g fill="#D98A19"><g><g><path d="M0,0 L0,75.92 L69.15,75.92 L0,0 Z M14.06,32.28 L42.95,64 L14.23,64 L14.06,32.28 Z"/></g></g></g>
              <g stroke-linecap="square" stroke="#FFF" transform="translate(0,14.11)">
                <path d="M0.42,54.96 L4.7,54.96"/><path d="M0.42,50.44 L4.7,50.44"/><path d="M0.42,45.92 L4.7,45.92"/>
                <path d="M0.42,41.39 L2.94,41.39"/><path d="M0.42,36.87 L4.7,36.87"/><path d="M0.42,32.35 L4.7,32.35"/>
                <path d="M0.42,27.82 L4.7,27.82"/><path d="M0.42,23.3 L2.94,23.3"/><path d="M0.42,18.77 L4.7,18.77"/>
                <path d="M0.42,14.25 L4.7,14.25"/><path d="M0.42,9.73 L4.7,9.73"/><path d="M0.42,5.2 L2.94,5.2"/>
                <path d="M0.42,0.68 L4.7,0.68"/>
              </g>
            </g>
          </svg>
        </div>
        <!-- Style 24: ticket elements -->
        <svg class="card-ticket-filter" aria-hidden="true">
          <filter id="ticketNoise">
            <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="4" stitchTiles="stitch"/>
            <feColorMatrix type="saturate" values="0"/>
            <feBlend in="SourceGraphic" mode="multiply"/>
          </filter>
        </svg>
        <div class="card-ticket-notes" aria-hidden="true">
          <span>♪♪♪♪♪</span><span>♪♪♪♪</span><span>♪♪♪♪♪</span>
        </div>
        <div class="card-ticket-barcode" aria-hidden="true"></div>
        <router-link :to="`/articles/${article.slug || article.id}`" class="grid-card-link">
          <div v-if="article.coverImage" class="grid-card-cover">
            <img :src="ossImg(article.coverImage)" :alt="articleTitle(article)" loading="lazy" />
          </div>
          <div class="grid-card-body">
            <h3 class="grid-card-title">{{ articleTitle(article) }}</h3>
            <p class="grid-card-summary">{{ stripMarkdown(articleSummary(article)) }}</p>
            <div class="grid-card-meta">
              <span v-if="article.categoryName" class="grid-card-category">{{ article.categoryName }}</span>
              <span class="grid-card-date">{{ relativeDate(article.createdAt) }}</span>
              <span class="grid-card-views">{{ article.viewCount }} {{ $t('articles.reads') }}</span>
            </div>
          </div>
          <!-- Style 19: flip card back face -->
          <div class="card-flip-back">
            <div class="card-flip-glow"></div>
            <div class="card-flip-circles" aria-hidden="true">
              <span></span><span></span><span></span>
            </div>
            <svg class="card-flip-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 12h6m-3-3v6m-7 4h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
            </svg>
            <h3 class="grid-card-title">{{ articleTitle(article) }}</h3>
            <p class="grid-card-summary">{{ stripMarkdown(articleSummary(article)) }}</p>
            <div class="grid-card-meta">
              <span v-if="article.categoryName" class="grid-card-category">{{ article.categoryName }}</span>
              <span class="grid-card-date">{{ relativeDate(article.createdAt) }}</span>
            </div>
            <span class="card-flip-hover-text">Hover Me</span>
          </div>
        </router-link>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="articles.length > 0">
      <div class="page-size-selector">
        <span class="page-size-label">{{ $t('articles.perPage') }}</span>
        <DropdownMenu :modelValue="pageSize" :items="pageSizeItems" @change="v => setPageSize(v)" />
      </div>
      <div class="page-nav" v-if="totalPages > 1">
        <button class="page-nav-btn" :disabled="page === 0" @click="prevPage">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <button v-for="(p, idx) in pageNumbers" :key="'p-' + idx" class="page-num-btn" :class="{ active: p === page + 1 }" @click="goToPage(p)">{{ p }}</button>
        <button class="page-nav-btn" :disabled="page >= totalPages - 1" @click="nextPage">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>
      <span class="page-total">{{ $t('articles.total', { count: totalElements }) }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import { relativeDate } from '../utils/date'
import { stripMarkdown } from '../utils/stripMarkdown'
import { ossImg } from '../utils/oss'
import ResourceError from '../components/ResourceError.vue'
import DropdownMenu from '../components/DropdownMenu.vue'
import DatePicker from '../components/DatePicker.vue'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const isEn = computed(() => locale.value === 'en-US')
function articleTitle(a) { return a && isEn.value && a.titleEn ? a.titleEn : (a?.title || '') }
function articleSummary(a) { return a && isEn.value && a.summaryEn ? a.summaryEn : (a?.summary || '') }

const articles = ref([])
const categories = ref([])
const tags = ref([])
const page = ref(0)
const viewMode = ref(localStorage.getItem('articles_view_mode') || 'card')
const pageSize = ref(Number(localStorage.getItem('articles_page_size')) || (viewMode.value === 'card' ? 8 : 10))
const totalPages = ref(0)
const totalElements = ref(0)
const cardPageSizeOptions = [8, 12, 16, 20]
const listPageSizeOptions = [5, 10, 20, 50]
const pageSizeOptions = computed(() => viewMode.value === 'card' ? cardPageSizeOptions : listPageSizeOptions)
const keyword = ref('')
const categoryId = ref('')
const tagId = ref('')
const startDate = ref('')
const endDate = ref('')
const sortBy = ref('newest')
const aiMode = ref(false)
const aiLoading = ref(false)
const loadError = ref(false)
const loading = ref(true)
const showFilters = ref(false)
let debounceTimer = null
let loadRequestId = 0

const hasFilter = computed(() => keyword.value || categoryId.value || tagId.value || startDate.value || endDate.value)
const activeFilterCount = computed(() => [categoryId.value, tagId.value, startDate.value, endDate.value].filter(Boolean).length)

const sortOptions = computed(() => [
  { value: 'newest', label: t('articles.newest') },
  { value: 'views', label: t('articles.mostViews') },
  { value: 'likes', label: t('articles.mostLikes') }
])
const categoryOptions = computed(() => [
  { value: '', label: t('articles.allCategories') },
  ...categories.value.map(c => ({ value: c.id, label: c.name }))
])
const tagOptions = computed(() => [
  { value: '', label: t('articles.allTags') },
  ...tags.value.map(tag => ({ value: tag.id, label: tag.name }))
])
const pageSizeItems = computed(() => pageSizeOptions.value.map(s => ({ value: s, label: `${s} ${t('articles.articles')}` })))

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

function goToPage(p) {
  if (p === '...' || p === page.value + 1) return
  page.value = p - 1
  syncUrl()
  loadArticles()
}
function prevPage() { if (page.value > 0) { page.value--; syncUrl(); loadArticles() } }
function nextPage() { if (page.value < totalPages.value - 1) { page.value++; syncUrl(); loadArticles() } }
function setPageSize(size) {
  pageSize.value = size
  page.value = 0
  localStorage.setItem('articles_page_size', size)
  syncUrl()
  loadArticles()
}

function setViewMode(mode) {
  viewMode.value = mode
  localStorage.setItem('articles_view_mode', mode)
  const validSizes = mode === 'card' ? cardPageSizeOptions : listPageSizeOptions
  if (!validSizes.includes(pageSize.value)) {
    pageSize.value = validSizes[0]
    localStorage.setItem('articles_page_size', validSizes[0])
  }
  page.value = 0
  syncUrl()
  loadArticles()
}

function restoreState() {
  if (route.query.keyword) keyword.value = route.query.keyword
  if (route.query.categoryId) categoryId.value = Number(route.query.categoryId)
  if (route.query.tagId) tagId.value = Number(route.query.tagId)
  if (route.query.startDate) startDate.value = route.query.startDate
  if (route.query.endDate) endDate.value = route.query.endDate
  if (route.query.page) page.value = Math.max(0, Number(route.query.page) || 0)

  if (!route.query.keyword && !route.query.categoryId && !route.query.tagId && !route.query.startDate && !route.query.endDate) {
    keyword.value = ''
    categoryId.value = ''
    tagId.value = ''
    startDate.value = ''
    endDate.value = ''
    page.value = 0
  }
}

function syncUrl() {
  const query = {}
  if (keyword.value) query.keyword = keyword.value
  if (categoryId.value) query.categoryId = categoryId.value
  if (tagId.value) query.tagId = tagId.value
  if (startDate.value) query.startDate = startDate.value
  if (endDate.value) query.endDate = endDate.value
  if (page.value > 0) query.page = page.value
  router.replace({ query })
}

function onKeywordInput() {
  clearTimeout(debounceTimer)
  if (aiMode.value) return // AI mode: wait for explicit submit
  debounceTimer = setTimeout(() => {
    doSearch()
  }, 300)
}

function doSearch() {
  page.value = 0
  syncUrl()
  loadArticles()
}

async function doAiSearch() {
  if (!keyword.value.trim()) return
  aiLoading.value = true
  try {
    const res = await request.post('/api/ai/search/parse', { message: keyword.value })
    if (res.keyword) keyword.value = res.keyword
    else keyword.value = ''
    if (res.categoryId) categoryId.value = res.categoryId
    if (res.tagId) tagId.value = res.tagId
    const validSorts = ['newest', 'views', 'likes']
    if (res.sortBy && validSorts.includes(res.sortBy)) sortBy.value = res.sortBy
    page.value = 0
    syncUrl()
    loadArticles()
  } catch (e) {
    console.error('AI search failed:', e)
    doSearch()
  } finally {
    aiLoading.value = false
  }
}

function clearSearch() {
  keyword.value = ''
  categoryId.value = ''
  tagId.value = ''
  startDate.value = ''
  endDate.value = ''
  page.value = 0
  syncUrl()
  loadArticles()
}

function filterByTag(id) {
  tagId.value = id
  keyword.value = ''
  categoryId.value = ''
  doSearch()
}

function onDateChange() {
  // 开始日期不能大于结束日期，结束日期不能小于开始日期
  if (startDate.value && endDate.value && startDate.value > endDate.value) {
    const tmp = startDate.value
    startDate.value = endDate.value
    endDate.value = tmp
  }
  page.value = 0
  syncUrl()
  loadArticles()
}

async function loadArticles() {
  const reqId = ++loadRequestId
  loadError.value = false
  try {
    const sortMap = { newest: 'latest', views: 'popular', likes: 'featured' }
    let url = `/api/articles?page=${page.value}&size=${pageSize.value}&sort=${sortMap[sortBy.value] || 'latest'}`
    if (keyword.value) url += `&keyword=${encodeURIComponent(keyword.value)}`
    if (categoryId.value) url += `&categoryId=${categoryId.value}`
    if (tagId.value) url += `&tagId=${tagId.value}`
    if (startDate.value) url += `&startDate=${startDate.value}`
    if (endDate.value) url += `&endDate=${endDate.value}`
    const data = await request.get(url)
    if (reqId !== loadRequestId) return
    articles.value = data.content || []
    totalPages.value = data.totalPages || 0
    totalElements.value = data.totalElements || 0
  } catch (e) {
    if (reqId !== loadRequestId) return
    console.error('Failed to load articles:', e)
    loadError.value = true
  }
}

onMounted(async () => {
  restoreState()
  const [cats, tagList] = await Promise.allSettled([
    request.get('/api/categories'),
    request.get('/api/tags')
  ])
  categories.value = cats.status === 'fulfilled' ? cats.value || [] : []
  tags.value = tagList.status === 'fulfilled' ? tagList.value || [] : []
  await loadArticles()
  loading.value = false
  // Mouse tracking for style-20 cyber3D cards
  document.addEventListener('mousemove', handleCyberMouseMove)
})

let cyberRaf = null
let cyberCards = null
watch(articles, () => { cyberCards = null })
function handleCyberMouseMove(e) {
  if (cyberRaf) return
  cyberRaf = requestAnimationFrame(() => {
    if (!cyberCards) cyberCards = document.querySelectorAll('.card-style-20')
    cyberCards.forEach(card => {
      const rect = card.getBoundingClientRect()
      const x = ((e.clientX - rect.left) / rect.width * 100).toFixed(1)
      const y = ((e.clientY - rect.top) / rect.height * 100).toFixed(1)
      card.style.setProperty('--mx', x + '%')
      card.style.setProperty('--my', y + '%')
    })
    cyberRaf = null
  })
}

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  if (cyberRaf) cancelAnimationFrame(cyberRaf)
  document.removeEventListener('mousemove', handleCyberMouseMove)
})
</script>

<style scoped>
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1.25rem;
}

/* ===== Search bar ===== */
.search-bar {
  margin-bottom: 1.5rem;
}
.search-inputs {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
/* Cir Search */
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
.ai-toggle:hover {
  color: #3b82f6;
}
.ai-toggle.active {
  color: #3b82f6;
}

/* Articles AI Loader */
.articles-ai-loader {
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
  box-shadow:
    0 0 25px 0 var(--color-three),
    0 20px 50px 0 var(--color-four);
  animation: articles-colorize calc(var(--time-animation) * 3) ease-in-out infinite;
  flex-shrink: 0;
  pointer-events: none;
}
.articles-ai-loader::before {
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
  box-shadow:
    inset 0 10px 10px 0 var(--color-three),
    inset 0 -10px 10px 0 var(--color-four);
}
.articles-ai-loader-box {
  width: 100px;
  height: 100px;
  background: linear-gradient(180deg, var(--color-one) 30%, var(--color-two) 70%);
  mask: url(#articles-ai-clipping);
  -webkit-mask: url(#articles-ai-clipping);
}
.articles-ai-loader svg { position: absolute; }
.articles-ai-loader svg #articles-ai-clipping {
  filter: contrast(15);
  animation: articles-roundness calc(var(--time-animation) / 2) linear infinite;
}
.articles-ai-loader svg #articles-ai-clipping polygon { filter: blur(7px); }
.articles-ai-loader svg #articles-ai-clipping polygon:nth-child(1) { transform-origin: 75% 25%; transform: rotate(90deg); }
.articles-ai-loader svg #articles-ai-clipping polygon:nth-child(2) { transform-origin: 50% 50%; animation: articles-rotation var(--time-animation) linear infinite reverse; }
.articles-ai-loader svg #articles-ai-clipping polygon:nth-child(3) { transform-origin: 50% 60%; animation: articles-rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -3); }
.articles-ai-loader svg #articles-ai-clipping polygon:nth-child(4) { transform-origin: 40% 40%; animation: articles-rotation var(--time-animation) linear infinite reverse; }
.articles-ai-loader svg #articles-ai-clipping polygon:nth-child(5) { transform-origin: 40% 40%; animation: articles-rotation var(--time-animation) linear infinite reverse; animation-delay: calc(var(--time-animation) / -2); }
.articles-ai-loader svg #articles-ai-clipping polygon:nth-child(6) { transform-origin: 60% 40%; animation: articles-rotation var(--time-animation) linear infinite; }
.articles-ai-loader svg #articles-ai-clipping polygon:nth-child(7) { transform-origin: 60% 40%; animation: articles-rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -1.5); }
@keyframes articles-rotation { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
@keyframes articles-roundness { 0% { filter: contrast(15); } 20% { filter: contrast(3); } 40% { filter: contrast(3); } 60% { filter: contrast(15); } 100% { filter: contrast(15); } }
@keyframes articles-colorize { 0% { filter: hue-rotate(0deg); } 20% { filter: hue-rotate(-30deg); } 40% { filter: hue-rotate(-60deg); } 60% { filter: hue-rotate(-90deg); } 80% { filter: hue-rotate(-45deg); } 100% { filter: hue-rotate(0deg); } }

.ai-hint {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  margin-top: 0.5rem;
}
.search-btn {
  padding: 0.5rem 1.25rem;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  cursor: pointer;
  transition: background var(--transition-fast);
}
.search-btn:hover:not(:disabled) { background: var(--color-primary-hover); }
.search-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.clear-btn {
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
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
  top: -4px;
  right: -4px;
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
  min-width: 56px;
}
.result-count {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-top: 0.5rem;
}

/* ===== Article list ===== */
.article-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.article-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  transition: all 0.3s ease-out;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}
.article-card::before {
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
.article-card:hover {
  transform: translateY(-5px) scale(1.005);
  box-shadow: 0 24px 36px rgba(0,0,0,0.08), 0 24px 46px rgba(59, 130, 246, 0.15);
  border-color: rgba(30, 94, 182, 0.2);
}
.article-card:hover::before {
  transform: scaleX(1);
  opacity: 0.08;
}
.article-card:active {
  transform: scale(1);
  box-shadow: 0 15px 24px rgba(0,0,0,0.08), 0 15px 24px rgba(59, 130, 246, 0.15);
}
.article-card > * {
  position: relative;
  z-index: 1;
}
.skeleton-card-articles {
  cursor: default;
  pointer-events: none;
}
.skeleton-card-articles:hover {
  transform: none;
  box-shadow: none;
}
.article-meta {
  display: flex;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.4rem;
}
.category { color: var(--color-primary); font-weight: 500; }
.article-title {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-text);
  text-decoration: none;
  display: block;
  margin-bottom: 0.35rem;
  transition: color var(--transition-fast);
}
.article-title:hover { color: var(--color-primary); }
.article-summary {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
  line-height: 1.5;
}
.article-tags { display: flex; gap: 0.4rem; flex-wrap: wrap; }
.tag {
  font-size: 0.72rem;
  padding: 0.12rem 0.55rem;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-light);
  border-radius: 2rem;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  cursor: pointer;
}
.tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
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

/* ===== View mode toggle ===== */
.view-mode-toggle {
  display: flex;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
}
.view-mode-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  border: none;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}
.view-mode-btn + .view-mode-btn {
  border-left: 1px solid var(--color-border);
}
.view-mode-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-glow, rgba(46, 125, 239, 0.08));
}
.view-mode-btn.active {
  color: #fff;
  background: var(--color-primary);
}

/* ===== Card grid view ===== */
.article-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 220px));
  gap: 1.25rem;
  margin-bottom: 1.5rem;
  justify-content: center;
}
.grid-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: 17px;
  overflow: hidden;
  box-shadow: 12px 17px 51px rgba(0, 0, 0, 0.22);
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
  cursor: pointer;
  position: relative;
  aspect-ratio: 5 / 7;
}
.grid-card:hover {
  transform: scale(1.05);
  border-color: var(--color-text);
  will-change: transform;
}
.grid-card:active {
  transform: scale(0.95) rotateZ(1.7deg);
}
.grid-card-link {
  text-decoration: none;
  color: inherit;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.grid-card-cover {
  width: 100%;
  flex: 1;
  overflow: hidden;
  background: var(--color-bg-muted);
}
.grid-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.grid-card:hover .grid-card-cover img {
  transform: scale(1.06);
}
.grid-card-body {
  padding: 1rem 1.1rem;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.grid-card-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 0.4rem;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.grid-card-summary {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0 0 0.6rem;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.grid-card-meta {
  display: flex;
  gap: 0.5rem;
  font-size: 0.72rem;
  color: var(--color-text-tertiary);
  flex-wrap: wrap;
}
.grid-card-category {
  color: var(--color-primary);
  font-weight: 500;
}

/* ===== Card Style 1: 阴影 ===== */
.card-style-1 {
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.08);
}
.card-style-1:hover {
  box-shadow: inset 0 0 30px rgba(0, 0, 0, 0.12), 12px 17px 51px rgba(0, 0, 0, 0.22);
}

/* ===== Card Style 2: 魔法 ===== */
.card-style-2 {
  position: relative;
  border: none;
  background: transparent;
}
.card-style-2::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 17px;
  padding: 2px;
  background: linear-gradient(135deg, #667eea, #764ba2, #f093fb, #667eea);
  background-size: 300% 300%;
  animation: magicGradient 4s ease infinite;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  z-index: 0;
}
.card-style-2::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 15px;
  background: var(--color-bg);
  z-index: 0;
}
.card-style-2 .grid-card-link { position: relative; z-index: 1; }
.card-style-2:hover::before {
  background-size: 100% 100%;
}
@keyframes magicGradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* ===== Card Style 3: 旋转边框 ===== */
.card-style-3 {
  position: relative;
  border: none;
  background: transparent;
  overflow: hidden;
}
.card-style-3::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: conic-gradient(from 0deg, #f09, #0ff, #f09, #0ff, #f09);
  animation: rotateBorder 4s linear infinite;
  z-index: 0;
}
.card-style-3::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 15px;
  background: var(--color-bg);
  z-index: 0;
}
.card-style-3 .grid-card-link { position: relative; z-index: 1; }
@keyframes rotateBorder {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ===== Card Style 4: 笔记本 ===== */
.card-style-4 {
  background: #fef9ef;
  border-color: #e8dcc8;
  border-left: 4px solid #f59e0b;
}
.card-style-4::before {
  content: '';
  position: absolute;
  top: 0;
  left: 4px;
  right: 0;
  height: 100%;
  background: repeating-linear-gradient(
    transparent,
    transparent 27px,
    #f0e6d3 27px,
    #f0e6d3 28px
  );
  opacity: 0.4;
  pointer-events: none;
  z-index: 0;
}
.card-style-4 .grid-card-link { position: relative; z-index: 1; }
.card-style-4 .grid-card-title { font-family: 'Georgia', serif; }
.card-style-4:hover {
  background: #fdf5e6;
  border-color: #d4a853;
}

/* ===== Card Style 5: 门票 ===== */
.card-style-5 {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  position: relative;
}
.card-style-5::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 50%;
  width: 16px;
  height: 16px;
  background: var(--color-bg);
  border-radius: 50%;
  transform: translateY(-50%);
  z-index: 2;
}
.card-style-5::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 50%;
  width: 16px;
  height: 16px;
  background: var(--color-bg);
  border-radius: 50%;
  transform: translateY(-50%);
  z-index: 2;
}
.card-style-5 .grid-card-link {
  position: relative;
  z-index: 1;
  border-top: 2px dashed rgba(255,255,255,0.35);
  border-bottom: 2px dashed rgba(255,255,255,0.35);
}
.card-style-5 .grid-card-title { color: #fff; }
.card-style-5 .grid-card-summary { color: rgba(255,255,255,0.8); }
.card-style-5 .grid-card-meta { color: rgba(255,255,255,0.6); }
.card-style-5 .grid-card-category { color: #ffd700; }
.card-style-5 .grid-card-cover { background: rgba(255,255,255,0.1); }
.card-style-5 .grid-card-placeholder-text { color: rgba(255,255,255,0.3); }
.card-style-5:hover {
  background: linear-gradient(135deg, #5a6fd6, #6a3f96);
}

/* ===== Card Style 6: 翻转 ===== */
.card-style-6 {
  perspective: 1000px;
  border: none;
  background: transparent;
  overflow: visible;
}
.card-style-6 .grid-card-link {
  transition: transform 0.6s;
  transform-style: preserve-3d;
  position: relative;
  z-index: 1;
}
.card-style-6:hover .grid-card-link {
  transform: rotateY(180deg);
}
.card-style-6 .grid-card-cover,
.card-style-6 .grid-card-body {
  backface-visibility: hidden;
}
.card-style-6 .grid-card-body {
  transform: rotateY(180deg);
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background: linear-gradient(135deg, #00c6fb, #005bea);
  border-radius: 17px;
}
.card-style-6 .grid-card-title { color: #fff; text-align: center; }
.card-style-6 .grid-card-summary { color: rgba(255,255,255,0.85); text-align: center; }
.card-style-6 .grid-card-meta { color: rgba(255,255,255,0.6); justify-content: center; }
.card-style-6 .grid-card-category { color: #ffd700; }

/* ===== Card Style 7: 赛博 ===== */
.card-style-7 {
  background: #0a0a2e;
  border-color: #0ff;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2), inset 0 0 10px rgba(0, 255, 255, 0.05);
  position: relative;
  overflow: hidden;
}
.card-style-7::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 255, 255, 0.03) 2px,
    rgba(0, 255, 255, 0.03) 4px
  );
  pointer-events: none;
  z-index: 0;
}
.card-style-7::after {
  content: '';
  position: absolute;
  top: -100%;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(180deg, transparent, rgba(0, 255, 255, 0.1), transparent);
  animation: cyberScan 3s linear infinite;
  pointer-events: none;
  z-index: 0;
}
.card-style-7 .grid-card-link { position: relative; z-index: 1; }
.card-style-7 .grid-card-title { color: #0ff; text-shadow: 0 0 10px rgba(0, 255, 255, 0.5); }
.card-style-7 .grid-card-summary { color: rgba(0, 255, 255, 0.7); }
.card-style-7 .grid-card-meta { color: rgba(0, 255, 255, 0.5); }
.card-style-7 .grid-card-category { color: #f0f; text-shadow: 0 0 8px rgba(255, 0, 255, 0.5); }
.card-style-7 .grid-card-cover { background: rgba(0, 255, 255, 0.05); }
.card-style-7 .grid-card-placeholder-text { color: rgba(0, 255, 255, 0.3); }
.card-style-7:hover {
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.4), inset 0 0 20px rgba(0, 255, 255, 0.1);
  border-color: #f0f;
}
@keyframes cyberScan {
  0% { top: -100%; }
  100% { top: 200%; }
}

/* ===== Card Style 8: 渐变边框 ===== */
.card-style-8 {
  position: relative;
  border: none;
  background: transparent;
}
.card-style-8::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 17px;
  padding: 2px;
  background: linear-gradient(135deg, #f09, #0ff, #f09);
  background-size: 200% 200%;
  animation: gradientBorder 3s ease infinite;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  z-index: 0;
  transition: transform 0.5s;
}
.card-style-8::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 15px;
  background: var(--color-bg);
  z-index: 0;
}
.card-style-8 .grid-card-link { position: relative; z-index: 1; }
.card-style-8:hover::before {
  transform: rotate(-90deg);
  box-shadow: 0 0 20px rgba(255, 0, 153, 0.3), 0 0 40px rgba(0, 255, 255, 0.2);
}
@keyframes gradientBorder {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* ===== Card Style 9: 教育 ===== */
.card-style-9 {
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
  border-top-left-radius: 10px;
  border-bottom-left-radius: 0;
  position: relative;
  overflow: hidden;
}
.card-style-9::before {
  content: '';
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--edu-bg, #ffd861);
  top: 30%;
  left: 50%;
  transform: translate(-50%, -50%) scale(1);
  z-index: 0;
  transition: transform 0.4s ease-out;
}
.card-style-9::after {
  content: '';
  position: absolute;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: var(--edu-bg, #ffd861);
  top: 30%;
  left: 50%;
  transform: translate(-50%, -50%) scale(0);
  z-index: 0;
  transition: transform 0.5s ease-out;
  opacity: 0.6;
}
.card-style-9 .grid-card-link { position: relative; z-index: 1; }
.card-style-9:hover {
  border-color: #ffd861;
  box-shadow: 0 24px 36px rgba(0,0,0,0.11), 0 24px 46px rgba(255, 215, 97, 0.48);
}
.card-style-9:hover::before {
  transform: translate(-50%, -50%) scale(4);
}
.card-style-9:hover::after {
  transform: translate(-50%, -50%) scale(3.5);
}

/* ===== Card Style 10: 全息门票 ===== */
.card-style-10 {
  position: relative;
  overflow: hidden;
  background: transparent;
  border: none;
  padding: 14px 0;
  filter: drop-shadow(0 2px 1px rgba(0,0,0,0.15)) drop-shadow(0 4px 3px rgba(0,0,0,0.12))
    drop-shadow(0 10px 9px rgba(0,0,0,0.1)) drop-shadow(0 20px 20px rgba(0,0,0,0.08));
  transition: transform 0.5s, filter 0.5s;
}
.card-style-10::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background-image:
    linear-gradient(to bottom, rgba(255,238,170,0.45), 90%, rgba(0,0,0,0.08)),
    conic-gradient(
      at 60% 50%,
      #ccc, #ff6bfe, #00f9f8, #ddd, #0081fd, #eef0bc,
      #0081fd, #ff6bfe, rgba(0,0,0,0.1), #0081fd, #ddd, #01fefb, #ccc
    );
  z-index: 0;
}
.card-style-10:hover::before {
  filter: brightness(1.15) contrast(1.1);
}
.card-style-10::after {
  content: '♪♪♪♪♪♪♪♪♪♪';
  position: absolute;
  inset: 0;
  font-size: 5rem;
  line-height: 1;
  color: #e7e7e7;
  mix-blend-mode: color-burn;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
  transform: translateY(20%);
}
/* Perforations top/bottom */
.card-style-10 .card-bg {
  display: block;
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: #fff;
  z-index: 0;
  mask-image:
    radial-gradient(circle 6px at 18px 0, transparent 5px, #000 6px),
    radial-gradient(circle 6px at calc(100% - 18px) 0, transparent 5px, #000 6px),
    radial-gradient(circle 6px at 18px 100%, transparent 5px, #000 6px),
    radial-gradient(circle 6px at calc(100% - 18px) 100%, transparent 5px, #000 6px),
    linear-gradient(#000 0 0);
  mask-size: 100% 100%, 100% 100%, 100% 100%, 100% 100%, 100% 100%;
  mask-position: 0 0, 0 0, 0 0, 0 0, 0 0;
  mask-repeat: no-repeat;
  mask-composite: exclude;
  -webkit-mask-composite: destination-out;
}
.card-style-10 .grid-card-link { position: relative; z-index: 1; }
.card-style-10 .blub,
.card-style-10 .moon { display: none; }
.card-style-10 .grid-card-cover { background: rgba(255,255,255,0.15); }
.card-style-10 .grid-card-title { color: #1a1a2e; font-weight: 700; }
.card-style-10 .grid-card-summary { color: rgba(0,0,0,0.65); }
.card-style-10 .grid-card-meta { color: rgba(0,0,0,0.55); }
.card-style-10 .grid-card-category { color: #0081fd; font-weight: 600; }
.card-style-10:hover {
  transform: translateY(-7px) scale(1.02);
  filter: drop-shadow(0 4px 3px rgba(0,0,0,0.1)) drop-shadow(0 6px 6px rgba(0,0,0,0.1))
    drop-shadow(0 16px 14px rgba(0,0,0,0.08)) drop-shadow(0 30px 28px rgba(0,0,0,0.06));
}

/* ===== Card Style 12: 立体 ===== */
.card-style-12 {
  transform: perspective(600px) rotateY(-5deg) rotateX(3deg);
  background: linear-gradient(135deg, #38bdf8, #0284c7, #075985);
  border-color: rgba(255,255,255,0.2);
  box-shadow: 12px 12px 0 0 #0d0d0d;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.card-style-12 .grid-card-link { position: relative; z-index: 1; }
.card-style-12 .grid-card-title { color: #fff; }
.card-style-12 .grid-card-summary { color: rgba(255,255,255,0.8); }
.card-style-12 .grid-card-meta { color: rgba(255,255,255,0.6); }
.card-style-12 .grid-card-category { color: #ffd700; }
.card-style-12 .grid-card-cover { background: rgba(255,255,255,0.1); }
.card-style-12 .grid-card-placeholder-text { color: rgba(255,255,255,0.3); }
.card-style-12:hover {
  transform: rotate3d(0, 0, 0, 0deg) scale(1.05);
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}
.card-style-12:active {
  transform: rotate3d(0, 0, 0, 0deg) scale(0.95) rotateZ(1.7deg);
}

/* ===== Card Style 13: 光晕 ===== */
.card-style-13 {
  background: #171717;
  border-color: transparent;
  position: relative;
  overflow: hidden;
  box-shadow: 0 0 3px 1px rgba(0,0,0,0.5);
}
.card-style-13::before {
  content: '';
  position: absolute;
  width: 80px;
  height: 360px;
  background: linear-gradient(#ff2288, #387ef0);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(0deg);
  opacity: 0;
  transition: opacity 0.3s;
  animation: glowRotate 4s linear infinite;
  animation-play-state: paused;
  z-index: 0;
}
.card-style-13::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(23, 23, 23, 0.92);
  z-index: 0;
}
.card-style-13 .grid-card-link { position: relative; z-index: 1; }
.card-style-13 .grid-card-title { color: #fff; }
.card-style-13 .grid-card-summary { color: rgba(255,255,255,0.7); }
.card-style-13 .grid-card-meta { color: rgba(255,255,255,0.5); }
.card-style-13 .grid-card-category { color: #ff2288; }
.card-style-13 .grid-card-cover { background: rgba(255,255,255,0.05); }
.card-style-13 .grid-card-placeholder-text { color: rgba(255,255,255,0.2); }
.card-style-13:hover::before {
  opacity: 1;
  animation-play-state: running;
}
.card-style-13:hover {
  border-color: transparent;
}
@keyframes glowRotate {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}

/* ===== Card Style 14: 霓虹边框 ===== */
.card-style-14 {
  background: linear-gradient(163deg, #00ff75, #3700ff);
  border: none;
  padding: 3px;
}
.card-style-14 .grid-card-link {
  position: relative;
  z-index: 1;
  background: var(--color-bg);
  border-radius: 14px;
  transition: transform 0.2s;
}
.card-style-14:hover {
  box-shadow: 0 0 30px 1px rgba(0, 255, 117, 0.3);
}
.card-style-14:hover .grid-card-link {
  transform: scale(0.98);
}

/* ===== Card Style 15: 渐变发光 ===== */
.card-style-15 {
  background: linear-gradient(to left, #f7ba2b, #ea5358);
  border: none;
  padding: 5px;
  position: relative;
}
.card-style-15::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 0;
  right: 0;
  height: 100%;
  width: 100%;
  transform: scale(0.8);
  filter: blur(25px);
  background: linear-gradient(to left, #f7ba2b, #ea5358);
  z-index: 0;
  transition: opacity 0.5s;
}
.card-style-15 .grid-card-link {
  position: relative;
  z-index: 1;
  background: var(--color-bg);
  border-radius: 12px;
}
.card-style-15:hover::before {
  opacity: 0;
}


/* ===== Card Style 17: 魔法渐变边框 ===== */
.card-style-17 {
  background: linear-gradient(to right, #74ebd5 0%, #acb6e5 100%);
  border: none;
  border-radius: 1rem;
  padding: 5px;
  overflow: visible;
}
.card-style-17::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 1rem;
  background: linear-gradient(to bottom right, #f6d365 0%, #fda085 100%);
  transform: rotate(2deg);
  z-index: 0;
  transition: opacity 0.5s ease;
}
.card-style-17::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 1rem;
  background: linear-gradient(to top right, #84fab0 0%, #8fd3f4 100%);
  transform: rotate(-2deg);
  z-index: 0;
  transition: opacity 0.5s ease;
}
.card-style-17 .grid-card-link {
  position: relative;
  z-index: 1;
  background: #292b2c;
  border-radius: 0.7rem;
  height: 100%;
}
.card-style-17:hover::before,
.card-style-17:hover::after {
  opacity: 0;
}
.card-style-17:hover .grid-card-link {
  background: #1a1a2e;
}
.card-style-17:hover .grid-card-title {
  color: #74ebd5;
  transition: color 1s;
}
.card-style-17:hover .grid-card-summary { color: #74ebd5; transition: color 1s; }
.card-style-17:hover .grid-card-meta { color: #74ebd5; transition: color 1s; }

/* ===== Card Style 18: 物理卡片 ===== */
.card-style-18 {
  background: #f8f9fa;
  border: 3px solid #222;
  border-radius: 1rem;
  box-shadow: 5px 5px 2.5px 6px rgb(209, 218, 218);
  position: relative;
  overflow: visible;
}
.card-tab {
  display: block;
  position: absolute;
  top: -3px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 20px;
  background: #222;
  border-radius: 0 0 0.7rem 0.7rem;
  z-index: 2;
}
.card-btn {
  display: block;
  position: absolute;
  right: -8px;
  width: 16px;
  height: 28px;
  background: #f8f9fa;
  border: 3px solid #222;
  border-radius: 0.3rem;
  z-index: 2;
}
.card-btn-top { top: 56px; }
.card-btn-bottom { bottom: 72px; height: 40px; }
.card-style-18 .grid-card-link {
  position: relative;
  z-index: 1;
}
.card-style-18:hover {
  box-shadow: 5px 5px 2.5px 10px rgb(180, 190, 190);
}
.card-style-18 .card-tab { display: block; }
.card-style-18 .card-btn { display: block; }

/* ===== Card Style 19: 3D翻转 ===== */
.card-style-19 {
  perspective: 1000px;
  border: none;
  background: transparent;
  overflow: visible;
}
.card-style-19 .grid-card-link {
  position: relative;
  z-index: 1;
  transform-style: preserve-3d;
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0px 0px 10px 1px rgba(0,0,0,0.93);
  border-radius: 5px;
  overflow: hidden;
}
.card-style-19:hover .grid-card-link {
  transform: rotateY(180deg);
}
.card-style-19 .grid-card-cover,
.card-style-19 .grid-card-body {
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
}
.card-style-19 .grid-card-body {
  position: relative;
  z-index: 1;
}
.card-flip-back {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: absolute;
  inset: 0;
  background: #151515;
  transform: rotateY(180deg);
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: 5px;
  padding: 1.2rem;
  z-index: 0;
}
.card-flip-glow {
  position: absolute;
  inset: 0;
  border-radius: 5px;
  overflow: hidden;
}
.card-flip-glow::before {
  content: '';
  position: absolute;
  width: 160px;
  height: 200%;
  top: -50%;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(90deg, transparent, #ffbb66, #ffbb66, #ffbb66, #ffbb66, transparent);
  animation: cardFlipGlow 5s linear infinite;
}
@keyframes cardFlipGlow {
  0% { transform: translateX(-50%) rotateZ(0deg); }
  100% { transform: translateX(-50%) rotateZ(360deg); }
}
.card-style-19 .card-flip-back .grid-card-title {
  color: #fff;
  text-align: center;
  font-size: 1.1rem;
  -webkit-line-clamp: 2;
}
.card-style-19 .card-flip-back .grid-card-summary {
  color: rgba(255,255,255,0.85);
  text-align: center;
  -webkit-line-clamp: 3;
  margin-top: 0.5rem;
}
.card-style-19 .card-flip-back .grid-card-meta {
  color: rgba(255,255,255,0.5);
  justify-content: center;
  margin-top: 0.8rem;
}
.card-style-19 .card-flip-back .grid-card-category {
  color: #ffbb66;
}
/* Floating colored circles */
.card-flip-circles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: 5px;
  pointer-events: none;
}
.card-flip-circles span {
  position: absolute;
  border-radius: 50%;
  filter: blur(1px);
}
.card-flip-circles span:nth-child(1) {
  width: 50px; height: 50px;
  background: #ffbb66;
  top: 10%; left: 10%;
  animation: flipFloat1 4s ease-in-out infinite;
}
.card-flip-circles span:nth-child(2) {
  width: 35px; height: 35px;
  background: #ff8866;
  top: 60%; right: 15%;
  animation: flipFloat2 5s ease-in-out infinite;
}
.card-flip-circles span:nth-child(3) {
  width: 25px; height: 25px;
  background: #ff2233;
  bottom: 15%; left: 30%;
  animation: flipFloat3 3.5s ease-in-out infinite;
}
@keyframes flipFloat1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(10px, -15px); }
}
@keyframes flipFloat2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-12px, 10px); }
}
@keyframes flipFloat3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(8px, -12px); }
}
/* SVG icon on back face */
.card-flip-icon {
  width: 40px;
  height: 40px;
  color: #ffbb66;
  margin-bottom: 0.5rem;
  z-index: 1;
  position: relative;
}
/* Hover Me text */
.card-flip-hover-text {
  color: rgba(255,255,255,0.4);
  font-size: 0.75rem;
  margin-top: 0.6rem;
  letter-spacing: 2px;
  text-transform: uppercase;
  z-index: 1;
  position: relative;
}
/* Scoped display overrides for style-19 flip elements */
.card-style-19 .card-flip-back { display: flex; }
.card-style-19 .card-flip-glow { display: block; }
.card-style-19 .card-flip-circles { display: block; }
.card-style-19 .card-flip-icon { display: block; }
.card-style-19 .card-flip-hover-text { display: block; }

/* ===== Card Style 20: 赛博3D ===== */
.card-style-20 {
  background: linear-gradient(45deg, #1a1a1a, #262626);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3), inset 0 0 20px rgba(0, 0, 0, 0.2);
  perspective: 800px;
  transition: transform 0.3s ease, filter 0.3s ease, box-shadow 0.3s ease;
}
.card-style-20::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(
    circle at var(--mx, 50%) var(--my, 50%),
    rgba(0, 255, 170, 0.1) 0%,
    rgba(0, 162, 255, 0.05) 50%,
    transparent 100%
  );
  filter: blur(20px);
  opacity: 0;
  z-index: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.card-style-20:hover::before {
  opacity: 1;
}
.card-style-20::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(
    125deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.05) 45%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0.05) 55%,
    rgba(255, 255, 255, 0) 100%
  );
  opacity: 0;
  z-index: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.card-style-20:hover::after {
  opacity: 1;
}
.card-style-20 .grid-card-link {
  position: relative;
  z-index: 1;
}
.card-style-20 .grid-card-title {
  background: linear-gradient(45deg, #00ffaa, #00a2ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 15px rgba(0, 255, 170, 0.3));
}
.card-style-20 .grid-card-category {
  color: #00ffaa;
}
.card-style-20 .grid-card-summary { color: rgba(255,255,255,0.7); }
.card-style-20 .grid-card-meta { color: rgba(255,255,255,0.5); }
.card-style-20:hover {
  filter: brightness(1.1);
  box-shadow: 0 0 30px rgba(0, 255, 170, 0.15), 0 0 20px rgba(0, 0, 0, 0.3);
}
/* Corner elements */
.card-style-20 .card-corners {
  display: block;
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}
.card-corners span {
  position: absolute;
  width: 15px;
  height: 15px;
  border: 2px solid rgba(92, 103, 255, 0.3);
  transition: all 0.3s ease;
}
.card-corners span:nth-child(1) { top: 10px; left: 10px; border-right: 0; border-bottom: 0; }
.card-corners span:nth-child(2) { top: 10px; right: 10px; border-left: 0; border-bottom: 0; }
.card-corners span:nth-child(3) { bottom: 10px; left: 10px; border-right: 0; border-top: 0; }
.card-corners span:nth-child(4) { bottom: 10px; right: 10px; border-left: 0; border-top: 0; }
.card-style-20:hover .card-corners span {
  border-color: rgba(92, 103, 255, 0.8);
  box-shadow: 0 0 10px rgba(92, 103, 255, 0.5);
}
/* Scan line */
.card-style-20 .card-scanline {
  display: block;
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
  border-radius: inherit;
}
.card-scanline::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(to bottom, transparent, rgba(92, 103, 255, 0.1), transparent);
  transform: translateY(-100%);
  animation: cyberScanLine 2s linear infinite;
}
@keyframes cyberScanLine {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(100%); }
}
/* Cyber lines */
.card-style-20 .grid-card-body::before {
  content: '';
  position: absolute;
  top: 20%;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(92, 103, 255, 0.2), transparent);
  transform: scaleX(0);
  transform-origin: left;
  animation: lineGrow 3s linear infinite;
  z-index: 1;
  pointer-events: none;
}
.card-style-20 .grid-card-body::after {
  content: '';
  position: absolute;
  top: 60%;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(92, 103, 255, 0.2), transparent);
  transform: scaleX(0);
  transform-origin: right;
  animation: lineGrow 3s linear infinite 1.5s;
  z-index: 1;
  pointer-events: none;
}
@keyframes lineGrow {
  0% { transform: scaleX(0); opacity: 0; }
  50% { transform: scaleX(1); opacity: 1; }
  100% { transform: scaleX(0); opacity: 0; }
}
/* Cyber particles */
.card-cyber-particles {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
  border-radius: inherit;
}
.card-cyber-particles span {
  position: absolute;
  width: 3px;
  height: 3px;
  background: rgba(92, 103, 255, 0.6);
  border-radius: 50%;
  animation: cyberFloat 6s ease-in-out infinite;
}
.card-cyber-particles span:nth-child(1) { top: 15%; left: 20%; animation-delay: 0s; }
.card-cyber-particles span:nth-child(2) { top: 45%; left: 75%; animation-delay: 1s; }
.card-cyber-particles span:nth-child(3) { top: 70%; left: 30%; animation-delay: 2s; }
.card-cyber-particles span:nth-child(4) { top: 25%; left: 60%; animation-delay: 0.5s; }
.card-cyber-particles span:nth-child(5) { top: 80%; left: 85%; animation-delay: 1.5s; }
.card-cyber-particles span:nth-child(6) { top: 55%; left: 10%; animation-delay: 3s; }
@keyframes cyberFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.6; }
  50% { transform: translateY(-20px) scale(1.5); opacity: 1; }
}
/* Cyber glow element */
.card-cyber-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 120px;
  height: 120px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(92, 103, 255, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  z-index: 0;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.card-style-20:hover .card-cyber-glow { opacity: 1; }
/* Scoped display overrides for style-20 */
.card-style-20 .card-corners { display: block; }
.card-style-20 .card-scanline { display: block; }
.card-style-20 .card-cyber-particles { display: block; }
.card-style-20 .card-cyber-glow { display: block; }

/* ===== Card Style 21: 渐变边框光晕 ===== */
.card-style-21 {
  position: relative;
  background: #000;
  border: none;
  border-radius: 8px;
}
.card-style-21::before {
  content: '';
  position: absolute;
  inset: -5px;
  border-radius: 10px;
  background: linear-gradient(-45deg, #e81cff 0%, #40c9ff 100%);
  z-index: 0;
  pointer-events: none;
  transition: all 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.card-style-21::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 8px;
  background: linear-gradient(-45deg, #fc00ff 0%, #00dbde 100%);
  transform: scale(0.95);
  filter: blur(20px);
  z-index: 0;
  pointer-events: none;
  transition: filter 0.3s ease;
}
.card-style-21 .grid-card-link {
  position: relative;
  z-index: 1;
  background: #000;
  border-radius: 8px;
}
.card-style-21 .grid-card-title { color: #fff; }
.card-style-21 .grid-card-summary { color: rgba(255,255,255,0.8); }
.card-style-21 .grid-card-meta { color: rgba(255,255,255,0.6); }
.card-style-21 .grid-card-category { color: #e81cff; font-weight: 600; }
.card-style-21 .grid-card-cover { background: rgba(255,255,255,0.1); }
.card-style-21:hover::before {
  transform: rotate(-90deg) scaleX(1.34) scaleY(0.77);
}
.card-style-21:hover::after {
  filter: blur(30px);
}

/* ===== Common: hide extra elements by default ===== */
.card-tab,
.card-btn,
.card-corners,
.card-scanline,
.card-flip-back,
.card-flip-glow,
.card-flip-circles,
.card-flip-icon,
.card-flip-hover-text,
.card-cyber-particles,
.card-cyber-glow,
.card-light-streak,
.card-edu-overlay,
.card-edu-circle,
.card-ticket-notes,
.card-ticket-barcode,
.card-ticket-filter { display: none; }

/* ===== Card Style 22: 教育增强版 ===== */
.card-style-22 {
  background: #fff;
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
  border-top-left-radius: 10px;
  border-bottom-left-radius: 0;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease-out;
}
.card-style-22 .card-edu-overlay {
  display: block;
  position: absolute;
  width: 131px;
  height: 131px;
  border-radius: 50%;
  background: #ffd861;
  top: 70px;
  left: 50px;
  z-index: 0;
  transition: transform 0.3s ease-out;
}
.card-style-22 .card-edu-circle {
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  width: 131px;
  height: 131px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid #ffd861;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1;
  transition: all 0.3s ease-out;
}
.card-style-22 .card-edu-circle::after {
  content: '';
  width: 118px;
  height: 118px;
  position: absolute;
  background: #ffd861;
  border-radius: 50%;
  transition: opacity 0.3s ease-out;
}
.card-edu-circle svg {
  z-index: 1;
  position: relative;
}
.card-style-22 .grid-card-link {
  position: relative;
  z-index: 1;
}
.card-style-22 .grid-card-body {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  padding: 1rem;
}
.card-style-22 .grid-card-title {
  font-size: 1rem;
  color: #4C5656;
  transition: color 0.3s ease-out;
}
.card-style-22:hover {
  transform: translateY(-5px) scale(1.005);
  box-shadow: 0 24px 36px rgba(0,0,0,0.11), 0 24px 46px rgba(255, 215, 97, 0.48);
  border-color: #ffd861;
}
.card-style-22:hover .card-edu-overlay {
  transform: scale(4);
}
.card-style-22:hover .card-edu-circle {
  border-color: #ffeeba;
  background: #ffd861;
}
.card-style-22:hover .card-edu-circle::after {
  background: #ffeeba;
}
.card-style-22:hover .grid-card-title {
  color: #fff;
}

/* ===== Card Style 23: 夜空 ===== */
.card-style-23 {
  background: linear-gradient(45deg, #000000, #0a0a2e);
  border: none;
  border-radius: 15px;
  box-shadow: 0 0 20px rgba(0, 0, 255, 0.1);
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}
.card-style-23::before {
  content: '';
  position: absolute;
  height: 60px;
  width: 60px;
  background: linear-gradient(145deg, #f0f0f0, #ffffff);
  border-radius: 50%;
  right: 40px;
  top: 15px;
  box-shadow: 0 0 40px rgba(235, 235, 235, 0.5), inset -5px -5px 15px rgba(0, 0, 0, 0.2);
  z-index: 0;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.card-style-23::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1px 1px at 20% 30%, rgba(255,255,255,0.8), transparent),
    radial-gradient(1px 1px at 40% 70%, rgba(173,216,230,0.6), transparent),
    radial-gradient(1px 1px at 60% 20%, rgba(255,255,255,0.7), transparent),
    radial-gradient(1px 1px at 80% 50%, rgba(173,216,230,0.5), transparent),
    radial-gradient(1px 1px at 10% 80%, rgba(255,255,255,0.6), transparent),
    radial-gradient(1px 1px at 70% 90%, rgba(173,216,230,0.7), transparent),
    radial-gradient(1px 1px at 30% 10%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 90% 40%, rgba(173,216,230,0.6), transparent),
    radial-gradient(1px 1px at 50% 60%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1px 1px at 15% 55%, rgba(173,216,230,0.5), transparent);
  z-index: 0;
  pointer-events: none;
}
.card-style-23 .grid-card-link { position: relative; z-index: 1; }
.card-style-23 .grid-card-title { color: #e0e0e0; }
.card-style-23 .grid-card-summary { color: #a0aec0; }
.card-style-23 .grid-card-meta { color: #718096; }
.card-style-23 .grid-card-category { color: #e0e0e0; }
.card-style-23 .grid-card-cover { background: rgba(255,255,255,0.03); }
.card-style-23:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(100, 149, 237, 0.3);
}
.card-style-23:hover::before {
  box-shadow: 0 0 60px rgba(173, 216, 230, 0.7), inset -8px -8px 20px rgba(0, 0, 0, 0.3);
  background: linear-gradient(145deg, #e0ffff, #ffffff);
}
.card-style-23:hover .card-bg .blub {
  animation-duration: calc(20s / var(--i));
}

/* ===== Card Style 24: 增强门票 ===== */
.card-style-24 {
  position: relative;
  overflow: hidden;
  background: transparent;
  border: none;
  padding: 12px 0;
  filter: drop-shadow(0 2px 1px rgba(0,0,0,0.15)) drop-shadow(0 4px 3px rgba(0,0,0,0.12))
    drop-shadow(0 10px 9px rgba(0,0,0,0.1)) drop-shadow(0 20px 18px rgba(0,0,0,0.06));
  transition: transform 0.5s, filter 0.5s;
}
.card-style-24::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background-image:
    linear-gradient(to bottom, rgba(255,238,170,0.45), 90%, rgba(0,0,0,0.08)),
    conic-gradient(
      at 60% 50%,
      #ccc, #ff6bfe, #00f9f8, #ddd, #0081fd, #eef0bc,
      #0081fd, #ff6bfe, rgba(0,0,0,0.1), #0081fd, #ddd, #01fefb, #ccc
    );
  z-index: 0;
}
.card-style-24::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: #fff;
  z-index: 0;
  mask:
    radial-gradient(circle 6px at 10% 0%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 25% 0%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 40% 0%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 55% 0%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 70% 0%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 85% 0%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 10% 100%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 25% 100%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 40% 100%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 55% 100%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 70% 100%, #fff0 100%, #000 100%),
    radial-gradient(circle 6px at 85% 100%, #fff0 100%, #000 100%),
    radial-gradient(circle 8px at left center, #000 98%, #0000 100%),
    radial-gradient(circle 8px at right center, #000 98%, #0000 100%);
  mask-composite: exclude;
  -webkit-mask-composite: destination-out;
}
.card-style-24 .card-ticket-notes {
  display: block;
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}
.card-ticket-notes span {
  position: absolute;
  left: 0;
  right: 0;
  font-size: 4rem;
  color: #e7e7e7;
  mix-blend-mode: color-burn;
  overflow: hidden;
}
.card-ticket-notes span:nth-child(1) { transform: translateY(20%); }
.card-ticket-notes span:nth-child(2) { transform: translateY(45%); }
.card-ticket-notes span:nth-child(3) { transform: translateY(70%); }
.card-style-24 .card-ticket-barcode {
  display: block;
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 24px;
  z-index: 1;
  box-shadow:
    0px 0 0 1px #000, 5px 0 0 1px #000, 7px 0 0 1px #000,
    11px 0 0 1px #000, 15px 0 0 1px #000, 16px 0 0 1px #000,
    22px 0 0 1px #000, 27px 0 0 1px #000, 30px 0 0 1px #000,
    35px 0 0 1px #000, 36px 0 0 1px #000, 39px 0 0 1px #000,
    43px 0 0 1px #000, 47px 0 0 1px #000, 50px 0 0 1px #000,
    55px 0 0 1px #000, 59px 0 0 1px #000, 60px 0 0 1px #000,
    64px 0 0 1px #000, 69px 0 0 1px #000, 70px 0 0 1px #000,
    74px 0 0 1px #000;
}
.card-style-24 .grid-card-link { position: relative; z-index: 1; }
.card-style-24 .grid-card-title { color: #1a1a2e; font-weight: 700; }
.card-style-24 .grid-card-summary { color: rgba(0,0,0,0.65); }
.card-style-24 .grid-card-meta { color: rgba(0,0,0,0.55); }
.card-style-24 .grid-card-category { color: #0081fd; font-weight: 600; }
.card-style-24 .grid-card-cover { background: rgba(255,255,255,0.15); }
.card-style-24:hover {
  transform: translateY(-7px) scale(1.02);
  filter: drop-shadow(0 4px 3px rgba(0,0,0,0.1)) drop-shadow(0 6px 6px rgba(0,0,0,0.1))
    drop-shadow(0 16px 14px rgba(0,0,0,0.08)) drop-shadow(0 25px 20px rgba(0,0,0,0.05));
}

/* ===== Card Style 25: 3D倾斜 ===== */
.card-style-25 {
  transform: perspective(800px) rotate3d(1, -1, 1, 15deg);
  background: linear-gradient(to bottom right, #38bdf8, #0ea5e9, #0369a1);
  border-color: #525252;
  box-shadow: 12px 12px 0 0 #0d0d0d;
  transition: transform 0.5s ease, box-shadow 0.5s ease;
}
.card-style-25 .grid-card-link { position: relative; z-index: 1; }
.card-style-25 .grid-card-title { color: #fff; }
.card-style-25 .grid-card-summary { color: rgba(255,255,255,0.8); }
.card-style-25 .grid-card-meta { color: rgba(255,255,255,0.6); }
.card-style-25 .grid-card-category { color: #ffd700; }
.card-style-25 .grid-card-cover { background: rgba(255,255,255,0.1); }
.card-style-25:hover {
  transform: perspective(800px) rotate3d(0, 0, 0, 0deg) scale(1.05);
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}

/* ===== Card Style 26: 光条扫过 ===== */
.card-style-26 {
  background: #171717;
  border-color: transparent;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 0 3px 1px rgba(0,0,0,0.5);
}
.card-style-26::before {
  content: '';
  position: absolute;
  width: 80px;
  height: 360px;
  background: linear-gradient(#ff2288, #387ef0);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(0deg);
  opacity: 0;
  transition: opacity 300ms;
  animation: glowRotate26 8s linear infinite;
  animation-play-state: paused;
  z-index: 0;
}
.card-style-26::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(23, 23, 23, 0.2);
  backdrop-filter: blur(50px);
  z-index: 0;
}
/* Light streak effect */
.card-style-26 .card-light-streak {
  position: absolute;
  width: 5px;
  height: 50px;
  background: #fff;
  filter: blur(50px);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 0;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s;
}
.card-style-26:hover .card-light-streak { opacity: 1; }
.card-style-26 .card-light-streak { display: block; }
.card-style-26 .grid-card-link { position: relative; z-index: 1; }
.card-style-26 .grid-card-title { color: #fff; }
.card-style-26 .grid-card-summary { color: rgba(255,255,255,0.7); }
.card-style-26 .grid-card-meta { color: rgba(255,255,255,0.5); }
.card-style-26 .grid-card-category { color: #ff2288; }
.card-style-26 .grid-card-cover { background: rgba(255,255,255,0.05); }
.card-style-26:hover::before {
  opacity: 1;
  animation-play-state: running;
}
.card-style-26:hover {
  border-color: transparent;
}
@keyframes glowRotate26 {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}

/* ===== Card Style 27: 霓虹缩放 ===== */
.card-style-27 {
  background: linear-gradient(163deg, #00ff75, #3700ff);
  border: none;
  padding: 3px;
  transition: box-shadow 0.3s;
}
.card-style-27 .grid-card-link {
  position: relative;
  z-index: 1;
  background: #1a1a1a;
  border-radius: 20px;
  transition: transform 0.2s;
}
.card-style-27:hover {
  box-shadow: 0 0 30px 1px rgba(0, 255, 117, 0.3);
}
.card-style-27:hover .grid-card-link {
  transform: scale(0.98);
}

/* Style 28: Glassmorphism */
.card-style-28 {
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s;
}
.card-style-28:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  transform: translateY(-4px);
}

/* Style 29: Aurora */
.card-style-29 {
  position: relative;
  border: none;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}
.card-style-29::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #0a2e38, #1a0533, #0a2e38);
  z-index: 0;
}
.card-style-29::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0,198,255,0.25), rgba(123,47,247,0.25), rgba(255,110,199,0.25), rgba(0,198,255,0.25));
  background-size: 300% 300%;
  animation: auroraFlow 6s ease infinite;
  opacity: 0.7;
  z-index: 0;
}
@keyframes auroraFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.card-style-29 .grid-card-link { position: relative; z-index: 1; }
.card-style-29 .grid-card-title { color: #e0f0ff; }
.card-style-29 .grid-card-summary { color: rgba(200, 220, 255, 0.7); }
.card-style-29 .grid-card-meta { color: rgba(200, 220, 255, 0.5); }
.card-style-29:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(123, 47, 247, 0.3);
}

/* Style 30: Retro Pixel */
.card-style-30 {
  border: 4px solid #333;
  border-radius: 0;
  box-shadow: inset -4px -4px 0 #666, inset 4px 4px 0 #e0e0e0;
  background: #d4d4d4;
  image-rendering: pixelated;
  transition: box-shadow 0.2s;
}
.card-style-30:hover {
  box-shadow: inset -4px -4px 0 #555, inset 4px 4px 0 #f0f0f0, 8px 8px 0 rgba(0,0,0,0.2);
}
.card-style-30 .grid-card-title { font-family: 'Courier New', monospace; font-weight: 900; }

/* Style 31: Water Ripple */
.card-style-31 {
  position: relative;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}
.card-style-31::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #e8f4f8, #d0e8f0);
  z-index: 0;
  transition: opacity 0.3s;
}
.card-style-31 .grid-card-link { position: relative; z-index: 1; }
.card-style-31 .grid-card-title { color: #1a5276; }
.card-style-31 .grid-card-summary { color: #2e86c1; }
.card-style-31:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(46, 134, 193, 0.2);
}
.card-style-31:hover::before {
  background: linear-gradient(135deg, #d0e8f0, #b8d4e3);
}

/* Style 32: Minimal Line */
.card-style-32 {
  border: 1px solid #eee;
  position: relative;
  transition: all 0.3s;
}
.card-style-32::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 4px;
  background: #333;
  border-radius: 17px 17px 0 0;
  z-index: 1;
}
.card-style-32:hover {
  border-color: #ddd;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.card-style-32:hover::before {
  background: #000;
}

/* Style 33: Sunset */
.card-style-33 {
  position: relative;
  border: none;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}
.card-style-33::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #1a0a2e 0%, #ff6b35 50%, #f7931e 75%, #ffd700 100%);
  z-index: 0;
  opacity: 0.9;
}
.card-style-33 .grid-card-link { position: relative; z-index: 1; }
.card-style-33 .grid-card-title { color: #fff; }
.card-style-33 .grid-card-summary { color: rgba(255,255,255,0.85); }
.card-style-33 .grid-card-meta { color: rgba(255,255,255,0.65); }
.card-style-33:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(255, 107, 53, 0.3);
}

/* Style 34: Frosted Edge */
.card-style-34 {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  box-shadow: inset 0 0 30px rgba(200, 200, 200, 0.3);
  transition: all 0.3s;
}
.card-style-34:hover {
  box-shadow: inset 0 0 40px rgba(200, 200, 200, 0.4), 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

/* Style 35: Magnetic */
.card-style-35 {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s;
}
.card-style-35 .grid-card-title { color: #fff; }
.card-style-35 .grid-card-summary { color: rgba(255,255,255,0.85); }
.card-style-35 .grid-card-meta { color: rgba(255,255,255,0.65); }
.card-style-35:hover {
  transform: scale(1.06);
  box-shadow: 0 16px 40px rgba(102, 126, 234, 0.4);
}

/* Style 36: Origami */
.card-style-36 {
  background: #f5f0e8;
  border: 1px solid #d4c5a9;
  position: relative;
  transition: transform 0.3s, box-shadow 0.3s;
}
.card-style-36::after {
  content: '';
  position: absolute;
  bottom: 0; right: 0;
  width: 60px; height: 60px;
  background: linear-gradient(135deg, transparent 50%, #e8dcc8 50%);
  box-shadow: -3px -3px 6px rgba(0, 0, 0, 0.08);
  z-index: 1;
  transition: width 0.3s, height 0.3s;
}
.card-style-36 .grid-card-title { color: #4a3728; font-family: Georgia, serif; }
.card-style-36 .grid-card-summary { color: #7a6a5a; }
.card-style-36:hover {
  transform: translateY(-4px);
  box-shadow: 8px 8px 0 rgba(0, 0, 0, 0.08);
}
.card-style-36:hover::after {
  width: 80px;
  height: 80px;
}

/* Style 37: Matrix */
.card-style-37 {
  background: #0a0a0a;
  border: 1px solid rgba(0, 255, 0, 0.3);
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}
.card-style-37::before {
  content: '01001 10110 01101 11010 00111 10100 01011 11001 00101 10111';
  position: absolute;
  inset: 0;
  color: rgba(0, 255, 0, 0.08);
  font-size: 14px;
  font-family: monospace;
  line-height: 1.5;
  word-break: break-all;
  overflow: hidden;
  z-index: 0;
}
.card-style-37 .grid-card-link { position: relative; z-index: 1; }
.card-style-37 .grid-card-title { color: #0f0; text-shadow: 0 0 10px rgba(0,255,0,0.5); }
.card-style-37 .grid-card-summary { color: rgba(0,255,0,0.6); }
.card-style-37 .grid-card-meta { color: rgba(0,255,0,0.4); }
.card-style-37 .grid-card-category { color: #0f0; }
.card-style-37:hover {
  border-color: rgba(0, 255, 0, 0.6);
  box-shadow: 0 0 20px rgba(0, 255, 0, 0.15), inset 0 0 20px rgba(0, 255, 0, 0.05);
}
.card-style-37:hover::before {
  color: rgba(0, 255, 0, 0.12);
}

/* Style 38: Vaporwave */
.card-style-38 {
  position: relative;
  border: none;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}
.card-style-38::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #2d1b69, #ff71ce, #01cdfe);
  z-index: 0;
}
.card-style-38::after {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(0deg, transparent, transparent 10px, rgba(255,255,255,0.04) 10px, rgba(255,255,255,0.04) 11px);
  z-index: 0;
}
.card-style-38 .grid-card-link { position: relative; z-index: 1; }
.card-style-38 .grid-card-title { color: #ff71ce; text-shadow: 0 0 12px rgba(255,113,206,0.5); }
.card-style-38 .grid-card-summary { color: rgba(1,205,254,0.8); }
.card-style-38 .grid-card-meta { color: rgba(5,255,161,0.6); }
.card-style-38 .grid-card-category { color: #ff71ce; }
.card-style-38:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(255, 113, 206, 0.3);
}

/* Style 39: Circuit */
.card-style-39 {
  background: #0d2818;
  border: 2px solid #2d8b4e;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}
.card-style-39::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(45,139,78,0.15) 1px, transparent 1px),
              linear-gradient(0deg, rgba(45,139,78,0.15) 1px, transparent 1px);
  background-size: 16px 16px;
  z-index: 0;
}
.card-style-39 .grid-card-link { position: relative; z-index: 1; }
.card-style-39 .grid-card-title { color: #4ade80; }
.card-style-39 .grid-card-summary { color: rgba(74,222,128,0.7); }
.card-style-39 .grid-card-meta { color: rgba(74,222,128,0.5); }
.card-style-39 .grid-card-category { color: #4ade80; }
.card-style-39:hover {
  border-color: #4ade80;
  box-shadow: 0 0 20px rgba(74, 222, 128, 0.15), inset 0 0 20px rgba(74, 222, 128, 0.05);
}

@media (max-width: 768px) {
  .search-inputs {
    flex-wrap: wrap;
  }
  .cir-search {
    min-width: 0;
    flex: 1;
  }
  .cir-search__kbd {
    display: none;
  }
  .search-btn {
    padding: 0.5rem 0.85rem;
    font-size: 0.8rem;
  }
  .clear-btn {
    padding: 0.5rem 0.65rem;
    font-size: 0.8rem;
  }
  .article-card-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 0.75rem;
  }
  .grid-card {
    border-radius: 10px;
  }
  .grid-card-cover {
    height: 100px;
    border-radius: 10px 10px 0 0;
  }
  .grid-card-link {
    padding: 0.65rem;
  }
  .grid-card-title {
    font-size: 0.82rem;
    -webkit-line-clamp: 2;
  }
  .grid-card-summary {
    font-size: 0.72rem;
    -webkit-line-clamp: 2;
  }
  .grid-card-meta {
    font-size: 0.72rem;
    gap: 0.4rem;
  }
  .grid-card-number {
    font-size: 1.2rem;
  }
  .filter-panel {
    padding: 0.75rem;
  }
  .filter-row {
    flex-direction: column;
    gap: 0.5rem;
  }
  .filter-group {
    flex-wrap: wrap;
  }
  .filter-label { white-space: normal; }
  .page-title {
    font-size: 1.1rem;
  }
  .result-count {
    font-size: 0.78rem;
  }
  .pagination {
    flex-wrap: wrap;
    gap: 0.25rem;
  }
  .page-num-btn, .page-nav-btn {
    min-width: 32px;
    height: 32px;
    font-size: 0.78rem;
  }
}
@media (max-width: 480px) {
  .article-card-grid {
    grid-template-columns: 1fr 1fr;
    gap: 0.6rem;
  }
  .view-mode-toggle {
    display: none;
  }
  .page-size-select {
    display: none;
  }
}
</style>

<style>
/* Night mode overrides (unscoped to match parent .night class) */
.night .cir-search__icon { color: #a0aec0; }
.night .cir-search__field { color: #e0e0e0; }
.night .cir-search__field::placeholder { color: #718096; }
.night .cir-search__kbd {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  color: #a0aec0;
}
.night .cir-search {
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.05);
}
.night .cir-search:focus-within {
  border-color: #5a9bff;
  box-shadow: 0 0 0 3px rgba(90, 155, 255, 0.15);
}
.night .cir-search.ai-active {
  border-color: #60a5fa;
}
.night .cir-search.ai-active:focus-within {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(167, 139, 250, 0.2);
}
/* Pagination night */
.night .pagination { border-top-color: rgba(255, 255, 255, 0.08); }
.night .page-nav-btn {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #a0aec0;
}
.night .page-nav-btn:hover:not(:disabled) { border-color: #5a9bff; color: #5a9bff; }
.night .page-num-btn { color: #a0aec0; }
.night .page-num-btn:hover { background: rgba(255, 255, 255, 0.08); }
.night .page-num-btn.active { background: #5a9bff; border-color: #5a9bff; color: #fff; }
.night .page-total { color: #718096; }
/* Filter night */
.night .filter-toggle {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #a0aec0;
}
.night .filter-toggle:hover,
.night .filter-toggle.active {
  border-color: #5a9bff;
  color: #5a9bff;
  background: rgba(90, 155, 255, 0.1);
}
.night .filter-badge { background: #5a9bff; }
.night .filter-panel {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
}
.night .filter-label { color: #a0aec0; }
/* Page title night */
.night .page-title { color: #e0e0e0; }
/* Buttons night */
.night .search-btn { background: #2b78c8; }
.night .search-btn:hover:not(:disabled) { background: #1e5eb6; }
.night .clear-btn {
  border-color: rgba(255, 255, 255, 0.12);
  color: #a0aec0;
}
.night .clear-btn:hover {
  border-color: #5a9bff;
  color: #e0e0e0;
}
/* Result / hint night */
.night .result-count { color: #a0aec0; }
.night .ai-hint { color: #a0aec0; }
/* Article card night */
.night .article-card {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.08);
  position: relative;
  overflow: hidden;
}
.night .article-card::before {
  display: none;
}
.night .article-card:hover {
  border-color: rgba(90, 155, 255, 0.25);
  box-shadow: 0 10px 30px rgba(100, 149, 237, 0.3);
}
.night .article-card:hover .moon {
  box-shadow: 0 0 60px rgba(173, 216, 230, 0.7), inset -8px -8px 20px rgba(0, 0, 0, 0.3);
  background: linear-gradient(145deg, #e0ffff, #ffffff);
}
.night .article-card:hover .blub {
  animation-duration: calc(30s / var(--i));
  opacity: 1;
}

/* Card background - hidden by default */
.card-bg { display: none; }

/* Night sky card background — list view always, card view default style only */
.night .article-card .card-bg,
.night .card-style-0 .card-bg {
  display: block;
  position: absolute;
  inset: 0;
  background: linear-gradient(45deg, #000000, #0a0a2e);
  border-radius: inherit;
  overflow: hidden;
  z-index: 0;
}
.night .article-meta,
.night .article-title,
.night .article-summary,
.night .article-tags {
  position: relative;
  z-index: 1;
}

/* Moon */
.night .moon {
  height: 80px;
  width: 80px;
  background: linear-gradient(145deg, #f0f0f0, #ffffff);
  border-radius: 50%;
  position: absolute;
  right: 55px;
  top: 9px;
  box-shadow: 0 0 40px rgba(235, 235, 235, 0.5), inset -5px -5px 15px rgba(0, 0, 0, 0.2);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Moon Craters */
.night .crater {
  position: absolute;
  background: rgba(200, 200, 200, 0.3);
  border-radius: 50%;
  box-shadow: inset 2px 2px 5px rgba(0, 0, 0, 0.1);
}
.night .cr1 { width: 15px; height: 15px; top: 20px; left: 15px; }
.night .cr2 { width: 20px; height: 20px; top: 45px; left: 40px; }
.night .cr3 { width: 12px; height: 12px; top: 55px; left: 20px; }

/* Falling Stars */
.night .blub {
  height: calc(3px * var(--j));
  width: calc(1px * var(--j));
  background: linear-gradient(90deg, rgba(255, 255, 255, 1) 0%, rgba(173, 216, 230, 1) 100%);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.8), 0 0 30px rgba(173, 216, 230, 0.6);
  animation: starFall linear infinite reverse;
  animation-duration: calc(40s / var(--i));
  rotate: 25deg;
  opacity: 0.8;
  filter: blur(calc(0.5px * var(--j)));
}
@keyframes starFall {
  0% { transform: translateY(250px) scale(0.3) rotate(25deg); }
  100% { transform: translateY(-40px) scale(1.2) rotate(25deg); }
}
.night .article-meta { color: #a0aec0; }
.night .category { color: #e0e0e0; }
.night .article-title { color: #e0e0e0; }
.night .article-title:hover { color: #ffffff; }
.night .article-summary { color: #a0aec0; }
.night .tag {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #a0aec0;
}
.night .tag:hover { border-color: #5a9bff; color: #5a9bff; }
/* Page size label night */
.night .page-size-label { color: #718096; }
/* View mode toggle night */
.night .view-mode-btn {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #a0aec0;
}
.night .view-mode-btn + .view-mode-btn {
  border-left-color: rgba(255, 255, 255, 0.12);
}
.night .view-mode-btn:hover {
  color: #5a9bff;
  background: rgba(90, 155, 255, 0.1);
}
.night .view-mode-btn.active {
  background: #5a9bff;
  color: #fff;
}
/* Card grid night — default card style gets dark mode */
.night .grid-card.card-style-0 {
  background: transparent !important;
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 20px rgba(0, 0, 255, 0.1);
}
.night .grid-card.card-style-0:hover {
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: 0 10px 30px rgba(100, 149, 237, 0.3);
}
.night .grid-card.card-style-0 .card-bg {
  display: block !important;
  position: absolute;
  inset: 0;
  background: linear-gradient(45deg, #000000, #0a0a2e);
  border-radius: inherit;
  overflow: hidden;
  z-index: 0;
}
.night .grid-card .grid-card-link {
  position: relative;
  z-index: 1;
}
.night .grid-card:hover .moon {
  box-shadow: 0 0 60px rgba(173, 216, 230, 0.7), inset -8px -8px 20px rgba(0, 0, 0, 0.3);
  background: linear-gradient(145deg, #e0ffff, #ffffff);
}
.night .grid-card:hover .blub {
  animation-duration: calc(30s / var(--i));
  opacity: 1;
}
.night .grid-card .grid-card-title { color: #e0e0e0; }
.night .grid-card .grid-card-summary { color: #a0aec0; }
.night .grid-card .grid-card-meta { color: #718096; }
.night .grid-card .grid-card-category { color: #e0e0e0; }
.night .grid-card .grid-card-cover { background: rgba(255, 255, 255, 0.03); }
/* Non-default card styles keep their original appearance, interactions, and effects in dark mode */

/* Ticket styles: keep dark text on white background in night mode */
.night .card-style-10 .grid-card-title,
.night .card-style-24 .grid-card-title { color: #1a1a2e !important; }
.night .card-style-10 .grid-card-summary,
.night .card-style-24 .grid-card-summary { color: rgba(0,0,0,0.65) !important; }
.night .card-style-10 .grid-card-meta,
.night .card-style-24 .grid-card-meta { color: rgba(0,0,0,0.55) !important; }
.night .card-style-10 .grid-card-category,
.night .card-style-24 .grid-card-category { color: #0081fd !important; }
</style>
