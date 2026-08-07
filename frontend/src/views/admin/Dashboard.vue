<template>
  <div class="admin-page dashboard-page" v-loading="loading">
    <!-- Header -->
    <div class="dashboard-header">
      <RefreshButton :onRefresh="refreshAll" :title="$t('dashboard.refreshDashboard')" />
      <span class="update-time" v-if="lastUpdate">{{ $t('dashboard.dataUpdatedAt', { time: lastUpdate }) }}</span>
    </div>

    <!-- Stat cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :lg="4" v-for="(stat, i) in statCards" :key="stat.label">
        <div class="stat-card" :class="`stat-card--${stat.theme}`" :style="{ animationDelay: i * 0.05 + 's' }" @click="$router.push(stat.route)">
          <svg class="stat-wave" viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
            <path d="M0 40 Q10 20 20 40 T40 40 T60 40 T80 40 V80 H0 Z" :fill="stat.wave" />
          </svg>
          <div class="stat-icon-wrap">
            <svg class="stat-svg-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" v-html="stat.svg"></svg>
          </div>
          <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- Charts: Publish trend (16) + Category pie (8) -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card class="dashboard-card" style="margin-bottom: 16px">
          <template #header>
            <div class="card-header">
              <span>{{ $t('dashboard.publishTrend') }}</span>
              <el-radio-group v-model="publishChartType" size="small">
                <el-radio-button value="bar">{{ $t('dashboard.barChart') }}</el-radio-button>
                <el-radio-button value="line">{{ $t('dashboard.lineChart') }}</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="publishChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card class="dashboard-card" style="margin-bottom: 16px">
          <template #header><span class="card-header">{{ $t('dashboard.categoryDistribution') }}</span></template>
          <div ref="categoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts: Views trend (16) + Tag pie (8) -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card class="dashboard-card" style="margin-bottom: 16px">
          <template #header>
            <div class="card-header">
              <span>{{ $t('dashboard.viewsTrend') }}</span>
              <el-radio-group v-model="viewsChartType" size="small">
                <el-radio-button value="line">{{ $t('dashboard.lineChart') }}</el-radio-button>
                <el-radio-button value="bar">{{ $t('dashboard.barChart') }}</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="viewsChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card class="dashboard-card" style="margin-bottom: 16px">
          <template #header><span class="card-header">{{ $t('dashboard.tagDistribution') }}</span></template>
          <div ref="tagChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI Report -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24">
        <el-card class="dashboard-card ai-report-card" style="margin-bottom: 16px">
          <template #header>
            <div class="card-header">
              <div class="ai-report-title">
                <el-icon :style="{ color: '#3b82f6', fontSize: '20px', marginRight: '8px' }"><MagicStick /></el-icon>
                <span>{{ $t('dashboard.aiReport') }}</span>
              </div>
              <div class="ai-report-actions">
                <el-radio-group v-model="reportPeriod" size="small">
                  <el-radio-button value="daily">{{ $t('dashboard.dailyReport') }}</el-radio-button>
                  <el-radio-button value="weekly">{{ $t('dashboard.weeklyReport') }}</el-radio-button>
                  <el-radio-button value="monthly">{{ $t('dashboard.monthlyReport') }}</el-radio-button>
                </el-radio-group>
                <el-button type="primary" :icon="MagicStick" :loading="aiReportLoading" @click="generateReport" style="margin-left: 12px">
                  {{ $t('dashboard.generateReport') }}
                </el-button>
              </div>
            </div>
          </template>
          <div v-if="aiReportContent" ref="aiReportSection" class="ai-report-content">
            <MdPreview :modelValue="aiReportContent" previewTheme="github" :codeFoldable="false" />
            <div class="ai-report-save-bar">
              <el-button v-if="!reportSaved" type="success" size="small" @click="handleSaveReport">
                {{ $t('dashboard.saveReport') }}
              </el-button>
              <span v-else class="ai-report-saved-hint">{{ $t('dashboard.reportSaved') }}</span>
            </div>
          </div>
          <div v-else-if="aiReportLoading" class="ai-report-loading">
            <el-icon class="is-loading" :style="{ fontSize: '24px', color: '#3b82f6' }"><Loading /></el-icon>
            <span>{{ $t('dashboard.aiAnalyzing') }}</span>
          </div>
          <el-empty v-else :description="$t('dashboard.aiEmptyHint')" :image-size="80" />
        </el-card>

        <!-- Report History -->
        <el-card class="dashboard-card" style="margin-bottom: 16px">
          <template #header>
            <div class="card-header">
              <span>{{ $t('dashboard.reportHistory') }}</span>
              <div style="display: flex; gap: 8px; align-items: center">
                <el-input v-model="reportSearchKeyword" :placeholder="$t('dashboard.reportSearch')" size="small" clearable style="width: 180px" @keyup.enter="loadReportHistory" @clear="loadReportHistory" />
                <el-select v-model="reportFilterType" :placeholder="$t('dashboard.reportType')" size="small" clearable style="width: 100px" @change="loadReportHistory">
                  <el-option :label="$t('dashboard.dailyReport')" value="daily" />
                  <el-option :label="$t('dashboard.weeklyReport')" value="weekly" />
                  <el-option :label="$t('dashboard.monthlyReport')" value="monthly" />
                </el-select>
              </div>
            </div>
          </template>
          <el-table :data="reportHistory" stripe :empty-text="$t('dashboard.noReports')" size="small" v-loading="reportHistoryLoading" @row-click="toggleReportExpand" row-key="id" style="cursor: pointer">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="report-expand-content">
                  <MdPreview :modelValue="row.content" previewTheme="github" :codeFoldable="false" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="title" :label="$t('dashboard.title')" min-width="200" show-overflow-tooltip />
            <el-table-column prop="reportType" :label="$t('dashboard.reportType')" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="reportTypeTag(row.reportType)" size="small" effect="plain">{{ reportTypeLabel(row.reportType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" :label="$t('dashboard.reportDate')" width="160" align="center">
              <template #default="{ row }"><span style="font-size: 12px; color: #94a3b8">{{ formatDateTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column :label="$t('dashboard.reportActions')" width="80" align="center">
              <template #default="{ row }">
                <el-button type="danger" text size="small" @click.stop="handleDeleteReport(row.id)">{{ $t('dashboard.deleteReport') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="reportHistoryTotal > 10" class="pagination" style="margin-top: 12px; display: flex; justify-content: flex-end">
            <el-pagination small layout="prev, pager, next" :total="reportHistoryTotal" :page-size="10" v-model:current-page="reportHistoryPage" @current-change="loadReportHistory" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- Left: articles + announcements -->
      <el-col :xs="24" :lg="16">
        <el-card class="dashboard-card" style="margin-bottom: 16px">
          <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <el-tab-pane :label="$t('dashboard.recentArticles')" name="recent">
              <el-table :data="recentArticles" row-key="id" stripe :empty-text="$t('dashboard.noArticles')" size="small">
                <el-table-column prop="title" :label="$t('dashboard.title')" min-width="200" show-overflow-tooltip>
                  <template #default="{ row }">
                    <router-link :to="`/admin/articles/edit/${row.id}`" class="article-link">{{ row.title }}</router-link>
                  </template>
                </el-table-column>
                <el-table-column prop="status" :label="$t('dashboard.status')" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="viewCount" :label="$t('dashboard.views')" width="70" align="center">
                  <template #default="{ row }"><span class="view-count">{{ row.viewCount || 0 }}</span></template>
                </el-table-column>
                <el-table-column prop="likeCount" :label="$t('dashboard.likes')" width="70" align="center">
                  <template #default="{ row }"><span class="like-count">{{ row.likeCount || 0 }}</span></template>
                </el-table-column>
                <el-table-column prop="categoryName" :label="$t('dashboard.category')" width="100" show-overflow-tooltip>
                  <template #default="{ row }"><span style="color: #475569; font-size: 12px">{{ row.categoryName || '-' }}</span></template>
                </el-table-column>
                <el-table-column prop="createdAt" :label="$t('dashboard.createdAt')" width="110" align="center">
                  <template #default="{ row }"><span style="font-size: 12px; color: #94a3b8">{{ formatDate(row.createdAt) }}</span></template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane :label="$t('dashboard.recentDrafts')" name="drafts">
              <el-table :data="draftArticles" row-key="id" stripe :empty-text="$t('dashboard.noDrafts')" size="small">
                <el-table-column prop="title" :label="$t('dashboard.title')" min-width="200" show-overflow-tooltip>
                  <template #default="{ row }">
                    <router-link :to="`/admin/articles/edit/${row.id}`" class="article-link">{{ row.title }}</router-link>
                  </template>
                </el-table-column>
                <el-table-column prop="categoryName" :label="$t('dashboard.category')" width="100" show-overflow-tooltip>
                  <template #default="{ row }"><span style="color: #475569; font-size: 12px">{{ row.categoryName || '-' }}</span></template>
                </el-table-column>
                <el-table-column prop="createdAt" :label="$t('dashboard.createdAt')" width="110" align="center">
                  <template #default="{ row }"><span style="font-size: 12px; color: #94a3b8">{{ formatDate(row.createdAt) }}</span></template>
                </el-table-column>
                <el-table-column :label="$t('dashboard.action')" width="80" align="center">
                  <template #default="{ row }">
                    <router-link :to="`/admin/articles/edit/${row.id}`">
                      <el-button type="primary" size="small" text>{{ $t('dashboard.continueEdit') }}</el-button>
                    </router-link>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <el-card class="dashboard-card">
          <template #header>
            <div class="card-header">
              <span>{{ $t('dashboard.recentAnnouncements') }}</span>
              <router-link to="/admin/announcements" class="view-all-link">{{ $t('dashboard.manageAnnouncements') }} →</router-link>
            </div>
          </template>
          <div v-if="recentAnnouncements.length" class="announcement-list">
            <div v-for="item in recentAnnouncements" :key="item.id" class="announcement-item">
              <el-tag :type="announcementType(item.type)" size="small" effect="plain">{{ item.type || 'info' }}</el-tag>
              <span class="announcement-title">{{ item.title }}</span>
              <span class="announcement-time">{{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
          <el-empty v-else :description="$t('dashboard.noAnnouncements')" :image-size="60" />
        </el-card>
      </el-col>

      <!-- Right sidebar -->
      <el-col :xs="24" :lg="8">
        <el-card class="dashboard-card" style="margin-bottom: 16px">
          <template #header><span class="card-header">{{ $t('dashboard.quickActions') }}</span></template>
          <div class="quick-actions">
            <router-link to="/admin/articles/create" class="action-btn primary">
              <el-icon :size="20"><EditPen /></el-icon><span>{{ $t('dashboard.writeArticle') }}</span>
            </router-link>
            <a href="/" target="_blank" class="action-btn">
              <el-icon :size="20"><Monitor /></el-icon><span>{{ $t('dashboard.sitePreview') }}</span>
            </a>
            <router-link to="/admin/categories" class="action-btn">
              <el-icon :size="20"><Menu /></el-icon><span>{{ $t('dashboard.categories') }}</span>
            </router-link>
            <router-link to="/admin/tags" class="action-btn">
              <el-icon :size="20"><PriceTag /></el-icon><span>{{ $t('dashboard.tags') }}</span>
            </router-link>
            <router-link to="/admin/projects" class="action-btn">
              <el-icon :size="20"><FolderOpened /></el-icon><span>{{ $t('dashboard.projects') }}</span>
            </router-link>
            <router-link to="/admin/announcements" class="action-btn">
              <el-icon :size="20"><Bell /></el-icon><span>{{ $t('dashboard.announcements') }}</span>
            </router-link>
            <router-link v-if="isSuperAdmin" to="/admin/ai-settings" class="action-btn">
              <el-icon :size="20"><MagicStick /></el-icon><span>{{ $t('dashboard.aiSettings') }}</span>
            </router-link>
            <router-link to="/admin/settings" class="action-btn">
              <el-icon :size="20"><Setting /></el-icon><span>{{ $t('dashboard.settings') }}</span>
            </router-link>
          </div>
        </el-card>

        <el-card class="dashboard-card">
          <template #header><span class="card-header">{{ $t('dashboard.hotArticles') }}</span></template>
          <div v-if="hotArticles.length" class="hot-list">
            <div v-for="(article, i) in hotArticles" :key="article.id" class="hot-item">
              <span class="hot-rank" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
              <div class="hot-info">
                <router-link :to="`/admin/articles/edit/${article.id}`" class="hot-title">{{ article.title }}</router-link>
                <div class="hot-bar-wrap">
                  <div class="hot-bar" :style="{ width: getHotBarWidth(article.viewCount) + '%' }"></div>
                </div>
                <span class="hot-views">{{ $t('dashboard.viewsCount', { count: article.viewCount }) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else :description="$t('common.noData')" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, shallowRef, nextTick, watch, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components'
import { EditPen, Menu, PriceTag, FolderOpened, Bell, Setting, Monitor, MagicStick, Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { aiChatStream } from '../../utils/ai'
const MdPreview = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdPreview))
import 'md-editor-v3/lib/preview.css'
import('../../utils/mdEditorConfig')
import RefreshButton from '../../components/RefreshButton.vue'
import { injectCopyButtons } from '../../utils/copyUtils'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { getDashboardStats, getRecentArticles, getRecentDrafts, getHotArticles, getAnnouncements, getAllArticlesForCharts, saveReport, getReports, deleteReport } from '../../api/dashboard'

echarts.use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent])

const router = useRouter()
const { t, locale } = useI18n()
const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.isSuperAdmin)

// State
const loading = ref(true)
const lastUpdate = ref('')
const stats = shallowRef({
  articleCount: 0, publishedCount: 0, draftCount: 0,
  categoryCount: 0, tagCount: 0, projectCount: 0,
  totalViews: 0, totalLikes: 0,
  categories: [], tags: []
})
const allArticlesData = shallowRef([])
const recentArticles = ref([])
const draftArticles = ref([])
const hotArticles = ref([])
const recentAnnouncements = ref([])
const activeTab = ref('recent')

// Chart type toggles
const publishChartType = ref('bar')
const viewsChartType = ref('line')

// AI report
const reportPeriod = ref('daily')
const aiReportContent = ref('')
const aiReportLoading = ref(false)
const aiReportSection = ref(null)

watch(aiReportContent, () => {
  nextTick(() => { if (aiReportSection.value) injectCopyButtons(aiReportSection.value) })
})
let aiAbortFn = null
const reportSaved = ref(false)

// Report history
const reportHistory = ref([])
const reportHistoryLoading = ref(false)
const reportSearchKeyword = ref('')
const reportFilterType = ref('')
const reportHistoryPage = ref(1)
const reportHistoryTotal = ref(0)
const expandedReportId = ref(null)

// Chart refs
const publishChartRef = ref(null)
const categoryChartRef = ref(null)
const viewsChartRef = ref(null)
const tagChartRef = ref(null)
const charts = shallowRef([])

// Chart instances (non-reactive)
let publishChart = null
let categoryChart = null
let viewsChart = null
let tagChart = null

const statCards = computed(() => [
  {
    label: t('dashboard.articleCount'), value: stats.value.articleCount, theme: 'blue', color: '#2563eb',
    wave: '#3b82f620', route: '/admin/articles',
    svg: '<path d="M6 2a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V7.414A2 2 0 0019.414 6L16 2.586A2 2 0 0014.586 2H6zm5 2v4a1 1 0 001 1h4M8 13h8M8 17h5" stroke="#2563eb" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
  },
  {
    label: t('dashboard.published'), value: stats.value.publishedCount, theme: 'green', color: '#059669',
    wave: '#10b98120', route: '/admin/articles',
    svg: '<path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" fill="#059669" stroke="none"/>'
  },
  {
    label: t('dashboard.drafts'), value: stats.value.draftCount, theme: 'amber', color: '#d97706',
    wave: '#f59e0b20', route: '/admin/articles',
    svg: '<path d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" stroke="#d97706" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
  },
  {
    label: t('dashboard.totalViews'), value: stats.value.totalViews, theme: 'cyan', color: '#0891b2',
    wave: '#06b6d420', route: '/admin/articles',
    svg: '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12z" stroke="#0891b2" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><circle cx="12" cy="12" r="3" stroke="#0891b2" stroke-width="1.8"/>'
  },
  {
    label: t('dashboard.totalLikes'), value: stats.value.totalLikes, theme: 'pink', color: '#db2777',
    wave: '#ec489920', route: '/admin/articles',
    svg: '<path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" fill="#db2777" stroke="none" opacity="0.2"/><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" stroke="#db2777" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
  },
  {
    label: t('dashboard.projectCount'), value: stats.value.projectCount, theme: 'purple', color: '#2563eb',
    wave: '#3b82f620', route: '/admin/projects',
    svg: '<path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z" fill="#2563eb" stroke="none" opacity="0.2"/><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z" stroke="#2563eb" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
  }
])

// Chart type watchers
watch(publishChartType, () => {
  if (publishChart) { publishChart.dispose(); publishChart = null }
  nextTick(() => { renderPublishChart(); charts.value = [publishChart, categoryChart, viewsChart, tagChart].filter(Boolean) })
})
watch(viewsChartType, () => {
  if (viewsChart) { viewsChart.dispose(); viewsChart = null }
  nextTick(() => { renderViewsChart(); charts.value = [publishChart, categoryChart, viewsChart, tagChart].filter(Boolean) })
})

// Helpers
function statusType(s) { return s === 1 ? 'success' : s === 2 ? 'warning' : 'info' }
function statusLabel(s) { return s === 1 ? t('dashboard.publishedLabel') : s === 2 ? t('dashboard.archivedLabel') : t('dashboard.draftLabel') }
function announcementType(t) { return t === 'feature' ? 'success' : t === 'update' ? 'warning' : 'info' }

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function getLast6Months() {
  const labels = []
  const now = new Date()
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    labels.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return labels
}

function groupByMonth(articles, field) {
  const months = getLast6Months()
  const counts = {}
  months.forEach(m => counts[m] = 0)
  articles.forEach(a => {
    if (!a.createdAt) return
    const d = new Date(a.createdAt)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    if (key in counts) counts[key] += (field ? (a[field] || 0) : 1)
  })
  return months.map(m => counts[m])
}

function groupByCategory(articles) {
  const map = {}
  articles.forEach(a => {
    const cat = a.categoryName || t('dashboard.uncategorized')
    map[cat] = (map[cat] || 0) + 1
  })
  return Object.entries(map).map(([name, value]) => ({ name, value }))
}

function groupByTag(articles) {
  const map = {}
  articles.forEach(a => {
    const tags = a.tags || []
    tags.forEach(tag => { map[tag.name] = (map[tag.name] || 0) + 1 })
  })
  return Object.entries(map)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
    .map(([name, value]) => ({ name, value }))
}

const maxViewCount = computed(() => {
  if (!hotArticles.value.length) return 1
  return Math.max(...hotArticles.value.map(a => a.viewCount || 0), 1)
})

function getHotBarWidth(count) {
  return Math.max((count / maxViewCount.value) * 100, 8)
}

// Chart rendering
function getMonthLabels() {
  return getLast6Months().map(m => {
    const [, mon] = m.split('-')
    return t('dashboard.monthLabel', { n: parseInt(mon) })
  })
}

function renderPublishChart() {
  if (!publishChartRef.value) return
  if (!publishChart) publishChart = echarts.init(publishChartRef.value)
  const months = getMonthLabels()
  const data = groupByMonth(allArticlesData.value)
  publishChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    dataZoom: [{ type: 'inside', start: 0, end: 100 }],
    xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' } },
    series: [{
      type: publishChartType.value,
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      barMaxWidth: 24,
      barBorderRadius: [4, 4, 0, 0],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#3b82f6' }, { offset: 1, color: '#93c5fd' }
        ])
      },
      lineStyle: { width: 3, color: '#3b82f6' },
      areaStyle: publishChartType.value === 'line' ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59,130,246,0.25)' }, { offset: 1, color: 'rgba(59,130,246,0)' }
        ])
      } : undefined,
    }]
  })
}

