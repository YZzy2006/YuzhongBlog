<template>
  <div class="photowall-page" :class="{ 'is-night': isNight }">
    <!-- Header -->
    <div class="pw-header">
      <h1 class="pw-title">{{ $t('photowall.title') }}</h1>
      <p class="pw-subtitle">{{ $t('photowall.desc') }}</p>

      <!-- Search -->
      <div class="pw-search-row">
        <div class="pw-search" :class="{ 'pw-search--focus': searchFocused }">
          <svg class="pw-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="m21 21-4.34-4.34"></path>
          </svg>
          <input v-if="!aiMode" v-model="searchQuery" class="pw-search__field" type="search"
            :placeholder="$t('photowall.searchPlaceholder')"
            @focus="searchFocused = true" @blur="searchFocused = false" />
          <input v-else v-model="aiQuery" class="pw-search__field" type="text"
            :placeholder="$t('photowall.aiSearchPlaceholder')"
            @focus="searchFocused = true" @blur="searchFocused = false"
            @keyup.enter="doAiSearch" />
          <button class="pw-ai-toggle" :class="{ active: aiMode }" @click="toggleAiMode" :title="$t('photowall.aiSearch')">
            <svg v-if="!aiSearching" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M12 2l2.09 6.26L21 9.27l-5 4.87L17.18 21 12 17.27 6.82 21 8 14.14l-5-4.87 6.91-1.01z"/></svg>
            <div v-else class="sun-loader"><div class="sun-loader-inner"></div></div>
          </button>
          <button v-if="aiMode" class="pw-ai-search-btn" @click="doAiSearch" :disabled="aiSearching">{{ $t('photowall.aiSearch') }}</button>
          <button v-if="searchQuery || aiQuery" class="pw-search__clear" @click="clearSearch">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <button class="pw-filter-toggle" :class="{ active: showFilters || hasFilters }" @click="showFilters = !showFilters">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
          {{ $t('photowall.filter') }}
          <span v-if="hasFilters" class="pw-filter-badge"></span>
        </button>
      </div>

      <!-- Collapsible Filter Panel -->
      <Transition name="filter-slide">
        <div v-if="showFilters" class="pw-filter-panel">
          <div class="pw-filter-row">
            <label class="pw-filter-label">{{ $t('photowall.dateFrom') }}</label>
            <DatePicker v-model="dateFrom" :placeholder="$t('photowall.dateFrom')" :max="dateTo || undefined" @change="currentAlbumPage = 1" />
          </div>
          <div class="pw-filter-row">
            <label class="pw-filter-label">{{ $t('photowall.dateTo') }}</label>
            <DatePicker v-model="dateTo" :placeholder="$t('photowall.dateTo')" :min="dateFrom || undefined" @change="currentAlbumPage = 1" />
          </div>
          <button v-if="hasFilters" class="pw-clear-btn" @click="clearFilters">{{ $t('photowall.clear') }}</button>
        </div>
      </Transition>
      <p v-if="aiMode" class="pw-ai-tip">{{ $t('photowall.aiSearchTip') }}</p>
    </div>

    <!-- Album list view -->
    <div v-if="!currentAlbum" class="albums-section" :class="{ 'is-transitioning': isTransitioning }">

      <!-- Search results: matched photos -->
      <div v-if="activeQuery && matchedPhotos.length > 0" class="search-results">
        <div class="section-header">
          <span class="section-bar"></span>
          <span>{{ $t('photowall.matchedPhotos') }} ({{ matchedPhotos.length }})</span>
        </div>
        <div class="photos-masonry">
          <div v-for="(photo, index) in matchedPhotos" :key="photo.id + '-search'" class="photo-item"
            :style="{ animationDelay: (index * 50) + 'ms' }"
            @click="openAlbumById(photo.albumId)">
            <img :src="ossImg(photo.url)" :alt="photo.caption || ''" loading="lazy" />
            <div class="photo-overlay">
              <span class="photo-overlay__album">{{ photo.albumName }}</span>
              <p v-if="photo.caption" class="photo-overlay__caption">{{ photo.caption }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading skeleton -->
      <div v-if="loading" class="albums-grid">
        <div v-for="i in 6" :key="i" class="album-stack skeleton">
          <div class="album-stack__layer album-stack__back skeleton-cover"></div>
          <div class="album-stack__layer album-stack__mid skeleton-cover"></div>
          <div class="album-stack__layer album-stack__front skeleton-cover"></div>
        </div>
      </div>

      <!-- Result count -->
      <div v-if="!loading && filteredAlbums.length > 0" class="pw-result-count">
        {{ filteredAlbums.length }} {{ $t('photowall.photos') }}
      </div>

      <!-- Albums -->
      <div v-if="paginatedAlbums.length > 0" class="albums-grid">
        <div v-for="album in paginatedAlbums" :key="album.id" class="album-card"
          @click="openAlbum(album)">
          <!-- Stacked polaroid effect -->
          <div class="album-stack">
            <div class="album-stack__layer album-stack__back">
              <img v-if="album.photos && album.photos[2]" :src="ossImg(album.photos[2].url)" alt="" loading="lazy" />
              <div v-else class="album-stack__placeholder"></div>
            </div>
            <div class="album-stack__layer album-stack__mid">
              <img v-if="album.photos && album.photos[1]" :src="ossImg(album.photos[1].url)" alt="" loading="lazy" />
              <div v-else class="album-stack__placeholder"></div>
            </div>
            <div class="album-stack__layer album-stack__front">
              <img :src="ossImg(album.coverUrl) || defaultCover" :alt="album.name" loading="lazy" />
              <div class="album-stack__overlay">
                <span class="album-stack__overlay-count">{{ album.photoCount || 0 }} {{ $t('photowall.photos') }}</span>
                <span class="album-stack__overlay-hint">{{ $t('photowall.clickToOpen') }}</span>
              </div>
            </div>
          </div>
          <div class="album-meta">
            <div class="album-name-row">
              <h3 class="album-name">{{ album.name }}</h3>
              <span class="album-date">{{ formatDate(album.createdAt) }}</span>
            </div>
            <p v-if="album.description" class="album-desc">{{ album.description }}</p>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="!loading && totalAlbumPages > 1" class="pw-pagination">
        <button class="pw-page-btn" :disabled="currentAlbumPage <= 1" @click="currentAlbumPage--">{{ $t('photowall.prev') }}</button>
        <span class="pw-page-info">{{ $t('photowall.pageInfo', { current: currentAlbumPage, total: totalAlbumPages }) }}</span>
        <button class="pw-page-btn" :disabled="currentAlbumPage >= totalAlbumPages" @click="currentAlbumPage++">{{ $t('photowall.next') }}</button>
      </div>

      <!-- Empty state -->
      <div v-else-if="!loading && paginatedAlbums.length === 0" class="empty-state">
        <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
        </svg>
        <p>{{ searchQuery ? $t('photowall.noResults') : $t('photowall.noAlbums') }}</p>
      </div>
    </div>

    <!-- Album detail view (photos) -->
    <div v-else class="photos-section">
      <div class="photos-header">
        <button class="back-btn" @click="closeAlbum">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>
          </svg>
          {{ $t('photowall.backToList') }}
        </button>
        <span class="photos-header__dot">&middot;</span>
        <span class="photos-header__date">{{ formatDate(currentAlbum.createdAt) }}</span>
      </div>

      <div class="album-detail-header">
        <h2 class="album-detail-title">{{ currentAlbum.name }}</h2>
        <p v-if="currentAlbum.description" class="album-detail-desc">{{ currentAlbum.description }}</p>
        <span class="album-detail-count">
          {{ $t('photowall.totalPhotos') }} <strong>{{ photos.length }}</strong> {{ $t('photowall.photosUnit') }}
        </span>
      </div>

      <!-- Loading -->
      <div v-if="photosLoading" class="photos-masonry">
        <div v-for="i in 8" :key="i" class="photo-item skeleton">
          <div class="skeleton-photo" :style="{ height: (150 + (i * 37) % 150) + 'px' }"></div>
        </div>
      </div>

      <!-- Masonry photos -->
      <div v-else-if="photos.length > 0" class="photos-masonry">
        <div v-for="(photo, index) in photos" :key="photo.id" class="photo-item"
          :style="{ animationDelay: (index * 50) + 'ms' }"
          @click="openLightbox(index)">
          <img :src="ossImg(photo.url)" :alt="photo.caption || ''" loading="lazy" />
          <div class="photo-overlay">
            <p v-if="photo.caption" class="photo-overlay__caption">{{ photo.caption }}</p>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p>{{ $t('photowall.noPhotos') }}</p>
      </div>
    </div>

    <!-- Lightbox -->
    <Teleport to="body">
      <Transition name="lightbox">
        <div v-if="lightboxOpen" class="lightbox" @click.self="closeLightbox">
          <button class="lightbox-close" @click="closeLightbox">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>

          <button v-if="photos.length > 1" class="lightbox-prev" @click.stop="prevPhoto">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </button>

          <div class="lightbox-content" @click.stop>
            <img :src="photos[lightboxIndex]?.url" :alt="photos[lightboxIndex]?.caption || ''" />
            <div v-if="photos[lightboxIndex]?.caption" class="lightbox-caption">
              {{ photos[lightboxIndex].caption }}
            </div>
          </div>

          <button v-if="photos.length > 1" class="lightbox-next" @click.stop="nextPhoto">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </button>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import DatePicker from '../components/DatePicker.vue'
import { ossImg } from '../utils/oss'

const { t } = useI18n()

const defaultCover = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22400%22 height=%22300%22%3E%3Crect fill=%22%23e2e8f0%22 width=%22400%22 height=%22300%22/%3E%3Ctext fill=%22%2394a3b8%22 font-family=%22sans-serif%22 font-size=%2220%22 x=%2250%25%22 y=%2250%25%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22%3ENo Cover%3C/text%3E%3C/svg%3E'

// State
const isNight = ref(document.body.classList.contains('body-night'))
const loading = ref(true)
const photosLoading = ref(false)
const albums = ref([])
const photos = ref([])
const currentAlbum = ref(null)
const searchQuery = ref('')
const activeQuery = ref('')
const searchFocused = ref(false)
const isTransitioning = ref(false)
const lightboxOpen = ref(false)
const lightboxIndex = ref(0)
let searchTimer = null

// AI search + filter + pagination
const aiMode = ref(false)
const aiQuery = ref('')
const aiSearching = ref(false)
const aiMatchedIds = ref(new Set())
const dateFrom = ref('')
const dateTo = ref('')
const currentAlbumPage = ref(1)
const albumPageSize = 30
const showFilters = ref(false)

const hasFilters = computed(() =>
  searchQuery.value || aiMatchedIds.value.size > 0 || dateFrom.value || dateTo.value
)

// Search debounce
watch(searchQuery, (val) => {
  isTransitioning.value = true
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    activeQuery.value = val.trim().toLowerCase()
    isTransitioning.value = false
    currentAlbumPage.value = 1
  }, 250)
})

