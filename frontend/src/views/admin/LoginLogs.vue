<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminLoginLogs.title') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="loadLogs" />
        <el-dropdown @command="exportCsv" :disabled="exporting">
          <el-button type="success" :loading="exporting">
            <el-icon><Download /></el-icon> {{ $t('adminLoginLogs.exportCsv') }}
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="page">{{ $t('adminLoginLogs.exportCurrentPage') }}</el-dropdown-item>
              <el-dropdown-item command="all">{{ $t('adminLoginLogs.exportAll') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminLoginLogs.loginRecords') }}</span>
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
              :placeholder="aiMode ? $t('adminLoginLogs.aiSearchPlaceholder') : $t('adminLoginLogs.searchPlaceholder')"
              @keyup.enter="aiMode ? doAiSearch() : doSearch()" @input="onKeywordInput" />
            <button class="ai-toggle" :class="{ active: aiMode }" @click="aiMode = !aiMode" :title="$t('adminLoginLogs.aiSmartSearch')">
              <div class="ai-loader">
                <svg width="100" height="100" viewBox="0 0 100 100">
                  <defs>
                    <mask id="log-ai-clipping">
                      <polygon points="0,0 100,0 100,100 0,100" fill="black"></polygon>
                      <polygon points="25,25 75,25 50,75" fill="white"></polygon>
                      <polygon points="50,25 75,75 25,75" fill="white"></polygon>
                      <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    </mask>
                  </defs>
                </svg>
                <div class="ai-loader-box"></div>
              </div>
            </button>
            <kbd class="cir-search__kbd">Enter</kbd>
          </label>
          <button class="filter-toggle" :class="{ active: showFilters || activeFilterCount > 0 }" @click="showFilters = !showFilters" :title="$t('adminLoginLogs.filter')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
            </svg>
            <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
          </button>
          <button class="search-btn" @click="aiMode ? doAiSearch() : doSearch()" :disabled="aiLoading">
            {{ aiLoading ? $t('adminLoginLogs.parsing') : (aiMode ? $t('adminLoginLogs.aiSearch') : $t('adminLoginLogs.search')) }}
          </button>
          <button v-if="hasFilter" class="clear-btn" @click="clearSearch">{{ $t('adminLoginLogs.clear') }}</button>
        </div>

        <!-- Filter Panel -->
        <Transition name="filter-slide">
          <div v-if="showFilters" class="filter-panel">
            <div class="filter-row">
              <label class="filter-label">{{ $t('adminLoginLogs.status') }}</label>
              <DropdownMenu v-model="statusFilter" :items="statusOptions" @change="doSearch()" />
            </div>
            <div class="filter-row">
              <label class="filter-label">{{ $t('adminLoginLogs.startDate') }}</label>
              <el-date-picker v-model="startDate" type="date" value-format="YYYY-MM-DD"
                :placeholder="$t('adminLoginLogs.startDatePlaceholder')" size="default" clearable @change="doSearch()" />
            </div>
            <div class="filter-row">
              <label class="filter-label">{{ $t('adminLoginLogs.endDate') }}</label>
              <el-date-picker v-model="endDate" type="date" value-format="YYYY-MM-DD"
                :placeholder="$t('adminLoginLogs.endDatePlaceholder')" size="default" clearable @change="doSearch()" />
            </div>
          </div>
        </Transition>

        <div v-if="hasFilter" class="result-count">
          {{ $t('adminLoginLogs.resultCount', { count: totalElements }) }}
        </div>
        <div v-if="aiMode" class="ai-hint">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
          {{ $t('adminLoginLogs.aiHint') }}
        </div>
      </div>

      <el-table :data="logs" row-key="id" stripe v-loading="loading" :empty-text="$t('adminLoginLogs.emptyText')">
        <el-table-column prop="username" :label="$t('adminLoginLogs.username')" width="120" />
        <el-table-column prop="loginTime" :label="$t('adminLoginLogs.loginTime')" width="170" />
        <el-table-column prop="deviceInfo" :label="$t('adminLoginLogs.deviceInfo')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.deviceInfo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="loginIp" :label="$t('adminLoginLogs.loginIp')" width="140">
          <template #default="{ row }">
            {{ row.loginIp || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="location" :label="$t('adminLoginLogs.location')" width="120">
          <template #default="{ row }">
            {{ row.location || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('adminLoginLogs.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain" size="small">
              {{ row.status === 1 ? $t('adminLoginLogs.success') : $t('adminLoginLogs.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" :label="$t('adminLoginLogs.failReason')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.failReason" class="fail-reason">{{ row.failReason }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="totalPages > 1">
        <el-pagination background
          layout="total, sizes, prev, pager, next"
          :total="totalElements"
          :current-page="page + 1"
          :page-size="pageSize"
          :page-sizes="[20, 30, 40, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Download } from '@element-plus/icons-vue'
import request from '../../utils/request'
import DropdownMenu from '../../components/DropdownMenu.vue'
import RefreshButton from '../../components/RefreshButton.vue'

const { t } = useI18n()

const logs = ref([])
const loading = ref(false)
const exporting = ref(false)
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(20)
const keyword = ref('')
const statusFilter = ref('')
const startDate = ref('')
const endDate = ref('')
const aiMode = ref(false)
const aiLoading = ref(false)
const showFilters = ref(false)
let debounceTimer = null

const hasFilter = computed(() => keyword.value || statusFilter.value !== '' || startDate.value || endDate.value)
const activeFilterCount = computed(() => [statusFilter.value !== '', !!startDate.value, !!endDate.value].filter(Boolean).length)

const statusOptions = computed(() => [
  { value: '', label: t('adminLoginLogs.allStatus') },
  { value: 1, label: t('adminLoginLogs.success') },
  { value: 0, label: t('adminLoginLogs.failed') }
])

function doSearch() {
  page.value = 0
  loadLogs()
}

function clearSearch() {
  keyword.value = ''
  statusFilter.value = ''
  startDate.value = ''
  endDate.value = ''
  page.value = 0
  loadLogs()
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
    page.value = 0
    loadLogs()
  } catch (e) {
    console.error('AI search failed:', e)
    doSearch()
  } finally {
    aiLoading.value = false
  }
}

function handlePageChange(p) {
  page.value = p - 1
  loadLogs()
}

function handleSizeChange(s) {
  pageSize.value = s
  page.value = 0
  loadLogs()
}

async function loadLogs() {
  loading.value = true
  try {
    let url = `/admin/login-logs?page=${page.value}&size=${pageSize.value}`
    if (keyword.value) url += `&keyword=${encodeURIComponent(keyword.value.trim())}`
    if (statusFilter.value !== '') url += `&status=${statusFilter.value}`
    if (startDate.value) url += `&startDate=${startDate.value}`
    if (endDate.value) url += `&endDate=${endDate.value}`
    const data = await request.get(url)
    logs.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch {
    ElMessage.error(t('adminLoginLogs.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function exportCsv(mode) {
  exporting.value = true
  try {
    let url = `/admin/login-logs/export?`
    if (keyword.value) url += `keyword=${encodeURIComponent(keyword.value.trim())}&`
    if (statusFilter.value !== '') url += `status=${statusFilter.value}&`
    if (startDate.value) url += `startDate=${startDate.value}&`
    if (endDate.value) url += `endDate=${endDate.value}&`
    if (mode === 'page') {
      url += `page=${page.value}&size=${pageSize.value}&`
    }

    const token = localStorage.getItem('accessToken')
    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!response.ok) throw new Error('Export failed')

    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = `login-logs-${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(downloadUrl)
    ElMessage.success(mode === 'page' ? t('adminLoginLogs.exportPageSuccess') : t('adminLoginLogs.exportAllSuccess'))
  } catch (e) {
    console.error('Export failed:', e)
    ElMessage.error(t('adminLoginLogs.exportFailed'))
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadLogs()
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 1.25rem;
}
.card-header {
  font-weight: 600;
  font-size: 15px;
}
.search-bar {
  margin-bottom: 16px;
}
.search-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.cir-search {
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 24px;
  padding: 0 12px;
  height: 36px;
  flex: 1;
  min-width: 240px;
  background: rgba(255, 255, 255, 0.6);
  transition: border-color 0.2s;
}
.cir-search:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.cir-search__icon {
  width: 16px;
  height: 16px;
  color: #94a3b8;
  flex-shrink: 0;
}
.cir-search__field {
  border: none;
  outline: none;
  flex: 1;
  padding: 0 8px;
  font-size: 14px;
  background: transparent;
}
.cir-search__kbd {
  font-size: 11px;
  color: #94a3b8;
  background: rgba(0, 0, 0, 0.03);
  padding: 2px 6px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.cir-search.ai-active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.1);
}
.ai-toggle {
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: background 0.2s;
  flex-shrink: 0;
}
.ai-toggle:hover {
  background: rgba(59, 130, 246, 0.06);
}
.ai-toggle.active {
  background: rgba(139, 92, 246, 0.1);
}
.ai-loader {
  width: 20px;
  height: 20px;
  position: relative;
}
.ai-loader svg {
  width: 100%;
  height: 100%;
}
.ai-loader-box {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  mask: url(#log-ai-clipping);
  -webkit-mask: url(#log-ai-clipping);
}
.filter-toggle {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
  position: relative;
  flex-shrink: 0;
}
.filter-toggle:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}
.filter-toggle.active {
  border-color: #3b82f6;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
}
.filter-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 16px;
  height: 16px;
  background: #3b82f6;
  color: #fff;
  font-size: 10px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.search-btn {
  height: 36px;
  padding: 0 16px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s;
}
.search-btn:hover {
  background: #2563eb;
}
.search-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.clear-btn {
  height: 36px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.6);
  color: #475569;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
}
.clear-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}
.filter-panel {
  margin-top: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-label {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
}
.result-count {
  margin-top: 8px;
  font-size: 13px;
  color: #94a3b8;
}
.ai-hint {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #3b82f6;
}
.fail-reason {
  color: #ef4444;
  font-size: 13px;
}
.text-muted {
  color: #94a3b8;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.filter-slide-enter-active,
.filter-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.filter-slide-enter-from,
.filter-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