function renderCategoryChart() {
  if (!categoryChartRef.value) return
  if (!categoryChart) categoryChart = echarts.init(categoryChartRef.value)
  const catData = groupByCategory(allArticlesData.value)
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#ec4899', '#06b6d4', '#84cc16']
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: p => `${p.name}: ${p.value} (${p.percent}%)` },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#64748b', fontSize: 12 } },
    color: colors,
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'],
      roseType: 'area',
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: catData
    }]
  })
}

function renderViewsChart() {
  if (!viewsChartRef.value) return
  if (!viewsChart) viewsChart = echarts.init(viewsChartRef.value)
  const months = getMonthLabels()
  const viewsData = groupByMonth(allArticlesData.value, 'viewCount')
  const likesData = groupByMonth(allArticlesData.value, 'likeCount')
  viewsChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [t('dashboard.views'), t('dashboard.likes')], top: 0, textStyle: { color: '#64748b' } },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    dataZoom: [{ type: 'inside', start: 0, end: 100 }],
    xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' } },
    series: [
      {
        name: t('dashboard.views'), type: viewsChartType.value, data: viewsData, smooth: true, symbol: 'circle', symbolSize: 6,
        barMaxWidth: 24, barBorderRadius: [4, 4, 0, 0],
        lineStyle: { color: '#3b82f6', width: 3 },
        itemStyle: { color: '#3b82f6' },
        areaStyle: viewsChartType.value === 'line' ? {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59,130,246,0.25)' }, { offset: 1, color: 'rgba(59,130,246,0)' }
          ])
        } : undefined,
      },
      {
        name: t('dashboard.likes'), type: viewsChartType.value, data: likesData, smooth: true, symbol: 'circle', symbolSize: 6,
        barMaxWidth: 24, barBorderRadius: [4, 4, 0, 0],
        lineStyle: { color: '#ec4899', width: 3 },
        itemStyle: { color: '#ec4899' },
        areaStyle: viewsChartType.value === 'line' ? {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(236,72,153,0.2)' }, { offset: 1, color: 'rgba(236,72,153,0)' }
          ])
        } : undefined,
      }
    ]
  })
}

