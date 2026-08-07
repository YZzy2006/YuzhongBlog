<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminReviews.title') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="load" />
      </div>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminReviews.reviewList') }}</span>
          <el-select v-model="statusFilter" clearable :placeholder="$t('adminReviews.allStatus')" style="width: 130px" @change="onFilterChange">
            <el-option :label="$t('adminReviews.pending')" value="PENDING" />
            <el-option :label="$t('adminReviews.approved')" value="APPROVED" />
            <el-option :label="$t('adminReviews.rejected')" value="REJECTED" />
          </el-select>
        </div>
      </template>

      <el-table :data="reviews" row-key="id" stripe v-loading="loading" :empty-text="$t('adminReviews.emptyText')">
        <el-table-column prop="contentType" :label="$t('adminReviews.type')" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ contentTypeLabel(row.contentType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contentTitle" :label="$t('adminReviews.titleCol')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="submittedByName" :label="$t('adminReviews.submittedBy')" width="100" />
        <el-table-column prop="aiAnalysis" :label="$t('adminReviews.aiAnalysis')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reviewStatus" :label="$t('adminReviews.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.reviewStatus)" size="small" effect="plain">
              {{ statusLabel(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" :label="$t('adminReviews.reviewComment')" width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" :label="$t('adminReviews.submittedAt')" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="$t('adminReviews.action')" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.reviewStatus === 'PENDING'">
              <el-button type="success" size="small" @click="handleApprove(row)">{{ $t('adminReviews.approve') }}</el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">{{ $t('adminReviews.reject') }}</el-button>
            </template>
            <span v-else style="color: #999; font-size: 12px">
              {{ row.reviewedAt ? formatDate(row.reviewedAt) : '' }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="totalPages > 1">
        <el-pagination background layout="prev, pager, next"
          :total="totalElements" :current-page="page + 1"
          @current-change="handlePageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'

const { t } = useI18n()

const reviews = ref([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const statusFilter = ref('PENDING')

function onFilterChange() { page.value = 0; load() }

function handlePageChange(p) {
  page.value = p - 1
  load()
}

async function load() {
  loading.value = true
  try {
    let url = `/admin/reviews?page=${page.value}&size=20`
    if (statusFilter.value) url += `&status=${statusFilter.value}`
    const data = await request.get(url)
    reviews.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } finally {
    loading.value = false
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(t('adminReviews.approveConfirm', { title: row.contentTitle }), t('adminReviews.approveConfirmTitle'), { type: 'success' })
    await request.post(`/admin/reviews/${row.id}/approve`)
    ElMessage.success(t('adminReviews.approvedMsg'))
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminReviews.actionFailed'))
  }
}

async function handleReject(row) {
  try {
    const { value: comment } = await ElMessageBox.prompt(t('adminReviews.rejectReason'), t('adminReviews.rejectTitle'), {
      type: 'warning',
      inputPlaceholder: t('adminReviews.rejectReasonPlaceholder'),
      inputValue: ''
    })
    await request.post(`/admin/reviews/${row.id}/reject`, { comment })
    ElMessage.success(t('adminReviews.rejectedMsg'))
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminReviews.actionFailed'))
  }
}

function contentTypeLabel(ct) {
  return { ARTICLE: t('adminReviews.contentTypeArticle'), PROJECT: t('adminReviews.contentTypeProject'), ANNOUNCEMENT: t('adminReviews.contentTypeAnnouncement') }[ct] || ct
}

function statusLabel(s) {
  return { PENDING: t('adminReviews.pending'), APPROVED: t('adminReviews.approved'), REJECTED: t('adminReviews.rejected') }[s] || s
}

function statusType(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
