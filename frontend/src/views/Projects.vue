<template>
  <div class="projects-page">
    <h1 class="page-title fade-in-up">{{ $t('projects.title') }}</h1>
    <p class="page-desc fade-in-up fade-in-up-delay-1">{{ $t('projects.desc') }}</p>

    <!-- Search bar -->
    <div class="search-bar fade-in-up fade-in-up-delay-1">
      <div class="search-input-wrap" :class="{ 'ai-active': aiMode }">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          v-model="keyword"
          type="text"
          :placeholder="aiMode ? $t('projects.aiSearchPlaceholder') : $t('projects.searchPlaceholder')"
          @keydown.enter="doSearch"
          @input="onSearchInput"
        />
        <button v-if="keyword" class="search-clear" @click="clearSearch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
        <button class="search-go" @click="doSearch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>
      <div class="search-actions">
        <button class="action-btn" :class="{ active: showFilters }" @click="showFilters = !showFilters">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
          </svg>
          <span>{{ $t('projects.filters') }}</span>
          <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
        </button>
        <button class="action-btn ai-btn" :class="{ active: aiMode }" @click="toggleAi">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2a4 4 0 0 1 4 4c0 1.95-1.4 3.58-3.25 3.93L12 22"/>
            <path d="M12 2a4 4 0 0 0-4 4c0 1.95 1.4 3.58 3.25 3.93"/>
            <path d="M8.56 13.68L3 17l2 5 5-2"/>
            <path d="M15.44 13.68L21 17l-2 5-5-2"/>
          </svg>
          <span>AI</span>
        </button>
      </div>
    </div>

    <!-- Filter panel -->
    <Transition name="filter-panel">
      <div v-if="showFilters" class="filter-panel fade-in-up">
        <div class="filter-row">
          <div class="filter-group">
            <label>{{ $t('projects.filterTechStack') }}</label>
            <input v-model="filterTechStack" type="text" :placeholder="$t('projects.filterTechStackPlaceholder')" @input="onFilterChange" />
          </div>
          <div class="filter-group">
            <label>{{ $t('projects.filterFeatured') }}</label>
            <DropdownMenu v-model="filterFeatured" :items="featuredOptions" @change="onFilterChange" />
          </div>
          <div class="filter-group">
            <label>{{ $t('projects.filterDateFrom') }}</label>
            <DatePicker v-model="filterDateFrom" :placeholder="$t('projects.filterDateFrom')" :max="filterDateTo || undefined" @change="onFilterChange" />
          </div>
          <div class="filter-group">
            <label>{{ $t('projects.filterDateTo') }}</label>
            <DatePicker v-model="filterDateTo" :placeholder="$t('projects.filterDateTo')" :min="filterDateFrom || undefined" @change="onFilterChange" />
          </div>
        </div>
        <div class="filter-actions">
          <button class="clear-btn" @click="clearFilters">{{ $t('projects.clearFilters') }}</button>
          <span v-if="activeFilterCount > 0" class="filter-count">{{ $t('projects.activeFilters', { count: activeFilterCount }) }}</span>
        </div>
      </div>
    </Transition>

    <!-- Result count -->
    <p v-if="!loading && !loadError && hasSearched" class="result-count fade-in-up">
      {{ $t('projects.resultCount', { count: projects.length }) }}
    </p>

    <!-- Skeleton loading -->
    <div v-if="loading" class="project-grid">
      <div v-for="n in 4" :key="n" class="project-card skeleton-card-project">
        <div class="skeleton" style="width: 100%; height: 160px; border-radius: 8px 8px 0 0;" />
        <div style="padding: 1.25rem;">
          <div class="skeleton" style="width: 60%; height: 18px; margin-bottom: 10px;" />
          <div class="skeleton" style="width: 100%; height: 13px; margin-bottom: 6px;" />
          <div class="skeleton" style="width: 80%; height: 13px; margin-bottom: 14px;" />
          <div style="display: flex; gap: 6px;">
            <div class="skeleton" style="width: 40px; height: 20px; border-radius: 10px;" />
            <div class="skeleton" style="width: 48px; height: 20px; border-radius: 10px;" />
            <div class="skeleton" style="width: 36px; height: 20px; border-radius: 10px;" />
          </div>
        </div>
      </div>
    </div>

    <!-- Featured projects -->
    <div v-if="featured.length && !loading" class="featured-section fade-in-up fade-in-up-delay-1">
      <div v-for="project in featured" :key="project.id" class="featured-card clickable" @click="openProject(project.id)">
        <div class="featured-cover">
          <img v-if="project.coverImage" :src="ossImg(project.coverImage)" :alt="project.name" loading="lazy" />
          <div v-else class="featured-placeholder">&#128187;</div>
        </div>
        <div class="featured-info">
          <span class="featured-tag">★ {{ $t('projects.featured') }}</span>
          <h2>{{ project.name }}</h2>
          <p v-if="project.subtitle" class="featured-subtitle">{{ stripMarkdown(project.subtitle) }}</p>
          <p class="featured-desc">{{ stripMarkdown(project.description) }}</p>
          <div class="tech-stack">
            <span v-for="t in splitTech(project.techStack)" :key="t" class="tech-tag">{{ t }}</span>
          </div>
          <div class="featured-links">
            <a v-if="project.githubUrl" :href="project.githubUrl" target="_blank" class="link-btn" @click.stop>
              <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>
              GitHub
            </a>
            <a v-if="project.demoUrl" :href="project.demoUrl" target="_blank" class="link-btn" @click.stop>{{ $t('projects.demo') }}</a>
            <a v-if="project.subdomainUrl" :href="project.subdomainUrl" target="_blank" class="link-btn primary" @click.stop>{{ $t('projects.visit') }} →</a>
          </div>
          <div v-if="parseScreenshots(project).length" class="featured-shots">
            <img v-for="(img, si) in parseScreenshots(project)" :key="si" :src="ossImg(img)" :alt="project.name" loading="lazy"
              class="featured-shot" @click.stop="openLightbox(parseScreenshots(project), si)" />
          </div>
        </div>
      </div>
    </div>

    <!-- All projects grid -->
    <div v-if="!loading" class="project-grid">
      <div v-for="(project, i) in regular" :key="project.id"
        class="project-card fade-in-up clickable"
        :class="`fade-in-up-delay-${Math.min(i + 1, 5)}`"
        @click="openProject(project.id)">
        <div class="card-cover">
          <img v-if="project.coverImage" :src="ossImg(project.coverImage)" :alt="project.name" loading="lazy" />
          <div v-else class="card-placeholder">&#128187;</div>
        </div>
        <div class="card-body">
          <h3>{{ project.name }}</h3>
          <p v-if="project.subtitle" class="card-subtitle">{{ stripMarkdown(project.subtitle) }}</p>
          <p class="card-desc">{{ stripMarkdown(project.description) }}</p>
          <div class="tech-stack">
            <span v-for="t in splitTech(project.techStack)" :key="t" class="tech-tag">{{ t }}</span>
          </div>
          <div class="card-links">
            <a v-if="project.githubUrl" :href="project.githubUrl" target="_blank" class="link-btn" @click.stop>
              <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>
              GitHub
            </a>
            <a v-if="project.demoUrl" :href="project.demoUrl" target="_blank" class="link-btn" @click.stop>{{ $t('projects.demoShort') }}</a>
            <a v-if="project.subdomainUrl" :href="project.subdomainUrl" target="_blank" class="link-btn primary" @click.stop>{{ $t('projects.visitShort') }} →</a>
          </div>
          <div v-if="parseScreenshots(project).length" class="card-shots">
            <img v-for="(img, si) in parseScreenshots(project)" :key="si" :src="ossImg(img)" :alt="project.name" loading="lazy"
              class="card-shot" @click.stop="openLightbox(parseScreenshots(project), si)" />
          </div>
        </div>
      </div>
    </div>

    <ResourceError v-if="loadError" :message="$t('projects.loadError')" @retry="loadProjects()" />

    <p v-if="!loading && !loadError && projects.length === 0" class="empty fade-in-up">{{ $t('projects.noProjects') }}</p>

    <ImageLightbox v-model="lightboxVisible" :images="lightboxImages" :initial-index="lightboxIndex" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import ResourceError from '../components/ResourceError.vue'