function renderTagChart() {
  if (!tagChartRef.value) return
  if (!tagChart) tagChart = echarts.init(tagChartRef.value)
  const tagData = groupByTag(allArticlesData.value)
  const colors = ['#3b82f6', '#3b82f6', '#60a5fa', '#bfdbfe', '#e879f9', '#f472b6', '#fb7185', '#fbbf24']
  tagChart.setOption({
    tooltip: { trigger: 'item', formatter: p => `${p.name}: ${p.value} (${p.percent}%)` },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#64748b', fontSize: 12 } },
    color: colors,
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: tagData
    }]
  })
}

function handleResize() {
  charts.value.forEach(c => c.resize())
}

function observeAndInit(el, initFn) {
  if (!el) return
  const observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) {
      observer.disconnect()
      initFn()
    }
  }, { rootMargin: '200px' })
  observer.observe(el)
}

// AI Report
function buildReportContext() {
  const months = getLast6Months().map(m => {
    const [, mon] = m.split('-')
    return t('dashboard.monthLabel', { n: parseInt(mon) })
  })
  const publishTrend = groupByMonth(allArticlesData.value).map((v, i) => `${months[i]}:${v}`).join(', ')
  const catDist = groupByCategory(allArticlesData.value).map(c => `${c.name}:${c.value}`).join(', ')
  const top5 = hotArticles.value.slice(0, 5).map(a => `${a.title}(${a.viewCount})`).join('; ')
  const periodMap = { daily: t('dashboard.reportPeriodDaily'), weekly: t('dashboard.reportPeriodWeekly'), monthly: t('dashboard.reportPeriodMonthly') }
  const period = periodMap[reportPeriod.value] || t('dashboard.reportPeriodDaily')

  return `${t('dashboard.reportContextTitle', { period })}

- ${t('dashboard.articleCount')}：${stats.value.articleCount}（${t('dashboard.published')}：${stats.value.publishedCount}，${t('dashboard.drafts')}：${stats.value.draftCount}）
- ${t('dashboard.totalViews')}：${stats.value.totalViews}，${t('dashboard.totalLikes')}：${stats.value.totalLikes}
- ${t('dashboard.categoryDistribution')}：${stats.value.categoryCount}，${t('dashboard.tagDistribution')}：${stats.value.tagCount}，${t('dashboard.projectCount')}：${stats.value.projectCount}
- ${t('dashboard.publishTrend')}：${publishTrend}
- ${t('dashboard.categoryDistribution')}：${catDist}
- ${t('dashboard.hotArticles')} Top5：${top5 || t('common.noData')}
`
}

