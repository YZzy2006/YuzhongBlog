<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminBackup.title') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="loadBackups" />
        <el-button v-if="selectedIds.length > 0" type="danger" @click="bulkDelete">
          {{ $t('adminBackup.batchDelete') }} ({{ selectedIds.length }})
        </el-button>
        <el-button type="primary" @click="createBackup">
          <el-icon><Plus /></el-icon> {{ $t('adminBackup.createBackup') }}
        </el-button>
      </div>
    </div>

    <!-- Import section -->
    <el-card style="margin-bottom: 16px">
      <template #header><span>{{ $t('adminBackup.importSection') }}</span></template>
      <div style="display: flex; gap: 12px; align-items: center">
        <input type="file" accept=".json" @change="onFileChange" ref="fileInput" />
        <el-button type="warning" :disabled="!selectedFile" :loading="importing" @click="handleImport">
          {{ $t('adminBackup.importBtn') }}
        </el-button>
      </div>
      <div v-if="importSummary" style="margin-top: 12px; padding: 12px; background: var(--el-fill-color-light); border-radius: 8px">
        <h4 style="margin: 0 0 8px">{{ $t('adminBackup.importSummaryTitle') }}</h4>
        <p style="margin: 0 0 8px; font-weight: 600">{{ $t('adminBackup.totalRecords', { count: importSummary.totalRecords }) }}</p>
        <div style="display: flex; flex-wrap: wrap; gap: 8px">
          <el-tag v-for="(count, key) in importSummary.counts" :key="key" size="small">
            {{ key }}: {{ count }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- Backup list -->
    <el-card>
      <el-table :data="backups" row-key="id" stripe v-loading="loading" @selection-change="onSelectionChange" :empty-text="$t('adminBackup.emptyText')">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="filename" :label="$t('adminBackup.filename')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="$t('adminBackup.fileSize')" width="120">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="recordCount" :label="$t('adminBackup.recordCount')" width="100" />
        <el-table-column prop="description" :label="$t('adminBackup.description')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdBy" :label="$t('adminBackup.createdBy')" width="120" />
        <el-table-column :label="$t('adminBackup.createdAt')" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="$t('adminBackup.actions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="downloadBackup(row)">
              {{ $t('adminBackup.download') }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteBackup(row)">
              {{ $t('adminBackup.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="totalPages > 1" class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="totalElements"
          :page-size="pageSize"
          :current-page="page + 1"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'

const { t } = useI18n()

const backups = ref([])
const loading = ref(false)
const importing = ref(false)
const page = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const selectedIds = ref([])
const selectedFile = ref(null)
const importSummary = ref(null)
const fileInput = ref(null)

onMounted(() => loadBackups())

async function loadBackups() {
  loading.value = true
  try {
    const res = await request.get('/admin/backups', {
      params: { page: page.value, size: pageSize.value }
    })
    backups.value = res.content
    totalElements.value = res.totalElements
    totalPages.value = res.totalPages
  } catch (e) {
    ElMessage.error(e.message || t('adminBackup.createFailed'))
  } finally {
    loading.value = false
  }
}

async function createBackup() {
  try {
    const { value } = await ElMessageBox.prompt(
      t('adminBackup.descriptionPrompt'),
      t('adminBackup.createBackup'),
      { confirmButtonText: 'OK', cancelButtonText: 'Cancel', inputType: 'textarea' }
    )
    await request.post('/admin/backups', null, { params: { description: value || '' } })
    ElMessage.success(t('adminBackup.createSuccess'))
    loadBackups()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminBackup.createFailed'))
  }
}

async function downloadBackup(row) {
  try {
    const token = localStorage.getItem('accessToken')
    const response = await fetch(`/admin/backups/${row.id}/download`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!response.ok) throw new Error(t('adminBackup.downloadFailed'))
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = row.filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(downloadUrl)
  } catch (e) {
    ElMessage.error(e.message || t('adminBackup.downloadFailed'))
  }
}

async function deleteBackup(row) {
  try {
    await ElMessageBox.confirm(
      t('adminBackup.deleteConfirmMsg', { name: row.filename }),
      t('adminBackup.deleteConfirmTitle'),
      { type: 'warning' }
    )
    await request.delete('/admin/backups', { params: { ids: [row.id] } })
    ElMessage.success(t('adminBackup.deleteSuccess'))
    loadBackups()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminBackup.deleteFailed'))
  }
}

async function bulkDelete() {
  try {
    await ElMessageBox.confirm(
      t('adminBackup.batchDeleteConfirmMsg', { count: selectedIds.value.length }),
      t('adminBackup.deleteConfirmTitle'),
      { type: 'warning' }
    )
    await request.delete('/admin/backups', { params: { ids: selectedIds.value } })
    ElMessage.success(t('adminBackup.batchDeleteSuccess'))
    selectedIds.value = []
    loadBackups()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminBackup.deleteFailed'))
  }
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

function onFileChange(e) {
  selectedFile.value = e.target.files[0] || null
  importSummary.value = null
}

async function handleImport() {
  if (!selectedFile.value) {
    ElMessage.warning(t('adminBackup.noFileSelected'))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('adminBackup.importConfirmMsg'),
      t('adminBackup.importConfirmTitle'),
      { type: 'warning', confirmButtonText: 'OK', cancelButtonText: 'Cancel' }
    )
  } catch {
    return
  }
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const result = await request.post('/admin/backups/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    importSummary.value = result
    ElMessage.success(t('adminBackup.importSuccess'))
    loadBackups()
  } catch (e) {
    ElMessage.error(e.message || t('adminBackup.importFailed'))
  } finally {
    importing.value = false
  }
}

function onPageChange(p) {
  page.value = p - 1
  loadBackups()
}

function onSizeChange(s) {
  pageSize.value = s
  page.value = 0
  loadBackups()
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(i > 0 ? 1 : 0)} ${units[i]}`
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString()
}
</script>