import DatePicker from '../components/DatePicker.vue'
import { ossImg } from '../utils/oss'
import { stripMarkdown } from '../utils/stripMarkdown'
import DropdownMenu from '../components/DropdownMenu.vue'
import ImageLightbox from '../components/ImageLightbox.vue'

const { t } = useI18n()
const router = useRouter()
function openProject(id) {
  router.push(`/projects/${id}`)
}

const projects = ref([])
const loading = ref(true)
const loadError = ref(false)

// Search state
const keyword = ref('')
const aiMode = ref(false)
const showFilters = ref(false)
const filterTechStack = ref('')
const filterFeatured = ref('')
const featuredOptions = computed(() => [
  { value: '', label: t('projects.filterAll') },
  { value: 'true', label: t('projects.filterFeaturedOnly') },
  { value: 'false', label: t('projects.filterNonFeatured') }
])
const filterDateFrom = ref('')
const filterDateTo = ref('')
const hasSearched = ref(false)
let searchTimer = null
let loadRequestId = 0

onBeforeUnmount(() => clearTimeout(searchTimer))

const featured = computed(() => projects.value.filter(p => p.isFeatured))
const regular = computed(() => projects.value.filter(p => !p.isFeatured))

const activeFilterCount = computed(() => {
  let count = 0
  if (filterTechStack.value) count++
  if (filterFeatured.value) count++
  if (filterDateFrom.value) count++
  if (filterDateTo.value) count++
  return count
})