function generateReport() {
  if (aiReportLoading.value) return
  if (aiAbortFn) { aiAbortFn(); aiAbortFn = null }
  aiReportContent.value = ''
  aiReportLoading.value = true
  reportSaved.value = false

  const msg = buildReportContext()
  aiAbortFn = aiChatStream(msg, {
    onChunk(content) { aiReportContent.value += content },
    onDone() { aiReportLoading.value = false; aiAbortFn = null },
    onError(e) {
      aiReportContent.value = t('dashboard.reportFailed', { error: e.message || t('home.aiServiceUnavailable') })
      aiReportLoading.value = false
      aiAbortFn = null
    }
  })
}

function generateReportTitle() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const dateStr = `${y}-${m}-${d}`
  if (reportPeriod.value === 'daily') return `${dateStr} ${t('dashboard.dailyReport')}`
  if (reportPeriod.value === 'monthly') return `${y}年${m}月 ${t('dashboard.monthlyReport')}`
  // weekly: show range
  const weekAgo = new Date(now.getTime() - 7 * 86400000)
  const wM = String(weekAgo.getMonth() + 1).padStart(2, '0')
  const wD = String(weekAgo.getDate()).padStart(2, '0')
  return `${weekAgo.getFullYear()}-${wM}-${wD} ~ ${dateStr} ${t('dashboard.weeklyReport')}`
}