// Filtered albums
const filteredAlbums = computed(() => {
  let list = [...albums.value]
  // AI match filter
  if (aiMatchedIds.value.size > 0) {
    list = list.filter(a => aiMatchedIds.value.has(String(a.id)))
  }
  // Text search filter
  if (activeQuery.value) {
    list = list.filter(a =>
      a.name.toLowerCase().includes(activeQuery.value) ||
      (a.description && a.description.toLowerCase().includes(activeQuery.value))
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

const totalAlbumPages = computed(() => Math.max(1, Math.ceil(filteredAlbums.value.length / albumPageSize)))
const paginatedAlbums = computed(() => {
  const start = (currentAlbumPage.value - 1) * albumPageSize
  return filteredAlbums.value.slice(start, start + albumPageSize)
})

// Matched photos (from search)
const matchedPhotos = computed(() => {
  if (!activeQuery.value) return []
  const results = []
  for (const album of albums.value) {
    if (!album.photos) continue
    for (const photo of album.photos) {
      if (photo.caption && photo.caption.toLowerCase().includes(activeQuery.value)) {
        results.push({ ...photo, albumId: album.id, albumName: album.name })
      }
    }
  }
  return results
})

// Fetch albums
async function fetchAlbums() {
  loading.value = true
  try {
    const data = await request.get('/api/photowall/albums', {
      params: { page: 0, size: 50 }
    })
    albums.value = data.content || []
  } catch (e) {
    console.error('Failed to fetch albums:', e)
  } finally {
    loading.value = false
  }
}

// Open album detail
async function openAlbum(album) {
  currentAlbum.value = album
  photosLoading.value = true
  try {
    const data = await request.get(`/api/photowall/albums/${album.id}`)
    photos.value = data.photos || []
    currentAlbum.value = { ...album, ...data }
  } catch (e) {
    console.error('Failed to fetch album detail:', e)
  } finally {
    photosLoading.value = false
  }
}

function openAlbumById(albumId) {
  const album = albums.value.find(a => a.id === albumId)
  if (!album) return
  openAlbum(album)
}

function closeAlbum() {
  currentAlbum.value = null
  photos.value = []
}

// Lightbox
function openLightbox(index) {
  lightboxIndex.value = index
  lightboxOpen.value = true
  document.body.style.overflow = 'hidden'
}

function closeLightbox() {
  lightboxOpen.value = false
  document.body.style.overflow = ''
}

function prevPhoto() {
  lightboxIndex.value = (lightboxIndex.value - 1 + photos.value.length) % photos.value.length
}

function nextPhoto() {
  lightboxIndex.value = (lightboxIndex.value + 1) % photos.value.length
}

function handleKeydown(e) {
  if (!lightboxOpen.value) return
  if (e.key === 'Escape') closeLightbox()
  if (e.key === 'ArrowLeft') prevPhoto()
  if (e.key === 'ArrowRight') nextPhoto()
}

function clearSearch() {
  searchQuery.value = ''
  activeQuery.value = ''
  aiQuery.value = ''
  aiMatchedIds.value = new Set()
  dateFrom.value = ''
  dateTo.value = ''
  currentAlbumPage.value = 1
}

// === AI Search ===
function toggleAiMode() {
  aiMode.value = !aiMode.value
  if (!aiMode.value) {
    aiQuery.value = ''
    aiMatchedIds.value = new Set()
  } else {
    searchQuery.value = ''
    activeQuery.value = ''
  }
}

async function doAiSearch() {
  const q = aiQuery.value.trim()
  if (!q) return
  aiSearching.value = true
  aiMatchedIds.value = new Set()
  try {
    const summary = albums.value.map((a, i) =>
      `[${i}] "${a.name}" (${(a.createdAt || '').substring(0, 10)})`
    ).join('\n')
    const prompt = `你是相册搜索助手。以下是博客相册列表，格式为 [序号] "名称" (日期)。
用户想查找: "${q}"
请找出匹配的相册序号，只返回JSON数组如[0,3,5]，无匹配返回[]。
相册列表:
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
        if (i >= 0 && i < albums.value.length) {
          ids.add(String(albums.value[i].id))
        }
      })
      aiMatchedIds.value = ids
      searchQuery.value = ''
      activeQuery.value = ''
      currentAlbumPage.value = 1
    }
  } catch {
    // Fallback: show all
  } finally {
    aiSearching.value = false
  }
}

function clearFilters() {
  searchQuery.value = ''
  activeQuery.value = ''
  aiQuery.value = ''
  aiMatchedIds.value = new Set()
  dateFrom.value = ''
  dateTo.value = ''
  currentAlbumPage.value = 1
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 7).replace('-', '.')
}

let nightObserver = null

onMounted(() => {
  fetchAlbums()
  document.addEventListener('keydown', handleKeydown)
  nightObserver = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  nightObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
  clearTimeout(searchTimer)
  if (nightObserver) { nightObserver.disconnect(); nightObserver = null }
})
</script>

<style scoped>
.photowall-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px 80px;
}

/* ========== Header ========== */
.pw-header {
  text-align: center;
  padding: 40px 0 32px;
}

.pw-title {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0 0 8px;
  color: var(--text-primary, #1e293b);
  letter-spacing: -0.02em;
}

.pw-subtitle {
  color: var(--text-secondary, #64748b);
  font-size: 0.95rem;
  margin: 0 0 28px;
}

/* ========== Search ========== */
.pw-search {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  padding: 10px 20px;
  width: 100%;
  max-width: 460px;
  transition: all 0.3s;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
}

.pw-search--focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.pw-search__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: #94a3b8;
}

.pw-search__field {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.9rem;
  color: var(--text-primary, #1e293b);
  min-width: 0;
}

.pw-search__field::placeholder {
  color: #94a3b8;
}

.pw-search__clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 50%;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
}

.pw-search__clear:hover {
  background: rgba(0, 0, 0, 0.1);
}

/* ========== Section header ========== */
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary, #1e293b);
  margin-bottom: 20px;
}

.section-bar {
  width: 4px;
  height: 20px;
  background: #3b82f6;
  border-radius: 2px;
}

/* ========== Search results ========== */
.search-results {
  margin-bottom: 40px;
}

/* ========== Albums grid ========== */
.albums-section {
  transition: opacity 0.3s, transform 0.3s;
}

.albums-section.is-transitioning {
  opacity: 0.5;
  transform: scale(0.98);
}

.albums-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 48px 32px;
}

.album-card {
  cursor: pointer;
  transition: all 0.3s;
}

/* ========== Stacked polaroid effect ========== */
.album-stack {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  margin-bottom: 16px;
}

.album-stack__layer {
  position: absolute;
  inset: 0;
  border: 5px solid #fff;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.album-stack__layer img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-stack__placeholder {
  width: 100%;
  height: 100%;
  background: #e2e8f0;
}

.album-stack__back {
  transform: rotate(6deg) translate(12px, 8px);
  opacity: 0.6;
  filter: blur(1px);
  z-index: 1;
}

.album-stack__mid {
  transform: rotate(-3deg) translate(-8px, -4px);
  opacity: 0.8;
  filter: grayscale(50%);
  z-index: 2;
}

.album-stack__front {
  z-index: 3;
  position: relative;
}

.album-card:hover .album-stack__back {
  transform: rotate(12deg) translate(20px, 12px);
  opacity: 0.5;
}

.album-card:hover .album-stack__mid {
  transform: rotate(-6deg) translate(-14px, -8px);
  opacity: 0.7;
}

.album-card:hover .album-stack__front {
  transform: translateY(-8px) scale(1.03);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15);
}

.album-card:hover .album-stack__front img {
  transform: scale(1.05);
  transition: transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

/* Hover overlay on front polaroid */
.album-stack__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.75), rgba(0, 0, 0, 0.15) 50%, transparent);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px;
  opacity: 0;
  transition: opacity 0.5s;
  z-index: 4;
}

.album-card:hover .album-stack__overlay {
  opacity: 1;
}

.album-stack__overlay-count {
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
  transform: translateY(6px);
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.album-card:hover .album-stack__overlay-count {
  transform: translateY(0);
}

.album-stack__overlay-hint {
  color: #93c5fd;
  font-size: 0.75rem;
  font-weight: 600;
  margin-top: 2px;
  transform: translateY(6px);
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.075s;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.album-card:hover .album-stack__overlay-hint {
  transform: translateY(0);
}

/* ========== Album meta ========== */
.album-meta {
  padding: 0 4px;
  text-align: center;
}

.album-name-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 4px;
}

.album-name {
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary, #1e293b);
  transition: color 0.3s;
}

.album-card:hover .album-name {
  color: #3b82f6;
}

.album-date {
  display: inline-block;
  font-size: 0.75rem;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 700;
}

.album-desc {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ========== Photos section ========== */
.photos-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.photos-header__dot {
  color: #cbd5e1;
  font-size: 1.2rem;
}

.photos-header__date {
  font-size: 0.85rem;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  padding: 8px 18px;
  cursor: pointer;
  color: var(--text-primary, #334155);
  font-size: 0.85rem;
  font-weight: 600;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.06);
}

.album-detail-header {
  margin-bottom: 32px;
}

.album-detail-title {
  font-size: 2rem;
  font-weight: 800;
  margin: 0 0 8px;
  color: var(--text-primary, #1e293b);
  letter-spacing: -0.02em;
}

.album-detail-desc {
  color: var(--text-secondary, #64748b);
  margin: 0 0 12px;
  font-size: 0.95rem;
  line-height: 1.6;
}

.album-detail-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 0.85rem;
  color: #64748b;
}

.album-detail-count strong {
  color: #3b82f6;
  font-weight: 700;
}

/* ========== Masonry ========== */
.photos-masonry {
  columns: 4;
  column-gap: 14px;
}

.photo-item {
  break-inside: avoid;
  margin-bottom: 14px;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.5s;
}

.photo-item:hover {
  transform: scale(1.02);
  box-shadow: 0 16px 40px rgba(59, 130, 246, 0.12);
}

.photo-item img {
  width: 100%;
  display: block;
  transition: transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

.photo-item:hover img {
  transform: scale(1.05);
}

/* Photo overlay */
.photo-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.75), rgba(0, 0, 0, 0.1) 50%, transparent);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px 14px;
  opacity: 0;
  transition: opacity 0.5s;
}

.photo-item:hover .photo-overlay {
  opacity: 1;
}

.photo-overlay__album {
  font-size: 0.7rem;
  color: #93c5fd;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 4px;
}

.photo-overlay__caption {
  color: #fff;
  font-size: 0.85rem;
  margin: 0;
  font-weight: 500;
  transform: translateY(8px);
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.photo-item:hover .photo-overlay__caption {
  transform: translateY(0);
}

/* ========== Lightbox ========== */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.95);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox-close {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  transition: all 0.2s;
  z-index: 10;
}

.lightbox-close:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: rotate(90deg);
}

.lightbox-prev,
.lightbox-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  transition: all 0.2s;
  z-index: 10;
}

.lightbox-prev { left: 20px; }
.lightbox-next { right: 20px; }

.lightbox-prev:hover,
.lightbox-next:hover {
  background: rgba(255, 255, 255, 0.16);
}

.lightbox-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 90vw;
  max-height: 85vh;
}

.lightbox-content img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6);
}

.lightbox-caption {
  margin-top: 16px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.9rem;
  text-align: center;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  padding: 8px 24px;
  max-width: 500px;
}

/* ========== Empty state ========== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #94a3b8;
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.4;
}

/* ========== Skeleton ========== */
.skeleton { pointer-events: none; }

.skeleton-cover {
  width: 100%;
  height: 100%;
  background: linear-gradient(110deg, #e2e8f0 8%, #f1f5f9 18%, #e2e8f0 33%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.skeleton-photo {
  width: 100%;
  border-radius: 16px;
  background: linear-gradient(110deg, #e2e8f0 8%, #f1f5f9 18%, #e2e8f0 33%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ========== Animations ========== */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.lightbox-enter-active,
.lightbox-leave-active {
  transition: opacity 0.4s ease;
}

.lightbox-enter-from,
.lightbox-leave-to {
  opacity: 0;
}

/* ========== Responsive ========== */
@media (max-width: 1024px) {
  .albums-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 36px 24px;
  }

  .photos-masonry {
    columns: 3;
  }
}

@media (max-width: 768px) {
  .pw-title {
    font-size: 1.8rem;
  }

  .albums-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 28px 16px;
  }

  .photos-masonry {
    columns: 2;
    column-gap: 10px;
  }

  .photo-item {
    margin-bottom: 10px;
    border-radius: 12px;
  }

  .album-detail-title {
    font-size: 1.5rem;
  }

  .lightbox-prev,
  .lightbox-next {
    width: 40px;
    height: 40px;
  }

  .lightbox-prev { left: 10px; }
  .lightbox-next { right: 10px; }

  .pw-search-row {
    flex-direction: column;
    align-items: stretch;
  }
  .pw-search {
    max-width: 100%;
  }
  .pw-filter-toggle {
    align-self: flex-start;
  }
  .pw-filter-row {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .albums-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .photos-masonry {
    columns: 2;
  }
}

/* ========== Search Row + Filter + Pagination ========== */
.pw-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}
.pw-ai-toggle {
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
  flex-shrink: 0;
}
.pw-ai-toggle:hover { color: #3b82f6; background: rgba(59, 130, 246, 0.15); }
.pw-ai-toggle.active { color: #3b82f6; background: rgba(59, 130, 246, 0.2); }
.is-night .pw-ai-toggle { color: #64748b; background: rgba(59, 130, 246, 0.1); }
.is-night .pw-ai-toggle:hover, .is-night .pw-ai-toggle.active { color: #60a5fa; background: rgba(59, 130, 246, 0.2); }
.pw-ai-search-btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: none;
  background: #3b82f6;
  color: white;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.pw-ai-search-btn:hover { background: #2563eb; }
.pw-ai-search-btn:disabled { opacity: 0.6; cursor: not-allowed; }

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
.pw-filter-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.5);
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
.pw-filter-toggle:hover { border-color: #3b82f6; color: #3b82f6; }
.pw-filter-toggle.active { border-color: #3b82f6; color: #3b82f6; background: rgba(59, 130, 246, 0.06); }
.is-night .pw-filter-toggle { background: rgba(30, 41, 59, 0.6); border-color: rgba(255, 255, 255, 0.08); color: #94a3b8; }
.is-night .pw-filter-toggle:hover, .is-night .pw-filter-toggle.active { border-color: #60a5fa; color: #60a5fa; }
.pw-filter-badge {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
}

/* Filter panel */
.pw-filter-panel {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
  margin: 12px auto 0;
  max-width: 460px;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 100;
}
.is-night .pw-filter-panel { background: rgba(30, 41, 59, 0.4); border-color: rgba(255, 255, 255, 0.05); }
.filter-slide-enter-active,
.filter-slide-leave-active {
  transition: all 0.25s ease;
}
.filter-slide-enter-from,
.filter-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
.pw-filter-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.pw-filter-label {
  font-size: 0.82rem;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
  min-width: 56px;
}
.is-night .pw-filter-label { color: #a0aec0; }
.pw-clear-btn {
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
.pw-clear-btn:hover { background: rgba(239, 68, 68, 0.2); }
.pw-ai-tip {
  font-size: 12px;
  color: #94a3b8;
  margin: 8px 0 0;
  text-align: center;
}

/* Result count */
.pw-result-count {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
  margin-bottom: 16px;
}
.is-night .pw-result-count { color: #94a3b8; }

/* Pagination */
.pw-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  padding: 16px 0;
}
.pw-page-btn {
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
.pw-page-btn:hover:not(:disabled) { background: #3b82f6; color: white; }
.pw-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.is-night .pw-page-btn { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.1); color: #60a5fa; }
.pw-page-info {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}
.is-night .pw-page-info { color: #94a3b8; }

/* ========== Night mode ========== */
.is-night .pw-title {
  color: #e2e8f0;
}

.is-night .pw-subtitle {
  color: #94a3b8;
}

.is-night .pw-search {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-night .pw-search__field {
  color: #e2e8f0;
}

.is-night .pw-search__clear {
  background: rgba(255, 255, 255, 0.08);
  color: #94a3b8;
}

.is-night .album-name {
  color: #e2e8f0;
}

.is-night .album-desc {
  color: #94a3b8;
}

.is-night .album-stack__placeholder {
  background: rgba(30, 41, 59, 0.8);
}

.is-night .album-stack__layer {
  border-color: rgba(255, 255, 255, 0.1);
}

.is-night .album-detail-title {
  color: #e2e8f0;
}

.is-night .album-detail-desc {
  color: #94a3b8;
}

.is-night .back-btn {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
}

.is-night .back-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.is-night .album-detail-count {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-night .photo-item {
  background: rgba(30, 41, 59, 0.3);
  border-color: rgba(255, 255, 255, 0.06);
}

.is-night .empty-state {
  color: #64748b;
}

.is-night .section-header {
  color: #e2e8f0;
}

.is-night .photos-header__dot {
  color: rgba(255, 255, 255, 0.2);
}

.is-night .photos-header__date {
  color: #64748b;
}

.is-night .album-date {
  background: rgba(0, 0, 0, 0.3);
  color: #94a3b8;
}

.is-night .pw-search__icon {
  color: #64748b;
}

.is-night .pw-search__field::placeholder {
  color: #64748b;
}

.is-night .pw-search--focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.15);
}

.is-night .pw-ai-search-btn {
  background: #60a5fa;
}
.is-night .pw-ai-search-btn:hover {
  background: #3b82f6;
}

.is-night .pw-clear-btn {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.is-night .pw-clear-btn:hover {
  background: rgba(239, 68, 68, 0.25);
}
</style>
