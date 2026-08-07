<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminArticles.pageTitle') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="loadArticles" />
        <router-link v-if="authStore.hasPermission('article:create')" to="/admin/articles/create">
        <el-button type="primary">
          <el-icon><EditPen /></el-icon> {{ $t('adminArticles.writeArticle') }}
        </el-button>
      </router-link>
      </div>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminArticles.articleList') }}</span>
        </div>
      </template>

      <!-- Search bar -->
      <div class="search-bar">
        <div class="search-inputs">
          <label class="cir-search" :class="{ 'ai-active': aiMode }">
            <svg class="cir-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="11" cy="11" r="8"></circle>
              <path d="m21 21-4.34-4.34"></path>
            </svg>
            <input v-model="keyword" class="cir-search__field" type="search"
              :placeholder="aiMode ? $t('adminArticles.aiPlaceholder') : $t('adminArticles.searchPlaceholder')"
              @keyup.enter="aiMode ? doAiSearch() : doSearch()" @input="onKeywordInput" />
            <button class="ai-toggle" :class="{ active: aiMode }" @click="aiMode = !aiMode" :title="$t('adminArticles.aiSearch')">
              <div class="articles-ai-loader">
                <svg width="100" height="100" viewBox="0 0 100 100">
                  <defs>
                    <mask id="admin-ai-clipping">
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
          <button class="filter-toggle" :class="{ active: showFilters || activeFilterCount > 0 }" @click="showFilters = !showFilters" :title="$t('adminArticles.filter')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
            </svg>
            <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
          </button>
          <button class="search-btn" @click="aiMode ? doAiSearch() : doSearch()" :disabled="aiLoading">
            {{ aiLoading ? $t('adminArticles.parsing') : (aiMode ? $t('adminArticles.aiSearchBtn') : $t('adminArticles.search')) }}
          </button>
          <button v-if="hasFilter" class="clear-btn" @click="clearSearch">{{ $t('adminArticles.clear') }}</button>
        </div>

        <!-- Filter Panel -->
        <Transition name="filter-slide">
          <div v-if="showFilters" class="filter-panel">
            <div class="filter-row">
              <label class="filter-label">{{ $t('adminArticles.status') }}</label>
              <DropdownMenu v-model="statusFilter" :items="statusOptions" @change="doSearch()" />
            </div>
            <div class="filter-row">
              <label class="filter-label">{{ $t('adminArticles.category') }}</label>
              <DropdownMenu v-model="categoryFilter" :items="categoryOptions" @change="doSearch()" />
            </div>
            <div class="filter-row">
              <label class="filter-label">{{ $t('adminArticles.tag') }}</label>
              <DropdownMenu v-model="tagFilter" :items="tagOptions" @change="doSearch()" />
            </div>
          </div>
        </Transition>

        <div v-if="hasFilter" class="result-count">
          {{ $t('adminArticles.found', { count: totalElements }) }}
        </div>
        <div v-if="aiMode" class="ai-hint">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
          {{ $t('adminArticles.aiHint') }}
        </div>
      </div>

      <el-table :data="articles" row-key="id" stripe v-loading="loading" :empty-text="$t('adminArticles.noArticles')">
        <el-table-column prop="title" :label="$t('adminArticles.title')" min-width="75" show-overflow-tooltip />
        <el-table-column prop="categoryName" :label="$t('adminArticles.category')" width="130">
          <template #default="{ row }">
            {{ row.categoryName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('adminArticles.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" :label="$t('adminArticles.views')" width="100" />
        <el-table-column :label="$t('adminArticles.mark')" width="110">
          <template #default="{ row }">
            <div class="mark-tags">
              <el-tag v-if="row.isTop" type="warning" effect="plain" size="small">{{ $t('adminArticles.top') }}</el-tag>
              <el-tag v-if="row.isFeatured" type="primary" effect="plain" size="small">{{ $t('adminArticles.featured') }}</el-tag>
              <span v-if="!row.isTop && !row.isFeatured">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cardStyle" :label="$t('adminArticles.cardStyle')" width="100">
          <template #default="{ row }">
            {{ cardStyleName(row.cardStyle) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="$t('adminArticles.createdAt')" width="190">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('adminArticles.action')" width="375">
          <template #default="{ row }">
            <div class="action-buttons">
              <router-link v-if="authStore.hasPermission('article:edit')" :to="`/admin/articles/edit/${row.id}`">
                <el-button type="primary" size="small">{{ $t('adminArticles.edit') }}</el-button>
              </router-link>
              <el-button v-if="authStore.hasPermission('article:publish') && (row.status !== 3 || authStore.isSuperAdmin)" :type="row.status === 1 ? 'warning' : (row.status === 3 ? 'success' : 'success')" size="small" :disabled="pendingIds.has(row.id)" @click="toggleStatus(row)">{{ row.status === 1 ? $t('adminArticles.unpublish') : (row.status === 3 ? $t('adminArticles.approve') : $t('adminArticles.publish')) }}</el-button>
              <el-button v-if="authStore.hasPermission('article:edit')" size="small" :disabled="pendingIds.has(row.id)" @click="toggleTop(row)">{{ row.isTop ? $t('adminArticles.cancelTop') : $t('adminArticles.setTop') }}</el-button>
              <el-button v-if="authStore.hasPermission('article:edit')" size="small" :disabled="pendingIds.has(row.id)" @click="toggleFeatured(row)">{{ row.isFeatured ? $t('adminArticles.cancelFeatured') : $t('adminArticles.setFeatured') }}</el-button>
              <el-button v-if="authStore.hasPermission('article:delete')" type="danger" size="small" :disabled="pendingIds.has(row.id)" @click="handleDelete(row)">{{ $t('adminArticles.delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="totalPages > 1">
        <el-pagination background layout="total, prev, pager, next, sizes"
          :total="totalElements" :current-page="page + 1" :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="s => { pageSize = s; page = 0; loadArticles() }" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { EditPen } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import { cachedFetch } from '../../utils/cache'
import DropdownMenu from '../../components/DropdownMenu.vue'
import RefreshButton from '../../components/RefreshButton.vue'

const { t, tm } = useI18n()
const authStore = useAuthStore()

const articles = ref([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const keyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref('')
const tagFilter = ref('')
const categories = ref([])
const tags = ref([])
const aiMode = ref(false)
const aiLoading = ref(false)
const showFilters = ref(false)
let debounceTimer = null
const pendingIds = new Set()

const hasFilter = computed(() => keyword.value || statusFilter.value !== '' || categoryFilter.value || tagFilter.value)
const activeFilterCount = computed(() => [statusFilter.value !== '', categoryFilter.value, tagFilter.value].filter(Boolean).length)

const statusOptions = computed(() => [
  { value: '', label: t('adminArticles.allStatus') },
  { value: 0, label: t('adminArticles.draft') },
  { value: 1, label: t('adminArticles.published') },
  { value: 2, label: t('adminArticles.archived') },
  { value: 3, label: t('adminArticles.pendingReview') }
])
const categoryOptions = computed(() => [
  { value: '', label: t('adminArticles.allCategories') },
  ...categories.value.map(c => ({ value: c.id, label: c.name }))
])
const tagOptions = computed(() => [
  { value: '', label: t('adminArticles.allTags') },
  ...tags.value.map(tag => ({ value: tag.id, label: tag.name }))
])

function doSearch() {
  page.value = 0
  loadArticles()
}

function clearSearch() {
  keyword.value = ''
  statusFilter.value = ''
  categoryFilter.value = ''
  tagFilter.value = ''
  page.value = 0
  loadArticles()
}

function onKeywordInput() {
  clearTimeout(debounceTimer)
  if (aiMode.value) return
  debounceTimer = setTimeout(() => {
    doSearch()
  }, 300)
}

async function doAiSearch() {
  if (!keyword.value.trim()) return
  aiLoading.value = true
  try {
    const res = await request.post('/api/ai/search/parse', { message: keyword.value })
    if (res.keyword) keyword.value = res.keyword
    else keyword.value = ''
    if (res.categoryId) categoryFilter.value = res.categoryId
    if (res.tagId) tagFilter.value = res.tagId
    page.value = 0
    loadArticles()
  } catch (e) {
    console.error('AI search failed:', e)
    doSearch()
  } finally {
    aiLoading.value = false
  }
}

function handlePageChange(p) {
  page.value = p - 1
  loadArticles()
}

async function loadArticles() {
  loading.value = true
  try {
    let url = `/admin/articles?page=${page.value}&size=${pageSize.value}`
    if (keyword.value) url += `&keyword=${encodeURIComponent(keyword.value.trim())}`
    if (statusFilter.value !== '') url += `&status=${statusFilter.value}`
    if (categoryFilter.value) url += `&categoryId=${categoryFilter.value}`
    if (tagFilter.value) url += `&tagId=${tagFilter.value}`
    const data = await request.get(url)
    articles.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch (e) {
    ElMessage.error(t('adminArticles.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function toggleStatus(article) {
  if (pendingIds.has(article.id)) return
  pendingIds.add(article.id)
  try {
    const oldStatus = article.status
    const newStatus = oldStatus === 1 ? 0 : 1
    try {
      await request.patch(`/admin/articles/${article.id}/status?status=${newStatus}`)
      // admin发布→后端设为待审核(3)，super_admin发布→直接发布(1)
      if (newStatus === 1 && authStore.role === 'admin' && oldStatus !== 1) {
        article.status = 3
        ElMessage.success(t('adminArticles.pendingReviewSuccess'))
      } else {
        article.status = newStatus
        if (oldStatus === 3) {
          ElMessage.success(t('adminArticles.approveSuccess'))
        } else {
          ElMessage.success(newStatus === 1 ? t('adminArticles.publishedSuccess') : t('adminArticles.unpublishedSuccess'))
        }
      }
    } catch (e) {
      if (e.code === 8002) {
        ElMessage.warning(e.message)
      } else {
        ElMessage.error(e.message || t('adminArticles.operationFailed'))
      }
    }
  } finally {
    pendingIds.delete(article.id)
  }
}

async function toggleTop(article) {
  if (pendingIds.has(article.id)) return
  pendingIds.add(article.id)
  try {
    const newTop = article.isTop ? 0 : 1
    try {
      await request.patch(`/admin/articles/${article.id}/top?isTop=${newTop}`)
      article.isTop = newTop
      ElMessage.success(newTop ? t('adminArticles.topSuccess') : t('adminArticles.cancelTopSuccess'))
    } catch (e) {
      ElMessage.error(e.message || t('adminArticles.operationFailed'))
    }
  } finally {
    pendingIds.delete(article.id)
  }
}

async function toggleFeatured(article) {
  if (pendingIds.has(article.id)) return
  pendingIds.add(article.id)
  try {
    const newVal = article.isFeatured ? 0 : 1
    try {
      await request.patch(`/admin/articles/${article.id}/featured?isFeatured=${newVal}`)
      article.isFeatured = newVal
      ElMessage.success(newVal ? t('adminArticles.featuredSuccess') : t('adminArticles.cancelFeaturedSuccess'))
    } catch (e) {
      ElMessage.error(e.message || t('adminArticles.operationFailed'))
    }
  } finally {
    pendingIds.delete(article.id)
  }
}

async function handleDelete(article) {
  if (pendingIds.has(article.id)) return
  pendingIds.add(article.id)
  try {
    await ElMessageBox.confirm(t('adminArticles.deleteConfirm', { title: article.title }), t('adminArticles.deleteConfirmTitle'), { type: 'warning' })
    await request.delete(`/admin/articles/${article.id}`)
    ElMessage.success(t('adminArticles.deleteSuccess'))
    loadArticles()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminArticles.deleteFailed'))
  } finally {
    pendingIds.delete(article.id)
  }
}

function statusText(s) {
  return { 0: t('adminArticles.draft'), 1: t('adminArticles.published'), 2: t('adminArticles.archived'), 3: t('adminArticles.pendingReview') }[s] || t('adminArticles.unknown')
}

function cardStyleName(s) {
  const cn = tm('adminArticles.cardStyleNames')
  return { 0: cn.default, 1: cn.shadow, 2: cn.magic, 3: cn.rotateBorder, 4: cn.notebook, 5: cn.ticket, 6: cn.flip, 7: cn.cyber, 8: cn.gradientBorder, 9: cn.education, 10: cn.holoTicket, 12: cn.stereo, 13: cn.glow, 14: cn.neonBorder, 15: cn.gradientGlow, 17: cn.magicGradient, 18: cn.physicsCard, 19: cn.flip3D, 20: cn.cyber3D, 21: cn.gradientHalo, 22: cn.educationPlus, 23: cn.nightSky, 24: cn.enhancedTicket, 25: cn.tilt3D, 26: cn.lightSweep, 27: cn.neonZoom }[s] || cn.default
}

function statusType(s) {
  return { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }[s] || 'info'
}

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

onMounted(async () => {
  const [catsResult, tagsResult] = await Promise.allSettled([
    cachedFetch('admin:categories', () => request.get('/admin/categories')),
    cachedFetch('admin:tags', () => request.get('/admin/tags'))
  ])
  categories.value = catsResult.status === 'fulfilled' ? (catsResult.value || []) : []
  tags.value = tagsResult.status === 'fulfilled' ? (tagsResult.value || []) : []
  loadArticles()
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
})
</script>

<style scoped>
.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* ===== Search bar ===== */
.search-bar {
  margin-bottom: 1rem;
}
.search-inputs {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  align-items: center;
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
  color: #94a3b8;
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
  color: #94a3b8;
}
.cir-search__kbd {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 24px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 500;
  color: #475569;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}
.cir-search:focus-within {
  border-color: var(--color-primary, #3b82f6);
  box-shadow: 0 0 0 3px var(--color-primary-glow, rgba(59, 130, 246, 0.22));
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
  transition: color var(--transition-fast);
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
  animation: admin-colorize calc(var(--time-animation) * 3) ease-in-out infinite;
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
  mask: url(#admin-ai-clipping);
  -webkit-mask: url(#admin-ai-clipping);
}
.articles-ai-loader svg { position: absolute; }
.articles-ai-loader svg #admin-ai-clipping {
  filter: contrast(15);
  animation: admin-roundness calc(var(--time-animation) / 2) linear infinite;
}
.articles-ai-loader svg #admin-ai-clipping polygon { filter: blur(7px); }
.articles-ai-loader svg #admin-ai-clipping polygon:nth-child(1) { transform-origin: 75% 25%; transform: rotate(90deg); }
.articles-ai-loader svg #admin-ai-clipping polygon:nth-child(2) { transform-origin: 50% 50%; animation: admin-rotation var(--time-animation) linear infinite reverse; }
.articles-ai-loader svg #admin-ai-clipping polygon:nth-child(3) { transform-origin: 50% 60%; animation: admin-rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -3); }
.articles-ai-loader svg #admin-ai-clipping polygon:nth-child(4) { transform-origin: 40% 40%; animation: admin-rotation var(--time-animation) linear infinite reverse; }
.articles-ai-loader svg #admin-ai-clipping polygon:nth-child(5) { transform-origin: 40% 40%; animation: admin-rotation var(--time-animation) linear infinite reverse; animation-delay: calc(var(--time-animation) / -2); }
.articles-ai-loader svg #admin-ai-clipping polygon:nth-child(6) { transform-origin: 60% 40%; animation: admin-rotation var(--time-animation) linear infinite; }
.articles-ai-loader svg #admin-ai-clipping polygon:nth-child(7) { transform-origin: 60% 40%; animation: admin-rotation var(--time-animation) linear infinite; animation-delay: calc(var(--time-animation) / -1.5); }
@keyframes admin-rotation { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
@keyframes admin-roundness { 0% { filter: contrast(15); } 20% { filter: contrast(3); } 40% { filter: contrast(3); } 60% { filter: contrast(15); } 100% { filter: contrast(15); } }
@keyframes admin-colorize { 0% { filter: hue-rotate(0deg); } 20% { filter: hue-rotate(-30deg); } 40% { filter: hue-rotate(-60deg); } 60% { filter: hue-rotate(-90deg); } 80% { filter: hue-rotate(-45deg); } 100% { filter: hue-rotate(0deg); } }

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
  flex-shrink: 0;
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
  transition: border-color var(--transition-fast), color var(--transition-fast);
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
  transition: border-color var(--transition-fast), color var(--transition-fast);
  flex-shrink: 0;
}
.filter-toggle:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.filter-toggle.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-glow, rgba(59, 130, 246, 0.08));
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
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.filter-slide-enter-active,
.filter-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
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
.mark-tags {
  display: flex;
  gap: 4px;
}
</style>

<style>
/* Night mode overrides (unscoped) */
.night .cir-search__icon { color: #94a3b8; }
.night .cir-search__field { color: #e2e8f0; }
.night .cir-search__field::placeholder { color: #64748b; }
.night .cir-search__kbd {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  color: #94a3b8;
}
.night .cir-search {
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.05);
}
.night .cir-search:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}
.night .filter-toggle {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #94a3b8;
}
.night .filter-toggle:hover,
.night .filter-toggle.active {
  border-color: #3b82f6;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
}
.night .filter-badge { background: #3b82f6; }
.night .filter-panel {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
}
.night .filter-label { color: #94a3b8; }
.night .search-btn { background: #2563eb; }
.night .search-btn:hover:not(:disabled) { background: #1d4ed8; }
.night .clear-btn {
  border-color: rgba(255, 255, 255, 0.12);
  color: #94a3b8;
}
.night .clear-btn:hover {
  border-color: #3b82f6;
  color: #e2e8f0;
}
.night .result-count { color: #94a3b8; }
.night .ai-hint { color: #94a3b8; }
.night .cir-search.ai-active {
  border-color: #60a5fa;
}
.night .cir-search.ai-active:focus-within {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(167, 139, 250, 0.2);
}
</style>