async function handleSaveReport() {
  if (!aiReportContent.value) return
  try {
    await saveReport({
      reportType: reportPeriod.value,
      title: generateReportTitle(),
      content: aiReportContent.value,
      dataSnapshot: JSON.stringify(stats.value),
      reportDate: new Date().toISOString().slice(0, 10)
    })
    reportSaved.value = true
    ElMessage.success(t('dashboard.reportSaved'))
    loadReportHistory()
  } catch (e) {
    ElMessage.error(t('dashboard.reportSaveFailed'))
  }
}

async function loadReportHistory() {
  reportHistoryLoading.value = true
  try {
    const params = { page: reportHistoryPage.value - 1, size: 10 }
    if (reportFilterType.value) params.type = reportFilterType.value
    if (reportSearchKeyword.value) params.keyword = reportSearchKeyword.value
    const data = await getReports(params)
    reportHistory.value = data.content || []
    reportHistoryTotal.value = data.totalElements || 0
  } catch (e) {
    console.error('Failed to load report history:', e)
  } finally {
    reportHistoryLoading.value = false
  }
}

async function handleDeleteReport(id) {
  try {
    await ElMessageBox.confirm(t('dashboard.reportDeleteConfirm'), t('common.confirm'), { type: 'warning' })
  } catch { return }
  try {
    await deleteReport(id)
    ElMessage.success(t('common.deleteSuccess'))
    loadReportHistory()
  } catch (e) {
    ElMessage.error(t('common.deleteFailed'))
  }
}