function parseScreenshots(project) {
  if (!project?.screenshots) return []
  try {
    const arr = JSON.parse(project.screenshots)
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch {
    return []
  }
}

const lightboxVisible = ref(false)
const lightboxImages = ref([])
const lightboxIndex = ref(0)
function openLightbox(images, idx) {
  lightboxImages.value = images
  lightboxIndex.value = idx
  lightboxVisible.value = true
}

function splitTech(str) {
  if (!str) return []
  return str.split(',').map(s => s.trim()).filter(Boolean)
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    if (!keyword.value.trim() && activeFilterCount.value === 0) {
      hasSearched.value = false
      loadProjects()
    }
  }, 500)
}

function onFilterChange() {
  doSearch()
}

async function doSearch() {
  if (aiMode.value && keyword.value.trim()) {
    await aiSearch()
  } else {
    hasSearched.value = true
    await loadProjects()
  }
}

async function aiSearch() {
  loading.value = true
  try {
    const res = await request.post('/api/ai/search/parse', { message: keyword.value })
    if (res.keyword) keyword.value = res.keyword
    if (res.status) filterFeatured.value = res.status === 'PUBLISHED' ? 'true' : ''
    hasSearched.value = true
    await loadProjects()
  } catch (e) {
    console.error('AI search failed:', e)
    hasSearched.value = true
    await loadProjects()
  }
}

function toggleAi() {
  aiMode.value = !aiMode.value
}

function clearSearch() {
  keyword.value = ''
  hasSearched.value = false
  loadProjects()
}

function clearFilters() {
  filterTechStack.value = ''
  filterFeatured.value = ''
  filterDateFrom.value = ''
  filterDateTo.value = ''
  if (!keyword.value.trim()) {
    hasSearched.value = false
  }
  loadProjects()
}

async function loadProjects() {
  const reqId = ++loadRequestId
  loading.value = true
  loadError.value = false
  try {
    let url = '/api/projects'
    const params = new URLSearchParams()
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    if (filterTechStack.value.trim()) params.set('techStack', filterTechStack.value.trim())
    if (filterFeatured.value) params.set('featured', filterFeatured.value)
    if (filterDateFrom.value) params.set('dateFrom', filterDateFrom.value + 'T00:00:00')
    if (filterDateTo.value) params.set('dateTo', filterDateTo.value + 'T23:59:59')
    const qs = params.toString()
    if (qs) url += '?' + qs
    const data = await request.get(url)
    if (reqId !== loadRequestId) return
    projects.value = data
  } catch (e) {
    if (reqId !== loadRequestId) return
    console.error('Failed to load projects:', e)
    loadError.value = true
  } finally {
    if (reqId === loadRequestId) loading.value = false
  }
}

onMounted(loadProjects)
</script>

<style scoped>
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}
.page-desc {
  color: var(--color-text-secondary);
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}

/* Search bar */
.search-bar {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}
.search-input-wrap {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 0 0.75rem;
  transition: all var(--transition-fast);
}
.search-input-wrap:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.search-input-wrap.ai-active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}
.search-icon {
  width: 18px;
  height: 18px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}
.search-input-wrap input {
  flex: 1;
  border: none;
  background: none;
  padding: 0.6rem 0.5rem;
  font-size: 0.9rem;
  color: var(--color-text);
  outline: none;
}
.search-input-wrap input::placeholder {
  color: var(--color-text-tertiary);
}
.search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--color-text-tertiary);
  border-radius: 50%;
  transition: all var(--transition-fast);
}
.search-clear:hover {
  background: var(--color-bg-muted);
  color: var(--color-text);
}
.search-clear svg {
  width: 14px;
  height: 14px;
}
.search-go {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.search-go:hover {
  background: var(--color-primary-hover);
}
.search-go svg {
  width: 16px;
  height: 16px;
}

/* Search actions */
.search-actions {
  display: flex;
  gap: 0.5rem;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.6rem 0.85rem;
  border: 1px solid var(--color-border-light);
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  position: relative;
}
.action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.action-btn.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-accent-blue);
}
.action-btn svg {
  width: 16px;
  height: 16px;
}
.ai-btn.active {
  border-color: #3b82f6;
  color: #3b82f6;
  background: rgba(139, 92, 246, 0.08);
}
.filter-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  background: var(--color-primary);
  color: #fff;
  font-size: 0.65rem;
  font-weight: 600;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Filter panel */