function toggleReportExpand(row) {
  expandedReportId.value = expandedReportId.value === row.id ? null : row.id
}

function reportTypeTag(type) {
  if (type === 'daily') return 'primary'
  if (type === 'weekly') return 'success'
  if (type === 'monthly') return 'warning'
  return 'info'
}

function reportTypeLabel(type) {
  if (type === 'daily') return t('dashboard.dailyReport')
  if (type === 'weekly') return t('dashboard.weeklyReport')
  if (type === 'monthly') return t('dashboard.monthlyReport')
  return type
}

function formatDateTime(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// Data loading
async function onTabChange(tab) {
  if (tab === 'drafts' && draftArticles.value.length === 0) {
    try {
      const data = await getRecentDrafts(5)
      draftArticles.value = data.content || []
    } catch (e) {
      console.error('Failed to load drafts:', e)
    }
  }
}

async function refreshAll() {
  try {
    const [statsData, recent, hot, announcements, allArticles] = await Promise.all([
      getDashboardStats(),
      getRecentArticles(5),
      getHotArticles(5),
      getAnnouncements(),
      getAllArticlesForCharts(100),
    ])

    const articles = allArticles.content || []
    const totalViews = articles.reduce((sum, a) => sum + (a.viewCount || 0), 0)
    const totalLikes = articles.reduce((sum, a) => sum + (a.likeCount || 0), 0)
    stats.value = { ...stats.value, ...statsData, totalViews, totalLikes }
    recentArticles.value = recent.content || []
    hotArticles.value = (hot.content || []).sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0))
    recentAnnouncements.value = (announcements || []).filter(a => a.active).slice(0, 3)

    allArticlesData.value = articles

    await nextTick()
    // Render above-fold charts immediately
    renderPublishChart()
    renderCategoryChart()
    // Defer below-fold charts until visible
    observeAndInit(viewsChartRef.value, () => { renderViewsChart(); charts.value = [publishChart, categoryChart, viewsChart, tagChart].filter(Boolean) })
    observeAndInit(tagChartRef.value, () => { renderTagChart(); charts.value = [publishChart, categoryChart, viewsChart, tagChart].filter(Boolean) })
    charts.value = [publishChart, categoryChart].filter(Boolean)

    lastUpdate.value = new Date().toLocaleString(locale.value === 'zh-CN' ? 'zh-CN' : 'en-US')
  } catch (e) {
    console.error('Failed to load dashboard:', e)
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await Promise.allSettled([refreshAll(), loadReportHistory()])
  loading.value = false
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (aiAbortFn) aiAbortFn()
  publishChart?.dispose()
  categoryChart?.dispose()
  viewsChart?.dispose()
  tagChart?.dispose()
})
</script>

<style scoped>
/* Header */
.dashboard-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.update-time {
  font-size: 12px;
  color: #94a3b8;
}

/* Charts */
.chart-row { margin-bottom: 0; }
.chart-container { width: 100%; height: 320px; }

/* Stat cards */
.stat-row { margin-bottom: 20px; }
.stat-card {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  padding: 20px 16px;
  margin-bottom: 16px;
  text-align: center;
  animation: cardIn 0.4s ease-out backwards;
  transition: box-shadow 0.3s, transform 0.3s;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.stat-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}
/* Theme tints */
.stat-card--blue { background: linear-gradient(135deg, rgba(255,255,255,0.5), rgba(239,246,255,0.6)); border-color: rgba(191,219,254,0.5); }
.stat-card--blue:hover { box-shadow: 0 8px 32px rgba(37, 99, 235, 0.15); }
.stat-card--green { background: linear-gradient(135deg, rgba(255,255,255,0.5), rgba(236,253,245,0.6)); border-color: rgba(167,243,208,0.5); }
.stat-card--green:hover { box-shadow: 0 8px 32px rgba(5, 150, 105, 0.15); }
.stat-card--amber { background: linear-gradient(135deg, rgba(255,255,255,0.5), rgba(255,251,235,0.6)); border-color: rgba(253,230,138,0.5); }
.stat-card--amber:hover { box-shadow: 0 8px 32px rgba(217, 119, 6, 0.15); }
.stat-card--cyan { background: linear-gradient(135deg, rgba(255,255,255,0.5), rgba(236,254,255,0.6)); border-color: rgba(165,243,252,0.5); }
.stat-card--cyan:hover { box-shadow: 0 8px 32px rgba(8, 145, 178, 0.15); }
.stat-card--pink { background: linear-gradient(135deg, rgba(255,255,255,0.5), rgba(253,242,248,0.6)); border-color: rgba(251,207,232,0.5); }
.stat-card--pink:hover { box-shadow: 0 8px 32px rgba(219, 39, 119, 0.15); }
.stat-card--purple { background: linear-gradient(135deg, rgba(255,255,255,0.5), rgba(245,243,255,0.6)); border-color: rgba(196,181,253,0.5); }
.stat-card--purple:hover { box-shadow: 0 8px 32px rgba(124, 58, 237, 0.15); }
/* Decorative wave */
.stat-wave {
  position: absolute;
  transform: rotate(90deg);
  left: -31px;
  bottom: -8px;
  width: 80px;
  pointer-events: none;
  opacity: 0.5;
}
/* Icon wrap */
.stat-icon-wrap {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  position: relative;
}
.stat-card--blue .stat-icon-wrap { background: rgba(37, 99, 235, 0.08); }
.stat-card--green .stat-icon-wrap { background: rgba(5, 150, 105, 0.08); }
.stat-card--amber .stat-icon-wrap { background: rgba(217, 119, 6, 0.08); }
.stat-card--cyan .stat-icon-wrap { background: rgba(8, 145, 178, 0.08); }
.stat-card--pink .stat-icon-wrap { background: rgba(219, 39, 119, 0.08); }
.stat-card--purple .stat-icon-wrap { background: rgba(124, 58, 237, 0.08); }
.stat-svg-icon {
  width: 26px;
  height: 26px;
}
.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}
.stat-label {
  color: #94a3b8;
  font-size: 0.8rem;
  margin-top: 4px;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Card header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  font-size: 15px;
  color: #1e293b;
}
.view-all-link {
  font-size: 13px;
  font-weight: 500;
  color: #3b82f6;
  text-decoration: none;
  transition: color 0.15s;
}
.view-all-link:hover { color: #2563eb; }

/* Article link */
.article-link {
  color: #1e293b;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: color 0.15s;
}
.article-link:hover { color: #3b82f6; }
.view-count { font-size: 12px; color: #0ea5e9; font-weight: 600; }
.like-count { font-size: 12px; color: #ec4899; font-weight: 600; }

/* Announcements */
.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.announcement-item { display: flex; align-items: center; gap: 10px; }
.announcement-title { flex: 1; font-size: 13px; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.announcement-time { font-size: 12px; color: #94a3b8; flex-shrink: 0; }

/* Quick actions */
.quick-actions { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.action-btn {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 14px 8px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  color: #64748b; text-decoration: none; font-size: 12px; font-weight: 600;
  transition: border-color 0.25s, color 0.25s, background 0.25s;
}
.action-btn:hover {
  border-color: rgba(59, 130, 246, 0.3);
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.05);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}
.action-btn.primary {
  border-color: rgba(59, 130, 246, 0.3);
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.06);
}
.action-btn.primary:hover {
  background: rgba(59, 130, 246, 0.1);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.15);
}

/* Hot articles */
.hot-list { display: flex; flex-direction: column; gap: 14px; }
.hot-item { display: flex; align-items: flex-start; gap: 10px; }
.hot-rank {
  width: 24px; height: 24px; border-radius: 8px;
  background: rgba(0, 0, 0, 0.04); color: #94a3b8; font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 1px;
}
.hot-rank.top3 { background: linear-gradient(135deg, #3b82f6, #60a5fa); color: #fff; box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3); }
.hot-info { flex: 1; min-width: 0; }
.hot-title {
  display: block; font-size: 13px; color: #1e293b; text-decoration: none;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; transition: color 0.15s; font-weight: 500;
}
.hot-title:hover { color: #3b82f6; }
.hot-bar-wrap {
  height: 4px; background: rgba(0, 0, 0, 0.04); border-radius: 4px; margin: 6px 0; overflow: hidden;
}
.hot-bar {
  height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, #3b82f6, #93c5fd);
  transition: width 0.6s ease;
}
.hot-views { font-size: 11px; color: #94a3b8; font-weight: 500; }

/* AI Report */
.ai-report-title { display: flex; align-items: center; }
.ai-report-actions { display: flex; align-items: center; }
.ai-report-content { max-height: 400px; overflow-y: auto; }
.ai-report-content :deep(.md-editor) { background: transparent; border: none; }
.ai-report-content :deep(.md-editor-preview) { font-size: 0.88rem; line-height: 1.7; }
.ai-report-save-bar { margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(0, 0, 0, 0.04); display: flex; align-items: center; }
.ai-report-saved-hint { font-size: 13px; color: #22c55e; font-weight: 600; }
.report-expand-content { padding: 12px 20px; background: rgba(0, 0, 0, 0.02); border-radius: 12px; }
.report-expand-content :deep(.md-editor) { background: transparent; border: none; }
.report-expand-content :deep(.md-editor-preview) { font-size: 0.85rem; line-height: 1.7; }
.ai-report-loading {
  display: flex; align-items: center; justify-content: center; gap: 10px;
  padding: 2rem; color: #3b82f6; font-size: 0.9rem; font-weight: 600;
}

/* Dashboard card */
.dashboard-card { border-radius: 20px; }
:deep(.dashboard-card .el-card__header) { padding: 14px 20px; }
:deep(.dashboard-card .el-card__body) { padding: 16px 20px; }
:deep(.dashboard-card .el-tabs__header) { margin-bottom: 12px; }
:deep(.dashboard-card .el-tabs__item) { font-weight: 600; }
:deep(.dashboard-card .el-tabs__item.is-active) { color: #3b82f6; }
:deep(.dashboard-card .el-tabs__active-bar) { background: #3b82f6; }
</style>