.filter-panel {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 1rem;
  margin-bottom: 1rem;
  position: relative;
  z-index: 100;
}
.filter-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 0.75rem;
}
.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.filter-group label {
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.filter-group input[type="text"] {
  padding: 0.5rem 0.65rem;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-sm);
  background: var(--color-bg-muted);
  font-size: 0.85rem;
  color: var(--color-text);
  outline: none;
  transition: border-color var(--transition-fast);
}
.filter-group input[type="text"]:focus {
  border-color: var(--color-primary);
}
.filter-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.75rem;
}
.clear-btn {
  padding: 0.4rem 0.85rem;
  border: 1px solid var(--color-border);
  background: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}
.clear-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.filter-count {
  font-size: 0.78rem;
  color: var(--color-text-tertiary);
}

/* Result count */
.result-count {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  margin-bottom: 1rem;
}

/* Filter panel transition */
.filter-panel-enter-active,
.filter-panel-leave-active {
  transition: all 0.25s ease;
}
.filter-panel-enter-from,
.filter-panel-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Featured */
.featured-section {
  margin-bottom: 2rem;
}
.featured-card {
  display: flex;
  gap: 1.5rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 1rem;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  transition: all var(--transition);
}
.featured-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}
.featured-cover {
  width: 280px;
  flex-shrink: 0;
  overflow: hidden;
}
.featured-cover img {
  width: 100%;
  height: 100%;
  min-height: 180px;
  object-fit: cover;
}
.featured-placeholder {
  width: 100%;
  height: 100%;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-muted);
  font-size: 2.5rem;
}
.featured-info {
  padding: 1.25rem 1.5rem 1.25rem 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.featured-tag {
  font-size: 0.72rem;
  font-weight: 600;
  color: #b45309;
  background: #fef3c7;
  padding: 0.15rem 0.55rem;
  border-radius: 2rem;
  align-self: flex-start;
  margin-bottom: 0.5rem;
}
.featured-info h2 {
  font-size: 1.2rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}
.featured-subtitle {
  font-size: 0.88rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.5rem;
}
.featured-desc {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 0.75rem;
}
.featured-links {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* Project grid */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}
.project-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition);
  cursor: pointer;
}
.project-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-3px);
}
.skeleton-card-project {
  cursor: default;
  pointer-events: none;
}
.skeleton-card-project:hover {
  transform: none;
  box-shadow: none;
}
.card-cover {
  height: 160px;
  overflow: hidden;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}
.project-card:hover .card-cover img {
  transform: scale(1.03);
}
.card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-muted);
  font-size: 2rem;
}
.card-body {
  padding: 1rem 1.25rem;
}
.card-body h3 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.2rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-subtitle {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.4rem;
}
.card-desc {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 0.75rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Tech stack */
.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
}
.tech-tag {
  font-size: 0.7rem;
  padding: 0.1rem 0.45rem;
  background: var(--color-accent-blue);
  color: var(--color-primary);
  border-radius: 2rem;
  font-weight: 500;
}

/* Links */
.card-links {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}
.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.78rem;
  padding: 0.3rem 0.7rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: all var(--transition-fast);
}
.link-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.link-btn.primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.link-btn.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #fff;
}

.empty {
  color: var(--color-text-tertiary);
  text-align: center;
  padding: 2rem;
}

@media (max-width: 768px) {
  .featured-card {
    flex-direction: column;
  }
  .featured-cover {
    width: 100%;
  }
  .featured-info {
    padding: 1rem 1.25rem;
  }
  .project-grid {
    grid-template-columns: 1fr;
  }
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
  .filter-panel {
    padding: 0.75rem;
  }
  .filter-row {
    flex-direction: column;
    gap: 0.5rem;
  }
  .project-card-cover {
    height: 140px;
  }
  .project-card-info {
    padding: 0.75rem;
  }
  .project-card-title {
    font-size: 0.9rem;
  }
  .card-body h3 { white-space: normal; }
  .project-card-desc {
    font-size: 0.78rem;
    -webkit-line-clamp: 2;
  }
  .project-card-meta {
    font-size: 0.72rem;
  }
}
@media (max-width: 480px) {
  .project-grid {
    grid-template-columns: 1fr;
  }
  .featured-cover {
    min-height: 150px;
  }
  .featured-cover img {
    min-height: 150px;
  }
}

/* 项目展示图片画廊 */
.featured-shots {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}
.featured-shot {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 8px;
  cursor: zoom-in;
  transition: transform 0.25s;
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.featured-shot:hover { transform: scale(1.04); }
.card-shots {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.card-shot {
  width: 64px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
  cursor: zoom-in;
  transition: transform 0.25s;
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.card-shot:hover { transform: scale(1.05); }
</style>
